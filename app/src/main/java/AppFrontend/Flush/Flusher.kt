package AppFrontend.Flush

import AppBackend.TaskContainer.Task

class Flusher {
    fun flushCheckpoint(task: Task){

    }

    fun flushCurrentTask(task: Task){

    }

    fun flushAll(task: Task): Boolean{
        TODO("looks for files, with pre_taskId prefix of the config files of the task thats done")
    }
}