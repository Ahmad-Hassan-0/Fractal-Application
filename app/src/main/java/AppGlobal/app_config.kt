package AppGlobal

data class app_config(
    var onWifi: Boolean = true,
    var onData: Boolean = true,
    var overNightUtilization: Boolean = false,
    var minChargeLimit: Boolean = false,
    var maxChargeLimit: Boolean = false,
    var onChargingExclusive: Boolean = false,
    var isLoggedIn: Boolean = false
)