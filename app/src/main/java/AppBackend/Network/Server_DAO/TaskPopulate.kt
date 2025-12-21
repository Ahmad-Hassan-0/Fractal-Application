package AppBackend.Network.Server_DAO

import AppBackend.TaskContainer.Task

interface TaskPopulate {
    fun GET_Task(flushPrevious: Boolean): Task
}