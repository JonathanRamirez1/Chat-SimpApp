package com.jonathan.chatsimpapp.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jonathan.chatsimpapp.data.repository.CommunityState
import com.jonathan.chatsimpapp.ui.auth.ForgotPasswordScreen
import com.jonathan.chatsimpapp.ui.auth.LoginScreen
import com.jonathan.chatsimpapp.ui.auth.RegisterScreen
import com.jonathan.chatsimpapp.ui.chat.ChatScreen
import com.jonathan.chatsimpapp.ui.community.CommunityInfoScreen
import com.jonathan.chatsimpapp.ui.community.FormKind
import com.jonathan.chatsimpapp.ui.community.FormScreen
import com.jonathan.chatsimpapp.ui.group.NewGroupScreen
import com.jonathan.chatsimpapp.ui.home.HomeActions
import com.jonathan.chatsimpapp.ui.home.HomeScreen
import com.jonathan.chatsimpapp.ui.newchat.NewChatScreen
import com.jonathan.chatsimpapp.ui.onboarding.OnboardingScreen
import com.jonathan.chatsimpapp.ui.polls.PollDetailScreen
import com.jonathan.chatsimpapp.ui.profile.ProfileScreen
import com.jonathan.chatsimpapp.ui.reports.ReportDetailScreen
import com.jonathan.chatsimpapp.ui.upgrade.UpgradeScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Raíz de la navegación: según la sesión muestra login, la bienvenida a una comunidad o el inicio. */
private fun rootFor(state: CommunityState): Any = when (state) {
    CommunityState.SignedOut -> LoginRoute
    CommunityState.None -> OnboardingRoute
    else -> HomeRoute
}

@Composable
fun AppNavHost(
    session: StateFlow<CommunityState>,
    openChatRequests: Flow<String>,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val state by session.collectAsStateWithLifecycle()

    // Cambios de sesión (iniciar/cerrar sesión, unirse o salir de una comunidad) reemplazan la pila.
    LaunchedEffect(state) {
        if (state is CommunityState.Loading) return@LaunchedEffect
        val target = rootFor(state)
        val dest = navController.currentBackStackEntry?.destination ?: return@LaunchedEffect
        val onAuth = dest.hasRoute<LoginRoute>() || dest.hasRoute<RegisterRoute>() || dest.hasRoute<ForgotPasswordRoute>()
        val onOnboarding = dest.hasRoute<OnboardingRoute>()
        val wrongPlace = when (target) {
            LoginRoute -> !onAuth
            OnboardingRoute -> !onOnboarding
            else -> onAuth || (onOnboarding && navController.previousBackStackEntry == null)
        }
        if (wrongPlace) navController.navigate(target) { popUpTo(0) { inclusive = true } }
        else if (target == HomeRoute && onOnboarding) navController.popBackStack()
    }

    LaunchedEffect(Unit) {
        openChatRequests.collect { chatId ->
            if (session.value is CommunityState.Ready) {
                navController.navigate(ChatRoute(chatId)) { launchSingleTop = true }
            }
        }
    }

    val duration = 280
    NavHost(
        navController = navController,
        startDestination = rootFor(state),
        modifier = modifier,
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(duration)) + fadeIn(tween(duration)) },
        exitTransition = { slideOutOfContainer(SlideDirection.Start, tween(duration), targetOffset = { it / 4 }) + fadeOut(tween(duration)) },
        popEnterTransition = { slideIntoContainer(SlideDirection.End, tween(duration), initialOffset = { it / 4 }) + fadeIn(tween(duration)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(duration)) + fadeOut(tween(duration)) },
    ) {
        composable<LoginRoute> {
            LoginScreen(
                onRegister = { navController.navigate(RegisterRoute) },
                onForgotPassword = { navController.navigate(ForgotPasswordRoute) },
            )
        }
        composable<RegisterRoute> { RegisterScreen(onBack = navController::popBackStack) }
        composable<ForgotPasswordRoute> { ForgotPasswordScreen(onBack = navController::popBackStack) }
        composable<OnboardingRoute> { OnboardingScreen() }

        composable<HomeRoute> {
            HomeScreen(
                HomeActions(
                    openChat = { navController.navigate(ChatRoute(it)) },
                    newChat = { navController.navigate(NewChatRoute) },
                    profile = { navController.navigate(ProfileRoute) },
                    communityInfo = { navController.navigate(CommunityInfoRoute) },
                    upgrade = { navController.navigate(UpgradeRoute) },
                    openPoll = { navController.navigate(PollRoute(it)) },
                    openReport = { navController.navigate(ReportRoute(it)) },
                    form = { navController.navigate(FormRoute(it.name)) },
                ),
            )
        }
        composable<ChatRoute> { ChatScreen(onBack = navController::popBackStack) }
        composable<NewChatRoute> {
            NewChatScreen(
                onBack = navController::popBackStack,
                onNewGroup = { navController.navigate(NewGroupRoute) },
                onOpenChat = { chatId -> navController.navigate(ChatRoute(chatId)) { popUpTo<HomeRoute>() } },
            )
        }
        composable<NewGroupRoute> {
            NewGroupScreen(
                onBack = navController::popBackStack,
                onCreated = { chatId -> navController.navigate(ChatRoute(chatId)) { popUpTo<HomeRoute>() } },
            )
        }
        composable<ProfileRoute> { ProfileScreen(onBack = navController::popBackStack) }
        composable<CommunityInfoRoute> {
            CommunityInfoScreen(
                onBack = navController::popBackStack,
                onUpgrade = { navController.navigate(UpgradeRoute) },
                onAddCommunity = { navController.navigate(OnboardingRoute) },
            )
        }
        composable<UpgradeRoute> { UpgradeScreen(onClose = navController::popBackStack) }
        composable<PollRoute> {
            PollDetailScreen(onBack = navController::popBackStack, onUpgrade = { navController.navigate(UpgradeRoute) })
        }
        composable<ReportRoute> { ReportDetailScreen(onBack = navController::popBackStack) }
        composable<FormRoute> { entry ->
            val kind = FormKind.valueOf(entry.toRoute<FormRoute>().kind)
            FormScreen(kind, onDone = navController::popBackStack)
        }
    }
}
