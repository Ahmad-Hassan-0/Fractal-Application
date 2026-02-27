package AppFrontend.Interface.Settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import AppGlobal.GlobalState
import AppGlobal.Utils.FileOperations
import com.example.fractal.FractalApplication

class Settings_ViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FractalApplication
    private val globalState = app.globalState
    private val fileOps = FileOperations(application)

    // Helper to get current config safely
    fun getConfig() = globalState.appConfig ?: AppGlobal.app_config()

    private fun saveConfig() {
        globalState.appConfig = getConfig()
        fileOps.writeJson("app_config.json", globalState.appConfig)
    }

    fun updateSettings(){

    }

    // --- Actions ---

    fun toggleWifi() {
        val config = getConfig()
        config.onWifi = !config.onWifi
        saveConfig()
    }

    fun toggleData() {
        val config = getConfig()
        config.onData = !config.onData
        saveConfig()
    }

    fun toggleOvernight() {
        val config = getConfig()
        config.overNightUtilization = !config.overNightUtilization
        saveConfig()
    }

    fun toggleIdle() {
        val config = getConfig()
        config.idleTimeUtilization = !config.idleTimeUtilization
        saveConfig()
    }

    fun toggleChargingExclusive() {
        val config = getConfig()
        config.onChargingExclusive = !config.onChargingExclusive
        saveConfig()
    }

    // Update the charge limit percentage
    fun updateChargeLimit(value: Int) {
        val config = getConfig()
        config.minChargeLimit = value
        saveConfig()
    }
}