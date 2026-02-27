package AppFrontend.Interface.Insights.UsageInsights

import android.app.Application
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import AppBackend.ResourceManagement.GpuUsageReader
import AppBackend.ResourceManagement.ResourceManager.ResourceManager_Live_DTO
import android.util.Log
import java.util.concurrent.Executors

class UsageInsights_ViewModel(application: Application) : AndroidViewModel(application) {

    private val resourceManager = ResourceManager_Live_DTO(application.applicationContext)

    private val _liveStats = MutableLiveData<Map<String, Float>>()
    val liveStats: LiveData<Map<String, Float>> = _liveStats

    private val bgExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isPolling = false

    // Tracks whether this device has a readable GPU sysfs node.
    // After the first null result we stop trying and use the weighted estimate.
    private var gpuSysfsAvailable = true

    // Last known CPU load — used as a weighted component for the GPU estimate
    // when no sysfs node is available. GPU load tends to lag CPU by ~1 cycle
    // and is typically 60-80% of CPU load on UI-heavy workloads.
    private var lastCpuLoad = 0f

    private val pollingRunnable = object : Runnable {
        override fun run() {
            if (!isPolling) return

            val context = getApplication<Application>().applicationContext
            resourceManager.updateStatistics(context)
            val romUsage = getRomUsagePercentage()

            val cpuLoad = (resourceManager.cpuPercentage / 100f).coerceIn(0f, 1f)
            val gpuLoad = resolveGpuLoad(cpuLoad)
            lastCpuLoad = cpuLoad

            val statsMap = mapOf(
                "CPU"  to cpuLoad,
                "RAM"  to (resourceManager.ramPercentage / 100f).coerceIn(0f, 1f),
                "TEMP" to (resourceManager.temperature  / 100f).coerceIn(0f, 1f),
                "ROM"  to romUsage.coerceIn(0f, 1f),
                "GPU"  to gpuLoad
            )

            _liveStats.postValue(statsMap)

            if (isPolling) {
                mainHandler.postDelayed({
                    if (isPolling) bgExecutor.execute(this)
                }, 1000)
            }
        }
    }

    /**
     * Attempts to read real GPU usage from vendor sysfs.
     * Falls back to a CPU-weighted estimate only when the device exposes nothing.
     *
     * The estimate uses:
     *   70% current CPU load  (GPU roughly tracks CPU on mobile SoCs)
     *  +20% previous CPU load (accounts for ~1 frame render lag)
     *  +10% small idle base   (GPU never reads 0 even at idle due to compositor)
     * Clamped to 0.05..0.90 — GPU never truly hits 0% (display compositor always runs)
     * and 90% is a realistic ceiling before thermal throttling kicks in.
     */

    private fun resolveGpuLoad(currentCpu: Float): Float {
        if (gpuSysfsAvailable) {
            val real = GpuUsageReader.read()
            if (real != null) return real
            gpuSysfsAvailable = false
            Log.i("UsageVM", "GPU sysfs unavailable on this device — switching to weighted estimate.")
        }

        // CPU-weighted estimate used only when sysfs is completely inaccessible.
        // 70% current CPU + 20% previous CPU (render lag) + 5% compositor idle floor.
        return ((currentCpu * 0.70f) + (lastCpuLoad * 0.20f) + 0.05f)
            .coerceIn(0.05f, 0.90f)
    }

    fun startHeartBeat() {
        if (!isPolling) {
            isPolling = true
            _liveStats.value = mapOf(
                "CPU" to 0f, "RAM" to 0f, "TEMP" to 0f, "ROM" to 0f, "GPU" to 0f
            )
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