package AppBackend.Network.Server_DAO

import AppBackend.Network.RegisteredInfo.Registered_DTO
import AppFrontend.Interface.Auth.DeviceAuthorization.LoginRegister_DTO
import AppFrontend.Interface.Auth.DeviceUnregister.Unregister_DTO
import AppFrontend.Interface.Auth.ForgetPassword.ForgetPassword_DTO

interface Auth {
    fun POST_RegisterLogin(registeredDto: Registered_DTO, loginregisterDto: LoginRegister_DTO): Boolean
    fun GET_RegisteredInfo(registeredDto: Registered_DTO): Registered_DTO
    fun POST_ForgetPassword_sendEmail(registeredDto: Registered_DTO):Boolean
    fun POST_Forgetpassword_verifyEmail(registeredDto: Registered_DTO, forgetpasswordDto: ForgetPassword_DTO): Boolean
    fun POST_Unregister(registeredDto: Registered_DTO, unregisterDto: Unregister_DTO): Boolean
}