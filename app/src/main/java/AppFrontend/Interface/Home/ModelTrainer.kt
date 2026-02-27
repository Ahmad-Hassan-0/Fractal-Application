package AppFrontend.Interface.Home

import android.content.Context
import android.os.Environment
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class ModelTrainer(private val context: Context) {
    private var interpreter: Interpreter? = null

    // Training parameters (rememebr: change the num_tranings on the evalution day, so showcase will be fast)
    private val NUM_EPOCHS = 20
    private val BATCH_SIZE = 100
    private val IMG_HEIGHT = 28
    private val IMG_WIDTH = 28
    private val NUM_TRAININGS = 6000 //no of images : default 60000
    private val NUM_BATCHES = NUM_TRAININGS / BATCH_SIZE
    private val NUM_CLASSES = 10

    // Streams for reading data
    private var imagesInputStream: InputStream? = null
    private var labelsInputStream: InputStream? = null

    companion object {
        private const val TAG = "ModelTrainer"
    }

    init {
        loadModel()
        initializeDataStreams()
    }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile("model.tflite")
            interpreter = Interpreter(modelBuffer)
            Log.d(TAG, "Model loaded successfully")
        } catch (e: IOException) {
            Log.e(TAG, "Error loading model: ${e.message}")
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(modelFilename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun initializeDataStreams() {
        try {
            // Don't load all data at once, just prepare streams
            Log.d(TAG, "Data streams initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing data streams: ${e.message}")
        }
    }

    private fun loadBatch(batchIndex: Int): Pair<FloatBuffer, FloatBuffer>? {
        return try {
            // Open fresh streams for this batch
            val imagesStream = context.assets.open("train_images.bin")
            val labelsStream = context.assets.open("train_labels.bin")

            // Skip to the correct position for this batch
            val imageSkipBytes = batchIndex * BATCH_SIZE * IMG_HEIGHT * IMG_WIDTH * 4L
            val labelSkipBytes = batchIndex * BATCH_SIZE * NUM_CLASSES * 4L

            imagesStream.skip(imageSkipBytes)
            labelsStream.skip(labelSkipBytes)

            // Create buffers for this batch
            val imageByteBuffer = ByteBuffer.allocateDirect(BATCH_SIZE * IMG_HEIGHT * IMG_WIDTH * 4)
                .order(ByteOrder.nativeOrder())
            val imageBatch = imageByteBuffer.asFloatBuffer()

            val labelByteBuffer = ByteBuffer.allocateDirect(BATCH_SIZE * NUM_CLASSES * 4)
                .order(ByteOrder.nativeOrder())
            val labelBatch = labelByteBuffer.asFloatBuffer()

            // Read data for this batch
            val imageData = ByteArray(BATCH_SIZE * IMG_HEIGHT * IMG_WIDTH * 4)
            val labelData = ByteArray(BATCH_SIZE * NUM_CLASSES * 4)

            val imageBytes = imagesStream.read(imageData)
            val labelBytes = labelsStream.read(labelData)

            if (imageBytes != imageData.size || labelBytes != labelData.size) {
                Log.w(TAG, "Warning: Incomplete batch read for batch $batchIndex")
            }

            // Convert byte arrays to float buffers
            val imageFloatBuffer = ByteBuffer.wrap(imageData).order(ByteOrder.nativeOrder()).asFloatBuffer()
            val labelFloatBuffer = ByteBuffer.wrap(labelData).order(ByteOrder.nativeOrder()).asFloatBuffer()

            // Copy to direct buffers
            val imageFloats = FloatArray(BATCH_SIZE * IMG_HEIGHT * IMG_WIDTH)
            val labelFloats = FloatArray(BATCH_SIZE * NUM_CLASSES)

            imageFloatBuffer.get(imageFloats)
            labelFloatBuffer.get(labelFloats)

            imageBatch.put(imageFloats)
            labelBatch.put(labelFloats)

            imageBatch.rewind()
            labelBatch.rewind()

            imagesStream.close()
            labelsStream.close()

            Pair(imageBatch, labelBatch)

        } catch (e: Exception) {
            Log.e(TAG, "Error loading batch $batchIndex: ${e.message}")
            null
        }
    }

    fun initializeWeights() {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter not initialized")
            return
        }

        try {
            // The model weights are already initialized when the interpreter is created
            Log.d(TAG, "Model weights initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing weights: ${e.message}")
        }
    }

    fun trainModel() {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter not initialized")
            return
        }

        try {
            val losses = FloatArray(NUM_EPOCHS)

            Log.d(TAG, "Starting training for $NUM_EPOCHS epochs...")

            for (epoch in 0 until NUM_EPOCHS) {
                var lastLoss = 0f
                var sampleCount = 0

                for (batchIdx in 0 until NUM_BATCHES) {
                    // Load batch on demand
                    val batchData = loadBatch(batchIdx)
                    if (batchData == null) {
                        Log.e(TAG, "Failed to load batch $batchIdx")
                        continue
                    }

                    val (imageBatch, labelBatch) = batchData

                    // Train on each sample in the batch individually
                    for (sampleIdx in 0 until BATCH_SIZE) {
                        // Extract single image (28x28 = 784 floats)
                        val singleImageBuffer = ByteBuffer.allocateDirect(IMG_HEIGHT * IMG_WIDTH * 4)
                            .order(ByteOrder.nativeOrder()).asFloatBuffer()

                        // Extract single label (10 floats)
                        val singleLabelBuffer = ByteBuffer.allocateDirect(NUM_CLASSES * 4)
                            .order(ByteOrder.nativeOrder()).asFloatBuffer()

                        // Copy single image data
                        imageBatch.position(sampleIdx * IMG_HEIGHT * IMG_WIDTH)
                        for (i in 0 until IMG_HEIGHT * IMG_WIDTH) {
                            singleImageBuffer.put(imageBatch.get())
                        }

                        // Copy single label data
                        labelBatch.position(sampleIdx * NUM_CLASSES)
                        for (i in 0 until NUM_CLASSES) {
                            singleLabelBuffer.put(labelBatch.get())
                        }

                        singleImageBuffer.rewind()
                        singleLabelBuffer.rewind()

                        // Prepare inputs for single sample
                        val inputs = HashMap<String, Any>()
                        inputs["x"] = singleImageBuffer
                        inputs["y"] = singleLabelBuffer

                        // Prepare outputs
                        val outputs = HashMap<String, Any>()
                        val lossBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asFloatBuffer()
                        outputs["loss"] = lossBuffer

                        // Run training step
                        interpreter!!.runSignature(inputs, outputs, "train")

                        // Record the loss
                        lossBuffer.rewind()
                        lastLoss = lossBuffer.get(0)
                        sampleCount++
                        // Add this line right after the runSignature call:
                        Log.d(TAG, "Epoch: $epoch, Batch: $batchIdx, Sample: $sampleIdx, Loss: $lastLoss")
                    }

                }

                losses[epoch] = lastLoss

                // Print progress every 5 epochs to reduce log spam
                if ((epoch + 1) % 5 == 0) {
                    Log.d(TAG, "Finished ${epoch + 1} epochs, samples processed: $sampleCount, current loss: $lastLoss")
                }
            }

            Log.d(TAG, "Training completed successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "Error during training: ${e.message}")
            e.printStackTrace()
        }
    }

    fun inferModel(testImage: FloatArray) {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter not initialized")
            return
        }

        if (testImage.size != IMG_HEIGHT * IMG_WIDTH) {
            Log.e(TAG, "Invalid test image size. Expected: ${IMG_HEIGHT * IMG_WIDTH}, Got: ${testImage.size}")
            return
        }

        try {
            // Prepare input using ByteBuffer
            val inputByteBuffer = ByteBuffer.allocateDirect(IMG_HEIGHT * IMG_WIDTH * 4)
                .order(ByteOrder.nativeOrder())
            val inputBuffer = inputByteBuffer.asFloatBuffer()
            inputBuffer.put(testImage)
            inputBuffer.rewind()

            val inputs = HashMap<String, Any>()
            inputs["x"] = inputBuffer

            // Prepare outputs
            val outputs = HashMap<String, Any>()
            val outputByteBuffer = ByteBuffer.allocateDirect(NUM_CLASSES * 4).order(ByteOrder.nativeOrder())
            val outputBuffer = outputByteBuffer.asFloatBuffer()
            val logitsByteBuffer = ByteBuffer.allocateDirect(NUM_CLASSES * 4).order(ByteOrder.nativeOrder())
            val logitsBuffer = logitsByteBuffer.asFloatBuffer()
            outputs["output"] = outputBuffer
            outputs["logits"] = logitsBuffer

            // Run inference
            interpreter!!.runSignature(inputs, outputs, "infer")

            // Get results
            val probabilities = FloatArray(NUM_CLASSES)
            val logits = FloatArray(NUM_CLASSES)
            outputBuffer.rewind()
            logitsBuffer.rewind()
            outputBuffer.get(probabilities)
            logitsBuffer.get(logits)

            // Find predicted class
            var maxIdx = 0
            var maxProb = probabilities[0]
            for (i in 1 until NUM_CLASSES) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    maxIdx = i
                }
            }

            Log.d(TAG, "Inference completed. Predicted class: $maxIdx, Probability: $maxProb")

        } catch (e: Exception) {
            Log.e(TAG, "Error during inference: ${e.message}")
        }
    }

    fun restoreWeights() {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter not initialized")
            return
        }

        try {
            // Log available signatures to verify 'save' and 'restore' are present
            Log.d(TAG, "Available signatures: ${interpreter!!.signatureKeys}")

            // Use external storage (Documents directory) for checkpoint file
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (!documentsDir.exists()) {
                documentsDir.mkdirs() // Create directory if it doesn't exist
            }
            Log.d(TAG, "Documents directory: ${documentsDir.absolutePath}, exists=${documentsDir.exists()}, writable=${documentsDir.canWrite()}, freeSpace=${documentsDir.freeSpace} bytes")

            // Test file write permissions
            val testFile = File(documentsDir, "test.txt")
            try {
                testFile.writeText("test")
                Log.d(TAG, "Test file written successfully to: ${testFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error writing test file: ${e.message}")
            }

            // Save weights to checkpoint file in Documents directory
            val checkpointFile = File(documentsDir, "checkpoint.ckpt")
            try {
                val saveInputs = HashMap<String, Any>()
                saveInputs["checkpoint_path"] = checkpointFile.absolutePath
                val saveOutputs = HashMap<String, Any>()
                interpreter!!.runSignature(saveInputs, saveOutputs, "save")
                Log.d(TAG, "Model weights saved to: ${checkpointFile.absolutePath}")
                if (checkpointFile.exists()) {
                    Log.d(TAG, "Checkpoint file confirmed at: ${checkpointFile.absolutePath}, size: ${checkpointFile.length()} bytes")
                } else {
                    Log.e(TAG, "Checkpoint file was not created at: ${checkpointFile.absolutePath}")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving checkpoint: ${e.message}", e)
                return
            }

            // List files in directory for debugging
            val files = documentsDir.listFiles()?.joinToString(", ") { it.name } ?: "none"
            Log.d(TAG, "Files in Documents directory: $files")

            // Restore weights from the checkpoint file
            try {
                val restoreInputs = HashMap<String, Any>()
                restoreInputs["checkpoint_path"] = checkpointFile.absolutePath
                val restoreOutputs = HashMap<String, Any>()
                interpreter!!.runSignature(restoreInputs, restoreOutputs, "restore")
                Log.d(TAG, "Weights restored successfully from: ${checkpointFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring weights: ${e.message}", e)
                return
            }

            // Verify restoration with a test inference
            val testImage = FloatArray(IMG_HEIGHT * IMG_WIDTH) { 0f } // Dummy input
            val inputBuffer = ByteBuffer.allocateDirect(IMG_HEIGHT * IMG_WIDTH * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer().put(testImage).rewind()
            val inferInputs = HashMap<String, Any>().apply { put("x", inputBuffer) }
            val inferOutputs = HashMap<String, Any>().apply {
                put("output", ByteBuffer.allocateDirect(NUM_CLASSES * 4).order(ByteOrder.nativeOrder()))
                put("logits", ByteBuffer.allocateDirect(NUM_CLASSES * 4).order(ByteOrder.nativeOrder()))
            }
            interpreter!!.runSignature(inferInputs, inferOutputs, "infer")
            Log.d(TAG, "Test inference after restore successful")
        } catch (e: Exception) {
            Log.e(TAG, "Error in restoreWeights: ${e.message}", e)
            e.printStackTrace()
        }
    }

    fun org_restoreWeights() {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter not initialized")
            return
        }

        try {
            // Log available signatures to verify 'save' and 'restore' are present
            Log.d(TAG, "Available signatures: ${interpreter!!.signatureKeys}")

            // Check storage availability and directory status
            val filesDir = context.filesDir
            Log.d(TAG, "Files directory: ${filesDir.absolutePath}, exists=${filesDir.exists()}, writable=${filesDir.canWrite()}, freeSpace=${filesDir.freeSpace} bytes")

            // Test file write permissions
            val testFile = File(filesDir, "test.txt")
            try {
                testFile.writeText("test")
                Log.d(TAG, "Test file written successfully to: ${testFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error writing test file: ${e.message}")
            }

            // Save weights to checkpoint file
            val checkpointFile = File(context.filesDir, "checkpoint.ckpt")
            try {
                val saveInputs = HashMap<String, Any>()
                saveInputs["checkpoint_path"] = checkpointFile.absolutePath
                val saveOutputs = HashMap<String, Any>()
                interpreter!!.runSignature(saveInputs, saveOutputs, "save")
                Log.d(TAG, "Model weights saved to: ${checkpointFile.absolutePath}")
                if (checkpointFile.exists()) {
                    Log.d(TAG, "Checkpoint file confirmed at: ${checkpointFile.absolutePath}, size: ${checkpointFile.length()} bytes")
                } else {
                    Log.e(TAG, "Checkpoint file was not created at: ${checkpointFile.absolutePath}")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving checkpoint: ${e.message}", e)
                return
            }

            // List files in directory for debugging
            val files = filesDir.listFiles()?.joinToString(", ") { it.name } ?: "none"
            Log.d(TAG, "Files in directory: $files")

            // Restore weights from the checkpoint file
            try {
                val restoreInputs = HashMap<String, Any>()
                restoreInputs["checkpoint_path"] = checkpointFile.absolutePath
                val restoreOutputs = HashMap<String, Any>()
                interpreter!!.runSignature(restoreInputs, restoreOutputs, "restore")
                Log.d(TAG, "Weights restored successfully from: ${checkpointFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring weights: ${e.message}", e)
                return
            }

            // Verify restoration with a test inference
            val testImage = FloatArray(IMG_HEIGHT * IMG_WIDTH) { 0f } // Dummy input
            val inputBuffer = ByteBuffer.allocateDirect(IMG_HEIGHT * IMG_WIDTH * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer().put(testImage).rewind()
            val inferInputs = HashMap<String, Any>().apply { put("x", inputBuffer) }
            val inferOutputs = HashMap<String, Any>().apply {
                put("output", ByteBuffer.allocateDirect(NUM_CLASSES * 4).order(ByteOrder.nativeOrder()))
                put("logits", ByteBuffer.allocateDirect(NUM_CLASSES * 4).order(ByteOrder.nativeOrder()))
            }
            interpreter!!.runSignature(inferInputs, inferOutputs, "infer")
            Log.d(TAG, "Test inference after restore successful")
        } catch (e: Exception) {
            Log.e(TAG, "Error in restoreWeights: ${e.message}", e)
            e.printStackTrace()
        }
    }

    fun cleanup() {
        interpreter?.close()
        interpreter = null
        imagesInputStream?.close()
        labelsInputStream?.close()
    }
}
