package AppBackend.Network.RegisteredInfo

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.net.NetworkInterface
import java.util.Collections

class RegistrationManager(private val context: Context) {

    fun generateNewRegistrationData(): Registered_DTO {
        return Registered_DTO(
            // Placeholders for Firebase data
            username = "whiteshadow69",
            email = "bted4389@gmail.com",
            joinedOn = "23rd Feb, 2004",

            // Live System Data
            platform = "Android",
            hardwareID = getHardwareId(),
            serialNumber = getSerial(),
            processor = getProcessorName(),
            storage = getTotalInternalMemorySize(),
            totalRam = getTotalRAM(),
            androidVersion = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            macAddress = getMacAddress()
        )
    }

    // --- HELPER FUNCTIONS ---

    private fun getHardwareId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            "Unknown ID"
        }
    }

    private fun getSerial(): String {
        return try {
            // Build.SERIAL is deprecated in newer Androids due to permissions,
            // usually returns "unknown". We use Build.MODEL as a reliable fallback for hardware id.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Build.MODEL // Returns e.g. "Pixel 6" or "Samsung S21"
            } else {
                Build.SERIAL
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getTotalRAM(): String {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalMemBytes = memInfo.totalMem
        return formatSize(totalMemBytes)
    }

    private fun getTotalInternalMemorySize(): String {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        return formatSize(totalBlocks * blockSize)
    }

    private fun getProcessorName(): String {
        // Attempt to read linux cpuinfo
        try {
            val br = BufferedReader(FileReader("/proc/cpuinfo"))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                if (line!!.contains("Hardware")) {
                    return line!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1].trim()
                }
            }
            br.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Build.BOARD // Fallback
    }

    private fun getMacAddress(): String {
        try {
            val all = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (nif in all) {
                if (!nif.name.equals("wlan0", ignoreCase = true)) continue
                val macBytes = nif.hardwareAddress ?: return "Unavailable"
                val res1 = StringBuilder()
                for (b in macBytes) {
                    res1.append(String.format("%02X:", b))
                }
                if (res1.isNotEmpty()) {
                    return res1.deleteCharAt(res1.length - 1).toString()
                }
            }
        } catch (ex: Exception) {
            return "Unavailable"
        }
        return "02:00:00:00:00:00" // Android 11+ often blocks this
    }

    private fun formatSize(size: Long): String {
        val kb = 1024L
        val mb = kb * 1024L
        val gb = mb * 1024L
        return when {
            size >= gb -> String.format("%.1f GB", size.toDouble() / gb)
            size >= mb -> String.format("%.1f MB", size.toDouble() / mb)
            else -> String.format("%.1f KB", size.toDouble() / kb)
        }
    }

    fun loadRegistrationData(): Registered_DTO {
        // Placeholder for loading saved JSON later
        return generateNewRegistrationData()
    }

    fun saveRegistrationData() {
        // Placeholder
    }
}