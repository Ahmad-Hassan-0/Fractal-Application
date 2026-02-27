package AppFrontend.Interface.Auth.DeviceAuthorization

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DeviceAuthorization_ViewModel : ViewModel() {

    // You can use these to observe status in the Fragment (e.g. show loading spinner)
    private val _authStatus = MutableLiveData<String>()
    val authStatus: LiveData<String> get() = _authStatus

    fun loginRegister(dto: LoginRegister_DTO) {
        // Logic to Determine if it's Login or Register happens on Server
        // OR you can split logic here.

        Log.d("AuthVM", "Attempting Auth for: ${dto.username}")

        // TODO: CALL SERVER_DAO HERE
        // val serverDao = GlobalState.server...
        // val success = serverDao.POST_RegisterLogin(registeredDto, dto)

        // For now, simulate success
        _authStatus.value = "Processing..."
    }
}