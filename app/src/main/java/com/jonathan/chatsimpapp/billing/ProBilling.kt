package com.jonathan.chatsimpapp.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.jonathan.chatsimpapp.BuildConfig
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Un plan de Pro tal como lo devuelve RevenueCat (precio ya localizado por Google Play). */
data class ProOffer(
    val tierId: String,
    val annual: Boolean,
    val formattedPrice: String,
    internal val rcPackage: Package? = null,
)

sealed interface PurchaseState {
    data object Idle : PurchaseState
    data object Processing : PurchaseState
    data object Success : PurchaseState
    data class Error(val message: String?) : PurchaseState
}

/**
 * Suscripción "Comunidad Pro" con RevenueCat.
 *
 * - El cliente de RevenueCat es la COMUNIDAD, no la persona: el App User ID es `community_<id>`.
 *   Así la suscripción queda atada al edificio aunque cambie la administración.
 * - En RevenueCat hay un "offering" por rango de unidades (`u30`, `u80`, `u200`, `u500`, `u5000`),
 *   cada uno con paquete mensual y anual, y todos dan el entitlement `pro`.
 * - El teléfono nunca activa Pro por su cuenta: tras comprar llama a la Cloud Function
 *   `syncProPlan`, que consulta RevenueCat con la clave secreta y actualiza Firestore. Las
 *   renovaciones y cancelaciones llegan por el webhook `revenueCatWebhook`.
 * - Las compras de prueba (sandbox / testers con licencia) funcionan igual que las reales.
 */
@Singleton
class ProBilling @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val functions: FirebaseFunctions,
) {
    private val _purchase = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchase: StateFlow<PurchaseState> = _purchase.asStateFlow()

    val available: Boolean get() = BuildConfig.REVENUECAT_API_KEY.isNotBlank()

    private fun ensureConfigured(): Boolean {
        if (!available) return false
        if (!Purchases.isConfigured) {
            Purchases.configure(PurchasesConfiguration.Builder(context, BuildConfig.REVENUECAT_API_KEY).build())
        }
        return true
    }

    /** Planes del rango que corresponde al edificio (mensual y anual). */
    suspend fun loadOffers(communityId: String, unitsCount: Int): List<ProOffer> {
        if (!ensureConfigured()) return emptyList()
        Purchases.sharedInstance.awaitLogIn(appUserId(communityId))
        val tier = ProTiers.forUnits(unitsCount)
        val offering = Purchases.sharedInstance.awaitOfferings()[tier.id] ?: return emptyList()
        return offering.availablePackages
            .filter { it.packageType == PackageType.MONTHLY || it.packageType == PackageType.ANNUAL }
            .sortedBy { it.packageType == PackageType.ANNUAL }
            .map { ProOffer(tier.id, it.packageType == PackageType.ANNUAL, it.product.price.formatted, it) }
    }

    suspend fun buy(activity: Activity, offer: ProOffer, communityId: String) {
        val pkg = offer.rcPackage ?: return
        _purchase.value = PurchaseState.Processing
        try {
            Purchases.sharedInstance.awaitLogIn(appUserId(communityId))
            Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(activity, pkg).build())
            sync(communityId)
            _purchase.value = PurchaseState.Success
        } catch (e: PurchasesTransactionException) {
            _purchase.value = if (e.userCancelled) PurchaseState.Idle else PurchaseState.Error(e.message)
        } catch (e: Exception) {
            _purchase.value = PurchaseState.Error(e.message)
        }
    }

    /** Pide al servidor que lea el estado real en RevenueCat (renovaciones, cambio de teléfono). */
    suspend fun sync(communityId: String) {
        runCatching {
            functions.getHttpsCallable("syncProPlan").call(mapOf("communityId" to communityId)).await()
        }.onFailure { Log.w("ProBilling", "syncProPlan falló", it) }
    }

    fun reset() {
        _purchase.value = PurchaseState.Idle
    }

    companion object {
        fun appUserId(communityId: String) = "community_$communityId"
    }
}
