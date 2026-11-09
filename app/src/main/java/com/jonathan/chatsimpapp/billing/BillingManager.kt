package com.jonathan.chatsimpapp.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.google.firebase.functions.FirebaseFunctions
import com.jonathan.chatsimpapp.BuildConfig
import com.jonathan.chatsimpapp.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Un plan de la suscripción tal como lo muestra Google Play (precio ya localizado). */
data class ProOffer(
    val basePlanId: String,
    val offerToken: String,
    val formattedPrice: String,
    val billingPeriod: String,
)

sealed interface PurchaseState {
    data object Idle : PurchaseState
    data object Processing : PurchaseState
    data object Success : PurchaseState
    data class Error(val message: String?) : PurchaseState
}

/**
 * Suscripción "Comunidad Pro" con Google Play Billing.
 *
 * La compra queda atada a la comunidad (obfuscatedProfileId) y a quien paga (obfuscatedAccountId).
 * El teléfono NUNCA activa el plan por sí mismo: envía el token a la Cloud Function
 * `verifyProSubscription`, que lo valida con Google Play y actualiza la comunidad.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext context: Context,
    private val functions: FirebaseFunctions,
    @param:ApplicationScope private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _purchase = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchase: StateFlow<PurchaseState> = _purchase.asStateFlow()

    private var details: ProductDetails? = null
    private var pendingCommunityId: String? = null

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        val result = CompletableDeferred<Boolean>()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                result.complete(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
            }

            override fun onBillingServiceDisconnected() {
                if (!result.isCompleted) result.complete(false)
            }
        })
        return result.await()
    }

    /** Planes disponibles (mensual, anual...) con su precio en la moneda del usuario. */
    suspend fun loadOffers(): List<ProOffer> {
        if (!connect()) return emptyList()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BuildConfig.PRO_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            ).build()
        val product = client.queryProductDetails(params).productDetailsList?.firstOrNull() ?: return emptyList()
        details = product
        return product.subscriptionOfferDetails.orEmpty().mapNotNull { offer ->
            // Solo los planes base (sin ofertas promocionales) y en orden: mensual antes que anual.
            if (offer.offerId != null) return@mapNotNull null
            val phase = offer.pricingPhases.pricingPhaseList.lastOrNull() ?: return@mapNotNull null
            ProOffer(offer.basePlanId, offer.offerToken, phase.formattedPrice, phase.billingPeriod)
        }
    }

    fun buy(activity: Activity, offer: ProOffer, communityId: String, uid: String) {
        val product = details ?: return
        pendingCommunityId = communityId
        _purchase.value = PurchaseState.Processing
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offer.offerToken)
                        .build(),
                ),
            )
            .setObfuscatedAccountId(uid)
            .setObfuscatedProfileId(communityId)
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchase.value = PurchaseState.Error(result.debugMessage)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach { handle(it) }
            BillingClient.BillingResponseCode.USER_CANCELED -> _purchase.value = PurchaseState.Idle
            else -> _purchase.value = PurchaseState.Error(result.debugMessage)
        }
    }

    /** Vuelve a validar las suscripciones activas (renovaciones, cambio de teléfono). */
    fun restore() {
        scope.launch {
            if (!connect()) return@launch
            val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
            client.queryPurchasesAsync(params).purchasesList.forEach { handle(it, silent = true) }
        }
    }

    fun reset() {
        _purchase.value = PurchaseState.Idle
    }

    private fun handle(purchase: Purchase, silent: Boolean = false) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val communityId = purchase.accountIdentifiers?.obfuscatedProfileId ?: pendingCommunityId ?: return
        scope.launch {
            runCatching {
                functions.getHttpsCallable("verifyProSubscription").call(
                    mapOf(
                        "communityId" to communityId,
                        "purchaseToken" to purchase.purchaseToken,
                        "productId" to BuildConfig.PRO_PRODUCT_ID,
                    ),
                ).await()
            }.onSuccess {
                if (!silent) _purchase.value = PurchaseState.Success
            }.onFailure { e ->
                if (!silent) _purchase.value = PurchaseState.Error(e.message)
            }
        }
    }
}
