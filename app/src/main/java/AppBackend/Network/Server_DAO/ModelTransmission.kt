package AppBackend.Network.Server_DAO

import AppBackend.Network.ModelUpdateTransmission.ModelTransmission_DTO

interface ModelTransmission {
    fun POST_UploadModelToServer(modeltransmissionDto: ModelTransmission_DTO):Boolean
}