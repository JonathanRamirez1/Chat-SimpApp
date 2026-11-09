package com.jonathan.chatsimpapp

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.jonathan.chatsimpapp.data.model.User
import com.jonathan.chatsimpapp.ui.auth.LoginContent
import com.jonathan.chatsimpapp.ui.auth.LoginUiState
import com.jonathan.chatsimpapp.ui.auth.RegisterContent
import com.jonathan.chatsimpapp.ui.auth.RegisterUiState
import com.jonathan.chatsimpapp.ui.chat.ChatContent
import com.jonathan.chatsimpapp.ui.chat.ChatItem
import com.jonathan.chatsimpapp.ui.chat.ChatSubtitle
import com.jonathan.chatsimpapp.ui.chat.ChatUiState
import com.jonathan.chatsimpapp.ui.chat.MessageStatus
import com.jonathan.chatsimpapp.ui.chats.ChatRow
import com.jonathan.chatsimpapp.ui.chats.ChatsContent
import com.jonathan.chatsimpapp.ui.chats.ChatsUiState
import com.jonathan.chatsimpapp.ui.newchat.NewChatContent
import com.jonathan.chatsimpapp.ui.newchat.SearchState
import com.jonathan.chatsimpapp.ui.profile.ProfileContent
import com.jonathan.chatsimpapp.ui.profile.ProfileUiState
import com.jonathan.chatsimpapp.ui.theme.ChatSimpAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Capturas de las pantallas principales con datos de ejemplo.
 * Generar: ./gradlew recordRoborazziDebug  ·  Comparar: ./gradlew verifyRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent { ChatSimpAppTheme(darkTheme = dark) { content() } }
        compose.onRoot().captureRoboImage("../screenshots/$name.png")
    }

    private val now = System.currentTimeMillis()
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    private val rows = listOf(
        row("1", "Valentina Ríos", "¿Nos vemos a las 7 en el café?", unread = 2, online = true, time = now - 4 * minute),
        row("2", "Equipo Diseño", "Ya subí los mockups 🎨", group = true, sender = "Andrés", unread = 5, time = now - 40 * minute),
        row("3", "Mateo Gómez", "Perfecto, gracias!", mine = true, time = now - 3 * hour),
        row("4", "Camila Torres", null, image = true, online = true, time = now - day),
        row("5", "Familia", "Mamá: ¿Quién trae el postre el domingo?", group = true, time = now - 3 * day),
        row("6", "Santiago Herrera", "Te paso el documento mañana", mine = true, time = now - 9 * day),
    )

    private fun row(
        id: String, title: String, preview: String?, group: Boolean = false, sender: String? = null,
        mine: Boolean = false, image: Boolean = false, unread: Long = 0, online: Boolean = false, time: Long,
    ) = ChatRow(id, title, null, group, title, preview?.substringAfter(": "), mine, sender, image, time, unread, online)

    @Test fun login() = capture("01_login") {
        LoginContent(LoginUiState(email = "jonathan@correo.com"), {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun register() = capture("02_register") {
        RegisterContent(
            RegisterUiState(name = "Jonathan", email = "jonathan@", emailError = R.string.error_email_invalid),
            {}, {}, {}, {}, {}, {},
        )
    }

    @Test fun chats() = capture("03_chats") {
        ChatsContent(ChatsUiState(loading = false, me = User(uid = "me", displayName = "Jonathan R"), rows = rows), {}, {}, {}, {})
    }

    @Test fun chatsDark() = capture("04_chats_dark", dark = true) {
        ChatsContent(ChatsUiState(loading = false, me = User(uid = "me", displayName = "Jonathan R"), rows = rows), {}, {}, {}, {})
    }

    @Test fun chatsEmpty() = capture("05_chats_empty") {
        ChatsContent(ChatsUiState(loading = false, me = User(uid = "me", displayName = "Jonathan R")), {}, {}, {}, {})
    }

    private val conversation: List<ChatItem> = run {
        var t = now - 26 * hour
        fun b(id: String, text: String, mine: Boolean, first: Boolean, last: Boolean, status: MessageStatus = MessageStatus.READ, step: Long = 2 * minute) =
            ChatItem.Bubble(id, text, null, mine, null, null, if (mine) "me" else "vale", t.also { t += step }, status, first, last)
        val yesterday = listOf(
            ChatItem.DateHeader(t - hour),
            b("a", "Hola! ¿Cómo te fue en la entrevista?", false, true, true),
            b("b", "Muy bien, creo que me va a ir bien 🤞", true, true, false),
            b("c", "Me preguntaron mucho de Kotlin y Compose", true, false, true, step = 22 * hour),
        )
        val today = listOf(
            ChatItem.DateHeader(t - minute),
            b("d", "¡Qué buena noticia! Hay que celebrarlo", false, true, false),
            b("e", "¿Nos vemos a las 7 en el café?", false, false, true),
            b("f", "Claro, ahí nos vemos 🙌", true, true, false, MessageStatus.READ),
            b("g", "Llevo el portátil para mostrarte la app nueva", true, false, true, MessageStatus.SENT),
            b("h", "Va a quedar increíble", true, false, true, MessageStatus.PENDING),
        )
        (yesterday + today).reversed()
    }

    private val chatState = ChatUiState(
        loading = false,
        title = "Valentina Ríos",
        colorKey = "Valentina Ríos",
        subtitle = ChatSubtitle.Typing(null),
        items = conversation,
    )

    @Test fun chat() = capture("06_chat") {
        ChatContent(chatState, "", false, null, {}, {}, {}, {}, {}, {})
    }

    @Test fun chatDark() = capture("07_chat_dark", dark = true) {
        ChatContent(chatState.copy(subtitle = ChatSubtitle.Online), "Te escribo cuando salga", false, null, {}, {}, {}, {}, {}, {})
    }

    @Test fun groupChat() = capture("08_group_chat") {
        var t = now - 30 * minute
        fun b(id: String, text: String, sender: String?, key: String, first: Boolean, last: Boolean) =
            ChatItem.Bubble(id, text, null, sender == null, sender, null, key, t.also { t += 3 * minute }, MessageStatus.SENT, first, last)
        val items = listOf(
            ChatItem.DateHeader(t),
            b("1", "Ya subí los mockups a la carpeta", "Andrés Molina", "andres", true, false),
            b("2", "Revisen la pantalla de chat 👀", "Andrés Molina", "andres", false, true),
            b("3", "Me encantan los colores nuevos", "Laura Pérez", "laura", true, true),
            b("4", "Gracias! Mañana los integro en la app", null, "me", true, true),
        ).reversed()
        ChatContent(
            ChatUiState(
                loading = false, title = "Equipo Diseño", isGroup = true, colorKey = "2",
                subtitle = ChatSubtitle.Members(listOf("Andrés", "Laura", "Sofía")), items = items,
            ),
            "", false, null, {}, {}, {}, {}, {}, {},
        )
    }

    @Test fun newChat() = capture("09_new_chat") {
        val results = listOf(
            User(uid = "a", displayName = "Valentina Ríos", email = "vale@correo.com", about = "Diseñadora UX ✨"),
            User(uid = "b", displayName = "Valeria Castro", email = "valeria@correo.com"),
            User(uid = "c", displayName = "Valentín Ortiz", email = "vortiz@correo.com", about = "En el gimnasio 💪"),
        )
        NewChatContent("val", SearchState(query = "val", results = results), {}, {}, {}, {})
    }

    @Test fun profile() = capture("10_profile") {
        val user = User(uid = "me", displayName = "Jonathan Ramírez", email = "jonathan@correo.com", about = "Desarrollador Android")
        ProfileContent(ProfileUiState(user = user, name = user.displayName, about = user.about), {}, {}, {}, {}, {}, {}, {})
    }
}
