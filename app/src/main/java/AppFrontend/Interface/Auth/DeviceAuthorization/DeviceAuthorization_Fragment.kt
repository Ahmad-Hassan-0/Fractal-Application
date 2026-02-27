package AppFrontend.Interface.Auth.DeviceAuthorization

import android.graphics.Paint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fractal.R
import com.example.fractal.databinding.FragmentDeviceAuthorizationBinding

class DeviceAuthorization_Fragment : Fragment() {

    private var _binding: FragmentDeviceAuthorizationBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DeviceAuthorization_ViewModel
    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceAuthorizationBinding.inflate(inflater, container, false)

        // Underline "Forget Password"
        binding.tvForgetPassword.paintFlags = binding.tvForgetPassword.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        // Underline "Terms And Conditions"
        binding.tvTermsLink.paintFlags = binding.tvTermsLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        // FORCE FONT ON INIT: Ensure Genos is used even though inputType="password"
        fixPasswordFont()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[DeviceAuthorization_ViewModel::class.java]
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnTogglePassword.setOnClickListener { togglePasswordVisibility() }
        binding.btnLoginRegister.setOnClickListener { button_LoginRegister() }

        // Make text clickable for checkbox too
        binding.tvTermsLink.setOnClickListener { binding.cbTerms.isChecked = !binding.cbTerms.isChecked }

        // --- CORRECTED PLACEMENT ---
        // This acts as the permanent listener for the Forget Password link
        binding.tvForgetPassword.setOnClickListener {
            // Make sure this ID matches your mobile_navigation.xml
            findNavController().navigate(R.id.navigation_forget_password)
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // HIDE PASSWORD
            binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_hidden)
            isPasswordVisible = false
        } else {
            // SHOW PASSWORD
            binding.etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_visibility)

            // REMOVED: Do not set the listener here! It belongs in setupClickListeners.

            isPasswordVisible = true
        }

        // CRITICAL FIX: Re-apply the Genos font because Android resets it to Monospace
        fixPasswordFont()

        // Keep cursor at end
        binding.etPassword.setSelection(binding.etPassword.text.length)
    }

    private fun fixPasswordFont() {
        // Load your custom Genos Regular font
        val customTypeface = ResourcesCompat.getFont(requireContext(), R.font.genos_regular)
        binding.etPassword.typeface = customTypeface
    }

    fun button_LoginRegister() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val agreedToTerms = binding.cbTerms.isChecked

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!agreedToTerms) {
            Toast.makeText(context, "You must agree to the Terms and Conditions", Toast.LENGTH_SHORT).show()
            return
        }

        val loginData = LoginRegister_DTO(username, email, password)
        viewModel.loginRegister(loginData)
        Toast.makeText(context, "Sending Request...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}