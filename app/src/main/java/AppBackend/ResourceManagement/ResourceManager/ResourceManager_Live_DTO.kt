package AppBackend.ResourceManagement.ResourceManager

class ResourceManager_Live_DTO (
    var cpuPercentage: Int,
    var gpuPercentage: Int,
    var romPercentage: Int,
    var ramPercentage: Int,
    var temprature: Int,
    var onCharging: Boolean,
    var batteryPercentage: Int,
    var wifi: Boolean,
    var cellular: Boolean
){
    fun updateStatistics(){

    }

    fun optimizeResources(){

    }
}