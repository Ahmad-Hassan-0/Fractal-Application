package AppFrontend.Interface.Auth.ForgetPassword

data class ForgetPassword_DTO (
    var password: String,
    var email: String,
    var confirmationCode: String
)