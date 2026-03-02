package AppBackend.LocalTrainingModule.TrainingExecutor

class Image_Trainer_Factory : Trainer_Factory {
    override fun createTrainer(): Trainer {
        return ImageTrainer()
    }
}