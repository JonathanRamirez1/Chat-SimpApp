package com.jonathan.chatsimpapp.ai

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jonathan.chatsimpapp.config.AppConfig
import com.jonathan.chatsimpapp.data.model.Announcement
import com.jonathan.chatsimpapp.data.model.Member
import com.jonathan.chatsimpapp.data.model.Message
import com.jonathan.chatsimpapp.data.model.Report
import com.jonathan.chatsimpapp.data.model.WeeklySummary
import com.jonathan.chatsimpapp.data.repository.ChatRepository
import com.jonathan.chatsimpapp.data.repository.CommunityContentRepository
import com.jonathan.chatsimpapp.data.repository.CommunityRepository
import com.jonathan.chatsimpapp.util.Weeks
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class SummaryJson(
    val overview: String = "",
    val decisions: List<String> = emptyList(),
    val commitments: List<String> = emptyList(),
    val notices: List<String> = emptyList(),
)

/**
 * Resumen semanal de la comunidad con Gemini (Firebase AI Logic, sin servidor propio).
 * Se genera una vez por semana y se guarda en Firestore, así todos los vecinos lo leen sin
 * volver a pagar la llamada al modelo.
 */
@Singleton
class SummaryGenerator @Inject constructor(
    private val db: FirebaseFirestore,
    private val content: CommunityContentRepository,
    private val config: AppConfig,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generate(cid: String, communityName: String): WeeklySummary {
        val since = Timestamp(Date(Weeks.lastSevenDaysStart()))
        val units = db.collection(CommunityRepository.COMMUNITIES).document(cid)
            .collection(CommunityRepository.MEMBERS).get().await()
            .toObjects(Member::class.java).associate { it.uid to it.unit }

        val messages = db.collection(ChatRepository.CHATS).document(cid).collection(ChatRepository.MESSAGES)
            .whereGreaterThan("sentAt", since).orderBy("sentAt", Query.Direction.ASCENDING).limit(MAX_MESSAGES)
            .get().await().toObjects(Message::class.java)
        val announcements = db.collection(CommunityRepository.COMMUNITIES).document(cid)
            .collection(CommunityContentRepository.ANNOUNCEMENTS).whereGreaterThan("createdAt", since)
            .get().await().toObjects(Announcement::class.java)
        val reports = db.collection(CommunityRepository.COMMUNITIES).document(cid)
            .collection(CommunityContentRepository.REPORTS).whereGreaterThan("updatedAt", since)
            .get().await().toObjects(Report::class.java)

        val prompt = buildPrompt(communityName, messages, announcements, reports, units)
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = config.aiModel,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.2f
            },
        )
        val text = model.generateContent(prompt).text.orEmpty()
        val parsed = json.decodeFromString<SummaryJson>(text.trim().removePrefix("```json").removeSuffix("```"))
        val summary = WeeklySummary(
            week = Weeks.key(),
            overview = parsed.overview,
            decisions = parsed.decisions,
            commitments = parsed.commitments,
            notices = parsed.notices,
        )
        content.saveSummary(cid, summary)
        return summary
    }

    companion object {
        private const val MAX_MESSAGES = 400L

        internal fun buildPrompt(
            communityName: String,
            messages: List<Message>,
            announcements: List<Announcement>,
            reports: List<Report>,
            units: Map<String, String>,
        ): String = buildString {
            appendLine("Eres el asistente de la comunidad de vecinos \"$communityName\".")
            appendLine("Resume la última semana para alguien que no leyó nada. Escribe en español, claro y breve.")
            appendLine("Ignora saludos, chistes y discusiones sin conclusión. No inventes nada que no esté abajo.")
            appendLine("Identifica a las personas solo por su unidad (por ejemplo \"Apto 502\"), nunca por otro dato.")
            appendLine()
            appendLine("Responde SOLO con JSON con esta forma:")
            appendLine("""{"overview": "2-3 frases", "decisions": ["acuerdos tomados"], "commitments": ["Apto X: se comprometió a ..."], "notices": ["avisos o fechas importantes"]}""")
            appendLine("Usa listas vacías si no hay nada. Máximo 6 elementos por lista.")
            appendLine()
            if (announcements.isNotEmpty()) {
                appendLine("## Avisos oficiales")
                announcements.forEach { appendLine("- [${if (it.isEmergency) "EMERGENCIA" else "aviso"}] ${it.title}: ${it.body}") }
            }
            if (reports.isNotEmpty()) {
                appendLine("## Reportes de daños")
                reports.forEach { appendLine("- ${it.title} (estado: ${it.status}, ${it.affectedUnits.size} unidades afectadas)") }
            }
            appendLine("## Chat general")
            if (messages.isEmpty()) appendLine("(sin mensajes)")
            messages.forEach { m ->
                val who = units[m.senderId]?.let { "Apto $it" } ?: "Vecino"
                val text = m.text.ifBlank { if (m.imageUrl != null) "[foto]" else "" }.replace('\n', ' ').take(500)
                appendLine("$who: $text")
            }
        }
    }
}
