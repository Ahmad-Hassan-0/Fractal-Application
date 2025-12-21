package AppGlobal

import AppBackend.Factory.PackageTypeTrainer.PackageTypeTrainer
import AppBackend.Factory.PackageTypeTrainer.PackageTypeTrainerBuilder
import AppBackend.Network.RegisteredInfo.RegistrationManager
import AppBackend.Network.Server_DAO.Server_DAO
import AppBackend.ResourceManagement.ResourceManager.ResourceManager_Live_DTO
import AppBackend.ResourceManagement.ResourceManager.ResourceManager_Record_DTO
import AppBackend.ResourceManagement.ResourceManager.ResourceStatistics
import AppBackend.TaskContainer.Task
import AppFrontend.Flush.Flusher

class GlobalState(
    var appConfig: app_config? = null,
    var server: Server_DAO? = null,
    var registrationManager: RegistrationManager? = null,
    var currentTask: Task? = null,
    var packageTypeTrainerBuilder: PackageTypeTrainerBuilder? = null,
    var packageTypeTrainer: PackageTypeTrainer? = null,
    var flusher: Flusher? = null,
    var resourceStatistics: ResourceStatistics? = null,
    var resourceManager_Live_DTO: ResourceManager_Live_DTO? = null,
    var resourceManager_Record_DTO: ResourceManager_Record_DTO? = null
){

    fun pingStatusCalculatorAndDispatcher(taskID: String){

    }
}