package com.jonathan.chatsimpapp

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.firebase.Timestamp
import com.jonathan.chatsimpapp.data.model.Announcement
import com.jonathan.chatsimpapp.data.model.Community
import com.jonathan.chatsimpapp.data.model.Member
import com.jonathan.chatsimpapp.data.model.Poll
import com.jonathan.chatsimpapp.data.model.Post
import com.jonathan.chatsimpapp.data.model.Report
import com.jonathan.chatsimpapp.data.model.ReportEvent
import com.jonathan.chatsimpapp.data.model.User
import com.jonathan.chatsimpapp.data.model.Vote
import com.jonathan.chatsimpapp.data.model.WeeklySummary
import com.jonathan.chatsimpapp.data.repository.CommunityContext
import com.jonathan.chatsimpapp.ui.board.BoardTab
import com.jonathan.chatsimpapp.ui.board.BoardUiState
import com.jonathan.chatsimpapp.ui.home.FeedTab
import com.jonathan.chatsimpapp.ui.home.FeedUiState
import com.jonathan.chatsimpapp.ui.home.HomeActions
import com.jonathan.chatsimpapp.ui.home.HomeScaffold
import com.jonathan.chatsimpapp.ui.home.HomeTab
import com.jonathan.chatsimpapp.ui.home.HomeUiState
import com.jonathan.chatsimpapp.ui.onboarding.OnboardingContent
import com.jonathan.chatsimpapp.ui.onboarding.OnboardingUiState
import com.jonathan.chatsimpapp.ui.polls.PollDetailContent
import com.jonathan.chatsimpapp.ui.polls.PollDetailUiState
import com.jonathan.chatsimpapp.ui.reports.ReportDetailContent
import com.jonathan.chatsimpapp.ui.reports.ReportDetailUiState
import com.jonathan.chatsimpapp.ui.reports.ReportsTab
import com.jonathan.chatsimpapp.ui.reports.ReportsUiState
import com.jonathan.chatsimpapp.ui.upgrade.UpgradeContent
import com.jonathan.chatsimpapp.ui.upgrade.UpgradeUiState
import com.jonathan.chatsimpapp.billing.ProOffer
import androidx.compose.foundation.layout.PaddingValues
import java.util.Date
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

    private fun ts(millis: Long) = Timestamp(Date(millis))

    private val community = Community(
        id = "c1", name = "Torres del Parque", address = "Cra 7 #24-89, Bogotá", unitsCount = 48,
        inviteCode = "K7P2QX", createdBy = "me", admins = listOf("me"),
    )
    private fun ctx(admin: Boolean = true, pro: Boolean = false) = CommunityContext(
        uid = "me",
        community = community.copy(
            admins = if (admin) listOf("me") else listOf("x"),
            plan = if (pro) Community.PLAN_PRO else Community.PLAN_FREE,
            planExpiresAt = if (pro) ts(now + 30 * day) else null,
        ),
        me = Member(uid = "me", unit = "502", role = if (admin) Member.ROLE_ADMIN else Member.ROLE_RESIDENT),
        all = listOf(community),
    )
    private val noActions = HomeActions({}, {}, {}, {}, {}, {}, {}, {})

    @Composable
    private fun home(tab: HomeTab, admin: Boolean = true, pro: Boolean = false, content: @Composable () -> Unit) {
        HomeScaffold(HomeUiState(ctx(admin, pro), User(uid = "me", displayName = "Jonathan R")), tab, {}, noActions) { content() }
    }

    @Test fun onboarding() = capture("00_onboarding") {
        OnboardingContent(OnboardingUiState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    private val feed = FeedUiState(
        loading = false,
        context = ctx(admin = true, pro = true),
        emergencies = listOf(
            Announcement(id = "e1", type = Announcement.TYPE_EMERGENCY, emergencyKind = "water", body = "Sale agua del cuarto de bombas", authorUnit = "203", createdAt = ts(now - 20 * minute)),
        ),
        announcements = listOf(
            Announcement(id = "a1", title = "Asamblea ordinaria", body = "Sábado 16 a las 10:00 en el salón comunal. Trae tu poder si vas a representar a otra unidad.", pinned = true, createdAt = ts(now - 2 * hour)),
            Announcement(id = "a2", title = "Fumigación de zonas comunes", body = "Martes desde las 8:00. Mantén a tus mascotas adentro.", createdAt = ts(now - day)),
        ),
        summary = WeeklySummary(
            overview = "Semana movida: se aprobó cambiar la empresa de vigilancia y sigue pendiente el arreglo del ascensor 2.",
            decisions = listOf("Se cambia la empresa de vigilancia desde diciembre (31 votos a favor)"),
            commitments = listOf("Apto 304 consigue tres cotizaciones para pintar la fachada", "Administración llama al técnico del ascensor el lunes"),
            notices = listOf("Asamblea ordinaria: sábado 16, 10:00"),
        ),
        openReports = 3,
        openPolls = 1,
        seenCounts = mapOf("a1" to 37L, "a2" to 41L),
    )

    @Test fun homeFeed() = capture("01_home") {
        home(HomeTab.FEED, pro = true) { FeedTab(feed, {}, {}, {}, {}, { _, _ -> }, {}, PaddingValues()) }
    }

    @Test fun homeFeedFree() = capture("02_home_free_dark", dark = true) {
        home(HomeTab.FEED, admin = false) {
            FeedTab(feed.copy(context = ctx(admin = false), summary = null, emergencies = emptyList(), seenCounts = emptyMap()), {}, {}, {}, {}, { _, _ -> }, {}, PaddingValues())
        }
    }

    private val poll = Poll(
        id = "p1", question = "¿Cambiamos la empresa de vigilancia a partir de diciembre?",
        description = "Cotización adjunta en el aviso del 2 de noviembre. Costo mensual similar, con un guarda más en la noche.",
        options = listOf("Sí, cambiar", "No, mantener la actual", "Me abstengo"), quorumPercent = 50,
        createdAt = ts(now - 3 * day), closesAt = ts(now + day),
    )

    @Test fun pollDetail() = capture("04_poll") {
        val votes = (0 until 27).map { Vote(unit = "${100 + it}", option = if (it % 5 == 0) 1L else if (it % 9 == 0) 2L else 0L) } +
            Vote(unit = "502", option = 0)
        PollDetailContent(PollDetailUiState(false, ctx(admin = true), poll, votes), false, {}, {}, {}, {}, {})
    }

    private val reports = listOf(
        Report(id = "r1", title = "Ascensor 2 no abre en el piso 5", category = "elevator", status = Report.STATUS_IN_PROGRESS,
            unit = "502", affectedUnits = listOf("502", "503", "504", "601", "702"), updatedAt = ts(now - 3 * hour),
            history = listOf(
                ReportEvent(Report.STATUS_OPEN, "", "502", ts(now - 2 * day)),
                ReportEvent(Report.STATUS_IN_PROGRESS, "El técnico viene el lunes a las 9:00", "101", ts(now - 3 * hour)),
            ),
            description = "Las puertas no abren y el ascensor sigue de largo. Pasa desde el domingo."),
        Report(id = "r2", title = "Bombillo fundido en el parqueadero", category = "electric", status = Report.STATUS_OPEN, affectedUnits = listOf("203"), updatedAt = ts(now - day)),
        Report(id = "r3", title = "Fuga en el tubo del sótano", category = "plumbing", status = Report.STATUS_RESOLVED, affectedUnits = listOf("101", "102"), updatedAt = ts(now - 4 * day)),
    )

    @Test fun reportsTab() = capture("05_reports") {
        home(HomeTab.REPORTS) { ReportsTab(ReportsUiState(false, ctx(), reports), {}, PaddingValues()) }
    }

    @Test fun reportDetail() = capture("06_report_detail") {
        ReportDetailContent(ReportDetailUiState(ctx(admin = false), reports.first().copy(createdBy = "x")), {}, { _, _ -> }, {})
    }

    @Test fun board() = capture("07_board") {
        val posts = listOf(
            Post(id = "1", type = "lend", text = "Presto taladro y escalera de 6 pasos. Me escriben y se los bajo.", authorId = "x", unit = "304", createdAt = ts(now - hour)),
            Post(id = "2", type = "recommend", text = "Recomiendo a Don Carlos, plomero: llegó en 1 hora y cobró justo. Tel. en el chat privado.", authorId = "y", unit = "1102", createdAt = ts(now - 5 * hour)),
            Post(id = "3", type = "lost", text = "Encontré unas llaves con llavero azul en el ascensor 1. Están en portería.", authorId = "me", unit = "502", createdAt = ts(now - day), resolved = true),
        )
        home(HomeTab.BOARD) { BoardTab(BoardUiState(false, ctx(), posts), {}, { _, _ -> }, {}, PaddingValues()) }
    }

    @Test fun upgrade() = capture("08_pro") {
        UpgradeContent(
            UpgradeUiState(ctx(admin = true), listOf(ProOffer("t", "US\$ 9,99", "P1M")), offersLoaded = true),
            {}, {},
        )
    }

    @Test fun login() = capture("10_login") {
        LoginContent(LoginUiState(email = "jonathan@correo.com"), {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun register() = capture("11_register") {
        RegisterContent(
            RegisterUiState(name = "Jonathan", email = "jonathan@", emailError = R.string.error_email_invalid),
            {}, {}, {}, {}, {}, {},
        )
    }

    @Test fun chats() = capture("03_chats") {
        ChatsContent(ChatsUiState(loading = false, me = User(uid = "me", displayName = "Jonathan R"), rows = rows), {}, {}, {}, {})
    }

    @Test fun chatsDark() = capture("12_chats_dark", dark = true) {
        ChatsContent(ChatsUiState(loading = false, me = User(uid = "me", displayName = "Jonathan R"), rows = rows), {}, {}, {}, {})
    }

    @Test fun chatsEmpty() = capture("13_chats_empty") {
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

    @Test fun chat() = capture("09_chat") {
        ChatContent(chatState, "", false, null, {}, {}, {}, {}, {}, {})
    }

    @Test fun chatDark() = capture("14_chat_dark", dark = true) {
        ChatContent(chatState.copy(subtitle = ChatSubtitle.Online), "Te escribo cuando salga", false, null, {}, {}, {}, {}, {}, {})
    }

    @Test fun groupChat() = capture("15_community_chat") {
        var t = now - 30 * minute
        fun b(id: String, text: String, sender: String?, key: String, first: Boolean, last: Boolean) =
            ChatItem.Bubble(id, text, null, sender == null, sender, null, key, t.also { t += 3 * minute }, MessageStatus.SENT, first, last)
        val items = listOf(
            ChatItem.DateHeader(t),
            b("1", "¿Alguien sabe si mañana hay agua?", "Andrés Molina · 304", "andres", true, false),
            b("2", "Tengo que lavar ropa 😅", "Andrés Molina · 304", "andres", false, true),
            b("3", "Sí, el corte es solo el jueves según el aviso", "Laura Pérez · 1102", "laura", true, true),
            b("4", "Confirmo, está en Avisos oficiales 👍", null, "me", true, true),
        ).reversed()
        ChatContent(
            ChatUiState(
                loading = false, title = "Torres del Parque", isGroup = true, colorKey = "c1",
                subtitle = ChatSubtitle.Members(listOf("Andrés", "Laura", "Sofía")), items = items,
            ),
            "", false, null, {}, {}, {}, {}, {}, {},
        )
    }

    @Test fun newChat() = capture("16_new_chat") {
        val results = listOf(
            User(uid = "a", displayName = "Valentina Ríos", email = "vale@correo.com", about = "Diseñadora UX ✨"),
            User(uid = "b", displayName = "Valeria Castro", email = "valeria@correo.com"),
            User(uid = "c", displayName = "Valentín Ortiz", email = "vortiz@correo.com", about = "En el gimnasio 💪"),
        )
        NewChatContent("val", SearchState(query = "val", results = results), {}, {}, {}, {})
    }

    @Test fun profile() = capture("17_profile") {
        val user = User(uid = "me", displayName = "Jonathan Ramírez", email = "jonathan@correo.com", about = "Desarrollador Android")
        ProfileContent(ProfileUiState(user = user, name = user.displayName, about = user.about), {}, {}, {}, {}, {}, {}, {})
    }
}
