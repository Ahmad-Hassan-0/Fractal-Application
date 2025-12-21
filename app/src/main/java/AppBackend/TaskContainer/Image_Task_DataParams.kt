package AppBackend.TaskContainer

interface Image_Task_DataParams:Task_DataParams{
    var BATCH_SIZE: Int
    var NUM_TRAININGS: Int
    var INPUT_SHAPE: Array<Int>
    var NUM_CLASSES: Int
    var TRAIN_IMAGES_FILENAME: String
    var TRAIN_LABELS_FILENAME: String
}
