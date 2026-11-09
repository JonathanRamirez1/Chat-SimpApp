package com.jonathan.chatsimpapp.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jonathan.chatsimpapp.BuildConfig
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.User
import com.jonathan.chatsimpapp.data.repository.AuthRepository
import com.jonathan.chatsimpapp.data.repository.UserRepository
import com.jonathan.chatsimpapp.ui.components.Avatar
import com.jonathan.chatsimpapp.ui.components.PrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val name: String = "",
    val about: String = "",
    val saving: Boolean = false,
    val uploadingPhoto: Boolean = false,
    @param:StringRes val message: Int? = null,
) {
    val dirty: Boolean get() = user != null && (name.trim() != user.displayName || about.trim() != user.about)
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val users: UserRepository,
) : ViewModel() {
    private val uid = auth.currentUserId.orEmpty()
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val first = users.observeUser(uid).filterNotNull().first()
            _state.update { it.copy(user = first, name = first.displayName, about = first.about) }
            users.observeUser(uid).filterNotNull().collect { u -> _state.update { it.copy(user = u) } }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value.take(40)) }
    fun onAboutChange(value: String) = _state.update { it.copy(about = value.take(140)) }
    fun onMessageShown() = _state.update { it.copy(message = null) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(message = R.string.error_name_empty) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            runCatching { users.updateProfile(uid, s.name, s.about) }
                .onSuccess { _state.update { it.copy(saving = false, message = R.string.saved) } }
                .onFailure { _state.update { it.copy(saving = false, message = R.string.error_generic) } }
        }
    }

    fun changePhoto(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(uploadingPhoto = true) }
            runCatching { users.updatePhoto(uid, uri) }
                .onFailure { _state.update { it.copy(message = R.string.error_upload) } }
            _state.update { it.copy(uploadingPhoto = false) }
        }
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}

@Composable
fun ProfileScreen(onBack: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let(viewModel::changePhoto)
    }
    ProfileContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onAboutChange = viewModel::onAboutChange,
        onSave = viewModel::save,
        onChangePhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onSignOut = viewModel::signOut,
        onBack = onBack,
        onMessageShown = viewModel::onMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    state: ProfileUiState,
    onNameChange: (String) -> Unit,
    onAboutChange: (String) -> Unit,
    onSave: () -> Unit,
    onChangePhoto: () -> Unit,
    onSignOut: () -> Unit,
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
    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.clickable(onClick = onChangePhoto)) {
                Avatar(
                    name = state.user?.displayName.orEmpty(),
                    photoUrl = state.user?.photoUrl,
                    colorKey = state.user?.uid.orEmpty(),
                    size = 120.dp,
                )
                if (state.uploadingPhoto) {
                    Box(Modifier.size(120.dp).background(colors.scrim.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.onPrimary)
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(38.dp)
                        .border(3.dp, colors.surface, CircleShape)
                        .background(colors.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = stringResource(R.string.change_photo), tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(state.user?.displayName.orEmpty(), style = MaterialTheme.typography.headlineSmall)
            Text(state.user?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))

            ProfileField(state.name, onNameChange, stringResource(R.string.name), Icons.Rounded.Person)
            Spacer(Modifier.height(12.dp))
            ProfileField(
                state.about,
                onAboutChange,
                stringResource(R.string.about),
                Icons.Rounded.Info,
                placeholder = stringResource(R.string.about_placeholder),
                singleLine = false,
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton(stringResource(R.string.save), onClick = onSave, enabled = state.dirty, loading = state.saving)

            Spacer(Modifier.height(32.dp))
            Text(
                stringResource(R.string.account),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
            )
            Surface(shape = MaterialTheme.shapes.large, color = colors.surfaceContainerLow) {
                Column {
                    InfoRow(Icons.Rounded.Mail, stringResource(R.string.email), state.user?.email.orEmpty())
                    InfoRow(
                        Icons.AutoMirrored.Rounded.Logout,
                        stringResource(R.string.sign_out),
                        null,
                        tint = colors.error,
                        onClick = onSignOut,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = colors.outline,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String? = null,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = { Icon(icon, contentDescription = null) },
        singleLine = singleLine,
        maxLines = 3,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (onClick != null) tint else MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
