package AppFrontend.Interface.RegisteredInfo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import AppBackend.Network.RegisteredInfo.Registered_DTO
import AppBackend.Network.RegisteredInfo.RegistrationManager
import AppBackend.Network.Server_DAO.Server_DAO
import com.example.fractal.FractalApplication
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisteredInfo_ViewModel(application: Application) : AndroidViewModel(application) {

    private val registrationManager = RegistrationManager(application.applicationContext)
    private val globalState = (application as FractalApplication).globalState

    private val _deviceInfo = MutableLiveData<Registered_DTO>()
    val deviceInfo: LiveData<Registered_DTO> get() = _deviceInfo

    // Cache the Firebase data so we don't spam the server
    private var cachedUsername = "Loading..."
    private var cachedEmail = "Loading..."
    private var cachedJoinedOn = "Loading..."

    // A background job to listen for email verification live
    private var pollingJob: Job? = null

    fun refreshFirebaseData() {
        viewModelScope.launch(Dispatchers.IO) {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            var isVerified = false

            if (user != null) {
                try {
                    // Check verification status
                    com.google.android.gms.tasks.Tasks.await(user.reload())
                    isVerified = user.isEmailVerified
                } catch (e: Exception) {
                    // Ignore transient network errors
                }
            }

            // Get the base data from Server_DAO
            val rawData = registrationManager.generateNewRegistrationData()
            val serverDao = globalState.server ?: Server_DAO()
            val finalData = serverDao.GET_RegisteredInfo(rawData)

            if (user != null && !isVerified) {
                cachedUsername = "Unverified User"
                cachedEmail = "Action Required: Verify Email"
                cachedJoinedOn = "N/A"

                // If they are unverified, start quietly checking Firebase every 3 seconds
                startPollingForVerification()
            } else {
                cachedUsername = finalData.username
                cachedEmail = finalData.email
                cachedJoinedOn = finalData.joinedOn

                // If they are fully verified (or logged out), ensure polling is stopped
                pollingJob?.cancel()
            }

            // Force an immediate UI update so it doesn't wait for the next 2-second tick
            val immediateUpdate = registrationManager.generateNewRegistrationData()
            immediateUpdate.username = cachedUsername
            immediateUpdate.email = cachedEmail
            immediateUpdate.joinedOn = cachedJoinedOn
            _deviceInfo.postValue(immediateUpdate)
        }
    }

    private fun startPollingForVerification() {
        // Prevent launching multiple loops if one is already running
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            val auth = FirebaseAuth.getInstance()

            while (true) {
                delay(3000) // Check every 3 seconds

                val user = auth.currentUser ?: break // Stop if they log out

                try {
                    com.google.android.gms.tasks.Tasks.await(user.reload())
                    if (user.isEmailVerified) {
                        // The exact second they verify, refresh the data to grab their real email!
                        refreshFirebaseData()
                        break // Kill the loop
                    }
                } catch (e: Exception) {
                    // Ignore transient network drops
                }
            }
        }
    }

    fun startLiveHardwareUpdates() {
        registrationManager.startLiveUpdates { liveHardwareData ->
            liveHardwareData.username = cachedUsername
            liveHardwareData.email = cachedEmail
            liveHardwareData.joinedOn = cachedJoinedOn
            _deviceInfo.postValue(liveHardwareData)
        }
    }

    fun stopLiveHardwareUpdates() {
        registrationManager.stopLiveUpdates()
        // CRITICAL: Stop the Firebase check when they leave the screen to save battery
        pollingJob?.cancel()
    }
}