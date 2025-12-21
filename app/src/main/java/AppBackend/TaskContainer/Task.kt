package AppBackend.TaskContainer

import java.time.LocalDate
import java.util.Date

interface Task: Task_ModelParams, Task_DataParams{
    var task_Id: Int
    var taskType: TaskType
    var task_expire_date: LocalDate
    var task_completion_status: Boolean
    var training_type: List<String>
    var CKPT_FILENAME: String

    fun save_data(): Boolean
}