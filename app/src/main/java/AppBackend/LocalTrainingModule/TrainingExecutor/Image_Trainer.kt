package AppBackend.LocalTrainingModule.TrainingExecutor

import AppBackend.TaskContainer.Task
import androidx.collection.emptyLongSet
import org.tensorflow.lite.Interpreter
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer

class ImageTrainer(
    override var task: Task,
    override var trainingData: Any,
    override var interpreter: Interpreter,
    override var currentEpoch: Int
) : Trainer {

    override fun setUpTrainer(pTrainingData: Any, pTask: Task) {
        TODO("Not yet implemented")
    }

    fun setUpTrainerInnerFunction(pTrainingData: Any, pTask: Task) {
        if (pTrainingData !is Pair<*, *> ||
            pTrainingData.first !is FloatBuffer ||
            pTrainingData.second !is FloatBuffer) {
            println("Invalid training data format. Please provide a Pair<FloatBuffer, FloatBuffer>")
        } else {
            val data = pTrainingData as Pair<FloatBuffer, FloatBuffer>
            TODO("Not yet implemented")
        }
    }

    override fun loadModelFile(): MappedByteBuffer {
        TODO("Not yet implemented")
    }

    override fun loadModel(): Interpreter {
        TODO("Not yet implemented")
    }

    override fun initializeWeights() {
        TODO("Not yet implemented")
    }

    override fun trainModel() {
        TODO("Not yet implemented")
    }
}