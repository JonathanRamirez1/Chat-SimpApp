package com.jonathan.chatsimpapp.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.repository.AuthRepository
import com.jonathan.chatsimpapp.data.repository.CommunityRepository
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.data.repository.InvalidInviteCodeException
import com.jonathan.chatsimpapp.ui.auth.AppLogo
import com.jonathan.chatsimpapp.ui.auth.AuthHeader
import com.jonathan.chatsimpapp.ui.auth.AuthTextField
import com.jonathan.chatsimpapp.ui.components.PrimaryButton
import com.jonathan.chatsimpapp.util.InviteCodes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep { CHOOSE, JOIN, CREATE }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.CHOOSE,
    val code: String = "",
    val unit: String = "",
    val name: String = "",
    val address: String = "",
    val unitsCount: String = "",
    val loading: Boolean = false,
    @param:StringRes val codeError: Int? = null,
    @param:StringRes val unitError: Int? = null,
    @param:StringRes val nameError: Int? = null,
    @param:StringRes val unitsError: Int? = null,
    @param:StringRes val message: Int? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val repository: CommunityRepository,
    private val session: CommunitySession,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun go(step: OnboardingStep) = _state.update { it.copy(step = step, message = null) }
    fun onCode(v: String) = _state.update { it.copy(code = v.uppercase().take(8), codeError = null) }
    fun onUnit(v: String) = _state.update { it.copy(unit = v.take(12), unitError = null) }
    fun onName(v: String) = _state.update { it.copy(name = v.take(60), nameError = null) }
    fun onAddress(v: String) = _state.update { it.copy(address = v.take(120)) }
    fun onUnitsCount(v: String) = _state.update { it.copy(unitsCount = v.filter(Char::isDigit).take(4), unitsError = null) }
    fun onMessageShown() = _state.update { it.copy(message = null) }

    fun join() {
        val s = _state.value
        val codeError = if (InviteCodes.isValid(s.code)) null else R.string.error_invite_code
        val unitError = if (s.unit.isNotBlank()) null else R.string.error_unit
        if (codeError != null || unitError != null) {
            _state.update { it.copy(codeError = codeError, unitError = unitError) }
            return
        }
        run { uid -> repository.join(uid, s.code, s.unit) }
    }

    fun create() {
        val s = _state.value
        val units = s.unitsCount.toIntOrNull()
        val nameError = if (s.name.isNotBlank()) null else R.string.error_community_name
        val unitsError = if (units != null && units in 2..5000) null else R.string.error_units_count
        val unitError = if (s.unit.isNotBlank()) null else R.string.error_unit
        if (nameError != null || unitsError != null || unitError != null) {
            _state.update { it.copy(nameError = nameError, unitsError = unitsError, unitError = unitError) }
            return
        }
        run { uid -> repository.create(uid, s.name, s.address, units!!, s.unit) }
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    private fun run(block: suspend (String) -> String) {
        val uid = auth.currentUserId ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { block(uid) }
                .onSuccess { cid -> session.select(cid) }
                .onFailure { e ->
                    val msg = when (e) {
                        is InvalidInviteCodeException -> R.string.error_invite_not_found
                        is FirebaseNetworkException -> R.string.error_network
                        else -> R.string.error_generic
                    }
                    _state.update { it.copy(loading = false, message = msg) }
                }
        }
    }
}

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingContent(
        state, viewModel::go, viewModel::onCode, viewModel::onUnit, viewModel::onName, viewModel::onAddress,
        viewModel::onUnitsCount, viewModel::join, viewModel::create, viewModel::signOut, viewModel::onMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingContent(
    state: OnboardingUiState,
    onStep: (OnboardingStep) -> Unit,
    onCode: (String) -> Unit,
    onUnit: (String) -> Unit,
    onName: (String) -> Unit,
    onAddress: (String) -> Unit,
    onUnitsCount: (String) -> Unit,
    onJoin: () -> Unit,
    onCreate: () -> Unit,
    onSignOut: () -> Unit,
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
                    if (state.step != OnboardingStep.CHOOSE) {
                        IconButton(onClick = { onStep(OnboardingStep.CHOOSE) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) { Icon(Icons.AutoMirrored.Rounded.Logout, stringResource(R.string.sign_out)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(state.step, label = "onboarding") { step ->
                Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
                    when (step) {
                        OnboardingStep.CHOOSE -> {
                            AppLogo()
                            Spacer(Modifier.height(28.dp))
                            AuthHeader(stringResource(R.string.onboarding_title), stringResource(R.string.onboarding_subtitle))
                            Spacer(Modifier.height(32.dp))
                            ChoiceCard(Icons.Rounded.Key, stringResource(R.string.join_with_code), stringResource(R.string.join_with_code_body)) {
                                onStep(OnboardingStep.JOIN)
                            }
                            Spacer(Modifier.height(12.dp))
                            ChoiceCard(Icons.Rounded.AddHome, stringResource(R.string.create_community), stringResource(R.string.create_community_body)) {
                                onStep(OnboardingStep.CREATE)
                            }
                        }
                        OnboardingStep.JOIN -> {
                            AuthHeader(stringResource(R.string.join_with_code), stringResource(R.string.join_subtitle))
                            Spacer(Modifier.height(28.dp))
                            AuthTextField(
                                state.code, onCode, stringResource(R.string.invite_code), Icons.Rounded.Key,
                                error = state.codeError, supporting = stringResource(R.string.invite_code_hint),
                                keyboardType = KeyboardType.Ascii, enabled = !state.loading,
                            )
                            Spacer(Modifier.height(12.dp))
                            AuthTextField(
                                state.unit, onUnit, stringResource(R.string.your_unit), Icons.Rounded.MeetingRoom,
                                error = state.unitError, supporting = stringResource(R.string.your_unit_hint),
                                imeAction = ImeAction.Done, onImeAction = onJoin, enabled = !state.loading,
                            )
                            Spacer(Modifier.height(24.dp))
                            PrimaryButton(stringResource(R.string.join), onJoin, loading = state.loading)
                        }
                        OnboardingStep.CREATE -> {
                            AuthHeader(stringResource(R.string.create_community), stringResource(R.string.create_subtitle))
                            Spacer(Modifier.height(28.dp))
                            AuthTextField(state.name, onName, stringResource(R.string.community_name), Icons.Rounded.Apartment, error = state.nameError, enabled = !state.loading)
                            Spacer(Modifier.height(12.dp))
                            AuthTextField(state.address, onAddress, stringResource(R.string.address_optional), Icons.Rounded.LocationOn, enabled = !state.loading)
                            Spacer(Modifier.height(12.dp))
                            AuthTextField(
                                state.unitsCount, onUnitsCount, stringResource(R.string.units_count), Icons.Rounded.Numbers,
                                error = state.unitsError, supporting = stringResource(R.string.units_count_hint),
                                keyboardType = KeyboardType.Number, enabled = !state.loading,
                            )
                            Spacer(Modifier.height(12.dp))
                            AuthTextField(
                                state.unit, onUnit, stringResource(R.string.your_unit), Icons.Rounded.MeetingRoom,
                                error = state.unitError, imeAction = ImeAction.Done, onImeAction = onCreate, enabled = !state.loading,
                            )
                            Spacer(Modifier.height(24.dp))
                            PrimaryButton(stringResource(R.string.create_community), onCreate, loading = state.loading)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp).size(26.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
