package AppFrontend.Interface.Auth.ForgetPassword

import android.util.Log
import androidx.lifecycle.ViewModel

class ForgetPassword_ViewModel : ViewModel() {

    fun recover(dto: ForgetPassword_DTO) {
        // Logic to send new password and code to server
        Log.d("ForgetPassVM", "Recovering for email: ${dto.email} with code: ${dto.confirmationCode}")

        // TODO: Implement server call
        // val serverDao = GlobalState.server...
        // serverDao.POST_Forgetpassword_verifyEmail(...)
    }
}