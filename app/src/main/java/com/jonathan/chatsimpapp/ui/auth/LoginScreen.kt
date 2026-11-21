package com.jonathan.chatsimpapp.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jonathan.chatsimpapp.R
import kotlinx.coroutines.launch

/** Una sola forma de entrar: "Continuar con Google". */
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LoginContent(
        state = state,
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
        onMessageShown = viewModel::onMessageShown,
    )
}

@Composable
fun LoginContent(state: LoginUiState, onGoogle: () -> Unit, onMessageShown: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.let { stringResource(it) }
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            onMessageShown()
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
                Spacer(Modifier.height(72.dp))
                AppLogo()
                Spacer(Modifier.height(32.dp))
                AuthHeader(stringResource(R.string.sign_in_title), stringResource(R.string.app_tagline))
                Spacer(Modifier.height(48.dp))
                GoogleButton(onClick = onGoogle, loading = state.loading, enabled = true)
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.google_only_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
