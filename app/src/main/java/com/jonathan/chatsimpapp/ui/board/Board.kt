package com.jonathan.chatsimpapp.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.Post
import com.jonathan.chatsimpapp.data.repository.AuthRepository
import com.jonathan.chatsimpapp.data.repository.ChatRepository
import com.jonathan.chatsimpapp.data.repository.CommunityContentRepository
import com.jonathan.chatsimpapp.data.repository.CommunityContext
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.ui.community.Pill
import com.jonathan.chatsimpapp.ui.community.postTypeIcon
import com.jonathan.chatsimpapp.ui.community.postTypeLabel
import com.jonathan.chatsimpapp.ui.components.CenteredLoading
import com.jonathan.chatsimpapp.ui.components.EmptyState
import com.jonathan.chatsimpapp.util.TimeFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BoardUiState(
    val loading: Boolean = true,
    val context: CommunityContext? = null,
    val posts: List<Post> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BoardViewModel @Inject constructor(
    private val session: CommunitySession,
    private val content: CommunityContentRepository,
    private val chats: ChatRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    val state: StateFlow<BoardUiState> = session.current.flatMapLatest { ctx ->
        if (ctx == null) flowOf(BoardUiState())
        else content.observePosts(ctx.id).catch { emit(emptyList()) }.map { BoardUiState(false, ctx, it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BoardUiState())

    private val _openChat = MutableStateFlow<String?>(null)
    val openChat: StateFlow<String?> = _openChat.asStateFlow()

    /** "Escribir" abre un chat privado con quien publicó, sin compartir números de teléfono. */
    fun contact(post: Post) {
        val me = auth.currentUserId ?: return
        viewModelScope.launch {
            runCatching { chats.getOrCreateDirectChat(me, post.authorId) }.onSuccess { _openChat.value = it }
        }
    }

    fun onChatOpened() {
        _openChat.value = null
    }

    fun setResolved(post: Post, resolved: Boolean) = launch { content.setPostResolved(it.id, post.id, resolved) }
    fun delete(post: Post) = launch { content.deletePost(it.id, post.id) }

    private fun launch(block: suspend (CommunityContext) -> Unit) {
        viewModelScope.launch { runCatching { block(session.current.filterNotNull().first()) } }
    }
}

@Composable
fun BoardTab(
    state: BoardUiState,
    onContact: (Post) -> Unit,
    onResolve: (Post, Boolean) -> Unit,
    onDelete: (Post) -> Unit,
    contentPadding: PaddingValues,
) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val ctx = state.context
    when {
        state.loading || ctx == null -> CenteredLoading(Modifier.fillMaxSize())
        state.posts.isEmpty() -> EmptyState(Icons.Rounded.Storefront, stringResource(R.string.no_posts_title), stringResource(R.string.no_posts_body), Modifier.fillMaxSize())
        else -> LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(filter == null, onClick = { filter = null }, label = { Text(stringResource(R.string.all)) }) }
                    items(Post.TYPES) { t ->
                        FilterChip(filter == t, onClick = { filter = t }, label = { Text(stringResource(postTypeLabel(t))) })
                    }
                }
            }
            items(state.posts.filter { filter == null || it.type == filter }, key = { it.id }) { post ->
                PostCard(
                    post,
                    mine = post.authorId == ctx.uid,
                    canModerate = ctx.isAdmin,
                    onContact = { onContact(post) },
                    onResolve = { onResolve(post, !post.resolved) },
                    onDelete = { onDelete(post) },
                )
            }
        }
    }
}

@Composable
private fun PostCard(post: Post, mine: Boolean, canModerate: Boolean, onContact: () -> Unit, onResolve: () -> Unit, onDelete: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.large, color = c.surfaceContainerLow, modifier = Modifier.alpha(if (post.resolved) 0.6f else 1f)) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(stringResource(postTypeLabel(post.type)), c.secondaryContainer, c.onSecondaryContainer, icon = postTypeIcon(post.type))
                Spacer(Modifier.width(8.dp))
                Text(
                    listOfNotNull(
                        post.unit,
                        post.createdAt?.toDate()?.time?.let { TimeFormat.chatListLabel(it, stringResource(R.string.yesterday)) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = c.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (mine || canModerate) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), Modifier.size(20.dp)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(post.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
            post.photoUrl?.let {
                Spacer(Modifier.height(10.dp))
                AsyncImage(
                    it, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp).height(180.dp).clip(MaterialTheme.shapes.medium).background(c.surfaceVariant),
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (post.resolved) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(16.dp), tint = c.tertiary)
                    Text(" " + stringResource(R.string.post_resolved), style = MaterialTheme.typography.labelMedium, color = c.tertiary, modifier = Modifier.weight(1f))
                }
                if (mine) {
                    TextButton(onClick = onResolve) { Text(stringResource(if (post.resolved) R.string.reopen else R.string.mark_resolved)) }
                } else if (!post.resolved) {
                    FilledTonalButton(onClick = onContact) {
                        Icon(Icons.AutoMirrored.Rounded.Chat, null, Modifier.size(18.dp))
                        Text("  " + stringResource(R.string.contact))
                    }
                }
            }
        }
    }
}
