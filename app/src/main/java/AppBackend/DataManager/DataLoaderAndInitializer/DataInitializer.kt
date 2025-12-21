package AppBackend.DataManager.DataLoaderAndInitializer

import AppBackend.TaskContainer.Task

interface DataInitializer {
    fun locadBatch(task: Task): Any;
    fun preprocess(task: Task): Any;
}