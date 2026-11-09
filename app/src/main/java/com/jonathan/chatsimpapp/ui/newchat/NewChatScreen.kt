package com.jonathan.chatsimpapp.ui.newchat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.User
import com.jonathan.chatsimpapp.data.repository.AuthRepository
import com.jonathan.chatsimpapp.data.repository.ChatRepository
import com.jonathan.chatsimpapp.data.repository.UserRepository
import com.jonathan.chatsimpapp.ui.components.EmptyState
import com.jonathan.chatsimpapp.ui.components.SearchField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val chats: ChatRepository,
    users: UserRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val search: StateFlow<SearchState> = _query.searchUsers(users, auth.currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    private val _openChat = MutableStateFlow<String?>(null)
    val openChat: StateFlow<String?> = _openChat.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun startChat(user: User) {
        val me = auth.currentUserId ?: return
        viewModelScope.launch {
            runCatching { chats.getOrCreateDirectChat(me, user.uid) }.onSuccess { _openChat.value = it }
        }
    }

    fun onChatOpened() {
        _openChat.value = null
    }
}

@Composable
fun NewChatScreen(
    onBack: () -> Unit,
    onNewGroup: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: NewChatViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val openChat by viewModel.openChat.collectAsStateWithLifecycle()
    LaunchedEffect(openChat) {
        openChat?.let {
            viewModel.onChatOpened()
            onOpenChat(it)
        }
    }
    NewChatContent(query, search, viewModel::onQueryChange, viewModel::startChat, onNewGroup, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatContent(
    query: String,
    search: SearchState,
    onQueryChange: (String) -> Unit,
    onPick: (User) -> Unit,
    onNewGroup: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_chat)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.search_people),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (search.searching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                item { NewGroupRow(onNewGroup) }
                when {
                    !search.hasQuery -> item {
                        EmptyState(
                            icon = Icons.Rounded.PersonSearch,
                            title = stringResource(R.string.search_hint_title),
                            body = stringResource(R.string.search_hint_body),
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        )
                    }
                    !search.searching && search.results.isEmpty() -> item {
                        EmptyState(
                            icon = Icons.Rounded.SearchOff,
                            title = stringResource(R.string.no_results),
                            body = "",
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        )
                    }
                    else -> items(search.results, key = { it.uid }) { user ->
                        PersonRow(user, onClick = { onPick(user) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NewGroupRow(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(14.dp))
        Text(stringResource(R.string.new_group), style = MaterialTheme.typography.titleMedium)
    }
}
