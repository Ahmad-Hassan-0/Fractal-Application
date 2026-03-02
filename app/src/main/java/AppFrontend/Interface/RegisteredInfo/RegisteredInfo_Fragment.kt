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

        viewModel = ViewModelProvider(requireActivity())[RegisteredInfo_ViewModel::class.java]

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.deviceInfo.observe(viewLifecycleOwner) { info ->
            updateUiStats(info)
        }
    }

    // FIX 2: Trigger refresh AND start the loop every time the screen is opened
    override fun onResume() {
        super.onResume()
        viewModel.refreshFirebaseData()
        viewModel.startLiveHardwareUpdates()
    }

    // Stop the loop when the user leaves the screen to save battery
    override fun onPause() {
        super.onPause()
        viewModel.stopLiveHardwareUpdates()
    }

    private fun updateUiStats(info: AppBackend.Network.RegisteredInfo.Registered_DTO) {
        binding.tvUsername.text = "Username: ${info.username}"
        binding.tvEmail.text = "Email: ${info.email}"
        binding.tvJoinedOn.text = "Joined On: ${info.joinedOn}"

        binding.tvPlatform.text = info.platform

        binding.tvHardwareId.text = "Hardware ID: ${info.hardwareID}"
        binding.tvSerialNumber.text = "Serial/Model: ${info.serialNumber}"
        binding.tvProcessor.text = "Processor: ${info.processor}"
        binding.tvStorage.text = "Storage: ${info.storage}"
        binding.tvRam.text = "Total RAM: ${info.totalRam}"

        binding.tvAndroidVersion.text = "Android Version: ${info.androidVersion}"
        binding.tvMacAddress.text = "MAC Address: ${info.macAddress}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}