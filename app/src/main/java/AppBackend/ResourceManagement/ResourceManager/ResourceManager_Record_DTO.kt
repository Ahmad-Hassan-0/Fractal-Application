package AppBackend.ResourceManagement.ResourceManager

import android.text.format.Time
import java.time.Instant

data class ResourceManager_Record_DTO (
    var deviceStandbyTime: Instant,
    var networkStandbyTime: Instant,
    var networkOnlineStartTime: Instant,
    var networkOnlineEndTime: Instant,
    var powerConsuption: Int,
    var tempStorage: Float,
    var backgroundTasksRunning: Int
)