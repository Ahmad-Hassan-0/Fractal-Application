package AppBackend.LocalTrainingModule.TrainingStateManager

import AppBackend.TaskContainer.Task

data class checkpoint_DTO (
    val task_Id: Int,
    val lastEpoch: Int,
    val checkpointTimestamp: Int,
    val task: Task
)