package com.jonathan.chatsimpapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonathan.chatsimpapp.ai.SummaryGenerator
import com.jonathan.chatsimpapp.data.model.Announcement
import com.jonathan.chatsimpapp.data.model.Poll
import com.jonathan.chatsimpapp.data.model.Report
import com.jonathan.chatsimpapp.data.model.WeeklySummary
import com.jonathan.chatsimpapp.data.repository.AlertsRepository
import com.jonathan.chatsimpapp.data.repository.CommunityContentRepository
import com.jonathan.chatsimpapp.util.LatLngPoint
import com.jonathan.chatsimpapp.data.repository.CommunityContext
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.util.Weeks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val loading: Boolean = true,
    val context: CommunityContext? = null,
    val emergencies: List<Announcement> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val summary: WeeklySummary? = null,
    val openReports: Int = 0,
    val openPolls: Int = 0,
    val seenCounts: Map<String, Long> = emptyMap(),
    val generating: Boolean = false,
    val summaryError: Boolean = false,
    /** Alertas activas en el barrio; null si el edificio aún no tiene ubicación. */
    val sectorAlerts: Int? = null,
)

private data class FeedData(
    val announcements: List<Announcement>,
    val summary: WeeklySummary?,
    val reports: List<Report>,
    val polls: List<Poll>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val session: CommunitySession,
    private val content: CommunityContentRepository,
    private val summaries: SummaryGenerator,
    alerts: AlertsRepository,
) : ViewModel() {

    private val sectorCount = session.current
        .map { ctx -> ctx?.community?.let { c -> if (c.lat != null && c.lng != null) LatLngPoint(c.lat!!, c.lng!!) else null } }
        .distinctUntilChanged()
        .flatMapLatest { p -> if (p == null) flowOf(null) else alerts.observeNearby(p, AlertsRepository.SECTOR_RADIUS_M).map { it.size } }


    private val generating = MutableStateFlow(false)
    private val summaryError = MutableStateFlow(false)
    private val seenCounts = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val markedSeen = mutableSetOf<String>()

    private val data = session.current.flatMapLatest { ctx ->
        if (ctx == null) return@flatMapLatest flowOf(null)
        combine(
            content.observeAnnouncements(ctx.id).catch { emit(emptyList()) },
            content.observeSummary(ctx.id, Weeks.key()).catch { emit(null) },
            content.observeReports(ctx.id).catch { emit(emptyList()) },
            content.observePolls(ctx.id).catch { emit(emptyList()) },
        ) { a, s, r, p -> FeedData(a, s, r, p) }
    }

    private val flags = combine(generating, summaryError, seenCounts, sectorCount) { g, e, s, c -> Flags(g, e, s, c) }

    private data class Flags(val generating: Boolean, val error: Boolean, val seen: Map<String, Long>, val sector: Int?)

    val state: StateFlow<FeedUiState> =
        combine(session.current, data, flags) { ctx, data, f ->
            val gen = f.generating
            val err = f.error
            val seen = f.seen
            if (ctx == null || data == null) return@combine FeedUiState()
            val recent = System.currentTimeMillis() - EMERGENCY_WINDOW_MS
            val (emergencies, notices) = data.announcements.partition { it.isEmergency }
            FeedUiState(
                loading = false,
                context = ctx,
                emergencies = emergencies.filter { (it.createdAt?.toDate()?.time ?: Long.MAX_VALUE) > recent },
                announcements = notices.sortedByDescending { it.pinned },
                summary = data.summary,
                openReports = data.reports.count { it.status != Report.STATUS_RESOLVED },
                openPolls = data.polls.count { it.isOpen() },
                seenCounts = seen,
                generating = gen,
                summaryError = err,
                sectorAlerts = f.sector,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState())

    init {
        // Registra "visto" una sola vez por aviso y, si eres admin, trae cuántos lo vieron.
        viewModelScope.launch {
            state.collect { s ->
                val ctx = s.context ?: return@collect
                s.announcements.take(10).forEach { a ->
                    if (markedSeen.add(a.id)) {
                        content.markAnnouncementSeen(ctx.id, a.id, ctx.uid, ctx.myUnit)
                        if (ctx.isAdmin && ctx.isPro) {
                            launch {
                                runCatching { content.seenCount(ctx.id, a.id) }
                                    .onSuccess { n -> seenCounts.update { it + (a.id to n) } }
                            }
                        }
                    }
                }
            }
        }
    }

    fun generateSummary() {
        viewModelScope.launch {
            val ctx = session.current.filterNotNull().first()
            if (!ctx.isPro || generating.value) return@launch
            generating.value = true
            summaryError.value = false
            runCatching { summaries.generate(ctx.id, ctx.community.name) }
                .onFailure { summaryError.value = true }
            generating.value = false
        }
    }

    fun raiseEmergency(kind: String, details: String) {
        viewModelScope.launch {
            val ctx = session.current.filterNotNull().first()
            runCatching { content.raiseEmergency(ctx.id, ctx.uid, ctx.myUnit, kind, details) }
        }
    }

    fun delete(announcement: Announcement) {
        viewModelScope.launch {
            val ctx = session.current.filterNotNull().first()
            runCatching { content.deleteAnnouncement(ctx.id, announcement.id) }
        }
    }

    private companion object {
        const val EMERGENCY_WINDOW_MS = 12 * 60 * 60 * 1000L
    }
}
