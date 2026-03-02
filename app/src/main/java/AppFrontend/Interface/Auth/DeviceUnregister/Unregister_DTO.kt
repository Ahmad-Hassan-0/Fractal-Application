package AppFrontend.Interface.Auth.DeviceUnregister

data class Unregister_DTO (
    var problemTitle: String,
    var description: String,
    var screenshots: List<ByteArray>,
    var wantsToUnregister: Boolean // NEW FLAG
)