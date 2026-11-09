package com.jonathan.chatsimpapp.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Valores que se pueden cambiar desde la consola de Firebase (Remote Config) sin publicar
 * una nueva versión. Útil, por ejemplo, cuando Google retira un modelo de Gemini.
 */
@Singleton
class AppConfig @Inject constructor() {
    private val remote = FirebaseRemoteConfig.getInstance()

    init {
        remote.setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = 6 * 3600 })
        remote.setDefaultsAsync(
            mapOf(
                KEY_AI_MODEL to "gemini-3.5-flash-lite",
                KEY_FREE_MAX_OPEN_POLLS to 2L,
                KEY_ADS_ENABLED to true,
            ),
        )
        remote.fetchAndActivate()
    }

    val aiModel: String get() = remote.getString(KEY_AI_MODEL)
    val freeMaxOpenPolls: Int get() = remote.getLong(KEY_FREE_MAX_OPEN_POLLS).toInt()
    val adsEnabled: Boolean get() = remote.getBoolean(KEY_ADS_ENABLED)

    private companion object {
        const val KEY_AI_MODEL = "ai_model"
        const val KEY_FREE_MAX_OPEN_POLLS = "free_max_open_polls"
        const val KEY_ADS_ENABLED = "ads_enabled"
    }
}
