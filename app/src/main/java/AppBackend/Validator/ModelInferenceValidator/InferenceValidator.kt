package AppBackend.Validator.ModelInferenceValidator

import AppBackend.TaskContainer.Task
import org.tensorflow.lite.Interpreter

interface InferenceValidator {
    fun infer(obj: Any, interpreter: Interpreter, task: Task): String
}