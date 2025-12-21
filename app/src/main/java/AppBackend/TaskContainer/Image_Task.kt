package AppBackend.TaskContainer

import java.time.LocalDate

class Image_Task: Task, Image_Task_ModelParams, Image_Task_DataParams{
    override var task_Id: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var taskType: TaskType
        get() = TODO("Not yet implemented")
        set(value) {}
    override var task_expire_date: LocalDate
        get() = TODO("Not yet implemented")
        set(value) {}
    override var task_completion_status: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    override var training_type: List<String>
        get() = TODO("Not yet implemented")
        set(value) {}
    override var CKPT_FILENAME: String
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun save_data(): Boolean {
        TODO("Not yet implemented")
    }

    override var NUM_EPOCHS: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var input_tensor_name: Map<List<String>, Any>
        get() = TODO("Not yet implemented")
        set(value) {}
    override var output_tensor_name: Map<List<String>, Any>
        get() = TODO("Not yet implemented")
        set(value) {}
    override var MODEL_FILENAME: String
        get() = TODO("Not yet implemented")
        set(value) {}
    override var BATCH_SIZE: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var NUM_TRAININGS: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var INPUT_SHAPE: Array<Int>
        get() = TODO("Not yet implemented")
        set(value) {}
    override var NUM_CLASSES: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var TRAIN_IMAGES_FILENAME: String
        get() = TODO("Not yet implemented")
        set(value) {}
    override var TRAIN_LABELS_FILENAME: String
        get() = TODO("Not yet implemented")
        set(value) {}
}