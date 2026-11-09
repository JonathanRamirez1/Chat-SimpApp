package com.jonathan.chatsimpapp.ui.community

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.Member
import com.jonathan.chatsimpapp.data.model.User
import com.jonathan.chatsimpapp.data.repository.CommunityContext
import com.jonathan.chatsimpapp.data.repository.CommunityRepository
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.data.repository.UserRepository
import com.jonathan.chatsimpapp.ui.components.Avatar
import com.jonathan.chatsimpapp.ui.components.CenteredLoading
import com.jonathan.chatsimpapp.ui.polls.shareText
import com.jonathan.chatsimpapp.util.TimeFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberRow(val member: Member, val user: User?)

data class CommunityInfoUiState(
    val context: CommunityContext? = null,
    val members: List<MemberRow> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CommunityInfoViewModel @Inject constructor(
    private val session: CommunitySession,
    private val communities: CommunityRepository,
    private val users: UserRepository,
) : ViewModel() {
    val state: StateFlow<CommunityInfoUiState> = session.current.flatMapLatest { ctx ->
        if (ctx == null) return@flatMapLatest flowOf(CommunityInfoUiState())
        communities.observeMembers(ctx.id).catch { emit(emptyList()) }.flatMapLatest { members ->
            users.observeUsers(members.map { it.uid }.toSet()).catch { emit(emptyMap()) }
                .map { people -> CommunityInfoUiState(ctx, members.map { MemberRow(it, people[it.uid]) }) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommunityInfoUiState())

    fun select(cid: String) = session.select(cid)
    fun regenerateCode() = launch { communities.regenerateInviteCode(it.id, it.community.inviteCode) }
    fun setAdmin(uid: String, admin: Boolean) = launch { communities.setAdmin(it.id, uid, admin) }
    fun remove(uid: String) = launch { communities.removeMember(it.id, uid) }
    fun leave() = launch { communities.leave(it.id, it.uid, it.isAdmin) }

    private fun launch(block: suspend (CommunityContext) -> Unit) {
        viewModelScope.launch { runCatching { block(session.current.filterNotNull().first()) } }
    }
}

@Composable
fun CommunityInfoScreen(
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    onAddCommunity: () -> Unit,
    viewModel: CommunityInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CommunityInfoContent(
        state, onBack, onUpgrade, onAddCommunity, viewModel::select, viewModel::regenerateCode,
        viewModel::setAdmin, viewModel::remove, viewModel::leave,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityInfoContent(
    state: CommunityInfoUiState,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    onAddCommunity: () -> Unit,
    onSelect: (String) -> Unit,
    onRegenerate: () -> Unit,
    onSetAdmin: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onLeave: () -> Unit,
) {
    val context = LocalContext.current
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.community)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        val ctx = state.context
        if (ctx == null) {
            CenteredLoading(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        val community = ctx.community
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(community.name, null, size = 80.dp, isGroup = true, colorKey = community.id)
                    Spacer(Modifier.height(12.dp))
                    Text(community.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    if (community.address.isNotBlank()) {
                        Text(community.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        stringResource(R.string.units_and_members, community.unitsCount.toInt(), state.members.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(stringResource(R.string.invite_code), style = MaterialTheme.typography.labelLarge)
                        Text(
                            community.inviteCode.chunked(3).joinToString(" "),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp,
                        )
                        Text(stringResource(R.string.invite_code_explain), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val text = stringResource(R.string.invite_message, community.name, community.inviteCode)
                            FilledTonalButton(onClick = { shareText(context, text) }) {
                                Icon(Icons.Rounded.Share, null, Modifier.size(18.dp))
                                Text("  " + stringResource(R.string.share))
                            }
                            if (ctx.isAdmin) {
                                TextButton(onClick = onRegenerate) {
                                    Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                                    Text("  " + stringResource(R.string.new_code))
                                }
                            }
                        }
                    }
                }
            }
            item {
                Surface(onClick = onUpgrade, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(if (ctx.isPro) R.string.plan_pro else R.string.plan_free), style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (ctx.isPro) {
                                    stringResource(R.string.plan_renews, community.planExpiresAt?.toDate()?.time?.let { TimeFormat.shortDate(it) }.orEmpty())
                                } else {
                                    stringResource(R.string.plan_free_body)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!ctx.isPro) ProBadge()
                    }
                }
            }
            if (ctx.all.size > 1) {
                item { Text(stringResource(R.string.my_communities), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                items(ctx.all, key = { "c" + it.id }) { c ->
                    Surface(onClick = { onSelect(c.id) }, shape = MaterialTheme.shapes.medium, color = if (c.id == ctx.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(c.name, null, size = 36.dp, isGroup = true, colorKey = c.id)
                            Spacer(Modifier.width(12.dp))
                            Text(c.name, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            item {
                TextButton(onClick = onAddCommunity) {
                    Icon(Icons.Rounded.AddHome, null, Modifier.size(18.dp))
                    Text("  " + stringResource(R.string.add_another_community))
                }
            }
            item { Text(stringResource(R.string.neighbors), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(state.members, key = { it.member.uid }) { row ->
                MemberItem(row, ctx, onSetAdmin, onRemove)
            }
            item {
                TextButton(onClick = { confirmLeave = true }, modifier = Modifier.padding(vertical = 16.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    Text("  " + stringResource(R.string.leave_community), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmLeave) {
        val lastAdmin = state.context?.let { it.isAdmin && it.community.admins.size <= 1 } == true
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.leave_community)) },
            text = { Text(stringResource(if (lastAdmin) R.string.leave_last_admin else R.string.leave_confirm)) },
            confirmButton = {
                if (!lastAdmin) TextButton(onClick = { confirmLeave = false; onLeave() }) { Text(stringResource(R.string.leave)) }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun MemberItem(row: MemberRow, ctx: CommunityContext, onSetAdmin: (String, Boolean) -> Unit, onRemove: (String) -> Unit) {
    val m = row.member
    val isAdmin = m.uid in ctx.community.admins
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(row.user?.displayName.orEmpty(), row.user?.photoUrl, size = 44.dp, colorKey = m.uid)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.user?.displayName ?: stringResource(R.string.unknown_user), style = MaterialTheme.typography.titleSmall)
            Text(
                listOfNotNull(m.unit, if (isAdmin) stringResource(R.string.administration) else null).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (ctx.isAdmin && m.uid != ctx.uid) {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (isAdmin) R.string.remove_admin else R.string.make_admin)) },
                    onClick = { menu = false; onSetAdmin(m.uid, !isAdmin) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.remove_from_community), color = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onRemove(m.uid) },
                )
            }
        }
    }
}
