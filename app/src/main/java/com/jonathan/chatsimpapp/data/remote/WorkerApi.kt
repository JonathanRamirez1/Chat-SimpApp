package com.jonathan.chatsimpapp.data.remote

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.chatsimpapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

class ApiException(val code: Int, message: String) : IOException(message)

/**
 * Cliente del servidor en Cloudflare (Worker `chat-simpapp-api`), que reemplaza las Cloud Functions.
 * Cada petición lleva el idToken de Firebase Auth y el token de App Check; el Worker los valida.
 */
@Singleton
class WorkerApi @Inject constructor(
    private val auth: FirebaseAuth,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Notificación de algo recién publicado (el Worker comprueba que exista y sea tuyo). */
    suspend fun notify(kind: String, vararg ids: Pair<String, String>) {
        post("/v1/notify", obj("kind" to kind, *ids))
    }

    suspend fun registerToken(token: String) {
        post("/v1/tokens", obj("token" to token))
    }

    suspend fun deleteToken(token: String) {
        post("/v1/tokens/delete", obj("token" to token))
    }

    /** "Estoy en la app": se guarda la hora para mostrar "últ. vez hace…". */
    suspend fun ping() {
        post("/v1/presence", JsonObject(emptyMap()))
    }

    /** Última vez que cada persona abrió la app (milisegundos), máx. 50 personas. */
    suspend fun lastSeen(uids: Collection<String>): Map<String, Long> {
        if (uids.isEmpty()) return emptyMap()
        val body = JsonObject(mapOf("uids" to JsonArray(uids.take(50).map { JsonPrimitive(it) })))
        val res = post("/v1/presence/query", body)
        val map = res["lastSeen"]?.jsonObject ?: return emptyMap()
        return map.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.longOrNull?.let { k to it } }.toMap()
    }

    /** Elimina la cuenta y sus datos en el servidor (perfil, membresías, tokens y usuario de Auth). */
    suspend fun deleteAccount() {
        post("/v1/account/delete", JsonObject(emptyMap()))
    }

    suspend fun syncProPlan(communityId: String) {
        post("/v1/pro/sync", obj("communityId" to communityId))
    }

    private fun obj(vararg pairs: Pair<String, String>) = JsonObject(pairs.associate { (k, v) -> k to JsonPrimitive(v) })

    private suspend fun post(path: String, body: JsonObject): JsonObject {
        val user = auth.currentUser ?: throw ApiException(401, "Sin sesión")
        val idToken = user.getIdToken(false).await().token ?: throw ApiException(401, "Sin token")
        val appCheck = runCatching { FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token }.getOrNull()
        return withContext(Dispatchers.IO) {
            val conn = (URL(BuildConfig.API_BASE_URL + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("content-type", "application/json")
                setRequestProperty("authorization", "Bearer $idToken")
                appCheck?.let { setRequestProperty("x-firebase-appcheck", it) }
            }
            try {
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) throw ApiException(code, text)
                (runCatching { json.parseToJsonElement(text) }.getOrNull() as? JsonObject) ?: JsonObject(emptyMap())
            } finally {
                conn.disconnect()
            }
        }
    }

}
