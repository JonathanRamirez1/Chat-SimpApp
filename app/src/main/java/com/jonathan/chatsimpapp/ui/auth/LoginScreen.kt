package com.jonathan.chatsimpapp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.ui.components.PrimaryButton
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSignIn = viewModel::signIn,
        onGoogle = {
            viewModel.onGoogleStarted()
            scope.launch {
                when (val result = requestGoogleIdToken(context)) {
                    is GoogleSignInResult.Success -> viewModel.signInWithGoogle(result.idToken)
                    GoogleSignInResult.Cancelled -> viewModel.onGoogleFailed(null)
                    is GoogleSignInResult.Failure -> viewModel.onGoogleFailed(R.string.error_google)
                }
            }
        },
        onRegister = onRegister,
        onForgotPassword = onForgotPassword,
        onMessageShown = viewModel::onMessageShown,
    )
}

@Composable
fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onGoogle: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.let { stringResource(it) }
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            onMessageShown()
        }
    }
    val busy = state.loading || state.googleLoading

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
                Spacer(Modifier.height(56.dp))
                AppLogo()
                Spacer(Modifier.height(32.dp))
                AuthHeader(
                    title = stringResource(R.string.sign_in_title),
                    subtitle = stringResource(R.string.app_tagline),
                )
                Spacer(Modifier.height(32.dp))

                AuthTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = stringResource(R.string.email),
                    leadingIcon = Icons.Rounded.Mail,
                    error = state.emailError,
                    keyboardType = KeyboardType.Email,
                    enabled = !busy,
                )
                Spacer(Modifier.height(12.dp))
                AuthTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = stringResource(R.string.password),
                    leadingIcon = Icons.Rounded.Lock,
                    error = state.passwordError,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onImeAction = onSignIn,
                    isPassword = true,
                    enabled = !busy,
                )
                TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.forgot_password))
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    text = stringResource(R.string.sign_in),
                    onClick = onSignIn,
                    loading = state.loading,
                    enabled = !state.googleLoading,
                )
                Spacer(Modifier.height(24.dp))
                OrDivider()
                Spacer(Modifier.height(24.dp))
                GoogleButton(onClick = onGoogle, loading = state.googleLoading, enabled = !state.loading)
                Spacer(Modifier.height(32.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.no_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onRegister, enabled = !busy) { Text(stringResource(R.string.sign_up)) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
