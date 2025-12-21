package AppBackend.Validator.ModelInferenceValidator

import org.tensorflow.lite.Interpreter
import java.nio.FloatBuffer

class Image_InferenceValidator : InferenceValidator {
    override fun infer(obj: Any, interpreter: Interpreter) {
        if (obj !is Pair<*, *>) {
            throw IllegalArgumentException("Expected input of type Pair<FloatBuffer, FloatBuffer>, but got ${obj::class.simpleName}")
        }
        val pair = obj as Pair<FloatBuffer, FloatBuffer>
        inferImageInferenceValidate(pair, interpreter)
    }

    fun inferImageInferenceValidate(obj: Pair<FloatBuffer, FloatBuffer>, interpreter: Interpreter) {
        TODO("Not yet implemented")
    }
}