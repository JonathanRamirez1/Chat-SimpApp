package com.jonathan.chatsimpapp.ui.polls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.Poll
import com.jonathan.chatsimpapp.data.model.Vote
import com.jonathan.chatsimpapp.data.repository.CommunityContentRepository
import com.jonathan.chatsimpapp.data.repository.CommunityContext
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.ui.community.ProBadge
import com.jonathan.chatsimpapp.ui.components.CenteredLoading
import com.jonathan.chatsimpapp.ui.components.PrimaryButton
import com.jonathan.chatsimpapp.ui.navigation.PollRoute
import com.jonathan.chatsimpapp.util.ActaPdf
import com.jonathan.chatsimpapp.util.PollResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PollDetailUiState(
    val loading: Boolean = true,
    val context: CommunityContext? = null,
    val poll: Poll? = null,
    val votes: List<Vote> = emptyList(),
    val sending: Boolean = false,
) {
    val myVote: Vote? get() = context?.let { ctx -> votes.firstOrNull { it.unit == ctx.myUnit } }
    val result: PollResult?
        get() = poll?.let { PollResult.of(votes, it.options.size, context?.community?.unitsCount?.toInt() ?: 0, it.quorumPercent.toInt()) }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PollDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val session: CommunitySession,
    private val content: CommunityContentRepository,
) : ViewModel() {
    private val pollId = savedStateHandle.toRoute<PollRoute>().pollId
    private val sending = MutableStateFlow(false)
    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error

    val state: StateFlow<PollDetailUiState> = session.current.flatMapLatest { ctx ->
        if (ctx == null) return@flatMapLatest flowOf(PollDetailUiState())
        combine(
            content.observePoll(ctx.id, pollId).catch { emit(null) },
            content.observeVotes(ctx.id, pollId).catch { emit(emptyList()) },
            sending,
        ) { poll, votes, s -> PollDetailUiState(false, ctx, poll, votes, s) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PollDetailUiState())

    fun vote(option: Int) {
        viewModelScope.launch {
            val ctx = session.current.filterNotNull().first()
            sending.value = true
            runCatching { content.vote(ctx.id, pollId, ctx.uid, ctx.myUnit, option) }.onFailure { _error.value = true }
            sending.value = false
        }
    }

    fun close() {
        viewModelScope.launch {
            val ctx = session.current.filterNotNull().first()
            runCatching { content.closePoll(ctx.id, pollId) }
        }
    }

    fun onErrorShown() {
        _error.value = false
    }
}

@Composable
fun PollDetailScreen(onBack: () -> Unit, onUpgrade: () -> Unit, viewModel: PollDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    PollDetailContent(state, error, viewModel::vote, viewModel::close, onBack, onUpgrade, viewModel::onErrorShown)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollDetailContent(
    state: PollDetailUiState,
    error: Boolean,
    onVote: (Int) -> Unit,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    onErrorShown: () -> Unit,
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val errorText = stringResource(R.string.error_vote)
    LaunchedEffect(error) {
        if (error) {
            snackbar.showSnackbar(errorText)
            onErrorShown()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.poll)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val poll = state.poll
        val ctx = state.context
        val result = state.result
        if (poll == null || ctx == null || result == null) {
            CenteredLoading(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        val open = poll.isOpen()
        val myVote = state.myVote
        val showResults = myVote != null || !open || ctx.isAdmin
        var selected by rememberSaveable { mutableIntStateOf(-1) }

        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PollStatusPill(open)
                Spacer(Modifier.width(8.dp))
                Text(pollDeadline(poll, open), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Text(poll.question, style = MaterialTheme.typography.headlineSmall)
            if (poll.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(poll.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))

            QuorumCard(result)
            Spacer(Modifier.height(20.dp))

            poll.options.forEachIndexed { i, option ->
                val mine = myVote?.option?.toInt() == i
                val isSelected = selected == i
                Surface(
                    onClick = { if (open && myVote == null) selected = i },
                    enabled = open && myVote == null,
                    shape = MaterialTheme.shapes.medium,
                    color = if (mine || isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (open && myVote == null) {
                                RadioButton(selected = isSelected, onClick = { selected = i })
                            } else if (mine) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                            }
                            Text(option, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            if (showResults) {
                                Text("${result.counts[i]} · ${(result.share(i) * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        if (showResults) {
                            Spacer(Modifier.height(8.dp))
                            ResultBar(result.share(i), highlight = result.winner == i)
                        }
                    }
                }
            }

            when {
                open && myVote == null -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.vote_explain, ctx.myUnit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(stringResource(R.string.vote), onClick = { onVote(selected) }, enabled = selected >= 0, loading = state.sending)
                }
                myVote != null -> Text(
                    stringResource(R.string.you_voted, ctx.myUnit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.acta), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { shareText(context, buildActa(context, ctx.community.name, poll, state.votes, result)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Share, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.share_acta_text))
            }
            OutlinedButton(
                onClick = {
                    if (ctx.isPro) ActaPdf.share(context, buildActa(context, ctx.community.name, poll, state.votes, result), poll.id)
                    else onUpgrade()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.PictureAsPdf, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.share_acta_pdf))
                if (!ctx.isPro) {
                    Spacer(Modifier.width(8.dp))
                    ProBadge()
                }
            }
            if (ctx.isAdmin && open) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.close_poll))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun QuorumCard(result: PollResult) {
    val ok = result.quorumReached
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (ok) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Groups, null, tint = if (ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.participation, result.totalVotes, result.unitsCount),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text("${(result.participation * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(10.dp))
            ResultBar(result.participation, highlight = ok)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    if (ok) R.string.quorum_reached else R.string.quorum_missing,
                    result.quorumPercent,
                    (result.quorumNeeded - result.totalVotes).coerceAtLeast(0),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

