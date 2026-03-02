package AppFrontend.Interface.Auth.DeviceAuthorization

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import AppBackend.Network.RegisteredInfo.RegistrationManager
import AppBackend.Network.Server_DAO.Server_DAO
import com.example.fractal.FractalApplication
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceAuthorization_ViewModel(application: Application) : AndroidViewModel(application) {

    private val _authStatus = MutableLiveData<String>()
    val authStatus: LiveData<String> get() = _authStatus

    private val globalState = (application as FractalApplication).globalState
    private var pollingJob: Job? = null

    fun loginRegister(dto: LoginRegister_DTO) {
        pollingJob?.cancel() // Cancel existing loops

        Log.d("AuthVM", "Attempting Auth for: ${dto.username}")
        _authStatus.value = "Processing..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val regManager = RegistrationManager(getApplication())
                val registeredDto = regManager.generateNewRegistrationData()
                val serverDao = globalState.server ?: Server_DAO()

                val success = serverDao.POST_RegisterLogin(registeredDto, dto)

                withContext(Dispatchers.Main) {
                    if (success) {
                        _authStatus.value = "Success"
                    }
                }
            } catch (e: Exception) {
                val msg = e.message ?: "An unknown error occurred."

                if (msg.startsWith("AWAITING_VERIFICATION:")) {
                    val displayMsg = msg.removePrefix("AWAITING_VERIFICATION:")
                    withContext(Dispatchers.Main) {
                        _authStatus.value = "AwaitingVerification:$displayMsg"
                    }
                    startPollingForVerification()
                } else {
                    withContext(Dispatchers.Main) {
                        _authStatus.value = "Error: $msg"
                    }
                    FirebaseAuth.getInstance().signOut()
                }
            }
        }
    }

    private fun startPollingForVerification() {
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            val auth = FirebaseAuth.getInstance()
            var verified = false
            val maxAttempts = 200 // 10 minutes (200 checks * 3 seconds)

            for (i in 0 until maxAttempts) {
                delay(3000)

                // Re-fetch the user instance inside the loop to guarantee fresh data
                val currentUser = auth.currentUser
                if (currentUser == null) break

                try {
                    com.google.android.gms.tasks.Tasks.await(currentUser.reload())
                    if (currentUser.isEmailVerified) {
                        verified = true
                        break
                    }
                } catch (e: Exception) {
                    // Ignore transient network drops during polling
                }
            }

            withContext(Dispatchers.Main) {
                if (verified) {
                    _authStatus.value = "Success"
                } else {
                    // Timeout hit. Stop checking and sign out locally.
                    auth.signOut()
                    _authStatus.value = "Error: Verification timed out. Please try logging in again."
                }
            }
        }
    }

    fun updateAuthStatus(newStatus: String) {
        _authStatus.value = newStatus
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}