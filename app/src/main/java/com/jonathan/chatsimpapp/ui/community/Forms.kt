package com.jonathan.chatsimpapp.ui.community

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.config.AppConfig
import com.jonathan.chatsimpapp.data.model.Post
import com.jonathan.chatsimpapp.data.model.Report
import com.jonathan.chatsimpapp.data.repository.CommunityContentRepository
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.ui.components.PrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/** Estado genérico de los formularios de creación. */
data class FormState(
    val title: String = "",
    val body: String = "",
    val choice: String = "",
    val toggle: Boolean = false,
    val options: List<String> = listOf("", ""),
    val days: Int = 3,
    val quorum: Int = 50,
    val photo: Uri? = null,
    val saving: Boolean = false,
    val done: Boolean = false,
    @param:StringRes val message: Int? = null,
)

/** Un único ViewModel para los cuatro formularios: avisos, votaciones, reportes y tablón. */
@HiltViewModel
class FormViewModel @Inject constructor(
    private val session: CommunitySession,
    private val content: CommunityContentRepository,
    private val config: AppConfig,
) : ViewModel() {
    private val _state = MutableStateFlow(FormState())
    val state: StateFlow<FormState> = _state.asStateFlow()

    fun update(block: (FormState) -> FormState) = _state.update(block)
    fun onMessageShown() = _state.update { it.copy(message = null) }

    fun submit(kind: FormKind) {
        val s = _state.value
        val invalid = when (kind) {
            FormKind.ANNOUNCEMENT -> s.title.isBlank()
            FormKind.POLL -> s.title.isBlank() || s.options.count { it.isNotBlank() } < 2
            FormKind.REPORT -> s.title.isBlank() || s.choice.isBlank()
            FormKind.POST -> s.body.isBlank() || s.choice.isBlank()
        }
        if (invalid) {
            _state.update { it.copy(message = R.string.error_form_incomplete) }
            return
        }
        viewModelScope.launch {
            val ctx = session.current.filterNotNull().first()
            _state.update { it.copy(saving = true) }
            runCatching {
                when (kind) {
                    FormKind.ANNOUNCEMENT -> content.postAnnouncement(ctx.id, ctx.uid, ctx.myUnit, s.title, s.body, s.toggle)
                    FormKind.POLL -> {
                        if (!ctx.isPro) {
                            val open = content.observePolls(ctx.id).first().count { it.isOpen() }
                            if (open >= config.freeMaxOpenPolls) throw PlanLimitException()
                        }
                        val closes = Date(System.currentTimeMillis() + s.days * 24L * 3600 * 1000)
                        content.createPoll(ctx.id, ctx.uid, s.title, s.body, s.options, s.quorum, closes)
                    }
                    FormKind.REPORT -> content.createReport(ctx.id, ctx.uid, ctx.myUnit, s.title, s.body, s.choice, s.photo)
                    FormKind.POST -> content.createPost(ctx.id, ctx.uid, ctx.myUnit, s.choice, s.body, s.photo)
                }
            }.onSuccess {
                _state.update { it.copy(saving = false, done = true) }
            }.onFailure { e ->
                val msg = if (e is PlanLimitException) R.string.error_poll_limit else R.string.error_generic
                _state.update { it.copy(saving = false, message = msg) }
            }
        }
    }
}

class PlanLimitException : Exception()

enum class FormKind { ANNOUNCEMENT, POLL, REPORT, POST }

@Composable
fun FormScreen(kind: FormKind, onDone: () -> Unit, viewModel: FormViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    FormContent(kind, state, viewModel::update, { viewModel.submit(kind) }, onDone, viewModel::onMessageShown)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormContent(
    kind: FormKind,
    state: FormState,
    onUpdate: ((FormState) -> FormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.let { stringResource(it) }
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            onMessageShown()
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onUpdate { it.copy(photo = uri) }
    }
    val title = when (kind) {
        FormKind.ANNOUNCEMENT -> R.string.new_announcement
        FormKind.POLL -> R.string.new_poll
        FormKind.REPORT -> R.string.new_report
        FormKind.POST -> R.string.new_post
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.cancel)) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (kind) {
                FormKind.ANNOUNCEMENT -> {
                    Field(state.title, { v -> onUpdate { it.copy(title = v.take(100)) } }, R.string.field_title)
                    Field(state.body, { v -> onUpdate { it.copy(body = v.take(2000)) } }, R.string.field_body, minLines = 5)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.pin_notice), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = state.toggle, onCheckedChange = { v -> onUpdate { it.copy(toggle = v) } })
                    }
                    Hint(R.string.announcement_hint)
                }
                FormKind.POLL -> {
                    Field(state.title, { v -> onUpdate { it.copy(title = v.take(200)) } }, R.string.field_question)
                    Field(state.body, { v -> onUpdate { it.copy(body = v.take(1000)) } }, R.string.field_context_optional, minLines = 2)
                    Text(stringResource(R.string.options), style = MaterialTheme.typography.titleSmall)
                    state.options.forEachIndexed { i, option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Field(
                                option,
                                { v -> onUpdate { s -> s.copy(options = s.options.toMutableList().also { it[i] = v.take(80) }) } },
                                R.string.option_n, Modifier.weight(1f), labelArg = i + 1,
                            )
                            if (state.options.size > 2) {
                                IconButton(onClick = { onUpdate { s -> s.copy(options = s.options.filterIndexed { j, _ -> j != i }) } }) {
                                    Icon(Icons.Rounded.Delete, stringResource(R.string.remove))
                                }
                            }
                        }
                    }
                    if (state.options.size < 6) {
                        TextButton(onClick = { onUpdate { it.copy(options = it.options + "") } }) { Text(stringResource(R.string.add_option)) }
                    }
                    Text(stringResource(R.string.poll_duration), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 3, 7, 14).forEach { d ->
                            FilterChip(state.days == d, onClick = { onUpdate { it.copy(days = d) } }, label = { Text(stringResource(R.string.days_n, d)) })
                        }
                    }
                    Text(stringResource(R.string.quorum_title), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 30, 50, 67, 75).forEach { q ->
                            FilterChip(state.quorum == q, onClick = { onUpdate { it.copy(quorum = q) } }, label = { Text("$q%") })
                        }
                    }
                    Hint(R.string.quorum_hint)
                }
                FormKind.REPORT -> {
                    Text(stringResource(R.string.category), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Report.CATEGORIES.forEach { c ->
                            FilterChip(
                                state.choice == c,
                                onClick = { onUpdate { it.copy(choice = c) } },
                                label = { Text(stringResource(reportCategoryLabel(c))) },
                                leadingIcon = { Icon(reportCategoryIcon(c), null, Modifier.size(18.dp)) },
                            )
                        }
                    }
                    Field(state.title, { v -> onUpdate { it.copy(title = v.take(100)) } }, R.string.report_title_hint)
                    Field(state.body, { v -> onUpdate { it.copy(body = v.take(1500)) } }, R.string.field_details, minLines = 3)
                    PhotoPicker(state.photo, { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        onUpdate { it.copy(photo = null) }
                    }
                }
                FormKind.POST -> {
                    Text(stringResource(R.string.post_type), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Post.TYPES.forEach { t ->
                            FilterChip(
                                state.choice == t,
                                onClick = { onUpdate { it.copy(choice = t) } },
                                label = { Text(stringResource(postTypeLabel(t))) },
                                leadingIcon = { Icon(postTypeIcon(t), null, Modifier.size(18.dp)) },
                            )
                        }
                    }
                    Field(state.body, { v -> onUpdate { it.copy(body = v.take(1000)) } }, R.string.post_text_hint, minLines = 4)
                    PhotoPicker(state.photo, { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        onUpdate { it.copy(photo = null) }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton(stringResource(R.string.publish), onSubmit, loading = state.saving)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    @StringRes label: Int,
    modifier: Modifier = Modifier.fillMaxWidth(),
    minLines: Int = 1,
    labelArg: Int? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(if (labelArg != null) stringResource(label, labelArg) else stringResource(label)) },
        minLines = minLines,
        maxLines = if (minLines > 1) 10 else 1,
        singleLine = minLines == 1,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    )
}

@Composable
private fun ColumnScope.Hint(@StringRes text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PhotoPicker(photo: Uri?, onPick: () -> Unit, onRemove: () -> Unit) {
    if (photo == null) {
        TextButton(onClick = onPick) {
            Icon(Icons.Rounded.AddPhotoAlternate, null)
            Text("  " + stringResource(R.string.add_photo))
        }
    } else {
        Box {
            AsyncImage(
                photo, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onPick),
            )
            IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Rounded.Close, stringResource(R.string.remove), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
