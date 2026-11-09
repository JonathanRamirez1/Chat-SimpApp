package com.jonathan.chatsimpapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.jonathan.chatsimpapp.ui.theme.OnlineGreen
import com.jonathan.chatsimpapp.ui.theme.colorForKey

/** Foto circular; si no hay foto, muestra las iniciales sobre un color estable para cada persona. */
@Composable
fun Avatar(
    name: String,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isGroup: Boolean = false,
    colorKey: String = name,
) {
    val fallback: @Composable () -> Unit = { InitialsAvatar(name, size, isGroup, colorKey) }
    Box(modifier.size(size).clip(CircleShape)) {
        if (photoUrl.isNullOrBlank()) {
            fallback()
        } else {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { fallback() },
                error = { fallback() },
                modifier = Modifier.size(size),
            )
        }
    }
}

@Composable
private fun InitialsAvatar(name: String, size: Dp, isGroup: Boolean, colorKey: String) {
    val color = colorForKey(colorKey)
    Box(
        Modifier
            .size(size)
            .background(color.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isGroup) {
            Icon(Icons.Rounded.Groups, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.5f))
        } else {
            Text(
                text = initials(name),
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.38f).sp,
            )
        }
    }
}

/** Avatar con el puntito verde de "en línea". */
@Composable
fun PresenceAvatar(
    name: String,
    photoUrl: String?,
    online: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    isGroup: Boolean = false,
    colorKey: String = name,
) {
    Box(modifier) {
        Avatar(name, photoUrl, size = size, isGroup = isGroup, colorKey = colorKey)
        if (online) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.28f)
                    .border(2.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .background(OnlineGreen, CircleShape),
            )
        }
    }
}

fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

