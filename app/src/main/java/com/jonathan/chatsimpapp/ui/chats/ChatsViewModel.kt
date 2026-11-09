package com.jonathan.chatsimpapp.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonathan.chatsimpapp.data.model.Chat
import com.jonathan.chatsimpapp.data.model.Presence
import com.jonathan.chatsimpapp.data.model.User
import com.jonathan.chatsimpapp.data.repository.AuthRepository
import com.jonathan.chatsimpapp.data.repository.ChatRepository
import com.jonathan.chatsimpapp.data.repository.PresenceRepository
import com.jonathan.chatsimpapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Fila ya lista para pintar en la lista de chats. */
data class ChatRow(
    val id: String,
    val title: String,
    val photoUrl: String?,
    val isGroup: Boolean,
    val colorKey: String,
    val preview: String?,
    val previewIsMine: Boolean,
    val previewSender: String?,
    val previewHasImage: Boolean,
    val timeMillis: Long?,
    val unread: Long,
    val online: Boolean,
)

data class ChatsUiState(
    val loading: Boolean = true,
    val me: User? = null,
    val rows: List<ChatRow> = emptyList(),
    val query: String = "",
) {
    val visibleRows: List<ChatRow>
        get() = if (query.isBlank()) rows else rows.filter { it.title.contains(query.trim(), ignoreCase = true) }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatsViewModel @Inject constructor(
    auth: AuthRepository,
    chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val presenceRepository: PresenceRepository,
) : ViewModel() {

    private val myUid = auth.currentUserId.orEmpty()
    private val query = MutableStateFlow("")

    private val chats: Flow<List<Chat>> = chatRepository.observeChats(myUid).catch { emit(emptyList()) }

    /** Perfiles de todas las personas que aparecen en la lista. */
    private val people: Flow<Map<String, User>> = chats
        .map { list -> list.flatMap { chat -> chat.members }.toSet() + myUid }
        .distinctUntilChanged()
        .flatMapLatest { ids -> userRepository.observeUsers(ids).catch { emit(emptyMap()) } }

    /** Presencia del otro miembro de cada chat directo. */
    private val presence: Flow<Map<String, Presence>> = chats
        .map { list -> list.filterNot { it.isGroup }.mapNotNull { it.otherMember(myUid) }.toSet() }
        .distinctUntilChanged()
        .flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(ids.map { id -> presenceRepository.observePresence(id).map { id to it } }) { it.toMap() }
            }
        }

    val state: StateFlow<ChatsUiState> = combine(chats, people, presence, query) { chats, people, presence, query ->
        ChatsUiState(
            loading = false,
            me = people[myUid],
            rows = chats.map { it.toRow(people, presence) },
            query = query,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatsUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    private fun Chat.toRow(people: Map<String, User>, presence: Map<String, Presence>): ChatRow {
        val other = otherMember(myUid)?.let { people[it] }
        val last = lastMessage
        return ChatRow(
            id = id,
            title = if (isGroup) name.orEmpty() else other?.displayName.orEmpty(),
            photoUrl = if (isGroup) photoUrl else other?.photoUrl,
            isGroup = isGroup,
            colorKey = if (isGroup) id else other?.uid ?: id,
            preview = last?.text,
            previewIsMine = last?.senderId == myUid,
            previewSender = if (isGroup && last != null && last.senderId != myUid) {
                people[last.senderId]?.displayName?.substringBefore(' ')
            } else null,
            previewHasImage = last?.hasImage == true,
            timeMillis = (last?.sentAt ?: updatedAt)?.toDate()?.time,
            unread = unread[myUid] ?: 0,
            online = !isGroup && other != null && presence[other.uid]?.online == true,
        )
    }
}
