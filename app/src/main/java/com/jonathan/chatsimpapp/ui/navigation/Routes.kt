package com.jonathan.chatsimpapp.ui.navigation

import kotlinx.serialization.Serializable

// Rutas con tipos seguros de Navigation Compose.
@Serializable data object LoginRoute
@Serializable data object RegisterRoute
@Serializable data object ForgotPasswordRoute
@Serializable data object OnboardingRoute
@Serializable data object HomeRoute
@Serializable data class ChatRoute(val chatId: String)
@Serializable data object NewChatRoute
@Serializable data object NewGroupRoute
@Serializable data object ProfileRoute
@Serializable data object CommunityInfoRoute
@Serializable data object UpgradeRoute
@Serializable data class PollRoute(val pollId: String)
@Serializable data class ReportRoute(val reportId: String)
@Serializable data class FormRoute(val kind: String)
