package AppFrontend.Interface.Auth.ForgetPassword

data class ForgetPassword_DTO (
    var email: String,
    var confirmationCode: String,
    var newPassword: String
)