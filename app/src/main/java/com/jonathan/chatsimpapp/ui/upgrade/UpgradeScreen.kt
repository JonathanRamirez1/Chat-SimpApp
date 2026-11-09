package com.jonathan.chatsimpapp.ui.upgrade

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.billing.BillingManager
import com.jonathan.chatsimpapp.billing.ProOffer
import com.jonathan.chatsimpapp.billing.ProTiers
import com.jonathan.chatsimpapp.billing.PurchaseState
import com.jonathan.chatsimpapp.data.repository.CommunityContext
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.ui.components.PrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpgradeUiState(
    val context: CommunityContext? = null,
    val offers: List<ProOffer> = emptyList(),
    val offersLoaded: Boolean = false,
    val purchase: PurchaseState = PurchaseState.Idle,
)

@HiltViewModel
class UpgradeViewModel @Inject constructor(
    session: CommunitySession,
    private val billing: BillingManager,
) : ViewModel() {
    private val offers = MutableStateFlow<List<ProOffer>?>(null)

    val state: StateFlow<UpgradeUiState> = combine(session.current, offers, billing.purchase) { ctx, offers, purchase ->
        // Solo se ofrecen los planes del rango que corresponde al tamaño del edificio.
        val tier = ProTiers.forUnits(ctx?.community?.unitsCount?.toInt() ?: 0)
        val forTier = offers.orEmpty().filter { ProTiers.ofBasePlan(it.basePlanId) == tier }.sortedBy { it.billingPeriod != "P1M" }
        UpgradeUiState(ctx, forTier, offers != null, purchase)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UpgradeUiState())

    init {
        billing.reset()
        billing.restore()
        viewModelScope.launch { offers.value = runCatching { billing.loadOffers() }.getOrDefault(emptyList()) }
    }

    fun buy(activity: Activity, offer: ProOffer) {
        val ctx = state.value.context ?: return
        billing.buy(activity, offer, ctx.id, ctx.uid)
    }
}

@Composable
fun UpgradeScreen(onClose: () -> Unit, viewModel: UpgradeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    LaunchedEffect(state.purchase) { if (state.purchase is PurchaseState.Success) onClose() }
    UpgradeContent(state, onBuy = { offer -> activity?.let { viewModel.buy(it, offer) } }, onClose = onClose)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeContent(state: UpgradeUiState, onBuy: (ProOffer) -> Unit, onClose: () -> Unit) {
    val ctx = state.context
    Scaffold(
        topBar = {
            TopAppBar(title = {}, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.cancel)) } })
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp).size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.pro_subtitle, ctx?.community?.name.orEmpty()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            ctx?.let {
                Spacer(Modifier.height(12.dp))
                val units = it.community.unitsCount.toInt()
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Text(
                        stringResource(R.string.pro_tier, units, ProTiers.forUnits(units).maxUnits),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Benefit(Icons.Rounded.DoNotDisturbOn, R.string.pro_no_ads, R.string.pro_no_ads_body)
                Benefit(Icons.Rounded.AutoAwesome, R.string.pro_ai, R.string.pro_ai_body)
                Benefit(Icons.Rounded.PictureAsPdf, R.string.pro_pdf, R.string.pro_pdf_body)
                Benefit(Icons.Rounded.HowToVote, R.string.pro_polls, R.string.pro_polls_body)
                Benefit(Icons.Rounded.Visibility, R.string.pro_seen, R.string.pro_seen_body)
            }
            Spacer(Modifier.height(28.dp))
            when {
                ctx == null -> Unit
                ctx.isPro -> Text(stringResource(R.string.pro_active), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                !ctx.isAdmin -> Text(
                    stringResource(R.string.pro_ask_admin),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                state.offersLoaded && state.offers.isEmpty() -> Text(
                    stringResource(R.string.pro_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                else -> state.offers.forEach { offer ->
                    PrimaryButton(
                        text = stringResource(R.string.pro_buy, offer.formattedPrice, periodLabel(offer.billingPeriod)),
                        onClick = { onBuy(offer) },
                        loading = state.purchase is PurchaseState.Processing,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            (state.purchase as? PurchaseState.Error)?.let {
                Text(stringResource(R.string.pro_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.pro_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun periodLabel(iso: String): String = when (iso) {
    "P1M" -> stringResource(R.string.per_month)
    "P1Y" -> stringResource(R.string.per_year)
    else -> iso
}

@Composable
private fun Benefit(icon: ImageVector, title: Int, body: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
