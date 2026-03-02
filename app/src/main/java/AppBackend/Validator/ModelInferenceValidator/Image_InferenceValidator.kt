package AppBackend.Validator.ModelInferenceValidator

import android.util.Log
import AppBackend.TaskContainer.Image_Task
import AppBackend.TaskContainer.Task
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Image_InferenceValidator : InferenceValidator {

    private val TAG = "ImageValidator"

    /**
     * Entry point for the validation logic.
     * Returns a formatted string for the UI (e.g., "Class 7 (98.5%)")
     */
    override fun infer(obj: Any, interpreter: Interpreter, task: Task): String {
        return if (obj is Pair<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val pair = obj as Pair<FloatBuffer, FloatBuffer>
            inferImageInferenceValidate(pair, interpreter, task)
        } else {
            Log.e(TAG, "Validation failed: Invalid data format.")
            "Format Error"
        }
    }

    private fun inferImageInferenceValidate(obj: Pair<FloatBuffer, FloatBuffer>, interpreter: Interpreter, task: Task): String {
        Log.i(TAG, "Starting Model Inference Validation (Sanity Check)...")

        return try {
            val imageTask = task as Image_Task
            val imageBatch = obj.first

            // --- SAFE EXTRACTION OF DIMENSIONS FROM TASK ---
            val shapeArray = imageTask.INPUT_SHAPE
            val imgHeight: Int
            val imgWidth: Int

            if (shapeArray.size == 2) {
                imgHeight = shapeArray[0]
                imgWidth = shapeArray[1]
            } else if (shapeArray.size >= 3) {
                imgHeight = shapeArray[1]
                imgWidth = shapeArray[2]
            } else {
                imgHeight = 28
                imgWidth = 28
            }

            val numClasses = imageTask.NUM_CLASSES
            // ----------------------------------------------

            // 1. Extract a single test image (the very first one) from the memory batch
            val singleImageFloats = FloatArray(imgHeight * imgWidth)
            imageBatch.position(0) // Go to the absolute start of the buffer
            imageBatch.get(singleImageFloats)
            imageBatch.rewind() // Reset the buffer position

            // 2. Prepare the input buffer using dynamic dimensions
            val inputBuffer = ByteBuffer.allocateDirect(imgHeight * imgWidth * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            inputBuffer.put(singleImageFloats).rewind()

            // 3. Prepare output buffers based on dynamic class numbers
            val outputBuffer = ByteBuffer.allocateDirect(numClasses * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            val logitsBuffer = ByteBuffer.allocateDirect(numClasses * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()

            val inputs = mutableMapOf<String, Any>("x" to inputBuffer)
            val outputs = mutableMapOf<String, Any>("output" to outputBuffer, "logits" to logitsBuffer)

            // 4. Run the validation inference
            interpreter.runSignature(inputs, outputs, "infer")

            // 5. Parse the results to find the highest probability
            outputBuffer.rewind()
            val probs = FloatArray(numClasses)
            outputBuffer.get(probs)

            val predictedClass = probs.indices.maxByOrNull { probs[it] } ?: -1
            val confidence = if (predictedClass != -1) probs[predictedClass] * 100 else 0f

            val resultString = String.format("Class %d (%.1f%%)", predictedClass, confidence)

            Log.i(TAG, "=========================================")
            Log.i(TAG, " VALIDATION SUCCESSFUL")
            Log.i(TAG, " Predicted Result: $resultString")
            Log.i(TAG, "=========================================")

            resultString

        } catch (e: Exception) {
            Log.e(TAG, "Inference Validation Failed: ${e.message}")
            "Inference Error"
        }
    }
}