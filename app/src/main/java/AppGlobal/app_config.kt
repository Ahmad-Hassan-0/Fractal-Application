package AppGlobal

data class app_config(
    var onWifi: Boolean = true,
    var onData: Boolean = false,
    var overNightUtilization: Boolean = false,
    var idleTimeUtilization: Boolean = true, // Added to match screenshot
    var minChargeLimit: Int = 34,            // Changed Boolean -> Int for Slider
    var maxChargeLimit: Boolean = false,
    var onChargingExclusive: Boolean = false,
    var isLoggedIn: Boolean = false
)