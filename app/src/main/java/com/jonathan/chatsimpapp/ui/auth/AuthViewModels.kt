package com.jonathan.chatsimpapp.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val loading: Boolean = false,
    @param:StringRes val message: Int? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onMessageShown() = _state.update { it.copy(message = null) }
    fun onGoogleStarted() = _state.update { it.copy(loading = true) }
    fun onGoogleFailed(@StringRes message: Int?) = _state.update { it.copy(loading = false, message = message) }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            runCatching { auth.signInWithGoogle(idToken) }
                .onFailure { e -> _state.update { it.copy(loading = false, message = e.toAuthMessage().takeIf { m -> m != R.string.error_generic } ?: R.string.error_google) } }
        }
    }
}
