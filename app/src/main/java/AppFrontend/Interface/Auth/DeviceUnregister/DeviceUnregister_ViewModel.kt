package AppFrontend.Interface.Auth.DeviceUnregister

import android.util.Log
import androidx.lifecycle.ViewModel

class DeviceUnregister_ViewModel : ViewModel() {

    fun unregister(dto: Unregister_DTO) {
        Log.d("UnregisterVM", "Title: ${dto.problemTitle}, Desc: ${dto.description}")

        // TODO: Call Server_DAO to post unregistration request
        // val serverDao = GlobalState.server...
        // serverDao.POST_Unregister(...)
    }
}