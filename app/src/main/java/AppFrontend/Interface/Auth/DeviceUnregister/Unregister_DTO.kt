package AppFrontend.Interface.Auth.DeviceUnregister

data class Unregister_DTO (
    var problemTitle: String,
    var screenshots: List<ByteArray>,
    var description: String
)