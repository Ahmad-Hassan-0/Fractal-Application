package AppFrontend.Interface.Auth.DeviceUnregister

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import AppBackend.Network.RegisteredInfo.RegistrationManager
import AppBackend.Network.Server_DAO.Server_DAO
import com.example.fractal.FractalApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceUnregister_ViewModel(application: Application) : AndroidViewModel(application) {

    private val _unregisterStatus = MutableLiveData<String>()
    val unregisterStatus: LiveData<String> get() = _unregisterStatus

    private val registrationManager = RegistrationManager(application.applicationContext)
    private val globalState = (application as FractalApplication).globalState

    fun unregister(dto: Unregister_DTO) {
        Log.d("UnregisterVM", "Title: ${dto.problemTitle}, Unregister: ${dto.wantsToUnregister}")
        _unregisterStatus.value = "Processing..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawData = registrationManager.generateNewRegistrationData()
                val serverDao = globalState.server ?: Server_DAO()
                val registeredDto = serverDao.GET_RegisteredInfo(rawData)

                val success = serverDao.POST_Unregister(registeredDto, dto)

                withContext(Dispatchers.Main) {
                    if (success) {
                        _unregisterStatus.value = "Success"
                    } else {
                        _unregisterStatus.value = "Error: Failed to submit feedback."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _unregisterStatus.value = "Error: ${e.message}"
                }
            }
        }
    }
}