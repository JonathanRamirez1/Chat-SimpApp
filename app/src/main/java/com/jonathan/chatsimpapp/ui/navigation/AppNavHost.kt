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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jonathan.chatsimpapp.ui.auth.ForgotPasswordScreen
import com.jonathan.chatsimpapp.ui.auth.LoginScreen
import com.jonathan.chatsimpapp.ui.auth.RegisterScreen
import com.jonathan.chatsimpapp.ui.chat.ChatScreen
import com.jonathan.chatsimpapp.ui.chats.ChatsScreen
import com.jonathan.chatsimpapp.ui.group.NewGroupScreen
import com.jonathan.chatsimpapp.ui.newchat.NewChatScreen
import com.jonathan.chatsimpapp.ui.profile.ProfileScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AppNavHost(
    signedIn: StateFlow<Boolean>,
    openChatRequests: Flow<String>,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val isSignedIn by signedIn.collectAsStateWithLifecycle()

    // Al iniciar o cerrar sesión se reemplaza toda la pila de pantallas.
    LaunchedEffect(isSignedIn) {
        val target: Any = if (isSignedIn) ChatsRoute else LoginRoute
        val current = navController.currentBackStackEntry?.destination ?: return@LaunchedEffect
        val onAuthSide = current.route?.contains("Login") == true ||
            current.route?.contains("Register") == true ||
            current.route?.contains("ForgotPassword") == true
        if (isSignedIn == onAuthSide) {
            navController.navigate(target) { popUpTo(0) { inclusive = true } }
        }
    }

    // Abrir un chat desde una notificación.
    LaunchedEffect(Unit) {
        openChatRequests.collect { chatId ->
            if (signedIn.value) {
                navController.navigate(ChatRoute(chatId)) {
                    popUpTo<ChatsRoute>()
                    launchSingleTop = true
                }
            }
        }
    }

    val duration = 280
    NavHost(
        navController = navController,
        startDestination = if (signedIn.value) ChatsRoute else LoginRoute,
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

        composable<ChatsRoute> {
            ChatsScreen(
                onOpenChat = { navController.navigate(ChatRoute(it)) },
                onNewChat = { navController.navigate(NewChatRoute) },
                onProfile = { navController.navigate(ProfileRoute) },
            )
        }
        composable<ChatRoute> { ChatScreen(onBack = navController::popBackStack) }
        composable<NewChatRoute> {
            NewChatScreen(
                onBack = navController::popBackStack,
                onNewGroup = { navController.navigate(NewGroupRoute) },
                onOpenChat = { chatId ->
                    navController.navigate(ChatRoute(chatId)) { popUpTo<ChatsRoute>() }
                },
            )
        }
        composable<NewGroupRoute> {
            NewGroupScreen(
                onBack = navController::popBackStack,
                onCreated = { chatId -> navController.navigate(ChatRoute(chatId)) { popUpTo<ChatsRoute>() } },
            )
        }
        composable<ProfileRoute> { ProfileScreen(onBack = navController::popBackStack) }
    }
}
