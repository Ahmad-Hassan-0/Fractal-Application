package AppFrontend.Interface.Auth.DeviceUnregister

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fractal.databinding.FragmentDeviceUnregisterBinding
import java.io.ByteArrayOutputStream

class DeviceUnregister_Fragment : Fragment() {

    private var _binding: FragmentDeviceUnregisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DeviceUnregister_ViewModel

    // Store selected image data here
    private val selectedScreenshots = mutableListOf<ByteArray>()

    // 1. Define the Image Picker Contract
    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        if (uris.isNullOrEmpty()) {
            Toast.makeText(context, "No images selected", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        // Clear previous selection if you want fresh picks every time
        selectedScreenshots.clear()

        // Convert URIs to ByteArrays
        var successCount = 0
        uris.forEach { uri ->
            val bytes = uriToByteArray(uri)
            if (bytes != null) {
                selectedScreenshots.add(bytes)
                successCount++
            }
        }

        // Update UI Status
        binding.tvAttachmentStatus.text = "$successCount Image(s) Attached"
        binding.tvAttachmentStatus.setTextColor(resources.getColor(android.R.color.black, null))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceUnregisterBinding.inflate(inflater, container, false)

        // Underline Terms Link
        binding.tvTermsLink.paintFlags = binding.tvTermsLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[DeviceUnregister_ViewModel::class.java]
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // 2. Launch Image Picker on Click
        binding.btnAttach.setOnClickListener {
            pickImagesLauncher.launch("image/*") // Filter for images only
        }

        binding.tvTermsLink.setOnClickListener {
            binding.cbTerms.isChecked = !binding.cbTerms.isChecked
        }

        binding.btnUnregister.setOnClickListener {
            button_unregister()
        }
    }

    fun button_unregister() {
        val title = binding.etProblemTitle.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()
        val agreedToTerms = binding.cbTerms.isChecked

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(context, "Please provide a title and description", Toast.LENGTH_SHORT).show()
            return
        }

        if (!agreedToTerms) {
            Toast.makeText(context, "You must agree to the Terms and Conditions", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Create DTO with actual image data
        val dto = Unregister_DTO(
            problemTitle = title,
            description = desc,
            screenshots = selectedScreenshots.toList() // Pass the populated list
        )

        viewModel.unregister(dto)
        Toast.makeText(context, "Unregister Request Sent", Toast.LENGTH_SHORT).show()
    }

    // --- Helper: Convert Uri to ByteArray ---
    private fun uriToByteArray(uri: Uri): ByteArray? {
        return try {
            val context = requireContext()
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            // Compress bitmap to PNG or JPEG byte array
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream) // 70% quality to save space
            stream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}