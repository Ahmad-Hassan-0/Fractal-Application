package AppBackend.LocalTrainingModule.TrainingStateManager

import AppBackend.TaskContainer.Task
import org.tensorflow.lite.Interpreter

class CheckpointManager(
    var completedEpochs: Int
) {
    fun createCheckpoint(task: Task, interpreter: Interpreter): Int {
        TODO("Not implemented yet")
    }

    fun loadCheckpoint(task: Task): Interpreter{
        TODO("Not implemented yet")
    }
}