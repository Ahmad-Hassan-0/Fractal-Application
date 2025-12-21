package AppBackend.TaskContainer

interface Image_Task_ModelParams:Task_ModelParams {
    override var NUM_EPOCHS: Int
    override var input_tensor_name: Map<List<String>, Any>
    override var output_tensor_name: Map<List<String>, Any>
    override var MODEL_FILENAME: String
}