package com.jonathan.chatsimpapp.ui.group

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.User
import com.jonathan.chatsimpapp.data.repository.AuthRepository
import com.jonathan.chatsimpapp.data.repository.ChatRepository
import com.jonathan.chatsimpapp.data.repository.UserRepository
import com.jonathan.chatsimpapp.ui.components.Avatar
import com.jonathan.chatsimpapp.ui.components.SearchField
import com.jonathan.chatsimpapp.ui.newchat.PersonRow
import com.jonathan.chatsimpapp.ui.newchat.SearchState
import com.jonathan.chatsimpapp.ui.newchat.searchUsers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewGroupUiState(
    val step: Int = 0,
    val selected: List<User> = emptyList(),
    val name: String = "",
    val photo: Uri? = null,
    val creating: Boolean = false,
    val createdChatId: String? = null,
)

@HiltViewModel
class NewGroupViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val chats: ChatRepository,
    users: UserRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val search: StateFlow<SearchState> = _query.searchUsers(users, auth.currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    private val _state = MutableStateFlow(NewGroupUiState())
    val state: StateFlow<NewGroupUiState> = _state.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun toggle(user: User) = _state.update { s ->
        val selected = if (s.selected.any { it.uid == user.uid }) s.selected.filterNot { it.uid == user.uid } else s.selected + user
        s.copy(selected = selected)
    }

    fun next() = _state.update { if (it.selected.isNotEmpty()) it.copy(step = 1) else it }
    fun back() = _state.update { it.copy(step = 0) }
    fun onNameChange(value: String) = _state.update { it.copy(name = value.take(60)) }
    fun onPhoto(uri: Uri?) = _state.update { it.copy(photo = uri) }

    fun create() {
        val s = _state.value
        val me = auth.currentUserId ?: return
        if (s.name.isBlank() || s.selected.isEmpty() || s.creating) return
        viewModelScope.launch {
            _state.update { it.copy(creating = true) }
            runCatching { chats.createGroup(me, s.name, s.selected.map { it.uid }, s.photo) }
                .onSuccess { id -> _state.update { it.copy(createdChatId = id) } }
                .onFailure { _state.update { it.copy(creating = false) } }
        }
    }
}

@Composable
fun NewGroupScreen(onBack: () -> Unit, onCreated: (String) -> Unit, viewModel: NewGroupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { viewModel.onPhoto(it) }

    LaunchedEffect(state.createdChatId) { state.createdChatId?.let(onCreated) }
    BackHandler(enabled = state.step == 1) { viewModel.back() }

    NewGroupContent(
        state = state,
        query = query,
        search = search,
        onQueryChange = viewModel::onQueryChange,
        onToggle = viewModel::toggle,
        onNext = viewModel::next,
        onNameChange = viewModel::onNameChange,
        onPickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onCreate = viewModel::create,
        onBack = { if (state.step == 1) viewModel.back() else onBack() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupContent(
    state: NewGroupUiState,
    query: String,
    search: SearchState,
    onQueryChange: (String) -> Unit,
    onToggle: (User) -> Unit,
    onNext: () -> Unit,
    onNameChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(if (state.step == 0) R.string.add_members else R.string.group_details))
                        Text(
                            if (state.step == 0) stringResource(R.string.selected_count, state.selected.size)
                            else stringResource(R.string.members_count, state.selected.size + 1),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            val enabled = if (state.step == 0) state.selected.isNotEmpty() else state.name.isNotBlank()
            AnimatedVisibility(enabled) {
                FloatingActionButton(
                    onClick = if (state.step == 0) onNext else onCreate,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    when {
                        state.creating -> CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp),
                        )
                        state.step == 0 -> Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.next))
                        else -> Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.create_group))
                    }
                }
            }
        },
    ) { padding ->
        if (state.step == 0) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (state.selected.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.selected, key = { it.uid }) { user ->
                            InputChip(
                                selected = true,
                                onClick = { onToggle(user) },
                                label = { Text(user.displayName.substringBefore(' ')) },
                                avatar = { Avatar(user.displayName, user.photoUrl, size = InputChipDefaults.AvatarSize, colorKey = user.uid) },
                                trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.remove), Modifier.size(16.dp)) },
                                shape = CircleShape,
                            )
                        }
                    }
                }
                SearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.search_people),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(search.results, key = { it.uid }) { user ->
                        val checked = state.selected.any { it.uid == user.uid }
                        PersonRow(user, onClick = { onToggle(user) }, trailing = {
                            Checkbox(checked = checked, onCheckedChange = { onToggle(user) })
                        })
                    }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).imePadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onPickPhoto),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.photo != null) {
                        AsyncImage(state.photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(
                            Icons.Rounded.CameraAlt,
                            contentDescription = stringResource(R.string.add_group_photo),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.group_name)) },
                    leadingIcon = { Icon(Icons.Rounded.Groups, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    items(state.selected, key = { it.uid }) { user ->
                        Avatar(user.displayName, user.photoUrl, size = 40.dp, colorKey = user.uid)
                    }
                }
            }
        }
    }
}
