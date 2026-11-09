package com.jonathan.chatsimpapp.ui.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.Chat
import com.jonathan.chatsimpapp.data.model.Message
import com.jonathan.chatsimpapp.data.model.Presence
import com.jonathan.chatsimpapp.data.model.User
import com.jonathan.chatsimpapp.data.repository.AuthRepository
import com.jonathan.chatsimpapp.data.repository.ChatRepository
import com.jonathan.chatsimpapp.data.repository.PresenceRepository
import com.jonathan.chatsimpapp.data.repository.UserRepository
import com.jonathan.chatsimpapp.ui.navigation.ChatRoute
import com.jonathan.chatsimpapp.util.TimeFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MessageStatus { PENDING, SENT, READ }

sealed interface ChatItem {
    val key: String

    data class DateHeader(val millis: Long) : ChatItem {
        override val key = "date-$millis"
    }

    data class Bubble(
        val id: String,
        val text: String,
        val imageUrl: String?,
        val mine: Boolean,
        val senderName: String?,
        val senderPhoto: String?,
        val senderKey: String,
        val timeMillis: Long,
        val status: MessageStatus,
        /** Primer mensaje (el más viejo) de una racha del mismo remitente. */
        val firstInRun: Boolean,
        /** Último mensaje (el más nuevo) de una racha del mismo remitente. */
        val lastInRun: Boolean,
    ) : ChatItem {
        override val key = id
    }
}

sealed interface ChatSubtitle {
    data object None : ChatSubtitle
    data object Online : ChatSubtitle
    data class LastSeen(val millis: Long) : ChatSubtitle
    data class Typing(val name: String?) : ChatSubtitle
    data class Members(val names: List<String>) : ChatSubtitle
}

data class ChatUiState(
    val loading: Boolean = true,
    val title: String = "",
    val photoUrl: String? = null,
    val isGroup: Boolean = false,
    val colorKey: String = "",
    val subtitle: ChatSubtitle = ChatSubtitle.None,
    val items: List<ChatItem> = emptyList(),
    val canLoadMore: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    auth: AuthRepository,
    private val chats: ChatRepository,
    private val users: UserRepository,
    private val presence: PresenceRepository,
) : ViewModel() {

    val chatId: String = savedStateHandle.toRoute<ChatRoute>().chatId
    private val myUid = auth.currentUserId.orEmpty()

    private val limit = MutableStateFlow(PAGE_SIZE)
    private val visible = MutableStateFlow(false)

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading

    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error

    private val chat = chats.observeChat(chatId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val messages = limit
        .flatMapLatest { chats.observeMessages(chatId, it).catch { emit(emptyList()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val people = chat.filterNotNull()
        .map { it.members.toSet() }
        .distinctUntilChanged()
        .flatMapLatest { users.observeUsers(it).catch { emit(emptyMap()) } }

    private val otherPresence = chat.filterNotNull()
        .map { if (it.isGroup) null else it.otherMember(myUid) }
        .distinctUntilChanged()
        .flatMapLatest { uid -> if (uid == null) flowOf(null) else presence.observePresence(uid) }

    private val typing = presence.observeTyping(chatId).map { it - myUid }

    val state: StateFlow<ChatUiState> =
        combine(chat, messages, people, otherPresence, typing) { chat, messages, people, presence, typing ->
            if (chat == null || messages == null) return@combine ChatUiState()
            buildState(chat, messages, people, presence, typing)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    private var typingJob: Job? = null

    init {
        // Mientras el chat esté en pantalla, cada mensaje nuevo de otra persona se marca como leído.
        viewModelScope.launch {
            combine(visible, chat, messages) { isVisible, chat, msgs ->
                isVisible && chat != null && msgs != null && needsMarkRead(chat, msgs)
            }.distinctUntilChanged().collect { if (it) chats.markRead(chatId, myUid) }
        }
    }

    fun setVisible(isVisible: Boolean) {
        visible.value = isVisible
        if (!isVisible) stopTyping()
    }

    fun onInputChange(text: String) {
        _input.value = text
        if (text.isBlank()) {
            stopTyping()
            return
        }
        if (typingJob == null) presence.setTyping(chatId, myUid, true)
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(TYPING_TIMEOUT_MS)
            stopTyping()
        }
    }

    fun send() {
        val text = _input.value.trim()
        if (text.isEmpty()) return
        _input.value = ""
        stopTyping()
        viewModelScope.launch {
            val current = chat.value ?: chat.filterNotNull().first()
            runCatching { chats.sendMessage(current, myUid, text) }
                .onFailure { _error.value = R.string.error_send_message }
        }
    }

    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            _uploading.value = true
            val current = chat.value ?: chat.filterNotNull().first()
            runCatching { chats.sendMessage(current, myUid, text = "", image = uri) }
                .onFailure { _error.value = R.string.error_upload }
            _uploading.value = false
        }
    }

    fun loadMore() {
        if (state.value.canLoadMore) limit.update { it + PAGE_SIZE }
    }

    fun onErrorShown() {
        _error.value = null
    }

    private fun stopTyping() {
        typingJob?.cancel()
        if (typingJob != null) presence.setTyping(chatId, myUid, false)
        typingJob = null
    }

    override fun onCleared() {
        stopTyping()
    }

    private fun needsMarkRead(chat: Chat, messages: List<Message>): Boolean {
        if ((chat.unread[myUid] ?: 0) > 0) return true
        val newestFromOthers = messages.firstOrNull { it.senderId != myUid }?.sentAt ?: return false
        val myLastRead = chat.lastRead[myUid] ?: return true
        return newestFromOthers > myLastRead
    }

    private fun buildState(
        chat: Chat,
        messages: List<Message>,
        people: Map<String, User>,
        presence: Presence?,
        typing: Set<String>,
    ): ChatUiState {
        val other = chat.otherMember(myUid)?.let { people[it] }
        val subtitle = when {
            typing.isNotEmpty() -> ChatSubtitle.Typing(
                if (chat.isGroup) people[typing.first()]?.displayName?.substringBefore(' ') else null,
            )
            chat.isGroup -> ChatSubtitle.Members(
                chat.members.filter { it != myUid }.mapNotNull { people[it]?.displayName?.substringBefore(' ') },
            )
            presence?.online == true -> ChatSubtitle.Online
            presence?.lastChanged != null -> ChatSubtitle.LastSeen(presence.lastChanged)
            else -> ChatSubtitle.None
        }
        return ChatUiState(
            loading = false,
            title = if (chat.isGroup) chat.name.orEmpty() else other?.displayName.orEmpty(),
            photoUrl = if (chat.isGroup) chat.photoUrl else other?.photoUrl,
            isGroup = chat.isGroup,
            colorKey = if (chat.isGroup) chat.id else other?.uid ?: chat.id,
            subtitle = subtitle,
            items = buildItems(chat, messages, people, canLoadMore = messages.size >= limit.value),
            canLoadMore = messages.size >= limit.value,
        )
    }

    /** Convierte los mensajes (del más nuevo al más viejo) en burbujas agrupadas con separadores de día. */
    private fun buildItems(chat: Chat, messages: List<Message>, people: Map<String, User>, canLoadMore: Boolean): List<ChatItem> {
        val othersLastRead = chat.members.filter { it != myUid }.map { chat.lastRead[it]?.toDate()?.time ?: 0L }
        val items = ArrayList<ChatItem>(messages.size + 8)
        messages.forEachIndexed { i, msg ->
            val time = msg.sentAt?.toDate()?.time ?: System.currentTimeMillis()
            val newer = messages.getOrNull(i - 1)
            val older = messages.getOrNull(i + 1)
            val newerTime = newer?.sentAt?.toDate()?.time
            val olderTime = older?.sentAt?.toDate()?.time
            val mine = msg.senderId == myUid

            val firstInRun = older == null || older.senderId != msg.senderId ||
                olderTime == null || !TimeFormat.sameDay(olderTime, time) || time - olderTime > RUN_GAP_MS
            val lastInRun = newer == null || newer.senderId != msg.senderId ||
                newerTime == null || !TimeFormat.sameDay(newerTime, time) || newerTime - time > RUN_GAP_MS

            val status = when {
                msg.pending -> MessageStatus.PENDING
                othersLastRead.isNotEmpty() && othersLastRead.all { it >= time } -> MessageStatus.READ
                else -> MessageStatus.SENT
            }
            val sender = people[msg.senderId]
            items += ChatItem.Bubble(
                id = msg.id,
                text = msg.text,
                imageUrl = msg.imageUrl,
                mine = mine,
                senderName = if (chat.isGroup && !mine) sender?.displayName else null,
                senderPhoto = sender?.photoUrl,
                senderKey = msg.senderId,
                timeMillis = time,
                status = status,
                firstInRun = firstInRun,
                lastInRun = lastInRun,
            )
            val isOldestLoaded = older == null
            val dayChanges = olderTime != null && !TimeFormat.sameDay(olderTime, time)
            if (dayChanges || (isOldestLoaded && !canLoadMore)) items += ChatItem.DateHeader(startOfDay(time))
        }
        return items
    }

    private fun startOfDay(millis: Long): Long =
        java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
            .toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    companion object {
        private const val PAGE_SIZE = 50L
        private const val TYPING_TIMEOUT_MS = 4_000L
        private const val RUN_GAP_MS = 5 * 60 * 1000L
    }
}

