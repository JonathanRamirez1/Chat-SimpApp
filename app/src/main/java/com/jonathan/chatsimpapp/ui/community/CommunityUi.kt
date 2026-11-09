package com.jonathan.chatsimpapp.ui.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Elevator
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Plumbing
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.Report

/** Etiqueta redondeada de color (estado, tipo, plan...). */
@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Surface(shape = CircleShape, color = container, contentColor = content, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Pill("PRO", Color(0xFFFFE8A3), Color(0xFF7A5200), modifier, Icons.Rounded.WorkspacePremium)
}

private val Amber = Color(0xFFB45309)
private val AmberBg = Color(0xFFFEF3C7)
private val Blue = Color(0xFF1D4ED8)
private val BlueBg = Color(0xFFDBEAFE)
private val Green = Color(0xFF047857)
private val GreenBg = Color(0xFFD1FAE5)

@Composable
fun ReportStatusPill(status: String, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (status) {
        Report.STATUS_IN_PROGRESS -> Triple(R.string.status_in_progress, BlueBg, Blue)
        Report.STATUS_RESOLVED -> Triple(R.string.status_resolved, GreenBg, Green)
        else -> Triple(R.string.status_open, AmberBg, Amber)
    }
    Pill(stringResource(label), bg, fg, modifier)
}

fun reportCategoryIcon(category: String): ImageVector = when (category) {
    "plumbing" -> Icons.Rounded.Plumbing
    "electric" -> Icons.Rounded.Bolt
    "elevator" -> Icons.Rounded.Elevator
    "security" -> Icons.Rounded.Shield
    "cleaning" -> Icons.Rounded.CleaningServices
    "common_areas" -> Icons.Rounded.Park
    "noise" -> Icons.AutoMirrored.Rounded.VolumeUp
    else -> Icons.Rounded.Construction
}

fun reportCategoryLabel(category: String): Int = when (category) {
    "plumbing" -> R.string.cat_plumbing
    "electric" -> R.string.cat_electric
    "elevator" -> R.string.cat_elevator
    "security" -> R.string.cat_security
    "cleaning" -> R.string.cat_cleaning
    "common_areas" -> R.string.cat_common_areas
    "noise" -> R.string.cat_noise
    else -> R.string.cat_other
}

fun postTypeIcon(type: String): ImageVector = when (type) {
    "lend" -> Icons.Rounded.VolunteerActivism
    "borrow" -> Icons.Rounded.Handshake
    "recommend" -> Icons.Rounded.ThumbUp
    "lost" -> Icons.Rounded.Search
    "sell" -> Icons.Rounded.Sell
    else -> Icons.Rounded.ChatBubble
}

fun postTypeLabel(type: String): Int = when (type) {
    "lend" -> R.string.post_lend
    "borrow" -> R.string.post_borrow
    "recommend" -> R.string.post_recommend
    "lost" -> R.string.post_lost
    "sell" -> R.string.post_sell
    else -> R.string.post_other
}

/** Tipos de emergencia que cualquier vecino puede lanzar. */
val EmergencyKinds = listOf(
    "water" to R.string.emergency_water,
    "gas" to R.string.emergency_gas,
    "fire" to R.string.emergency_fire,
    "security" to R.string.emergency_security,
    "power" to R.string.emergency_power,
    "other" to R.string.emergency_other,
)
