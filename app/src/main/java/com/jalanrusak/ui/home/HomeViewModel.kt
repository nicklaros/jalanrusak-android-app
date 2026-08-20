package com.jalanrusak.ui.home

import android.content.Context
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

    companion object {
        private const val PREFS_NAME = "home_prefs"
        private const val KEY_LEVEL = "selected_level"
        private const val DEFAULT_LEVEL = "city"
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _showLoginPrompt = MutableStateFlow(false)
    val showLoginPrompt: StateFlow<Boolean> = _showLoginPrompt.asStateFlow()

    private var currentLevel = DEFAULT_LEVEL

    sealed class HomeUiState {
        object Idle : HomeUiState()
        object Loading : HomeUiState()
        data class Success(val data: List<TopAreaResponse>) : HomeUiState()
        data class Error(val message: String) : HomeUiState()
    }

    init {
        // Load saved level (will be set from Activity context later)
        checkLoginStatus()
    }

    fun initialize(context: Context) {
        currentLevel = getSavedLevel(context)
        loadTopAreas(currentLevel)
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoggedIn.value = loginUseCase.isLoggedIn()
            if (_isLoggedIn.value) {
                _userEmail.value = loginUseCase.getUserEmail()
            }
        }
    }

    fun loadTopAreas(level: String = DEFAULT_LEVEL) {
        currentLevel = level
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
            _showLoginPrompt.value = false
        } else {
            _showLoginPrompt.value = true
        }
    }

    fun clearLoginPrompt() {
        _showLoginPrompt.value = false
    }

    fun refreshLoginStatus() {
        checkLoginStatus()
    }

    fun getSavedLevel(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LEVEL, DEFAULT_LEVEL) ?: DEFAULT_LEVEL
    }

    fun saveLevel(context: Context, level: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LEVEL, level)
            .apply()
    }
}
