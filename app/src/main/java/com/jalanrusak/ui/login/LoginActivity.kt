package com.jalanrusak.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jalanrusak.JalanRusakApp
import com.jalanrusak.R
import com.jalanrusak.databinding.ActivityLoginBinding
import com.jalanrusak.ui.home.HomeActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels {
        // TODO: Inject dependencies properly
        LoginViewModelFactory((application as JalanRusakApp).provideLoginUseCase())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeState()
    }

    private fun setupUI() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text?.toString()?.trim() ?: ""
            val password = binding.passwordInput.text?.toString() ?: ""

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan password harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginViewModel.LoginUiState.Idle -> {
                            // Do nothing
                        }
                        is LoginViewModel.LoginUiState.Loading -> {
                            binding.loginButton.isEnabled = false
                            binding.loginButton.text = "Memproses..."
                        }
                        is LoginViewModel.LoginUiState.Success -> {
                            binding.loginButton.isEnabled = true
                            binding.loginButton.text = getString(R.string.login_button)
                            Toast.makeText(this@LoginActivity, "Berhasil masuk!", Toast.LENGTH_SHORT).show()
                            goToHome()
                        }
                        is LoginViewModel.LoginUiState.Error -> {
                            binding.loginButton.isEnabled = true
                            binding.loginButton.text = getString(R.string.login_button)
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoggedIn.collect { isLoggedIn ->
                    if (isLoggedIn) {
                        goToHome()
                    }
                }
            }
        }
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}

// Temporary factory - TODO: Replace with proper DI
class LoginViewModelFactory(
    private val loginUseCase: com.jalanrusak.domain.usecase.LoginUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LoginViewModel(loginUseCase) as T
    }
}
