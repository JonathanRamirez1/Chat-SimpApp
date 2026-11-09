package com.jonathan.chatsimpapp.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.notifications.NotificationHelper
import com.jonathan.chatsimpapp.ui.components.Avatar
import com.jonathan.chatsimpapp.ui.components.CenteredLoading
import com.jonathan.chatsimpapp.ui.theme.colorForKey
import com.jonathan.chatsimpapp.util.ActiveChat
import com.jonathan.chatsimpapp.util.TimeFormat
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(onBack: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val uploading by viewModel.uploading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Mientras el chat está visible: no se notifican sus mensajes y se marcan como leídos.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel.chatId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    ActiveChat.chatId = viewModel.chatId
                    NotificationHelper.cancel(context, viewModel.chatId)
                    viewModel.setVisible(true)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    ActiveChat.chatId = null
                    viewModel.setVisible(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ActiveChat.chatId = null
            viewModel.setVisible(false)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let(viewModel::sendImage)
    }

    ChatContent(
        state = state,
        input = input,
        uploading = uploading,
        error = error?.let { stringResource(it) },
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::send,
        onAttach = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onLoadMore = viewModel::loadMore,
        onBack = onBack,
        onErrorShown = viewModel::onErrorShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContent(
    state: ChatUiState,
    input: String,
    uploading: Boolean,
    error: String?,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onLoadMore: () -> Unit,
    onBack: () -> Unit,
    onErrorShown: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        if (error != null) {
            snackbar.showSnackbar(error)
            onErrorShown()
        }
    }
    var viewerUrl by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                title = { ChatTitle(state) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .imePadding(),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.loading) {
                    CenteredLoading(Modifier.fillMaxSize())
                } else if (state.items.isEmpty()) {
                    EmptyChat(Modifier.align(Alignment.Center))
                } else {
                    MessageList(state, listState, onLoadMore, onImageClick = { viewerUrl = it })
                }
                ScrollToBottom(listState, Modifier.align(Alignment.BottomEnd).padding(16.dp))
            }
            if (uploading) LinearProgressIndicator(Modifier.fillMaxWidth())
            InputBar(
                value = input,
                onValueChange = onInputChange,
                onSend = onSend,
                onAttach = onAttach,
                enabled = !state.loading,
            )
        }
    }

    viewerUrl?.let { url -> ImageViewer(url, onDismiss = { viewerUrl = null }) }
}

@Composable
private fun ChatTitle(state: ChatUiState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(state.title, state.photoUrl, size = 40.dp, isGroup = state.isGroup, colorKey = state.colorKey)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                state.title.ifBlank { " " },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val (subtitle, highlight) = subtitleText(state.subtitle)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun subtitleText(subtitle: ChatSubtitle): Pair<String?, Boolean> = when (subtitle) {
    ChatSubtitle.None -> null to false
    ChatSubtitle.Online -> stringResource(R.string.online) to true
    is ChatSubtitle.Typing -> (subtitle.name?.let { stringResource(R.string.typing_named, it) } ?: stringResource(R.string.typing)) to true
    is ChatSubtitle.LastSeen -> {
        val time = TimeFormat.time(subtitle.millis)
        when (TimeFormat.dayOf(subtitle.millis)) {
            TimeFormat.Day.TODAY -> stringResource(R.string.last_seen_today, time)
            TimeFormat.Day.YESTERDAY -> stringResource(R.string.last_seen_yesterday, time)
            else -> stringResource(R.string.last_seen_date, TimeFormat.shortDate(subtitle.millis))
        } to false
    }
    is ChatSubtitle.Members -> {
        val you = stringResource(R.string.you)
        val names = subtitle.names
        when {
            names.size <= 3 -> (names + you).joinToString(", ")
            else -> stringResource(R.string.group_members_summary, names.take(3).joinToString(", "), names.size - 3 + 1)
        } to false
    }
}

@Composable
private fun MessageList(
    state: ChatUiState,
    listState: LazyListState,
    onLoadMore: () -> Unit,
    onImageClick: (String) -> Unit,
) {
    // Carga más mensajes al acercarse al principio de la conversación.
    val nearTop by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 5
        }
    }
    LaunchedEffect(nearTop, state.canLoadMore) { if (nearTop && state.canLoadMore) onLoadMore() }

    // Sigue el final de la conversación cuando llega un mensaje nuevo.
    val newest = state.items.firstOrNull() as? ChatItem.Bubble
    LaunchedEffect(newest?.id) {
        if (newest != null && (newest.mine || listState.firstVisibleItemIndex <= 2)) listState.animateScrollToItem(0)
    }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.items, key = { it.key }, contentType = { it::class }) { item ->
            when (item) {
                is ChatItem.DateHeader -> DateChip(item.millis)
                is ChatItem.Bubble -> MessageBubble(item, showAvatar = state.isGroup, onImageClick = onImageClick)
            }
        }
    }
}

@Composable
private fun DateChip(millis: Long) {
    val label = when (TimeFormat.dayOf(millis)) {
        TimeFormat.Day.TODAY -> stringResource(R.string.today)
        TimeFormat.Day.YESTERDAY -> stringResource(R.string.yesterday)
        else -> TimeFormat.longDate(millis)
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

private val ReadTickColor = Color(0xFF7DD3FC)

@Composable
fun MessageBubble(bubble: ChatItem.Bubble, showAvatar: Boolean, onImageClick: (String) -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    val big = 20.dp
    val small = 6.dp
    val shape = if (bubble.mine) {
        RoundedCornerShape(topStart = big, topEnd = if (bubble.firstInRun) big else small, bottomEnd = small, bottomStart = big)
    } else {
        RoundedCornerShape(topStart = if (bubble.firstInRun) big else small, topEnd = big, bottomEnd = big, bottomStart = small)
    }
    val container = if (bubble.mine) colors.primary else colors.surfaceContainerHigh
    val content = if (bubble.mine) colors.onPrimary else colors.onSurface
    val meta = content.copy(alpha = 0.7f)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = if (bubble.firstInRun) 8.dp else 2.dp),
        horizontalArrangement = if (bubble.mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (showAvatar && !bubble.mine) {
            if (bubble.lastInRun) {
                Avatar(bubble.senderName.orEmpty(), bubble.senderPhoto, size = 28.dp, colorKey = bubble.senderKey)
            } else {
                Spacer(Modifier.width(28.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            color = container,
            contentColor = content,
            shape = shape,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(Modifier.padding(if (bubble.imageUrl != null) 4.dp else 0.dp)) {
                if (bubble.senderName != null && bubble.firstInRun) {
                    Text(
                        bubble.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorForKey(bubble.senderKey),
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
                    )
                }
                if (bubble.imageUrl != null) {
                    AsyncImage(
                        model = bubble.imageUrl,
                        contentDescription = stringResource(R.string.photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(240.dp)
                            .heightIn(min = 160.dp, max = 300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surfaceVariant)
                            .clickable { onImageClick(bubble.imageUrl) },
                    )
                }
                Column(Modifier.padding(start = 12.dp, end = 10.dp, top = if (bubble.text.isNotEmpty()) 8.dp else 4.dp, bottom = 6.dp)) {
                    if (bubble.text.isNotEmpty()) {
                        Text(bubble.text, style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(
                        Modifier.align(Alignment.End).padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(TimeFormat.time(bubble.timeMillis), style = MaterialTheme.typography.labelSmall, color = meta)
                        if (bubble.mine) StatusIcon(bubble.status, meta)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: MessageStatus, tint: Color) {
    val (icon, description, color) = when (status) {
        MessageStatus.PENDING -> Triple(Icons.Rounded.Schedule, R.string.status_sending, tint)
        MessageStatus.SENT -> Triple(Icons.Rounded.Done, R.string.status_sent, tint)
        MessageStatus.READ -> Triple(Icons.Rounded.DoneAll, R.string.status_read, ReadTickColor)
    }
    Icon(icon, contentDescription = stringResource(description), tint = color, modifier = Modifier.size(15.dp))
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    enabled: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(stringResource(R.string.message_hint)) },
                enabled = enabled,
                maxLines = 5,
                shape = RoundedCornerShape(26.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                leadingIcon = {
                    IconButton(onClick = onAttach, enabled = enabled) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = stringResource(R.string.attach_image))
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceContainerHigh,
                    unfocusedContainerColor = colors.surfaceContainerHigh,
                    disabledContainerColor = colors.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    disabledContainerColor = colors.surfaceContainerHigh,
                ),
                modifier = Modifier.size(52.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = stringResource(R.string.send))
            }
        }
    }
}

@Composable
private fun ScrollToBottom(listState: LazyListState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val show by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }
    AnimatedVisibility(show, modifier, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
        SmallFloatingActionButton(
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.scroll_to_bottom))
        }
    }
}

@Composable
private fun EmptyChat(modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Text(
            stringResource(R.string.chat_empty),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun ImageViewer(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
    }
}
