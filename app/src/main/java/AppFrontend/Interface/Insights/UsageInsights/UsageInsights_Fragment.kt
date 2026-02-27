package AppFrontend.Interface.Insights.UsageInsights

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fractal.databinding.FragmentUsageBinding

class UsageInsights_Fragment : Fragment() {

    private var _binding: FragmentUsageBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: UsageInsights_ViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsageBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[UsageInsights_ViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe real-time data from ViewModel
        viewModel.liveStats.observe(viewLifecycleOwner) { stats ->
            if (stats.isNotEmpty() && _binding != null) {
                // Update the chart view
                binding.resourceChart.updateStats(stats)
                Log.d("UsageInsights", "Chart updated with REAL data: $stats")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Start polling when screen is visible
        viewModel.startHeartBeat()
    }

    override fun onPause() {
        super.onPause()
        // Stop polling to save battery when screen is hidden
        viewModel.stopHeartBeat()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}