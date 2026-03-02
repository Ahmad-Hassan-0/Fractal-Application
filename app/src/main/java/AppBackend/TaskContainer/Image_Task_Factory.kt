package AppBackend.TaskContainer

class Image_Task_Factory: Task_Factory {
    override fun createTask(): Task {
        return Image_Task();
    }
}