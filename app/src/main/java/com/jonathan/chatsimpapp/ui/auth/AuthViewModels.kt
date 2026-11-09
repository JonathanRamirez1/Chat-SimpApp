package com.jonathan.chatsimpapp.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.repository.AuthRepository
import com.jonathan.chatsimpapp.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    val loading: Boolean = false,
    val googleLoading: Boolean = false,
    @param:StringRes val message: Int? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null) }
    fun onMessageShown() = _state.update { it.copy(message = null) }

    fun signIn() {
        val s = _state.value
        val emailError = if (Validators.isValidEmail(s.email)) null else R.string.error_email_invalid
        val passwordError = if (s.password.isNotEmpty()) null else R.string.error_password_short
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { auth.signIn(s.email, s.password) }
                .onFailure { e -> _state.update { it.copy(loading = false, message = e.toAuthMessage()) } }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(googleLoading = true) }
            runCatching { auth.signInWithGoogle(idToken) }
                .onFailure { _state.update { it.copy(googleLoading = false, message = R.string.error_google) } }
        }
    }

    fun onGoogleStarted() = _state.update { it.copy(googleLoading = true) }
    fun onGoogleFailed(@StringRes message: Int?) = _state.update { it.copy(googleLoading = false, message = message) }
}

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    @param:StringRes val nameError: Int? = null,
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    val loading: Boolean = false,
    @param:StringRes val message: Int? = null,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null) }
    fun onMessageShown() = _state.update { it.copy(message = null) }

    fun register() {
        val s = _state.value
        val nameError = if (Validators.isValidName(s.name)) null else R.string.error_name_empty
        val emailError = if (Validators.isValidEmail(s.email)) null else R.string.error_email_invalid
        val passwordError = if (Validators.isValidPassword(s.password)) null else R.string.error_password_short
        if (nameError != null || emailError != null || passwordError != null) {
            _state.update { it.copy(nameError = nameError, emailError = emailError, passwordError = passwordError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { auth.register(s.name, s.email, s.password) }
                .onFailure { e -> _state.update { it.copy(loading = false, message = e.toAuthMessage()) } }
        }
    }
}

data class ForgotPasswordUiState(
    val email: String = "",
    @param:StringRes val emailError: Int? = null,
    val loading: Boolean = false,
    val sentTo: String? = null,
    @param:StringRes val message: Int? = null,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null) }
    fun onMessageShown() = _state.update { it.copy(message = null) }

    fun send() {
        val email = _state.value.email.trim()
        if (!Validators.isValidEmail(email)) {
            _state.update { it.copy(emailError = R.string.error_email_invalid) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { auth.sendPasswordReset(email) }
                .onSuccess { _state.update { it.copy(loading = false, sentTo = email) } }
                .onFailure { e -> _state.update { it.copy(loading = false, message = e.toAuthMessage()) } }
        }
    }
}
