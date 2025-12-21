package AppBackend.LocalTrainingModule.TrainingExecutor

import AppBackend.TaskContainer.Task
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer

interface Trainer {
    val task: Task
    val trainingData: Any
    val interpreter: Interpreter
    val currentEpoch: Int

    fun setUpTrainer(pTrainingData: Any, pTask: Task)
    fun loadModelFile(): MappedByteBuffer
    fun loadModel(): Interpreter
    fun initializeWeights();
    fun trainModel();
}