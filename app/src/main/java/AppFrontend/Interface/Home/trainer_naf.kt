package AppFrontend.Interface.Home

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class trainer_naf(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val TAG = "FRACTAL_AI_ENGINE"

    // EXACT Parameters from your working code
    private val NUM_EPOCHS = 2
    private val BATCH_SIZE = 100
    private val IMG_HEIGHT = 28
    private val IMG_WIDTH = 28
    private val NUM_TRAININGS = 6000
    private val NUM_BATCHES = NUM_TRAININGS / BATCH_SIZE
    private val NUM_CLASSES = 10

    // Filenames for server-downloaded data
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
                val fileDescriptor = context.assets.openFd("model.tflite")
                val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
                modelBuffer = inputStream.channel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
                inputStream.close()
            }

            interpreter = Interpreter(modelBuffer)
            Log.i(TAG, "Interpreter successfully initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}")
        }
    }

    fun trainModel(callback: TrainingCallback?) {
        if (interpreter == null) initializeInterpreter()

        val imgFile = File(context.filesDir, SERVER_IMAGES_FILENAME)
        val lblFile = File(context.filesDir, SERVER_LABELS_FILENAME)

        try {
            Log.d(TAG, "Starting training for $NUM_EPOCHS epochs...")
            val totalSteps = NUM_EPOCHS * NUM_BATCHES * BATCH_SIZE
            var currentStep = 0

            for (epoch in 0 until NUM_EPOCHS) {
                var lastLoss = 0f

                for (batchIdx in 0 until NUM_BATCHES) {
                    val batchData = loadBatchFromFile(batchIdx, imgFile, lblFile)
                    if (batchData == null) {
                        Log.e(TAG, "Failed to load batch $batchIdx")
                        continue
                    }

                    val (imageBatch, labelBatch) = batchData

                    for (sampleIdx in 0 until BATCH_SIZE) {
                        // EXACT Single-Sample Extraction Methodology
                        val singleImageBuffer = ByteBuffer.allocateDirect(IMG_HEIGHT * IMG_WIDTH * 4)
                            .order(ByteOrder.nativeOrder()).asFloatBuffer()

                        val singleLabelBuffer = ByteBuffer.allocateDirect(NUM_CLASSES * 4)
                            .order(ByteOrder.nativeOrder()).asFloatBuffer()

                        imageBatch.position(sampleIdx * IMG_HEIGHT * IMG_WIDTH)
                        for (i in 0 until IMG_HEIGHT * IMG_WIDTH) {
                            singleImageBuffer.put(imageBatch.get())
                        }

                        labelBatch.position(sampleIdx * NUM_CLASSES)
                        for (i in 0 until NUM_CLASSES) {
                            singleLabelBuffer.put(labelBatch.get())
                        }

                        singleImageBuffer.rewind()
                        singleLabelBuffer.rewind()

                        // FIX: Explicitly typed Map to avoid the mismatch error shown in your image
                        val inputs = mutableMapOf<String, Any>(
                            "x" to singleImageBuffer,
                            "y" to singleLabelBuffer
                        )

                        val lossBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asFloatBuffer()
                        val outputs = mutableMapOf<String, Any>("loss" to lossBuffer)

                        interpreter!!.runSignature(inputs, outputs, "train")

                        lossBuffer.rewind()
                        lastLoss = lossBuffer.get(0)
                        currentStep++

                        if (currentStep % 100 == 0) {
                            val percent = (currentStep * 100) / totalSteps
                            callback?.onProgress(percent)
                            Log.d(TAG, "Step $currentStep/$totalSteps | Loss: $lastLoss")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during training: ${e.message}")
        }
    }

    private fun loadBatchFromFile(batchIndex: Int, imgFile: File, lblFile: File): Pair<FloatBuffer, FloatBuffer>? {
        return try {
            val imagesStream = FileInputStream(imgFile)
            val labelsStream = FileInputStream(lblFile)

            // EXACT Skip Logic using channel position
            val imageSkipBytes = batchIndex * BATCH_SIZE * IMG_HEIGHT * IMG_WIDTH * 4L
            val labelSkipBytes = batchIndex * BATCH_SIZE * NUM_CLASSES * 4L

            imagesStream.channel.position(imageSkipBytes)
            labelsStream.channel.position(labelSkipBytes)

            val imageBatch = ByteBuffer.allocateDirect(BATCH_SIZE * IMG_HEIGHT * IMG_WIDTH * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()

            val labelBatch = ByteBuffer.allocateDirect(BATCH_SIZE * NUM_CLASSES * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()

            val imageData = ByteArray(BATCH_SIZE * IMG_HEIGHT * IMG_WIDTH * 4)
            val labelData = ByteArray(BATCH_SIZE * NUM_CLASSES * 4)

            imagesStream.read(imageData)
            labelsStream.read(labelData)

            // EXACT Methodology: Wrap -> get into FloatArray -> put into DirectBuffer
            val imageFloatBuffer = ByteBuffer.wrap(imageData).order(ByteOrder.nativeOrder()).asFloatBuffer()
            val labelFloatBuffer = ByteBuffer.wrap(labelData).order(ByteOrder.nativeOrder()).asFloatBuffer()

            val imageFloats = FloatArray(BATCH_SIZE * IMG_HEIGHT * IMG_WIDTH)
            val labelFloats = FloatArray(BATCH_SIZE * NUM_CLASSES)

            imageFloatBuffer.get(imageFloats)
            labelFloatBuffer.get(labelFloats)

            imageBatch.put(imageFloats).rewind()
            labelBatch.put(labelFloats).rewind()

            imagesStream.close()
            labelsStream.close()

            Pair(imageBatch, labelBatch)
        } catch (e: Exception) {
            null
        }
    }

    fun inferModel(testImage: FloatArray): Int {
        if (interpreter == null) return -1
        return try {
            val inputBuffer = ByteBuffer.allocateDirect(IMG_HEIGHT * IMG_WIDTH * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            inputBuffer.put(testImage).rewind()

            val outputBuffer = ByteBuffer.allocateDirect(NUM_CLASSES * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            val logitsBuffer = ByteBuffer.allocateDirect(NUM_CLASSES * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

            // Explicit typing for inputs and outputs
            val inputs = mutableMapOf<String, Any>("x" to inputBuffer)
            val outputs = mutableMapOf<String, Any>("output" to outputBuffer, "logits" to logitsBuffer)

            interpreter!!.runSignature(inputs, outputs, "infer")

            outputBuffer.rewind()
            val probs = FloatArray(NUM_CLASSES)
            outputBuffer.get(probs)
            probs.indices.maxByOrNull { probs[it] } ?: -1
        } catch (e: Exception) { -1 }
    }

    fun saveWeights(): Boolean {
        return try {
            val checkpointFile = File(context.filesDir, "checkpoint.ckpt")
            val inputs = mutableMapOf<String, Any>("checkpoint_path" to checkpointFile.absolutePath)
            interpreter!!.runSignature(inputs, mutableMapOf<String, Any>(), "save")
            true
        } catch (e: Exception) { false }
    }

    fun restoreWeights(): Boolean {
        val checkpointFile = File(context.filesDir, "checkpoint.ckpt")
        if (!checkpointFile.exists()) return false
        return try {
            val inputs = mutableMapOf<String, Any>("checkpoint_path" to checkpointFile.absolutePath)
            interpreter!!.runSignature(inputs, mutableMapOf<String, Any>(), "restore")
            true
        } catch (e: Exception) { false }
    }
}
