package com.jonathan.chatsimpapp.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.ui.components.PrimaryButton

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ForgotPasswordContent(state, viewModel::onEmailChange, viewModel::send, onBack, viewModel::onMessageShown)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordContent(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedContent(targetState = state.sentTo, label = "forgot") { sentTo ->
                Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
                    Spacer(Modifier.height(8.dp))
                    if (sentTo == null) {
                        AuthHeader(
                            title = stringResource(R.string.reset_password_title),
                            subtitle = stringResource(R.string.reset_password_subtitle),
                        )
                        Spacer(Modifier.height(32.dp))
                        AuthTextField(
                            value = state.email,
                            onValueChange = onEmailChange,
                            label = stringResource(R.string.email),
                            leadingIcon = Icons.Rounded.Mail,
                            error = state.emailError,
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                            onImeAction = onSend,
                            enabled = !state.loading,
                        )
                        Spacer(Modifier.height(24.dp))
                        PrimaryButton(stringResource(R.string.send_link), onClick = onSend, loading = state.loading)
                    } else {
                        Box(
                            Modifier.size(80.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.MarkEmailRead,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        AuthHeader(
                            title = stringResource(R.string.reset_password_title),
                            subtitle = stringResource(R.string.reset_password_sent, sentTo),
                        )
                        Spacer(Modifier.height(32.dp))
                        PrimaryButton(stringResource(R.string.back_to_sign_in), onClick = onBack)
                    }
                }
            }
        }
    }
}
