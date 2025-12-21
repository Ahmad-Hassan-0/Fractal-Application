package AppBackend.Validator.ModelInferenceValidator

import org.tensorflow.lite.Interpreter

interface InferenceValidator {
    fun infer(obj: Any, interpreter: Interpreter)
}