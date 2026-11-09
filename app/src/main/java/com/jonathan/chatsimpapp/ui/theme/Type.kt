package com.jonathan.chatsimpapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jonathan.chatsimpapp.R

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val PlusJakartaSans = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

private val base = Typography()

private fun TextStyle.jakarta(weight: FontWeight? = null) =
    copy(fontFamily = PlusJakartaSans, fontWeight = weight ?: fontWeight)

val AppTypography = Typography(
    displayLarge = base.displayLarge.jakarta(FontWeight.Bold),
    displayMedium = base.displayMedium.jakarta(FontWeight.Bold),
    displaySmall = base.displaySmall.jakarta(FontWeight.Bold),
    headlineLarge = base.headlineLarge.jakarta(FontWeight.Bold),
    headlineMedium = base.headlineMedium.jakarta(FontWeight.Bold).copy(letterSpacing = (-0.25).sp),
    headlineSmall = base.headlineSmall.jakarta(FontWeight.Bold),
    titleLarge = base.titleLarge.jakarta(FontWeight.Bold),
    titleMedium = base.titleMedium.jakarta(FontWeight.SemiBold),
    titleSmall = base.titleSmall.jakarta(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.jakarta(),
    bodyMedium = base.bodyMedium.jakarta(),
    bodySmall = base.bodySmall.jakarta(),
    labelLarge = base.labelLarge.jakarta(FontWeight.SemiBold),
    labelMedium = base.labelMedium.jakarta(FontWeight.Medium),
    labelSmall = base.labelSmall.jakarta(FontWeight.Medium),
)
