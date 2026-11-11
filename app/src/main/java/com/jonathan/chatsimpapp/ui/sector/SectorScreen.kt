package com.jonathan.chatsimpapp.ui.sector

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.maps.android.compose.rememberCameraPositionState
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.Alert
import com.jonathan.chatsimpapp.data.model.AlertGroup
import com.jonathan.chatsimpapp.data.model.AlertType
import com.jonathan.chatsimpapp.data.repository.AlertsRepository
import com.jonathan.chatsimpapp.data.repository.CommunityContext
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.util.Distance
import com.jonathan.chatsimpapp.util.LatLngPoint
import com.jonathan.chatsimpapp.util.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectorUiState(
    val loading: Boolean = true,
    val context: CommunityContext? = null,
    /** Centro del barrio: el edificio, o la ubicación del teléfono si aún no está marcado. */
    val center: LatLngPoint? = null,
    val centerIsBuilding: Boolean = false,
    val alerts: List<Alert> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SectorViewModel @Inject constructor(
    private val session: CommunitySession,
    private val repo: AlertsRepository,
) : ViewModel() {
    private val device = MutableStateFlow<LatLngPoint?>(null)

    private val center = combine(session.current, device) { ctx, dev ->
        val c = ctx?.community
        if (c?.lat != null && c.lng != null) LatLngPoint(c.lat!!, c.lng!!) to true else dev?.let { it to false }
    }.distinctUntilChanged()

    val state: StateFlow<SectorUiState> = combine(session.current, center) { ctx, c -> ctx to c }
        .flatMapLatest { (ctx, c) ->
            if (ctx == null || c == null) {
                flowOf(SectorUiState(loading = ctx == null, context = ctx))
            } else {
                repo.observeNearby(c.first, AlertsRepository.SECTOR_RADIUS_M)
                    .map { SectorUiState(false, ctx, c.first, c.second, it) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SectorUiState())

    fun onDeviceLocation(point: LatLngPoint?) {
        if (point != null) device.value = point
    }

    fun confirm(alert: Alert, confirmed: Boolean) = launch { repo.confirm(alert.id, it.uid, confirmed) }
    fun markGone(alert: Alert) = launch { repo.markGone(alert.id, it.uid) }
    fun delete(alert: Alert) = launch { repo.delete(alert.id) }

    /** Solo administración: "el edificio está aquí". */
    fun setBuildingHere(point: LatLngPoint) = launch { repo.setCommunityLocation(it.id, point) }

    private fun launch(block: suspend (CommunityContext) -> Unit) {
        viewModelScope.launch { runCatching { block(session.current.filterNotNull().first()) } }
    }
}

@Composable
fun SectorScreen(onBack: () -> Unit, onReport: () -> Unit, viewModel: SectorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasPermission by rememberSaveable { mutableStateOf(LocationProvider.hasPermission(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasPermission = result.values.any { it }
        if (hasPermission) scope.launch { viewModel.onDeviceLocation(LocationProvider.current(context)) }
    }
    LaunchedEffect(Unit) {
        if (hasPermission) viewModel.onDeviceLocation(LocationProvider.current(context))
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val camera = rememberCameraPositionState()
    LaunchedEffect(state.center) { state.center?.let { camera.centerOn(it) } }

    SectorContent(
        state = state,
        onBack = onBack,
        onReport = onReport,
        onConfirm = viewModel::confirm,
        onGone = viewModel::markGone,
        onDelete = viewModel::delete,
        onAskLocation = {
            permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        },
        onSetBuildingHere = {
            scope.launch { LocationProvider.current(context)?.let(viewModel::setBuildingHere) }
        },
        onRecenter = { scope.launch { state.center?.let { camera.animateTo(it) } } },
        map = { modifier, onClick ->
            SectorGoogleMap(
                cameraState = camera,
                home = state.center?.takeIf { state.centerIsBuilding },
                radiusMeters = AlertsRepository.SECTOR_RADIUS_M,
                alerts = state.alerts,
                onAlertClick = onClick,
                modifier = modifier,
                showMyLocation = hasPermission,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectorContent(
    state: SectorUiState,
    onBack: () -> Unit,
    onReport: () -> Unit,
    onConfirm: (Alert, Boolean) -> Unit,
    onGone: (Alert) -> Unit,
    onDelete: (Alert) -> Unit,
    onAskLocation: () -> Unit,
    onSetBuildingHere: () -> Unit,
    onRecenter: () -> Unit,
    map: @Composable (Modifier, (Alert) -> Unit) -> Unit,
    initialSelected: String? = null,
) {
    var selectedId by rememberSaveable { mutableStateOf(initialSelected) }
    val selected = state.alerts.firstOrNull { it.id == selectedId }
    val ctx = state.context

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.sector_title), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.sector_subtitle, ctx?.community?.name.orEmpty()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
            )
        },
        bottomBar = {
            Box(Modifier.navigationBarsPadding().padding(16.dp)) {
                Button(
                    onClick = onReport,
                    colors = ButtonDefaults.buttonColors(containerColor = styleOf(AlertGroup.SECURITY).strong),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                ) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(26.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.report_something), style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AlertGroup.entries) { g -> LegendChip(g) }
                }
            }
            item {
                if (state.center == null) {
                    NoLocationCard(isAdmin = ctx?.isAdmin == true, onAskLocation = onAskLocation, onSetBuildingHere = onSetBuildingHere)
                } else {
                    Box(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(MaterialTheme.shapes.large),
                    ) {
                        map(Modifier.fillMaxSize()) { selectedId = it.id }
                        Surface(
                            onClick = onRecenter,
                            shape = CircleShape,
                            shadowElevation = 4.dp,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                        ) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.MyLocation, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.back_home), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    if (ctx?.isAdmin == true && !state.centerIsBuilding) {
                        TextButton(onClick = onSetBuildingHere, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(stringResource(R.string.set_building_here))
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.most_recent),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                )
            }
            if (state.center != null && state.alerts.isEmpty()) {
                item {
                    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.TaskAlt, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.sector_empty), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            items(state.alerts, key = { it.id }) { alert ->
                AlertRow(alert, state.center, onClick = { selectedId = alert.id })
            }
        }
    }

    if (selected != null && ctx != null) {
        ModalBottomSheet(onDismissRequest = { selectedId = null }) {
            AlertDetail(
                alert = selected,
                center = state.center,
                myUid = ctx.uid,
                onConfirm = { onConfirm(selected, it) },
                onGone = { onGone(selected); selectedId = null },
                onDelete = { onDelete(selected); selectedId = null },
            )
        }
    }
}

@Composable
private fun LegendChip(group: AlertGroup) {
    val style = styleOf(group)
    val icon = when (group) {
        AlertGroup.SECURITY -> iconOf(AlertType.THEFT)
        AlertGroup.DAMAGE -> iconOf(AlertType.ROAD)
        AlertGroup.OTHER -> iconOf(AlertType.NOISE)
    }
    Surface(shape = CircleShape, color = style.soft, contentColor = style.strong) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(style.label), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun NoLocationCard(isAdmin: Boolean, onAskLocation: () -> Unit, onSetBuildingHere: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Icon(Icons.Rounded.MyLocation, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.sector_no_location_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (isAdmin) R.string.sector_no_location_admin else R.string.sector_no_location_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = if (isAdmin) onSetBuildingHere else onAskLocation, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text(stringResource(if (isAdmin) R.string.set_building_here else R.string.use_my_location))
            }
        }
    }
}

@Composable
fun AlertRow(alert: Alert, center: LatLngPoint?, onClick: () -> Unit) {
    val type = AlertType.of(alert.type)
    val style = styleOf(type.group)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).background(style.soft, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                Icon(iconOf(type), null, tint = style.strong, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(labelOf(type)), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(whereAndWhen(alert, center), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (alert.confirmedBy.isNotEmpty()) {
                    Text(
                        "✓ " + pluralStringResource(R.plurals.confirmed_by, alert.confirmedBy.size, alert.confirmedBy.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertDetail(
    alert: Alert,
    center: LatLngPoint?,
    myUid: String,
    onConfirm: (Boolean) -> Unit,
    onGone: () -> Unit,
    onDelete: () -> Unit,
) {
    val type = AlertType.of(alert.type)
    val style = styleOf(type.group)
    val confirmed = myUid in alert.confirmedBy
    val mine = alert.createdBy == myUid
    Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).background(style.soft, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                Icon(iconOf(type), null, tint = style.strong, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(stringResource(labelOf(type)), style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(10.dp))
        Text(whereAndWhen(alert, center), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.reported_by_neighbor_of, alert.communityName), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (alert.description.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(alert.description, style = MaterialTheme.typography.bodyLarge)
        }
        if (alert.confirmedBy.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(pluralStringResource(R.plurals.confirmed_by, alert.confirmedBy.size, alert.confirmedBy.size), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        if (mine) {
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                Icon(Icons.Rounded.DeleteOutline, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.delete_my_alert), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(onClick = { onConfirm(!confirmed) }, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                Icon(Icons.Rounded.Check, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (confirmed) R.string.you_confirmed else R.string.i_saw_it_too), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onGone, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                Icon(Icons.Rounded.VisibilityOff, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.not_there_anymore), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** "A 2 cuadras · hace 20 minutos": distancias en cuadras y tiempos en palabras. */
@Composable
fun whereAndWhen(alert: Alert, center: LatLngPoint?): String {
    val distance = center?.let {
        val m = Distance.meters(it, LatLngPoint(alert.lat, alert.lng))
        if (m < 60) stringResource(R.string.very_close)
        else if (m < 1_000) pluralStringResource(R.plurals.blocks_away, Distance.blocks(m), Distance.blocks(m))
        else stringResource(R.string.km_away, String.format("%.1f", m / 1000))
    }
    val time = alert.createdAt?.toDate()?.time?.let { timeAgo(it) }
    return listOfNotNull(distance, time).joinToString(" · ")
}

@Composable
fun timeAgo(millis: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - millis) / 60_000).coerceAtLeast(0).toInt()
    return when {
        minutes < 2 -> stringResource(R.string.just_now)
        minutes < 60 -> pluralStringResource(R.plurals.minutes_ago, minutes, minutes)
        minutes < 60 * 24 -> (minutes / 60).let { pluralStringResource(R.plurals.hours_ago, it, it) }
        else -> (minutes / (60 * 24)).let { pluralStringResource(R.plurals.days_ago, it, it) }
    }
}

