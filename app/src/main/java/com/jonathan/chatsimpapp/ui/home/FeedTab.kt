package com.jonathan.chatsimpapp.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.Announcement
import com.jonathan.chatsimpapp.data.model.WeeklySummary
import com.jonathan.chatsimpapp.ui.community.EmergencyKinds
import com.jonathan.chatsimpapp.ui.community.ProBadge
import com.jonathan.chatsimpapp.ui.components.CenteredLoading
import com.jonathan.chatsimpapp.util.TimeFormat

val EmergencyRed = Color(0xFFDC2626)

@Composable
fun FeedTab(
    state: FeedUiState,
    onGenerateSummary: () -> Unit,
    onUpgrade: () -> Unit,
    onOpenPolls: () -> Unit,
    onOpenReports: () -> Unit,
    onRaiseEmergency: (String, String) -> Unit,
    onDelete: (Announcement) -> Unit,
    contentPadding: PaddingValues,
) {
    if (state.loading) {
        CenteredLoading(Modifier.fillMaxSize())
        return
    }
    val ctx = state.context ?: return
    var showEmergency by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.emergencies, key = { "e" + it.id }) { EmergencyBanner(it) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Rounded.HowToVote, state.openPolls, stringResource(R.string.open_polls), onOpenPolls, Modifier.weight(1f))
                StatCard(Icons.Rounded.Construction, state.openReports, stringResource(R.string.open_reports), onOpenReports, Modifier.weight(1f))
            }
        }

        item {
            OutlinedButton(
                onClick = { showEmergency = true },
                border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Rounded.Warning, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.raise_emergency))
            }
        }

        item { SummaryCard(state.summary, ctx.isPro, state.generating, state.summaryError, onGenerateSummary, onUpgrade) }

        item {
            Text(
                stringResource(R.string.official_notices),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        }
        if (state.announcements.isEmpty()) {
            item {
                Text(
                    stringResource(if (ctx.isAdmin) R.string.no_notices_admin else R.string.no_notices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
        items(state.announcements, key = { it.id }) { a ->
            AnnouncementCard(
                a,
                seen = state.seenCounts[a.id]?.takeIf { ctx.isAdmin },
                canDelete = ctx.isAdmin,
                onDelete = { onDelete(a) },
            )
        }
    }

    if (showEmergency) {
        EmergencyDialog(onDismiss = { showEmergency = false }, onSend = { kind, details ->
            showEmergency = false
            onRaiseEmergency(kind, details)
        })
    }
}

@Composable
private fun StatCard(icon: ImageVector, count: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmergencyBanner(a: Announcement) {
    val label = EmergencyKinds.firstOrNull { it.first == a.emergencyKind }?.second ?: R.string.emergency_other
    Surface(shape = MaterialTheme.shapes.large, color = EmergencyRed, contentColor = Color.White) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.emergency_title, stringResource(label)), style = MaterialTheme.typography.titleMedium)
                if (a.body.isNotBlank()) Text(a.body, style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.reported_by_unit, a.authorUnit) + " · " + (a.createdAt?.toDate()?.time?.let { TimeFormat.time(it) } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    summary: WeeklySummary?,
    isPro: Boolean,
    generating: Boolean,
    error: Boolean,
    onGenerate: () -> Unit,
    onUpgrade: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.weekly_summary), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (!isPro) ProBadge()
            }
            Spacer(Modifier.height(8.dp))
            when {
                !isPro -> {
                    Text(stringResource(R.string.weekly_summary_teaser), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = onUpgrade) { Text(stringResource(R.string.see_pro)) }
                }
                summary != null -> {
                    Text(summary.overview, style = MaterialTheme.typography.bodyMedium)
                    SummarySection(R.string.summary_decisions, summary.decisions)
                    SummarySection(R.string.summary_commitments, summary.commitments)
                    SummarySection(R.string.summary_notices, summary.notices)
                    TextButton(onClick = onGenerate, enabled = !generating) { Text(stringResource(R.string.summary_refresh)) }
                }
                else -> {
                    Text(stringResource(R.string.weekly_summary_empty), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onGenerate, enabled = !generating) {
                        if (generating) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(stringResource(R.string.summary_generate))
                        }
                    }
                }
            }
            if (error) {
                Text(stringResource(R.string.summary_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (isPro) {
                Text(
                    stringResource(R.string.summary_privacy),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SummarySection(title: Int, items: List<String>) {
    if (items.isEmpty()) return
    Spacer(Modifier.height(10.dp))
    Text(stringResource(title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    items.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun AnnouncementCard(a: Announcement, seen: Long?, canDelete: Boolean, onDelete: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (a.pinned) Icons.Rounded.PushPin else Icons.Rounded.Campaign,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(a.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), Modifier.size(20.dp))
                    }
                }
            }
            if (a.body.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(a.body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val time = a.createdAt?.toDate()?.time
                Text(
                    listOfNotNull(
                        stringResource(R.string.administration),
                        time?.let { TimeFormat.chatListLabel(it, stringResource(R.string.yesterday)) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (seen != null) {
                    Icon(Icons.Rounded.Visibility, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.seen_by, seen.toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyDialog(onDismiss: () -> Unit, onSend: (String, String) -> Unit) {
    var kind by rememberSaveable { mutableStateOf(EmergencyKinds.first().first) }
    var details by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Warning, null, tint = EmergencyRed) },
        title = { Text(stringResource(R.string.raise_emergency)) },
        text = {
            Column {
                Text(stringResource(R.string.emergency_explain), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                EmergencyKinds.forEach { (key, label) ->
                    Row(
                        Modifier.fillMaxWidth().selectable(kind == key, onClick = { kind = key }).padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = kind == key, onClick = { kind = key })
                        Text(stringResource(label))
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(200) },
                    label = { Text(stringResource(R.string.details_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSend(kind, details) }, colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)) {
                Text(stringResource(R.string.send_alert))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
