package com.jonathan.chatsimpapp.data.repository

import android.content.Context
import androidx.core.content.edit
import com.jonathan.chatsimpapp.data.model.Community
import com.jonathan.chatsimpapp.data.model.Member
import com.jonathan.chatsimpapp.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/** Todo lo que una pantalla necesita saber de la comunidad abierta. */
data class CommunityContext(
    val uid: String,
    val community: Community,
    val me: Member,
    val all: List<Community>,
) {
    val id: String get() = community.id
    val isAdmin: Boolean get() = community.isAdmin(uid)
    val isPro: Boolean get() = community.isPro
    val myUnit: String get() = me.unit
}

sealed interface CommunityState {
    data object Loading : CommunityState
    data object SignedOut : CommunityState
    data object None : CommunityState
    data class Ready(val context: CommunityContext) : CommunityState
}

/**
 * Comunidad seleccionada (una persona puede vivir en un edificio y administrar otro).
 * La selección se guarda en el teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CommunitySession @Inject constructor(
    @ApplicationContext context: Context,
    auth: AuthRepository,
    private val repository: CommunityRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val selectedId = MutableStateFlow(prefs.getString(KEY_SELECTED, null))

    val state: StateFlow<CommunityState> = auth.authState
        .flatMapLatest { uid -> if (uid == null) flowOf(CommunityState.SignedOut) else stateFor(uid) }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), CommunityState.Loading)

    /** Atajo para ViewModels: el contexto listo, o null. */
    val current: Flow<CommunityContext?> = state.map { (it as? CommunityState.Ready)?.context }.distinctUntilChanged()

    fun select(cid: String) {
        prefs.edit { putString(KEY_SELECTED, cid) }
        selectedId.value = cid
    }

    private fun stateFor(uid: String): Flow<CommunityState> =
        repository.observeMyCommunityIds(uid)
            .catch { emit(emptyList()) }
            .flatMapLatest { ids ->
                if (ids.isEmpty()) return@flatMapLatest flowOf(CommunityState.None)
                combine(repository.observeCommunities(ids), selectedId) { list, selected -> list to selected }
                    .flatMapLatest { (list, selected) ->
                        val community = list.firstOrNull { it.id == selected } ?: list.firstOrNull()
                        if (community == null) {
                            flowOf(CommunityState.None)
                        } else {
                            repository.observeMember(community.id, uid).map { me ->
                                if (me == null) CommunityState.Loading
                                else CommunityState.Ready(CommunityContext(uid, community, me, list))
                            }
                        }
                    }
            }
            .catch { emit(CommunityState.None) }

    private companion object {
        const val KEY_SELECTED = "selected_community"
    }
}
