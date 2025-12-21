package AppBackend.Network.RegisteredInfo

data class Registered_DTO (
    var username: String,
    var email: String,
    var joinedOn: String,
    var platform: String,
    var hardwareID: String,
    var serialNumber: String,
    var processor: String,
    var storage: Int,
    var totalRam: Int,
    var androidVersion: String,
    var macAddress: String
)