package com.jonathan.chatsimpapp.ui.sector

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.maps.android.compose.rememberCameraPositionState
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.AlertType
import com.jonathan.chatsimpapp.data.repository.AlertsRepository
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.ui.components.PrimaryButton
import com.jonathan.chatsimpapp.util.LatLngPoint
import com.jonathan.chatsimpapp.util.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewAlertUiState(
    val step: Int = 0,
    val type: AlertType? = null,
    val description: String = "",
    val start: LatLngPoint? = null,
    val saving: Boolean = false,
    val done: Boolean = false,
    val error: Boolean = false,
)

@HiltViewModel
class NewAlertViewModel @Inject constructor(
    private val session: CommunitySession,
    private val repo: AlertsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(NewAlertUiState())
    val state: StateFlow<NewAlertUiState> = _state.asStateFlow()

    init {
        // Punto de partida: el edificio (si ya está marcado). La ubicación del teléfono lo reemplaza.
        viewModelScope.launch {
            val c = session.current.filterNotNull().first().community
            if (c.lat != null && c.lng != null && _state.value.start == null) {
                _state.update { it.copy(start = LatLngPoint(c.lat!!, c.lng!!)) }
            }
        }
    }

    fun pick(type: AlertType) = _state.update { it.copy(type = type) }
    fun next() = _state.update { if (it.type != null) it.copy(step = 1) else it }
    fun back() = _state.update { it.copy(step = 0) }
    fun onDescription(v: String) = _state.update { it.copy(description = v.take(280)) }
    fun onDeviceLocation(p: LatLngPoint?) {
        if (p != null) _state.update { it.copy(start = p) }
    }

    fun publish(point: LatLngPoint) {
        val s = _state.value
        val type = s.type ?: return
        viewModelScope.launch {
            val ctx = session.current.filterNotNull().first()
            _state.update { it.copy(saving = true, error = false) }
            runCatching { repo.create(type, s.description, point, ctx.id, ctx.community.name, ctx.uid) }
                .onSuccess { _state.update { it.copy(saving = false, done = true) } }
                .onFailure { _state.update { it.copy(saving = false, error = true) } }
        }
    }
}

@Composable
fun NewAlertScreen(onDone: () -> Unit, viewModel: NewAlertViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) scope.launch { viewModel.onDeviceLocation(LocationProvider.current(context)) }
    }
    LaunchedEffect(Unit) {
        if (LocationProvider.hasPermission(context)) viewModel.onDeviceLocation(LocationProvider.current(context))
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(state.done) { if (state.done) onDone() }
    BackHandler(enabled = state.step == 1) { viewModel.back() }

    val camera = rememberCameraPositionState()
    LaunchedEffect(state.start) { state.start?.let { camera.centerOn(it, 17f) } }

    NewAlertContent(
        state = state,
        onPick = viewModel::pick,
        onNext = viewModel::next,
        onClose = { if (state.step == 1) viewModel.back() else onDone() },
        onDescription = viewModel::onDescription,
        onPublish = {
            val t = camera.position.target
            viewModel.publish(LatLngPoint(t.latitude, t.longitude))
        },
        map = { modifier -> PickLocationMap(camera, modifier) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAlertContent(
    state: NewAlertUiState,
    onPick: (AlertType) -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onDescription: (String) -> Unit,
    onPublish: () -> Unit,
    map: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.cancel)) } },
            )
        },
        bottomBar = {
            Box(Modifier.navigationBarsPadding().imePadding().padding(16.dp)) {
                if (state.step == 0) {
                    PrimaryButton(stringResource(R.string.next), onNext, enabled = state.type != null)
                } else {
                    PrimaryButton(stringResource(R.string.yes_here_publish), onPublish, enabled = state.start != null, loading = state.saving)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(progress = { 1f }, modifier = Modifier.weight(1f).height(6.dp), drawStopIndicator = {})
                LinearProgressIndicator(progress = { if (state.step == 1) 1f else 0f }, modifier = Modifier.weight(1f).height(6.dp), drawStopIndicator = {})
            }
            if (state.step == 0) {
                Text(stringResource(R.string.what_happened), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 20.dp, top = 18.dp))
                Text(
                    stringResource(R.string.what_happened_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(AlertType.entries) { t -> TypeTile(t, selected = state.type == t, onClick = { onPick(t) }) }
                }
            } else {
                Text(stringResource(R.string.was_it_here), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 20.dp, top = 18.dp))
                Text(
                    stringResource(R.string.was_it_here_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
                Box(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().weight(1f).clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.start != null) {
                        map(Modifier.fillMaxSize())
                        // Pin fijo en el centro: se mueve el mapa, no el pin.
                        Icon(
                            Icons.Rounded.LocationOn, null,
                            tint = styleOf(state.type?.group ?: AlertType.ROAD.group).strong,
                            modifier = Modifier.size(56.dp).offset(y = (-24).dp),
                        )
                    } else {
                        Text(stringResource(R.string.finding_location), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    }
                }
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescription,
                    label = { Text(stringResource(R.string.details_optional)) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    maxLines = 3,
                )
                if (state.error) {
                    Text(stringResource(R.string.error_generic), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}

@Composable
private fun TypeTile(type: AlertType, selected: Boolean, onClick: () -> Unit) {
    val style = styleOf(type.group)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) style.soft else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(if (selected) 3.dp else 1.5.dp, if (selected) style.strong else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(140.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(60.dp).background(style.soft, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                Icon(iconOf(type), null, tint = style.strong, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(labelOf(type)), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}
