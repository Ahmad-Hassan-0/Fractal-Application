//package AppFrontend.Interface.RegisteredInfo
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import androidx.navigation.fragment.findNavController
//import com.example.fractal.databinding.FragmentRegisteredInfoBinding // Update if your package name is different
//
//class RegisteredInfo_Fragment : Fragment() {
//
//    // Safely implement ViewBinding
//    private var _binding: FragmentRegisteredInfoBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var registeredInfoViewModel: RegisteredInfo_ViewModel
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentRegisteredInfoBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // Initialize ViewModel
//        registeredInfoViewModel = ViewModelProvider(this)[RegisteredInfo_ViewModel::class.java]
//
//        // Handle Back Button Click
//        binding.btnBack.setOnClickListener {
//            findNavController().navigateUp()
//        }
//
//        // Call your UI updater
//        update_ui_stats()
//    }
//
//    fun update_ui_stats() {
//        // You will eventually pull this data from registeredInfoViewModel.
//        // For now, they act as placeholders overwriting the XML defaults.
//
//        binding.tvUsername.text = "Username: whiteshadow69"
//        binding.tvEmail.text = "Email: bted4389@gmail.com"
//        binding.tvJoinedOn.text = "Joined On: 23rd Feb, 2004"
//
//        binding.tvPlatform.text = "Android"
//
//        binding.tvHardwareId.text = "Hardware ID: 203-2930"
//        binding.tvSerialNumber.text = "Serial Number: 203-2930"
//        binding.tvProcessor.text = "Processor: Snapdragon 4.2"
//        binding.tvStorage.text = "Storage: 64GB"
//        binding.tvRam.text = "Total RAM: 4GB"
//
//        binding.tvAndroidVersion.text = "Android Version: 7.1 Oreo"
//
//        binding.tvMacAddress.text = "MAC Address: 04-2D-91-98-32-0A"
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        // Prevent memory leaks
//        _binding = null
//    }
//}

package AppFrontend.Interface.RegisteredInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fractal.databinding.FragmentRegisteredInfoBinding

class RegisteredInfo_Fragment : Fragment() {

    private var _binding: FragmentRegisteredInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: RegisteredInfo_ViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisteredInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[RegisteredInfo_ViewModel::class.java]

        // Handle Back Button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Observe Live Data
        viewModel.deviceInfo.observe(viewLifecycleOwner) { info ->
            updateUiStats(info)
        }

        // Refresh data just in case
        viewModel.loadDeviceData()
    }

    private fun updateUiStats(info: AppBackend.Network.RegisteredInfo.Registered_DTO) {
        // User Info (Placeholders)
        binding.tvUsername.text = "Username: ${info.username}"
        binding.tvEmail.text = "Email: ${info.email}"
        binding.tvJoinedOn.text = "Joined On: ${info.joinedOn}"

        // Platform
        binding.tvPlatform.text = info.platform

        // Hardware Info
        binding.tvHardwareId.text = "Hardware ID: ${info.hardwareID}"
        binding.tvSerialNumber.text = "Serial/Model: ${info.serialNumber}"
        binding.tvProcessor.text = "Processor: ${info.processor}"
        binding.tvStorage.text = "Storage: ${info.storage}"
        binding.tvRam.text = "Total RAM: ${info.totalRam}"

        // Software Info
        binding.tvAndroidVersion.text = "Android Version: ${info.androidVersion}"

        // Network Info
        binding.tvMacAddress.text = "MAC Address: ${info.macAddress}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}