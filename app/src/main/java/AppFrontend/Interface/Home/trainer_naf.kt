package com.example.frac_exp_20

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class trainer_naf(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val TAG = "FRACTAL_AI_ENGINE"

    private val BATCH_SIZE = 100
    private val IMG_SIZE = 28 * 28
    private val NUM_CLASSES = 10
    private val NUM_TRAININGS = 6000
    private val NUM_EPOCHS = 2

    // File references for server-side data
    private val SERVER_IMAGES_FILENAME = "train_images_server.bin"
    private val SERVER_LABELS_FILENAME = "train_labels_server.bin"

    interface TrainingCallback {
        fun onProgress(percentage: Int)
        fun onLog(message: String)
    }

    init {
        try {
            Log.d(TAG, "Initializing TFLite Interpreter...")
            val fileDescriptor = context.assets.openFd("model.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val modelBuffer = inputStream.channel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
            interpreter = Interpreter(modelBuffer)
            Log.i(TAG, "SUCCESS: Model loaded. Signatures found: ${interpreter?.signatureKeys}")
        } catch (e: Exception) {
            Log.e(TAG, "FATAL ERROR: Model Load Failure: ${e.message}")
        }
    }

    /**
     * MODIFIED: This function now looks for the server files in internal storage.
     * If they don't exist, it can fallback or log an error.
     */
    fun trainModel(callback: TrainingCallback?) {
        if (interpreter == null) {
            Log.e(TAG, "ABORT: Interpreter is null.")
            return
        }

        // Check if server files exist before starting
        val imgFile = File(context.filesDir, SERVER_IMAGES_FILENAME)
        val lblFile = File(context.filesDir, SERVER_LABELS_FILENAME)

        if (!imgFile.exists() || !lblFile.exists()) {
            Log.e(TAG, "ABORT: Server data files not found in internal storage. Run download first.")
            callback?.onLog("Error: Data files missing")
            return
        }

        val totalSteps = NUM_EPOCHS * (NUM_TRAININGS / BATCH_SIZE) * BATCH_SIZE
        var currentStep = 0

        Log.d(TAG, "Starting Training: $NUM_EPOCHS epochs, $totalSteps samples total using SERVER DATA.")

        try {
            for (epoch in 0 until NUM_EPOCHS) {
                Log.d(TAG, ">>> EPOCH ${epoch + 1} START")
                for (batchIdx in 0 until (NUM_TRAININGS / BATCH_SIZE)) {
                    // Load batch from FILE instead of ASSETS
                    val batchData = loadBatchFromFile(batchIdx, imgFile, lblFile) ?: continue
                    val (imageBatch, labelBatch) = batchData

                    for (sampleIdx in 0 until BATCH_SIZE) {
                        val imgBuffer = ByteBuffer.allocateDirect(IMG_SIZE * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
                        val lblBuffer = ByteBuffer.allocateDirect(NUM_CLASSES * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

                        imageBatch.position(sampleIdx * IMG_SIZE * 4)
                        labelBatch.position(sampleIdx * NUM_CLASSES * 4)

                        for (i in 0 until IMG_SIZE) imgBuffer.put(imageBatch.float)
                        for (i in 0 until NUM_CLASSES) lblBuffer.put(labelBatch.float)

                        imgBuffer.rewind(); lblBuffer.rewind()

                        val inputs: MutableMap<String, Any> = mutableMapOf()
                        inputs["x"] = imgBuffer
                        inputs["y"] = lblBuffer

                        val outputs: MutableMap<String, Any> = mutableMapOf()
                        val lossBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asFloatBuffer()
                        outputs["loss"] = lossBuffer

                        interpreter?.runSignature(inputs, outputs, "train")

                        currentStep++

                        if (currentStep % 100 == 0 || currentStep == totalSteps) {
                            val lossValue = lossBuffer.get(0)
                            val percent = (currentStep * 100) / totalSteps
                            Log.v(TAG, "Step $currentStep/$totalSteps | Loss: $lossValue | Progress: $percent%")
                            callback?.onProgress(percent)
                        }
                    }
                }
            }
            Log.i(TAG, "TRAINING LOOP COMPLETED SUCCESSFULLY")
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR during training loop: ${e.message}")
        }
    }

    // Inference and Weight Saving/Restoring remain the same logic
    fun inferModel(testImage: FloatArray): Int {
        Log.d(TAG, "Running Inference...")
        return try {
            val inputBuffer = ByteBuffer.allocateDirect(IMG_SIZE * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            inputBuffer.put(testImage).rewind()
            val inputs: MutableMap<String, Any> = mutableMapOf("x" to inputBuffer)
            val outputBuffer = ByteBuffer.allocateDirect(NUM_CLASSES * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            val outputs: MutableMap<String, Any> = mutableMapOf("output" to outputBuffer)
            interpreter?.runSignature(inputs, outputs, "infer")
            outputBuffer.rewind()
            val probs = FloatArray(NUM_CLASSES)
            outputBuffer.get(probs)
            val result = probs.indices.maxByOrNull { probs[it] } ?: -1
            Log.i(TAG, "Inference result: Class $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}")
            -1
        }
    }

    fun saveWeights(): Boolean {
        return try {
            val checkpointFile = File(context.filesDir, "checkpoint.ckpt")
            Log.d(TAG, "Saving weights to: ${checkpointFile.absolutePath}")
            val inputs: MutableMap<String, Any> = mutableMapOf("checkpoint_path" to checkpointFile.absolutePath)
            interpreter?.runSignature(inputs, mutableMapOf(), "save")
            Log.i(TAG, "WEIGHTS SAVED SUCCESSFULLY")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Save Weights failed: ${e.message}")
            false
        }
    }

    fun restoreWeights(): Boolean {
        val checkpointFile = File(context.filesDir, "checkpoint.ckpt")
        if (!checkpointFile.exists()) {
            Log.w(TAG, "No checkpoint found at ${checkpointFile.absolutePath}. Skipping restore.")
            return false
        }
        return try {
            Log.d(TAG, "Restoring weights from: ${checkpointFile.absolutePath}")
            val inputs: MutableMap<String, Any> = mutableMapOf("checkpoint_path" to checkpointFile.absolutePath)
            interpreter?.runSignature(inputs, mutableMapOf(), "restore")
            Log.i(TAG, "WEIGHTS RESTORED SUCCESSFULLY")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Restore Weights failed: ${e.message}")
            false
        }
    }

    /**
     * MODIFIED: Uses FileInputStream and FileChannel to read from internal storage.
     */
    private fun loadBatchFromFile(index: Int, imgFile: File, lblFile: File): Pair<ByteBuffer, ByteBuffer>? {
        return try {
            val imgIn = FileInputStream(imgFile)
            val lblIn = FileInputStream(lblFile)

            val imgOffset = index * BATCH_SIZE * IMG_SIZE * 4L
            val lblOffset = index * BATCH_SIZE * NUM_CLASSES * 4L

            // Using FileChannel to jump to the correct position efficiently
            imgIn.channel.position(imgOffset)
            lblIn.channel.position(lblOffset)

            val imgData = ByteArray(BATCH_SIZE * IMG_SIZE * 4)
            val lblData = ByteArray(BATCH_SIZE * NUM_CLASSES * 4)

            imgIn.read(imgData)
            lblIn.read(lblData)

            imgIn.close()
            lblIn.close()

            Pair(
                ByteBuffer.wrap(imgData).order(ByteOrder.nativeOrder()),
                ByteBuffer.wrap(lblData).order(ByteOrder.nativeOrder())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Batch Load Failed from FILE at index $index: ${e.message}")
            null
        }
    }
}