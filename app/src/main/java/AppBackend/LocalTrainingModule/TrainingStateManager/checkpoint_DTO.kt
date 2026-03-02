package AppBackend.LocalTrainingModule.TrainingStateManager

data class checkpoint_DTO (
    val task_Id: Int,
    val lastEpoch: Int,
    val checkpointTimestamp: Long
)