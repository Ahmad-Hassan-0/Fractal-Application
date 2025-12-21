package com.example.frac_exp_20

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class trainer_naf(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val TAG = "FRACTAL_AI_ENGINE"

    private val BATCH_SIZE = 100
    private val IMG_SIZE = 28 * 28
    private val NUM_CLASSES = 10
    private val NUM_TRAININGS = 6000
    private val NUM_EPOCHS = 2

    private val SERVER_IMAGES_FILENAME = "train_images_server.bin"
    private val SERVER_LABELS_FILENAME = "train_labels_server.bin"

    interface TrainingCallback {
        fun onProgress(percentage: Int)
        fun onLog(message: String)
    }

    init {
        initializeInterpreter()
    }

    private fun initializeInterpreter() {
        try {
            val serverModel = File(context.filesDir, "model_server.tflite")
            val modelBuffer: MappedByteBuffer

            if (serverModel.exists() && serverModel.length() > 0) {
                Log.d(TAG, "Loading MODEL from SERVER storage")
                val inputStream = FileInputStream(serverModel)
                modelBuffer = inputStream.channel.map(FileChannel.MapMode.READ_ONLY, 0, serverModel.length())
                inputStream.close()
            } else {
                Log.d(TAG, "Loading MODEL from ASSETS (Fallback)")
                // Note: Fixed filename to "model.tflite" to match your server setup
                val fileDescriptor = context.assets.openFd("model.tflite")
                val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
                modelBuffer = inputStream.channel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
                inputStream.close()
            }

            val options = Interpreter.Options()
            // Ensure compatibility with the signature runner
            interpreter = Interpreter(modelBuffer, options)
            Log.i(TAG, "Interpreter successfully initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}")
        }
    }

    fun trainModel(callback: TrainingCallback?) {
        if (interpreter == null) initializeInterpreter()

        if (interpreter == null) {
            Log.e(TAG, "ABORT: Interpreter is null.")
            return
        }

        val imgFile = File(context.filesDir, SERVER_IMAGES_FILENAME)
        val lblFile = File(context.filesDir, SERVER_LABELS_FILENAME)

        if (!imgFile.exists() || !lblFile.exists()) {
            Log.e(TAG, "ABORT: Data files missing.")
            callback?.onLog("Error: Data files missing")
            return
        }

        val totalSteps = NUM_EPOCHS * (NUM_TRAININGS / BATCH_SIZE) * BATCH_SIZE
        var currentStep = 0

        try {
            for (epoch in 0 until NUM_EPOCHS) {
                Log.d(TAG, ">>> EPOCH ${epoch + 1} START")
                for (batchIdx in 0 until (NUM_TRAININGS / BATCH_SIZE)) {
                    val batchData = loadBatchFromFile(batchIdx, imgFile, lblFile) ?: continue
                    val (imageBatch, labelBatch) = batchData

                    for (sampleIdx in 0 until BATCH_SIZE) {
                        // FIX 1: Use Direct ByteBuffers with Native Byte Order
                        val imgBB = ByteBuffer.allocateDirect(IMG_SIZE * 4).order(ByteOrder.nativeOrder())
                        val lblBB = ByteBuffer.allocateDirect(NUM_CLASSES * 4).order(ByteOrder.nativeOrder())
                        val lossBB = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())

                        // FIX 2: Copy data using putFloat directly into the ByteBuffer
                        imageBatch.position(sampleIdx * IMG_SIZE * 4)
                        for (i in 0 until IMG_SIZE) imgBB.putFloat(imageBatch.float)
                        imgBB.rewind()

                        labelBatch.position(sampleIdx * NUM_CLASSES * 4)
                        for (i in 0 until NUM_CLASSES) lblBB.putFloat(labelBatch.float)
                        lblBB.rewind()

                        // FIX 3: Pass the ByteBuffers (NOT the views) to runSignature
                        val inputs = mutableMapOf<String, Any>("x" to imgBB, "y" to lblBB)
                        val outputs = mutableMapOf<String, Any>("loss" to lossBB)

                        interpreter?.runSignature(inputs, outputs, "train")

                        currentStep++
                        if (currentStep % 100 == 0 || currentStep == totalSteps) {
                            lossBB.rewind()
                            val lossValue = lossBB.asFloatBuffer().get(0)
                            val percent = (currentStep * 100) / totalSteps
                            callback?.onProgress(percent)
                            Log.v(TAG, "Step $currentStep/$totalSteps | Loss: $lossValue")
                        }
                    }
                }
            }
            Log.i(TAG, "TRAINING COMPLETED")
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR during training: ${e.message}")
        }
    }

    fun inferModel(testImage: FloatArray): Int {
        if (interpreter == null) return -1
        return try {
            val inputBB = ByteBuffer.allocateDirect(IMG_SIZE * 4).order(ByteOrder.nativeOrder())
            for (f in testImage) inputBB.putFloat(f)
            inputBB.rewind()

            val outputBB = ByteBuffer.allocateDirect(NUM_CLASSES * 4).order(ByteOrder.nativeOrder())

            val inputs = mutableMapOf<String, Any>("x" to inputBB)
            val outputs = mutableMapOf<String, Any>("output" to outputBB)

            interpreter?.runSignature(inputs, outputs, "infer")
            outputBB.rewind()

            val probs = FloatArray(NUM_CLASSES)
            outputBB.asFloatBuffer().get(probs)
            probs.indices.maxByOrNull { probs[it] } ?: -1
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}")
            -1
        }
    }

    fun saveWeights(): Boolean {
        return try {
            val checkpointFile = File(context.filesDir, "checkpoint.ckpt")
            val inputs = mutableMapOf<String, Any>("checkpoint_path" to checkpointFile.absolutePath)
            interpreter?.runSignature(inputs, mutableMapOf(), "save")
            Log.i(TAG, "WEIGHTS SAVED")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Save failed: ${e.message}")
            false
        }
    }

    fun restoreWeights(): Boolean {
        val checkpointFile = File(context.filesDir, "checkpoint.ckpt")
        if (!checkpointFile.exists()) return false
        return try {
            val inputs = mutableMapOf<String, Any>("checkpoint_path" to checkpointFile.absolutePath)
            interpreter?.runSignature(inputs, mutableMapOf(), "restore")
            Log.i(TAG, "WEIGHTS RESTORED")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed: ${e.message}")
            false
        }
    }

    private fun loadBatchFromFile(index: Int, imgFile: File, lblFile: File): Pair<ByteBuffer, ByteBuffer>? {
        return try {
            val imgIn = FileInputStream(imgFile)
            val lblIn = FileInputStream(lblFile)

            val imgOffset = index * BATCH_SIZE * IMG_SIZE * 4L
            val lblOffset = index * BATCH_SIZE * NUM_CLASSES * 4L

            imgIn.channel.position(imgOffset)
            lblIn.channel.position(lblOffset)

            val imgData = ByteArray(BATCH_SIZE * IMG_SIZE * 4)
            val lblData = ByteArray(BATCH_SIZE * NUM_CLASSES * 4)

            imgIn.read(imgData)
            lblIn.read(lblData)

            imgIn.close(); lblIn.close()

            // Ensure the wrapped data has the correct byte order
            Pair(
                ByteBuffer.wrap(imgData).order(ByteOrder.nativeOrder()),
                ByteBuffer.wrap(lblData).order(ByteOrder.nativeOrder())
            )
        } catch (e: Exception) {
            null
        }
    }
}