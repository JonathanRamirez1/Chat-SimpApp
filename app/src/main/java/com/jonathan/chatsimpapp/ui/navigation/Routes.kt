package com.jonathan.chatsimpapp.ui.navigation

import kotlinx.serialization.Serializable

// Rutas con tipos seguros de Navigation Compose.
@Serializable data object LoginRoute
@Serializable data object RegisterRoute
@Serializable data object ForgotPasswordRoute
@Serializable data object ChatsRoute
@Serializable data class ChatRoute(val chatId: String)
@Serializable data object NewChatRoute
@Serializable data object NewGroupRoute
@Serializable data object ProfileRoute
