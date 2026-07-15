package com.example.echo.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.echo.R

val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium,  FontWeight.Medium),
    Font(R.font.vazirmatn_bold,    FontWeight.Bold),
)

//private val Brand: FontFamily = FontFamily.Default

private val tight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(size: Int, line: Int, weight: FontWeight, spacing: Double = 0.0) = TextStyle(
    fontFamily = Vazir,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp,
    lineHeightStyle = tight,
)

val EchoTypography = Typography(
    displayLarge  = style(44, 52, FontWeight.Bold, (-0.5)),
    displayMedium = style(34, 42, FontWeight.Bold, (-0.25)),
    displaySmall  = style(28, 36, FontWeight.SemiBold),

    headlineLarge  = style(26, 34, FontWeight.SemiBold),
    headlineMedium = style(22, 30, FontWeight.SemiBold),
    headlineSmall  = style(20, 28, FontWeight.SemiBold),

    titleLarge  = style(18, 26, FontWeight.SemiBold),
    titleMedium = style(16, 24, FontWeight.Medium, 0.1),
    titleSmall  = style(14, 20, FontWeight.Medium, 0.1),

    bodyLarge  = style(16, 24, FontWeight.Normal, 0.15),
    bodyMedium = style(14, 20, FontWeight.Normal, 0.15),
    bodySmall  = style(12, 16, FontWeight.Normal, 0.2),

    labelLarge  = style(14, 20, FontWeight.Medium, 0.1),
    labelMedium = style(12, 16, FontWeight.Medium, 0.4),
    labelSmall  = style(11, 14, FontWeight.Medium, 0.5),
)

/** Applied by the user's font-size preference from DataStore. */
fun Typography.scaled(factor: Float): Typography = Typography(
    displayLarge = displayLarge.scale(factor),
    displayMedium = displayMedium.scale(factor),
    displaySmall = displaySmall.scale(factor),
    headlineLarge = headlineLarge.scale(factor),
    headlineMedium = headlineMedium.scale(factor),
    headlineSmall = headlineSmall.scale(factor),
    titleLarge = titleLarge.scale(factor),
    titleMedium = titleMedium.scale(factor),
    titleSmall = titleSmall.scale(factor),
    bodyLarge = bodyLarge.scale(factor),
    bodyMedium = bodyMedium.scale(factor),
    bodySmall = bodySmall.scale(factor),
    labelLarge = labelLarge.scale(factor),
    labelMedium = labelMedium.scale(factor),
    labelSmall = labelSmall.scale(factor),
)

private fun TextStyle.scale(f: Float) = copy(
    fontSize = fontSize * f,
    lineHeight = lineHeight * f,
)
