package AppBackend.DataManager.DataLoaderAndInitializer

import AppBackend.TaskContainer.Task
import java.io.InputStream
import java.nio.FloatBuffer

class Image_DataInitializer : DataInitializer{
    override fun locadBatch(task: Task): Pair<InputStream, InputStream> {
        TODO("Not yet implemented")
    }

    override fun preprocess(task: Task): Pair <FloatBuffer, FloatBuffer> {
        TODO("Not yet implemented")
    }
}