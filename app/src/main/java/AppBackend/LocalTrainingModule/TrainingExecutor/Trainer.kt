package AppBackend.LocalTrainingModule.TrainingExecutor

import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer

interface Trainer {
    var task: AppBackend.TaskContainer.Task
    var trainingData: Any
    var interpreter: Interpreter
    var currentEpoch: Int

    fun setUpTrainer(pTrainingData: Any?, pTask: AppBackend.TaskContainer.Task)
    fun loadModelFile(): MappedByteBuffer
    fun loadModel(): Interpreter
    fun initializeWeights()

    // Add the callback here!
    fun trainModel(callback: TrainingCallback? = null)
}