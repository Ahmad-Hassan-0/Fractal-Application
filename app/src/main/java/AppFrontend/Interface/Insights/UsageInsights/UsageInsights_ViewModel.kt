package AppFrontend.Interface.Insights.UsageInsights

import android.app.Application
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.AndroidViewModel
import AppBackend.ResourceManagement.ResourceManager.ResourceManager_Live_DTO

class UsageInsights_ViewModel(application: Application) : AndroidViewModel(application) {

    fun transformLiveStats(stats: ResourceManager_Live_DTO): Map<String, Float> {
        val romUsage = getRomUsagePercentage()

        return mapOf(
            "CPU"  to (stats.cpuPercentage / 100f).coerceIn(0f, 1f),
            "RAM"  to (stats.ramPercentage / 100f).coerceIn(0f, 1f),
            "TEMP" to (stats.temperature  / 100f).coerceIn(0f, 1f),
            "ROM"  to romUsage.coerceIn(0f, 1f),
            "GPU"  to (stats.gpuPercentage / 100f).coerceIn(0f, 1f) // Pulled cleanly from the central DTO!
        )
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
}