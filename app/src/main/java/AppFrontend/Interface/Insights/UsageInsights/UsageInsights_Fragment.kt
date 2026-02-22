package AppFrontend.Interface.Insights.UsageInsights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fractal.databinding.FragmentDeviceBinding
import kotlin.random.Random
import android.util.Log
import org.json.JSONObject
import java.io.File

class UsageInsights_Fragment : Fragment() {

    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    // Target stats that will be randomly generated
    private val targetStats = mutableMapOf(
        "CPU" to 0.90f,
        "GPU" to 0.80f,
        "ROM" to 0.85f,
        "RAM" to 0.75f,
        "TEMP" to 0.70f
    )

    private var animationRunning = false
    private val statsFileName = "resource_stats.json"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceBinding.inflate(inflater, container, false)
        Log.e("UsageInsights", "Fragment created")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("UsageInsights", "View created - starting systems")

        // Start both systems
        startStatsGenerator()
        startStatsReader()
    }

    /**
     * SYSTEM 1: Continuously generates random stats and saves to JSON
     */
    private fun startStatsGenerator() {
        view?.postDelayed(object : Runnable {
            override fun run() {
                if (_binding == null) return

                // Generate new random target values
                targetStats.keys.forEach { key ->
                    if (Random.nextFloat() < 0.3f) { // 30% chance to change
                        targetStats[key] = when (key) {
                            "CPU" -> Random.nextFloat().coerceIn(0.25f, 0.95f)
                            "GPU" -> Random.nextFloat().coerceIn(0.20f, 0.90f)
                            "ROM" -> Random.nextFloat().coerceIn(0.50f, 0.95f)
                            "RAM" -> Random.nextFloat().coerceIn(0.35f, 0.95f)
                            "TEMP" -> Random.nextFloat().coerceIn(0.40f, 0.85f)
                            else -> Random.nextFloat()
                        }
                    }
                }

                // Save to JSON file
                saveStatsToJson(targetStats)

                // Continue the loop - update every 100ms
                view?.postDelayed(this, 100)
            }
        }, 100)

        Log.e("UsageInsights", "Stats generator started")
    }

    /**
     * SYSTEM 2: Continuously reads from JSON and updates the chart
     */
    private fun startStatsReader() {
        view?.postDelayed(object : Runnable {
            override fun run() {
                if (_binding == null) return

                // Read stats from JSON file
                val stats = readStatsFromJson()

                if (stats.isNotEmpty()) {
                    // Update the chart view
                    binding.resourceChart.updateStats(stats)
                    Log.d("UsageInsights", "Chart updated with: $stats")
                }

                // Continue the loop - read every 50ms for smooth animation
                view?.postDelayed(this, 50)
            }
        }, 50)

        Log.e("UsageInsights", "Stats reader started")
    }

    private fun saveStatsToJson(stats: Map<String, Float>) {
        try {
            val jsonObject = JSONObject()
            stats.forEach { (key, value) ->
                jsonObject.put(key, value)
            }

            val file = File(requireContext().filesDir, statsFileName)
            file.writeText(jsonObject.toString())

            Log.d("UsageInsights", "Saved to JSON: ${jsonObject.toString()}")
        } catch (e: Exception) {
            Log.e("UsageInsights", "Error saving stats: ${e.message}")
        }
    }

    private fun readStatsFromJson(): Map<String, Float> {
        return try {
            val file = File(requireContext().filesDir, statsFileName)

            if (!file.exists()) {
                Log.w("UsageInsights", "JSON file doesn't exist yet")
                return emptyMap()
            }

            val jsonString = file.readText()
            val jsonObject = JSONObject(jsonString)

            val stats = mutableMapOf<String, Float>()
            jsonObject.keys().forEach { key ->
                stats[key] = jsonObject.getDouble(key).toFloat()
            }

            stats
        } catch (e: Exception) {
            Log.e("UsageInsights", "Error reading stats: ${e.message}")
            emptyMap()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.e("UsageInsights", "Fragment destroyed - cleaning up")

        // Clean up the JSON file
        try {
            val file = File(requireContext().filesDir, statsFileName)
            if (file.exists()) {
                file.delete()
                Log.e("UsageInsights", "JSON file deleted")
            }
        } catch (e: Exception) {
            Log.e("UsageInsights", "Error deleting file: ${e.message}")
        }

        _binding = null
    }
}