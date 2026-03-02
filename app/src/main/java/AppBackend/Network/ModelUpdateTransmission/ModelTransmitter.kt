package AppBackend.Network.ModelUpdateTransmission

import android.util.Log
import AppBackend.Network.Server_DAO.Server_DAO

class ModelTransmitter {
    private val TAG = "ModelTransmitter"

    fun transmitModel(modeltransmissionDto: ModelTransmission_DTO): Boolean {
        Log.i(TAG, "Preparing to transmit model for Task ID: ${modeltransmissionDto.task.task_Id}")

        val serverDao = Server_DAO()
        return serverDao.POST_UploadModelToServer(modeltransmissionDto)
    }
}