package AppBackend.LocalTrainingModule.TrainingExecutor

interface Trainer_Factory {
    fun createTrainer(): Trainer
}