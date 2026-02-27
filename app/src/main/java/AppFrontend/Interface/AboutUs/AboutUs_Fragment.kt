package AppFrontend.Interface.AboutUs

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fractal.R
import com.example.fractal.databinding.FragmentAboutUsBinding

class AboutUs_Fragment : Fragment() {

    private var _binding: FragmentAboutUsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutUsBinding.inflate(inflater, container, false)

        // Add Underline to "Join Us"
        binding.tvJoinUs.paintFlags = binding.tvJoinUs.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Back Button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Join Us Button
        binding.tvJoinUs.setOnClickListener {
            joinUs()
        }
    }

    fun joinUs() {
        // Navigate to the Device Authorization (Register) Screen
        // Ensure this ID matches your navigation graph
        findNavController().navigate(R.id.navigation_device_auth)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}