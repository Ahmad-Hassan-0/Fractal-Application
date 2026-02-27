package AppFrontend.Interface.Auth.ForgetPassword

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
import com.example.fractal.databinding.FragmentForgetPasswordBinding

class ForgetPassword_Fragment : Fragment() {

    private var _binding: FragmentForgetPasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ForgetPassword_ViewModel
    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgetPasswordBinding.inflate(inflater, container, false)

        // Force font on init
        fixPasswordFont()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ForgetPassword_ViewModel::class.java]
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Back Button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Toggle Password Visibility
        binding.btnTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        // Recover Button
        binding.btnRecover.setOnClickListener {
            button_recover()
        }
    }

    fun button_recover() {
        val email = binding.etEmail.text.toString().trim()
        val code = binding.etCode.text.toString().trim()
        val newPass = binding.etNewPassword.text.toString().trim()

        if (email.isEmpty() || code.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val dto = ForgetPassword_DTO(email, code, newPass)
        viewModel.recover(dto)

        Toast.makeText(context, "Recovery Request Sent", Toast.LENGTH_SHORT).show()
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // HIDE PASSWORD
            binding.etNewPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_hidden)
            isPasswordVisible = false
        } else {
            // SHOW PASSWORD
            binding.etNewPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_visibility)
            isPasswordVisible = true
        }

        // Re-apply Genos font
        fixPasswordFont()

        // Keep cursor at end
        binding.etNewPassword.setSelection(binding.etNewPassword.text?.length ?: 0)
    }

    private fun fixPasswordFont() {
        val customTypeface = ResourcesCompat.getFont(requireContext(), R.font.genos_regular)
        binding.etNewPassword.typeface = customTypeface
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}