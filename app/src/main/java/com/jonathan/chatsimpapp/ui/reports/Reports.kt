package com.jonathan.chatsimpapp.ui.reports

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import coil.compose.AsyncImage
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.Report
import com.jonathan.chatsimpapp.data.repository.CommunityContentRepository
import com.jonathan.chatsimpapp.data.repository.CommunityContext
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.ui.community.ReportStatusPill
import com.jonathan.chatsimpapp.ui.community.reportCategoryIcon
import com.jonathan.chatsimpapp.ui.community.reportCategoryLabel
import com.jonathan.chatsimpapp.ui.components.CenteredLoading
import com.jonathan.chatsimpapp.ui.components.EmptyState
import com.jonathan.chatsimpapp.ui.navigation.ReportRoute
import com.jonathan.chatsimpapp.util.TimeFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsUiState(
    val loading: Boolean = true,
    val context: CommunityContext? = null,
    val reports: List<Report> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    session: CommunitySession,
    content: CommunityContentRepository,
) : ViewModel() {
    val state: StateFlow<ReportsUiState> = session.current.flatMapLatest { ctx ->
        if (ctx == null) flowOf(ReportsUiState())
        else content.observeReports(ctx.id).catch { emit(emptyList()) }.map { ReportsUiState(false, ctx, it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())
}

private val StatusFilters = listOf(null, Report.STATUS_OPEN, Report.STATUS_IN_PROGRESS, Report.STATUS_RESOLVED)

@Composable
fun ReportsTab(state: ReportsUiState, onOpen: (String) -> Unit, contentPadding: PaddingValues) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    if (state.loading) {
        CenteredLoading(Modifier.fillMaxSize())
        return
    }
    if (state.reports.isEmpty()) {
        EmptyState(Icons.Rounded.TaskAlt, stringResource(R.string.no_reports_title), stringResource(R.string.no_reports_body), Modifier.fillMaxSize())
        return
    }
    val visible = state.reports.filter { filter == null || it.status == filter }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StatusFilters) { s ->
                    val label = when (s) {
                        Report.STATUS_OPEN -> R.string.status_open
                        Report.STATUS_IN_PROGRESS -> R.string.status_in_progress
                        Report.STATUS_RESOLVED -> R.string.status_resolved
                        else -> R.string.all
                    }
                    val count = state.reports.count { s == null || it.status == s }
                    FilterChip(filter == s, onClick = { filter = s }, label = { Text("${stringResource(label)} · $count") })
                }
            }
        }
        items(visible, key = { it.id }) { r -> ReportCard(r, state.context?.myUnit, onClick = { onOpen(r.id) }) }
    }
}

@Composable
private fun ReportCard(report: Report, myUnit: String?, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(reportCategoryIcon(report.category), null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(report.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                val updated = (report.updatedAt ?: report.createdAt)?.toDate()?.time
                Text(
                    listOfNotNull(
                        stringResource(reportCategoryLabel(report.category)),
                        updated?.let { TimeFormat.chatListLabel(it, stringResource(R.string.yesterday)) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportStatusPill(report.status)
                    Text(
                        stringResource(R.string.affected_units, report.affectedUnits.size) +
                            if (myUnit != null && myUnit in report.affectedUnits) " · " + stringResource(R.string.includes_you) else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------- Detalle ----------

data class ReportDetailUiState(val context: CommunityContext? = null, val report: Report? = null)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val session: CommunitySession,
    private val content: CommunityContentRepository,
) : ViewModel() {
    private val reportId = savedStateHandle.toRoute<ReportRoute>().reportId

    val state: StateFlow<ReportDetailUiState> = session.current.flatMapLatest { ctx ->
        if (ctx == null) flowOf(ReportDetailUiState())
        else content.observeReport(ctx.id, reportId).catch { emit(null) }.map { ReportDetailUiState(ctx, it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportDetailUiState())

    fun toggleAffected(affected: Boolean) = launch { ctx -> content.toggleAffected(ctx.id, reportId, ctx.myUnit, affected) }

    fun setStatus(status: String, note: String) = launch { ctx -> content.updateReportStatus(ctx.id, reportId, status, note, ctx.myUnit) }

    private fun launch(block: suspend (CommunityContext) -> Unit) {
        viewModelScope.launch { runCatching { block(session.current.filterNotNull().first()) } }
    }
}

@Composable
fun ReportDetailScreen(onBack: () -> Unit, viewModel: ReportDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportDetailContent(state, viewModel::toggleAffected, viewModel::setStatus, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailContent(
    state: ReportDetailUiState,
    onToggleAffected: (Boolean) -> Unit,
    onSetStatus: (String, String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        val r = state.report
        val ctx = state.context
        if (r == null || ctx == null) {
            CenteredLoading(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        val affected = ctx.myUnit in r.affectedUnits
        var note by rememberSaveable { mutableStateOf("") }

        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportStatusPill(r.status)
                Icon(reportCategoryIcon(r.category), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(reportCategoryLabel(r.category)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Text(r.title, style = MaterialTheme.typography.headlineSmall)
            if (r.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(r.description, style = MaterialTheme.typography.bodyLarge)
            }
            r.photoUrl?.let {
                Spacer(Modifier.height(16.dp))
                AsyncImage(
                    it, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            Spacer(Modifier.height(16.dp))
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Apartment, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.affected_units, r.affectedUnits.size), style = MaterialTheme.typography.titleSmall)
                        Text(r.affectedUnits.sorted().joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (r.status != Report.STATUS_RESOLVED && r.createdBy != ctx.uid) {
                Spacer(Modifier.height(12.dp))
                if (affected) {
                    OutlinedButton(onClick = { onToggleAffected(false) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.not_affected_me)) }
                } else {
                    Button(onClick = { onToggleAffected(true) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.affects_me_too)) }
                }
            }

            if (ctx.isAdmin) {
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.update_status), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    note, { note = it.take(300) },
                    label = { Text(stringResource(R.string.status_note)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Report.STATUS_IN_PROGRESS to R.string.status_in_progress, Report.STATUS_RESOLVED to R.string.status_resolved, Report.STATUS_OPEN to R.string.status_open)
                        .filter { it.first != r.status }
                        .forEach { (s, label) ->
                            FilterChip(
                                selected = false,
                                onClick = { onSetStatus(s, note); note = "" },
                                label = { Text(stringResource(label)) },
                                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            )
                        }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.history), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            r.history.sortedByDescending { it.at }.forEach { e ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 6.dp).size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReportStatusPill(e.status)
                            Text(
                                (e.at?.toDate()?.time?.let { TimeFormat.shortDate(it) + " " + TimeFormat.time(it) } ?: "") +
                                    " · " + e.byUnit,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (e.note.isNotBlank()) Text(e.note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
