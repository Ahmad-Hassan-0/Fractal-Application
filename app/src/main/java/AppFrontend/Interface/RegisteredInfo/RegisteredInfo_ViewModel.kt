//package AppFrontend.Interface.RegisteredInfo
//
//import androidx.lifecycle.ViewModel
//
//class RegisteredInfo_ViewModel : ViewModel() {
//    // Add your LiveData or StateFlows here later to feed update_ui_stats()
//}

package AppFrontend.Interface.RegisteredInfo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import AppBackend.Network.RegisteredInfo.Registered_DTO
import AppBackend.Network.RegisteredInfo.RegistrationManager

class RegisteredInfo_ViewModel(application: Application) : AndroidViewModel(application) {

    private val registrationManager = RegistrationManager(application.applicationContext)

    // LiveData to hold the DTO
    private val _deviceInfo = MutableLiveData<Registered_DTO>()
    val deviceInfo: LiveData<Registered_DTO> get() = _deviceInfo

    init {
        loadDeviceData()
    }

    fun loadDeviceData() {
        val data = registrationManager.generateNewRegistrationData()
        _deviceInfo.value = data
    }
}