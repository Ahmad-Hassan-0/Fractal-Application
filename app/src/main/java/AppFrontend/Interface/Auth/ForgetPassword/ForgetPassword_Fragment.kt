package AppFrontend.Interface.Auth.ForgetPassword

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fractal.databinding.FragmentForgetPasswordBinding

class ForgetPassword_Fragment : Fragment() {

    private var _binding: FragmentForgetPasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ForgetPassword_ViewModel
    private var isEmailSent = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ForgetPassword_ViewModel::class.java]

        setupInitialUI()
        setupClickListeners()
        setupObservers()
    }

    private fun setupInitialUI() {
        // Disable and grey out the lower fields to guide the user to the email box
        binding.etCode.isEnabled = false
        binding.etCode.alpha = 0.6f
        binding.etCode.setText("Waiting for email...")

        binding.etNewPassword.isEnabled = false
        binding.etNewPassword.alpha = 0.6f
        binding.etNewPassword.setText("Waiting for email...")
    }

    private fun setupObservers() {
        viewModel.resetStatus.observe(viewLifecycleOwner) { status ->
            if (status == "Sending...") {
                binding.btnRecover.isEnabled = false
                binding.btnSendEmail.isEnabled = false
                binding.btnRecover.text = "Processing..."
            } else if (status == "Success: Link Sent") {
                Toast.makeText(context, "Reset link sent to your inbox!", Toast.LENGTH_LONG).show()
                binding.btnRecover.isEnabled = true
                binding.btnRecover.text = "Back to Login"
            } else {
                Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                unlockEmailField() // Re-enable if the email was invalid so they can fix it
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // The new explicit "Send Email" arrow button
        binding.btnSendEmail.setOnClickListener {
            if (!isEmailSent) {
                attemptSendEmail()
            }
        }

        binding.btnRecover.setOnClickListener {
            if (isEmailSent) {
                // If it's already sent, button takes them back to Login
                findNavController().navigateUp()
            } else {
                // If they click the big Recover button instead of the little arrow, just send the email
                attemptSendEmail()
            }
        }
    }

    private fun attemptSendEmail() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        lockAndSend(email)
    }

    private fun lockAndSend(email: String) {
        isEmailSent = true

        // 1. Lock the email input UI
        binding.etEmail.isEnabled = false
        binding.etEmail.alpha = 0.6f
        binding.btnSendEmail.isEnabled = false
        binding.btnSendEmail.alpha = 0.6f

        // 2. Auto-populate the disabled fields with success text
        binding.etCode.setText("Link securely sent to inbox")
        binding.etNewPassword.setText("Check your email to reset")

        // 3. Fire the ViewModel request
        val dto = ForgetPassword_DTO(email, "N/A", "N/A")
        viewModel.recover(dto)
    }

    private fun unlockEmailField() {
        isEmailSent = false

        binding.etEmail.isEnabled = true
        binding.etEmail.alpha = 1.0f
        binding.btnSendEmail.isEnabled = true
        binding.btnSendEmail.alpha = 1.0f

        binding.etCode.setText("Waiting for email...")
        binding.etNewPassword.setText("Waiting for email...")

        binding.btnRecover.text = "Recover"
        binding.btnRecover.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}