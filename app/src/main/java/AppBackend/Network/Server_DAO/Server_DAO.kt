package AppBackend.Network.Server_DAO

import AppBackend.Network.ModelUpdateTransmission.ModelTransmission_DTO
import AppBackend.Network.RegisteredInfo.Registered_DTO
import AppBackend.TaskContainer.Task
import AppFrontend.Interface.Auth.DeviceAuthorization.LoginRegister_DTO
import AppFrontend.Interface.Auth.DeviceUnregister.Unregister_DTO
import AppFrontend.Interface.Auth.ForgetPassword.ForgetPassword_DTO

class Server_DAO: Auth, ModelTransmission, TaskPopulate {
    fun POST_Ping(taskID: String, pingStatus: Boolean){

    }

    override fun POST_RegisterLogin(
        registeredDto: Registered_DTO,
        loginregisterDto: LoginRegister_DTO
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun GET_RegisteredInfo(registeredDto: Registered_DTO): Registered_DTO {
        TODO("Not yet implemented")
    }

    override fun POST_ForgetPassword_sendEmail(registeredDto: Registered_DTO): Boolean {
        TODO("Not yet implemented")
    }

    override fun POST_Forgetpassword_verifyEmail(
        registeredDto: Registered_DTO,
        forgetpasswordDto: ForgetPassword_DTO
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun POST_Unregister(
        registeredDto: Registered_DTO,
        unregisterDto: Unregister_DTO
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun POST_UploadModelToServer(modeltransmissionDto: ModelTransmission_DTO): Boolean {
        TODO("Not yet implemented")
    }

    override fun GET_Task(flushPrevious: Boolean): Task {
        TODO("Not yet implemented")
    }
}