package AppFrontend.Interface.Insights.UsageInsights

import android.app.Application
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import AppBackend.ResourceManagement.ResourceManager.ResourceManager_Live_DTO
import java.util.concurrent.Executors

class UsageInsights_ViewModel(application: Application) : AndroidViewModel(application) {

    private val resourceManager = ResourceManager_Live_DTO(application.applicationContext)

    private val _liveStats = MutableLiveData<Map<String, Float>>()
    val liveStats: LiveData<Map<String, Float>> = _liveStats

    // Dedicated single background thread for all heavy stat reads.
    // Never touches the main thread until postValue() at the very end.
    private val bgExecutor = Executors.newSingleThreadExecutor()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isPolling = false

    private val pollingRunnable = object : Runnable {
        override fun run() {
            if (!isPolling) return

            val context = getApplication<Application>().applicationContext

            // All heavy work runs here — on the background thread, never the main thread
            resourceManager.updateStatistics(context)
            val romUsage = getRomUsagePercentage()

            val statsMap = mapOf(
                "CPU"  to (resourceManager.cpuPercentage / 100f).coerceIn(0f, 1f),
                "RAM"  to (resourceManager.ramPercentage / 100f).coerceIn(0f, 1f),
                "TEMP" to (resourceManager.temperature  / 100f).coerceIn(0f, 1f),
                "ROM"  to romUsage.coerceIn(0f, 1f),
                "GPU"  to (resourceManager.cpuPercentage / 100f).coerceIn(0f, 1f)
            )

            // postValue() is safe to call from background — it marshals to main thread internally
            _liveStats.postValue(statsMap)

            // Schedule the next poll on the background thread, not the main thread
            if (isPolling) {
                mainHandler.postDelayed({
                    if (isPolling) bgExecutor.execute(this)
                }, 1000)
            }
        }
    }

    fun startHeartBeat() {
        if (!isPolling) {
            isPolling = true

            // Immediate empty state so chart draws right away
            _liveStats.value = mapOf(
                "CPU" to 0f, "RAM" to 0f, "TEMP" to 0f, "ROM" to 0f, "GPU" to 0f
            )

            // First execution goes straight to background thread
            bgExecutor.execute(pollingRunnable)
        }
    }

    fun stopHeartBeat() {
        isPolling = false
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun getRomUsagePercentage(): Float {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            ((totalBytes - availableBytes).toFloat() / totalBytes.toFloat())
        } catch (e: Exception) {
            0.5f
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopHeartBeat()
        bgExecutor.shutdown()
    }
}