package AppBackend.TaskContainer

interface Task_ModelParams{
    var NUM_EPOCHS: Int
    var input_tensor_name: Map<List<String>, Any>
    var output_tensor_name: Map<List<String>, Any>
    var MODEL_FILENAME: String
}