package com.jonathan.chatsimpapp.ui.newchat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jonathan.chatsimpapp.data.model.User
import com.jonathan.chatsimpapp.data.repository.UserRepository
import com.jonathan.chatsimpapp.ui.components.Avatar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** Estado de la búsqueda de personas, compartido por "Nuevo chat" y "Nuevo grupo". */
data class SearchState(
    val query: String = "",
    val searching: Boolean = false,
    val results: List<User> = emptyList(),
) {
    val hasQuery: Boolean get() = query.trim().length >= MIN_QUERY || '@' in query

    companion object {
        const val MIN_QUERY = 2
    }
}

/** Convierte lo que escribe el usuario en resultados, esperando a que deje de teclear. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
fun Flow<String>.searchUsers(users: UserRepository, myUid: String?): Flow<SearchState> =
    map { it.trim() }
        .distinctUntilChanged()
        .debounce(250)
        .flatMapLatest { q ->
            flow {
                val base = SearchState(query = q)
                if (!base.hasQuery) {
                    emit(base)
                    return@flow
                }
                emit(base.copy(searching = true))
                val results = runCatching { users.search(q, excludeUid = myUid) }.getOrDefault(emptyList())
                emit(base.copy(results = results))
            }
        }

@Composable
fun PersonRow(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(user.displayName, user.photoUrl, size = 48.dp, colorKey = user.uid)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(user.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                user.about.ifBlank { user.email },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}
