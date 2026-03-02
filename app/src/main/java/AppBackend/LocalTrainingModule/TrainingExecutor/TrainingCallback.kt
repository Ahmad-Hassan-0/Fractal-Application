package AppBackend.LocalTrainingModule.TrainingExecutor

interface TrainingCallback {
    fun onProgress(percentage: Int)
    fun onStatusUpdate(message: String)
    fun onValidationUpdate(result: String)
    fun onEpochUpdate(completedEpochs: Int, totalEpochs: Int, loss: Float, timeLeft: String)

    // NEW: Let the Backend ask the Frontend about the user's taps!
    fun isPaused(): Boolean
    fun isCancelled(): Boolean
    fun onWaitingStateChanged(isWaiting: Boolean)
    // NEW: Allow the active trainer to pull live hardware conditions
    fun checkLiveConditions(): String?
}