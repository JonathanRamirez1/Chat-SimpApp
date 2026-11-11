package com.jonathan.chatsimpapp.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.jonathan.chatsimpapp.BuildConfig
import com.jonathan.chatsimpapp.config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Actualización OBLIGATORIA (regla de RJ2, ver CLAUDE.md). No se puede quitar.
 *
 * Mismo patrón que Paxora, con Google Play In-App Updates en modo IMMEDIATE:
 * - onCreate: si hay versión nueva y se permite IMMEDIATE, se lanza el flujo a pantalla completa.
 * - onResume: si quedó una actualización en curso, se relanza para que no se pueda saltar.
 * - Si el usuario cancela o falla, la app queda bloqueada en "Debes actualizar para continuar"
 *   con un botón que reintenta (o abre la ficha de Play si el flujo no está disponible).
 * - Respaldo: Remote Config `min_version_code`; si la versión instalada es menor, también se bloquea.
 *
 * In-App Updates solo funciona con la app instalada desde Google Play (pista interna o
 * Internal App Sharing); en builds locales no detecta versiones nuevas.
 */
class ForcedUpdateManager(
    private val activity: ComponentActivity,
    /** Versión mínima permitida (Remote Config `min_version_code`). */
    private val minVersionCode: () -> Long,
    /** Avisa cuando Remote Config trae valores nuevos. */
    private val onConfigRefreshed: (() -> Unit) -> Unit = {},
    private val installedVersionCode: Int = BuildConfig.VERSION_CODE,
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(activity),
) {
    constructor(activity: ComponentActivity, config: AppConfig) :
        this(activity, { config.minVersionCode }, { config.onActivated(it) })

    private val _blocked = MutableStateFlow(false)

    /** true = la app no se puede usar hasta actualizar. */
    val blocked: StateFlow<Boolean> = _blocked.asStateFlow()

    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                // Cancelado (RESULT_CANCELED) o fallido (RESULT_IN_APP_UPDATE_FAILED): no se deja continuar.
                Log.w(TAG, "Actualización obligatoria no completada, resultCode=${result.resultCode}")
                _blocked.value = true
            }
        }

    /** Llamar en onCreate de la Activity principal. */
    fun onCreate() {
        checkMinVersion()
        onConfigRefreshed { checkMinVersion() }
        check(onlyInProgress = false)
    }

    /** Llamar en onResume de la Activity principal. */
    fun onResume() {
        check(onlyInProgress = true)
    }

    /** Botón de la pantalla bloqueante. */
    fun retry() {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.isUpdatableImmediately() || info.isInProgress()) start(info) else openPlayStore()
            }
            .addOnFailureListener { openPlayStore() }
    }

    private fun check(onlyInProgress: Boolean) {
        runCatching {
            manager.appUpdateInfo
                .addOnSuccessListener { info ->
                    when {
                        info.isInProgress() -> start(info)
                        !onlyInProgress && info.isUpdatableImmediately() -> start(info)
                    }
                }
                .addOnFailureListener { Log.e(TAG, "No se pudo consultar appUpdateInfo", it) }
        }.onFailure { Log.e(TAG, "Fallo al revisar actualizaciones", it) }
    }

    private fun checkMinVersion() {
        if (minVersionCode() > installedVersionCode) _blocked.value = true
    }

    private fun start(info: AppUpdateInfo) {
        runCatching {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
        }.onFailure {
            Log.e(TAG, "No se pudo iniciar el flujo IMMEDIATE", it)
            _blocked.value = true
        }
    }

    private fun openPlayStore() {
        val pkg = activity.packageName
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
        }
    }

    private fun AppUpdateInfo.isUpdatableImmediately() =
        updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)

    private fun AppUpdateInfo.isInProgress() =
        updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS

    private companion object {
        const val TAG = "ForcedUpdate"
    }
}
