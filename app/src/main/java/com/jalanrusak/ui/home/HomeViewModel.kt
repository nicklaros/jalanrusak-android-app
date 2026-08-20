package com.jalanrusak.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jalanrusak.data.api.dto.TopAreaResponse
import com.jalanrusak.data.api.dto.TopAreasListResponse
import com.jalanrusak.domain.usecase.GetTopAreasUseCase
import com.jalanrusak.domain.usecase.LoginUseCase
import com.jalanrusak.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getTopAreasUseCase: GetTopAreasUseCase,
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _showLoginPrompt = MutableStateFlow(false)
    val showLoginPrompt: StateFlow<Boolean> = _showLoginPrompt.asStateFlow()

    sealed class HomeUiState {
        object Idle : HomeUiState()
        object Loading : HomeUiState()
        data class Success(val data: List<TopAreaResponse>) : HomeUiState()
        data class Error(val message: String) : HomeUiState()
    }

    init {
        checkLoginStatus()
        loadTopAreas()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoggedIn.value = loginUseCase.isLoggedIn()
            if (_isLoggedIn.value) {
                _userEmail.value = loginUseCase.getUserEmail()
            }
        }
    }

    fun loadTopAreas(level: String = "city") {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            when (val result = getTopAreasUseCase(level)) {
                is Result.Success -> {
                    _uiState.value = HomeUiState.Success(result.data.data)
                }
                is Result.Error -> {
                    _uiState.value = HomeUiState.Error(result.message)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            loginUseCase.logout()
            _userEmail.value = null
            _isLoggedIn.value = false
        }
    }

    fun onReportNowClicked() {
        if (_isLoggedIn.value) {
            // Logged in - will be handled by Activity
            _showLoginPrompt.value = false
        } else {
            // Not logged in - show login prompt
            _showLoginPrompt.value = true
        }
    }

    fun clearLoginPrompt() {
        _showLoginPrompt.value = false
    }

    fun refreshLoginStatus() {
        checkLoginStatus()
    }
}
