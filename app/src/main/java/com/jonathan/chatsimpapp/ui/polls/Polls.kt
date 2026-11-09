package com.jonathan.chatsimpapp.ui.polls

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.Poll
import com.jonathan.chatsimpapp.data.model.Vote
import com.jonathan.chatsimpapp.data.repository.CommunityContentRepository
import com.jonathan.chatsimpapp.data.repository.CommunityContext
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.ui.community.Pill
import com.jonathan.chatsimpapp.ui.components.CenteredLoading
import com.jonathan.chatsimpapp.ui.components.EmptyState
import com.jonathan.chatsimpapp.util.PollResult
import com.jonathan.chatsimpapp.util.TimeFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PollsUiState(
    val loading: Boolean = true,
    val context: CommunityContext? = null,
    val polls: List<Poll> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PollsViewModel @Inject constructor(
    session: CommunitySession,
    content: CommunityContentRepository,
) : ViewModel() {
    val state: StateFlow<PollsUiState> = session.current.flatMapLatest { ctx ->
        if (ctx == null) flowOf(PollsUiState())
        else content.observePolls(ctx.id).catch { emit(emptyList()) }.map { PollsUiState(false, ctx, it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PollsUiState())
}

@Composable
fun PollsTab(state: PollsUiState, onOpen: (String) -> Unit, contentPadding: PaddingValues) {
    when {
        state.loading -> CenteredLoading(Modifier.fillMaxSize())
        state.polls.isEmpty() -> EmptyState(
            Icons.Rounded.HowToVote,
            stringResource(R.string.no_polls_title),
            stringResource(if (state.context?.isAdmin == true) R.string.no_polls_admin else R.string.no_polls_body),
            Modifier.fillMaxSize(),
        )
        else -> LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.polls, key = { it.id }) { poll -> PollCard(poll, onClick = { onOpen(poll.id) }) }
        }
    }
}

@Composable
private fun PollCard(poll: Poll, onClick: () -> Unit) {
    val open = poll.isOpen()
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PollStatusPill(open)
                Spacer(Modifier.width(8.dp))
                Text(
                    pollDeadline(poll, open),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(poll.question, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.poll_options_count, poll.options.size) + " · " + stringResource(R.string.quorum_label, poll.quorumPercent.toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PollStatusPill(open: Boolean) {
    val c = MaterialTheme.colorScheme
    if (open) Pill(stringResource(R.string.poll_open), c.tertiaryContainer, c.onTertiaryContainer)
    else Pill(stringResource(R.string.poll_closed), c.surfaceContainerHighest, c.onSurfaceVariant)
}

@Composable
fun pollDeadline(poll: Poll, open: Boolean): String {
    val millis = poll.closesAt?.toDate()?.time ?: return ""
    val text = when (TimeFormat.dayOf(millis)) {
        TimeFormat.Day.TODAY -> stringResource(R.string.today).lowercase() + " " + TimeFormat.time(millis)
        else -> TimeFormat.shortDate(millis) + " " + TimeFormat.time(millis)
    }
    return stringResource(if (open) R.string.poll_closes_at else R.string.poll_closed_at, text)
}

@Composable
fun ResultBar(fraction: Float, highlight: Boolean) {
    LinearProgressIndicator(
        progress = { fraction },
        strokeCap = StrokeCap.Round,
        color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        gapSize = 0.dp,
        drawStopIndicator = {},
        modifier = Modifier.fillMaxWidth().height(8.dp),
    )
}

/** Texto del acta, listo para compartir por WhatsApp, correo o imprimir. */
fun buildActa(context: Context, communityName: String, poll: Poll, votes: List<Vote>, result: PollResult): String = buildString {
    val r = context.resources
    appendLine(r.getString(R.string.acta_title, communityName))
    appendLine()
    appendLine(r.getString(R.string.acta_question, poll.question))
    if (poll.description.isNotBlank()) appendLine(poll.description)
    poll.createdAt?.toDate()?.time?.let { appendLine(r.getString(R.string.acta_opened, TimeFormat.shortDate(it) + " " + TimeFormat.time(it))) }
    poll.closesAt?.toDate()?.time?.let { appendLine(r.getString(R.string.acta_closes, TimeFormat.shortDate(it) + " " + TimeFormat.time(it))) }
    appendLine()
    appendLine(r.getString(R.string.acta_participation, result.totalVotes, result.unitsCount, (result.participation * 100).toInt()))
    appendLine(
        r.getString(
            if (result.quorumReached) R.string.acta_quorum_ok else R.string.acta_quorum_missing,
            result.quorumPercent, result.quorumNeeded,
        ),
    )
    appendLine()
    appendLine(r.getString(R.string.acta_results))
    poll.options.forEachIndexed { i, option ->
        appendLine("• $option: ${result.counts[i]} (${(result.share(i) * 100).toInt()}%)")
    }
    result.winner?.let { appendLine(); appendLine(r.getString(R.string.acta_winner, poll.options[it])) }
    appendLine()
    appendLine(r.getString(R.string.acta_votes_by_unit))
    votes.sortedWith(compareBy({ it.unit.toIntOrNull() ?: Int.MAX_VALUE }, { it.unit })).forEach { v ->
        val time = v.votedAt?.toDate()?.time?.let { TimeFormat.shortDate(it) + " " + TimeFormat.time(it) }.orEmpty()
        appendLine("  ${v.unit}: ${poll.options.getOrNull(v.option.toInt()).orEmpty()}  $time")
    }
    appendLine()
    appendLine(r.getString(R.string.acta_footer))
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
