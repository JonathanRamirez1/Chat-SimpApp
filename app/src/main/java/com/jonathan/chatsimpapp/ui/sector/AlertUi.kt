package com.jonathan.chatsimpapp.ui.sector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.AlertGroup
import com.jonathan.chatsimpapp.data.model.AlertType

/** Colores fijos y siempre acompañados de ícono y palabra (nadie tiene que adivinar). */
data class GroupStyle(val strong: Color, val soft: Color, val label: Int)

fun styleOf(group: AlertGroup): GroupStyle = when (group) {
    AlertGroup.SECURITY -> GroupStyle(Color(0xFFDC2626), Color(0xFFFEE2E2), R.string.alert_group_security)
    AlertGroup.DAMAGE -> GroupStyle(Color(0xFFEA580C), Color(0xFFFFEDD5), R.string.alert_group_damage)
    AlertGroup.OTHER -> GroupStyle(Color(0xFF2563EB), Color(0xFFDBEAFE), R.string.alert_group_other)
}

fun iconOf(type: AlertType): ImageVector = when (type) {
    AlertType.THEFT -> Icons.Rounded.Shield
    AlertType.STREETLIGHT -> Icons.Rounded.Lightbulb
    AlertType.ROAD -> Icons.Rounded.Warning
    AlertType.WATER -> Icons.Rounded.WaterDrop
    AlertType.TRASH -> Icons.Rounded.Delete
    AlertType.NOISE -> Icons.AutoMirrored.Rounded.VolumeUp
}

fun labelOf(type: AlertType): Int = when (type) {
    AlertType.THEFT -> R.string.alert_theft
    AlertType.STREETLIGHT -> R.string.alert_streetlight
    AlertType.ROAD -> R.string.alert_road
    AlertType.WATER -> R.string.alert_water
    AlertType.TRASH -> R.string.alert_trash
    AlertType.NOISE -> R.string.alert_noise
}

/** Pin redondo grande con borde blanco: se ve bien sobre cualquier mapa. */
@Composable
fun AlertPin(type: AlertType, size: Dp = 48.dp) {
    val style = styleOf(type.group)
    Box(
        Modifier
            .shadow(6.dp, CircleShape)
            .size(size)
            .background(style.strong, CircleShape)
            .border(4.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(iconOf(type), null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun HomePin(size: Dp = 52.dp) {
    Box(
        Modifier
            .shadow(6.dp, CircleShape)
            .size(size)
            .background(Color(0xFF4F46E5), CircleShape)
            .border(4.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Home, null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}
