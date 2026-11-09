package com.jonathan.chatsimpapp.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.ads.AdsController
import com.jonathan.chatsimpapp.ads.BannerAd
import com.jonathan.chatsimpapp.data.model.User
import com.jonathan.chatsimpapp.data.repository.CommunityContext
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.data.repository.UserRepository
import com.jonathan.chatsimpapp.ui.board.BoardTab
import com.jonathan.chatsimpapp.ui.board.BoardViewModel
import com.jonathan.chatsimpapp.ui.chats.ChatsContent
import com.jonathan.chatsimpapp.ui.chats.ChatsViewModel
import com.jonathan.chatsimpapp.ui.components.Avatar
import com.jonathan.chatsimpapp.ui.polls.PollsTab
import com.jonathan.chatsimpapp.ui.polls.PollsViewModel
import com.jonathan.chatsimpapp.ui.reports.ReportsTab
import com.jonathan.chatsimpapp.ui.reports.ReportsViewModel
import com.jonathan.chatsimpapp.ui.community.FormKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class HomeTab(val label: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    FEED(R.string.tab_home, Icons.Outlined.Home, Icons.Rounded.Home),
    CHATS(R.string.chats, Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Rounded.Chat),
    POLLS(R.string.tab_polls, Icons.Outlined.HowToVote, Icons.Rounded.HowToVote),
    REPORTS(R.string.tab_reports, Icons.Outlined.Construction, Icons.Rounded.Construction),
    BOARD(R.string.tab_board, Icons.Outlined.Storefront, Icons.Rounded.Storefront),
}

data class HomeUiState(val context: CommunityContext? = null, val me: User? = null, val showAds: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    session: CommunitySession,
    users: UserRepository,
    ads: AdsController,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = session.current.flatMapLatest { ctx ->
        if (ctx == null) return@flatMapLatest flowOf(HomeUiState())
        combine(users.observeUser(ctx.uid).catch { emit(null) }, ads.enabled) { me, showAds ->
            HomeUiState(ctx, me, showAds = showAds)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

/** Navegación que necesita la pantalla principal. */
data class HomeActions(
    val openChat: (String) -> Unit,
    val newChat: () -> Unit,
    val profile: () -> Unit,
    val communityInfo: () -> Unit,
    val upgrade: () -> Unit,
    val openPoll: (String) -> Unit,
    val openReport: (String) -> Unit,
    val form: (FormKind) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(HomeTab.FEED) }
    val ctx = state.context

    val feedVm: FeedViewModel = hiltViewModel()
    val chatsVm: ChatsViewModel = hiltViewModel()
    val pollsVm: PollsViewModel = hiltViewModel()
    val reportsVm: ReportsViewModel = hiltViewModel()
    val boardVm: BoardViewModel = hiltViewModel()

    // Android 13+ pide permiso explícito para mostrar notificaciones (avisos y emergencias).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    val boardOpenChat by boardVm.openChat.collectAsStateWithLifecycle()
    LaunchedEffect(boardOpenChat) { boardOpenChat?.let { boardVm.onChatOpened(); actions.openChat(it) } }

    HomeScaffold(state, tab, { tab = it }, actions) { inner ->
        when (tab) {
            HomeTab.FEED -> {
                val s by feedVm.state.collectAsStateWithLifecycle()
                FeedTab(
                    s, feedVm::generateSummary, actions.upgrade,
                    onOpenPolls = { tab = HomeTab.POLLS }, onOpenReports = { tab = HomeTab.REPORTS },
                    onRaiseEmergency = feedVm::raiseEmergency, onDelete = feedVm::delete, contentPadding = inner,
                )
            }
            HomeTab.CHATS -> {
                val s by chatsVm.state.collectAsStateWithLifecycle()
                ChatsContent(s, chatsVm::onQueryChange, actions.openChat, actions.newChat, actions.profile, embedded = true)
            }
            HomeTab.POLLS -> {
                val s by pollsVm.state.collectAsStateWithLifecycle()
                PollsTab(s, actions.openPoll, inner)
            }
            HomeTab.REPORTS -> {
                val s by reportsVm.state.collectAsStateWithLifecycle()
                ReportsTab(s, actions.openReport, inner)
            }
            HomeTab.BOARD -> {
                val s by boardVm.state.collectAsStateWithLifecycle()
                BoardTab(s, boardVm::contact, boardVm::setResolved, boardVm::delete, inner)
            }
        }
    }
}

/** Marco de la pantalla principal (barra superior, pestañas, anuncio y botón flotante). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScaffold(
    state: HomeUiState,
    tab: HomeTab,
    onTab: (HomeTab) -> Unit,
    actions: HomeActions,
    content: @Composable (PaddingValues) -> Unit,
) {
    val ctx = state.context
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(Modifier.clickable(onClick = actions.communityInfo)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                ctx?.community?.name.orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Icon(Icons.Rounded.ExpandMore, null, Modifier.size(20.dp))
                        }
                        Text(
                            listOfNotNull(
                                ctx?.myUnit,
                                if (ctx?.isAdmin == true) stringResource(R.string.administration) else null,
                                if (ctx?.isPro == true) "Pro" else null,
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = actions.profile) {
                        Avatar(state.me?.displayName.orEmpty(), state.me?.photoUrl, size = 36.dp, colorKey = state.me?.uid.orEmpty())
                    }
                },
            )
        },
        bottomBar = {
            Column {
                if (state.showAds && tab != HomeTab.CHATS) BannerAd()
                NavigationBar {
                    HomeTab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { onTab(t) },
                            icon = { Icon(if (tab == t) t.selectedIcon else t.icon, null) },
                            label = { Text(stringResource(t.label), maxLines = 1) },
                        )
                    }
                }
            }
        },
        floatingActionButton = { HomeFab(tab, ctx, actions) },
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding)) {
            content(PaddingValues(0.dp))
        }
    }
}

@Composable
private fun HomeFab(tab: HomeTab, ctx: CommunityContext?, actions: HomeActions) {
    val admin = ctx?.isAdmin == true
    val (label, icon, onClick) = when (tab) {
        HomeTab.FEED -> if (admin) Triple(R.string.new_announcement, Icons.Rounded.Campaign, { actions.form(FormKind.ANNOUNCEMENT) }) else return
        HomeTab.CHATS -> Triple(R.string.new_chat, Icons.Rounded.Edit, actions.newChat)
        HomeTab.POLLS -> if (admin) Triple(R.string.new_poll, Icons.Rounded.Add, { actions.form(FormKind.POLL) }) else return
        HomeTab.REPORTS -> Triple(R.string.report_action, Icons.Rounded.Add, { actions.form(FormKind.REPORT) })
        HomeTab.BOARD -> Triple(R.string.publish, Icons.Rounded.Add, { actions.form(FormKind.POST) })
    }
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(icon, null) },
        text = { Text(stringResource(label)) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    )
}

