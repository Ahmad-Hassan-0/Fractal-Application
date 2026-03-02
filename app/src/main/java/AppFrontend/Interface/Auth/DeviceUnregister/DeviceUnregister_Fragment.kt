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
import com.example.fractal.R
import com.example.fractal.databinding.FragmentDeviceUnregisterBinding
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream

class DeviceUnregister_Fragment : Fragment() {

    private var _binding: FragmentDeviceUnregisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DeviceUnregister_ViewModel
    private val selectedScreenshots = mutableListOf<ByteArray>()

    // NEW: A listener to instantly catch when the user logs out from the sidebar
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        if (uris.isNullOrEmpty()) {
            Toast.makeText(context, "No images selected", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        selectedScreenshots.clear()

        val safeUris = if (uris.size > 3) {
            Toast.makeText(context, "Maximum 3 images allowed. Truncating selection.", Toast.LENGTH_LONG).show()
            uris.take(3)
        } else {
            uris
        }

        var successCount = 0
        safeUris.forEach { uri ->
            val bytes = uriToByteArray(uri)
            if (bytes != null) {
                selectedScreenshots.add(bytes)
                successCount++
            }
        }

        binding.tvAttachmentStatus.text = "$successCount Image(s) Attached"
        binding.tvAttachmentStatus.setTextColor(resources.getColor(android.R.color.black, null))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceUnregisterBinding.inflate(inflater, container, false)
        binding.tvTermsLink.paintFlags = binding.tvTermsLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[DeviceUnregister_ViewModel::class.java]

        setupClickListeners()
        setupObservers()
    }

    // --- LIVE AUTH LISTENER FIX ---
    override fun onStart() {
        super.onStart()
        // Attach the listener so it watches for logouts in real-time
        authStateListener = FirebaseAuth.AuthStateListener {
            enforceAuthenticationUI()
        }
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener!!)
    }

    override fun onStop() {
        super.onStop()
        // Prevent memory leaks by removing the listener when the fragment is hidden
        authStateListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
    }

    override fun onResume() {
        super.onResume()
        enforceAuthenticationUI()
    }

    private fun enforceAuthenticationUI() {
        // Safe call just in case the binding was destroyed
        if (_binding == null) return

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            binding.layoutUnregister.visibility = View.VISIBLE
            binding.btnSubmitFeedback.text = "Submit & Unregister"
        } else {
            binding.layoutUnregister.visibility = View.GONE
            binding.cbUnregister.isChecked = false
            binding.btnSubmitFeedback.text = "Submit Feedback"
        }
    }

    private fun setupObservers() {
        viewModel.unregisterStatus.observe(viewLifecycleOwner) { status ->
            if (status == "Processing...") {
                binding.btnSubmitFeedback.isEnabled = false
                binding.btnSubmitFeedback.text = "Uploading Feedback..."
            } else if (status == "Success") {

                if (binding.cbUnregister.isChecked) {
                    Toast.makeText(context, "Feedback Submitted & Device Unregistered.", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.navigation_device_auth)
                } else {
                    Toast.makeText(context, "Feedback Submitted Successfully.", Toast.LENGTH_LONG).show()
                    findNavController().navigateUp()
                }

            } else {
                Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                binding.btnSubmitFeedback.isEnabled = true
                enforceAuthenticationUI()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnAttach.setOnClickListener { pickImagesLauncher.launch("image/*") }
        binding.tvTermsLink.setOnClickListener { binding.cbTerms.isChecked = !binding.cbTerms.isChecked }
        binding.layoutUnregister.setOnClickListener { binding.cbUnregister.isChecked = !binding.cbUnregister.isChecked }
        binding.btnSubmitFeedback.setOnClickListener { button_unregister() }
    }

    fun button_unregister() {
        val title = binding.etProblemTitle.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()
        val agreedToTerms = binding.cbTerms.isChecked
        val wantsToUnregister = binding.cbUnregister.isChecked

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(context, "Please provide a title and description", Toast.LENGTH_SHORT).show()
            return
        }

        if (!agreedToTerms) {
            Toast.makeText(context, "You must agree to the Terms and Conditions", Toast.LENGTH_SHORT).show()
            return
        }

        val dto = Unregister_DTO(
            problemTitle = title,
            description = desc,
            screenshots = selectedScreenshots.toList(),
            wantsToUnregister = wantsToUnregister
        )

        viewModel.unregister(dto)
    }

    private fun uriToByteArray(uri: Uri): ByteArray? {
        return try {
            val context = requireContext()
            val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            val maxSize = 800
            var width = originalBitmap.width
            var height = originalBitmap.height
            val bitmapRatio = width.toFloat() / height.toFloat()
            if (bitmapRatio > 1) {
                width = maxSize
                height = (width / bitmapRatio).toInt()
            } else {
                height = maxSize
                width = (height * bitmapRatio).toInt()
            }
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

            val stream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
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