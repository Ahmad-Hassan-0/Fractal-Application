package AppFrontend.Interface.Auth.ForgetPassword

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import AppBackend.Network.RegisteredInfo.Registered_DTO
import AppBackend.Network.Server_DAO.Server_DAO
import com.example.fractal.FractalApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgetPassword_ViewModel(application: Application) : AndroidViewModel(application) {

    private val _resetStatus = MutableLiveData<String>()
    val resetStatus: LiveData<String> get() = _resetStatus

    private val globalState = (application as FractalApplication).globalState

    fun recover(dto: ForgetPassword_DTO) {
        _resetStatus.value = "Sending..."
        Log.d("ForgetPassVM", "Triggering reset email for: ${dto.email}")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val registeredDto = Registered_DTO(email = dto.email)
                val serverDao = globalState.server ?: Server_DAO()

                // If it succeeds, it proceeds. If it fails, it jumps straight to the catch block below.
                serverDao.POST_ForgetPassword_sendEmail(registeredDto)

                withContext(Dispatchers.Main) {
                    _resetStatus.value = "Success: Link Sent"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // This will now print the EXACT reason Firebase rejected the request
                    _resetStatus.value = "Error: ${e.localizedMessage}"
                }
            }
        }
    }
}