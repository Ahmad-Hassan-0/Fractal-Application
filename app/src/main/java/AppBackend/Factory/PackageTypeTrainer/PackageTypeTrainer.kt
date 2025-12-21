package AppBackend.Factory.PackageTypeTrainer

import AppBackend.DataManager.DataLoaderAndInitializer.DataInitializer
import AppBackend.LocalTrainingModule.TrainingExecutor.Trainer
import AppBackend.TaskContainer.Task
import AppBackend.Validator.ModelInferenceValidator.InferenceValidator

class PackageTypeTrainer(
    val task: Task,
    val dataInitializer: DataInitializer,
    val trainer: Trainer,
    val validator: InferenceValidator
) {
    fun run(task: Task) {
        TODO("To be implemented")
    }
}
