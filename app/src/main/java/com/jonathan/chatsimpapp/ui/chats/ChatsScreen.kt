package com.jonathan.chatsimpapp.ui.chats

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.ui.components.Avatar
import com.jonathan.chatsimpapp.ui.components.CenteredLoading
import com.jonathan.chatsimpapp.ui.components.EmptyState
import com.jonathan.chatsimpapp.ui.components.PresenceAvatar
import com.jonathan.chatsimpapp.ui.components.SearchField
import com.jonathan.chatsimpapp.ui.components.UnreadBadge
import com.jonathan.chatsimpapp.util.TimeFormat

@Composable
fun ChatsScreen(
    onOpenChat: (String) -> Unit,
    onNewChat: () -> Unit,
    onProfile: () -> Unit,
    viewModel: ChatsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Android 13+ pide permiso explícito para mostrar notificaciones.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    ChatsContent(state, viewModel::onQueryChange, onOpenChat, onNewChat, onProfile)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsContent(
    state: ChatsUiState,
    onQueryChange: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onNewChat: () -> Unit,
    onProfile: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val profileLabel = stringResource(R.string.profile)
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chats), style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = onProfile) {
                        Avatar(
                            name = state.me?.displayName.orEmpty(),
                            photoUrl = state.me?.photoUrl,
                            colorKey = state.me?.uid.orEmpty(),
                            size = 36.dp,
                            modifier = Modifier.semantics { contentDescription = profileLabel },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (state.rows.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onNewChat,
                    icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    text = { Text(stringResource(R.string.new_chat)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> CenteredLoading(Modifier.fillMaxSize().padding(padding))
            state.rows.isEmpty() -> EmptyState(
                icon = Icons.Rounded.ChatBubbleOutline,
                title = stringResource(R.string.no_chats_title),
                body = stringResource(R.string.no_chats_body),
                modifier = Modifier.fillMaxSize().padding(padding),
                action = {
                    FilledTonalButton(onClick = onNewChat) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.start_chat))
                    }
                },
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 96.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    SearchField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        placeholder = stringResource(R.string.search_chats),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                val rows = state.visibleRows
                if (rows.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.SearchOff,
                            title = stringResource(R.string.no_results),
                            body = "",
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        )
                    }
                }
                items(rows, key = { it.id }) { row ->
                    ChatRowItem(row, onClick = { onOpenChat(row.id) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun ChatRowItem(row: ChatRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasUnread = row.unread > 0
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PresenceAvatar(
            name = row.title,
            photoUrl = row.photoUrl,
            online = row.online,
            isGroup = row.isGroup,
            colorKey = row.colorKey,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.title.ifBlank { stringResource(R.string.unknown_user) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                row.timeMillis?.let {
                    Text(
                        text = TimeFormat.chatListLabel(it, stringResource(R.string.yesterday)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Preview(row, hasUnread) }
                if (hasUnread) {
                    Spacer(Modifier.width(8.dp))
                    val label = stringResource(R.string.unread_count, row.unread.toInt())
                    UnreadBadge(row.unread, Modifier.semantics { contentDescription = label })
                }
            }
        }
    }
}

@Composable
private fun Preview(row: ChatRow, emphasize: Boolean) {
    val color = if (emphasize) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val weight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal
    val base = when {
        row.preview.isNullOrBlank() && row.previewHasImage -> stringResource(R.string.photo)
        row.preview.isNullOrBlank() -> ""
        else -> row.preview
    }
    val text = when {
        row.previewIsMine -> stringResource(R.string.you_prefix, base)
        row.previewSender != null -> "${row.previewSender}: $base"
        else -> base
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (row.previewHasImage) {
            Icon(Icons.Rounded.Image, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = weight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
