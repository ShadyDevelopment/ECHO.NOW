package com.echonow.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import com.echonow.domain.repository.UserPreferences

object EchoTokens {
    val background = Color(0xFF0D0F12)
    val surface = Color(0xFF15191F)
    val elevatedSurface = Color(0xFF1C222A)
    val primaryText = Color(0xFFF5F7FA)
    val secondaryText = Color(0xFFB2BAC5)
    val badgeBackground = Color.White.copy(alpha = .12f)
    val badgeBorder = Color.White.copy(alpha = .15f)
    val focusBackgroundAlpha = .24f
    val cardCornerRadius = 14.dp
    val contentSafePadding = 24.dp
}

data class ButtonCustomization(
    val shape: Shape,
    val fillColor: Color?,
    val textColor: Color?,
    val borderColor: Color?,
)

val LocalButtonCustomization = staticCompositionLocalOf {
    ButtonCustomization(RoundedCornerShape(12.dp), null, null, null)
}

private val DefaultDark = darkColorScheme(
    background = EchoTokens.background,
    surface = EchoTokens.surface,
    primary = Color(0xFF607D8B),
    secondary = Color(0xFF00838F),
    tertiary = Color(0xFF31A7A0),
)

private val DefaultLight = lightColorScheme(
    primary = Color(0xFF6740C8),
    secondary = Color(0xFFB52F62),
)

@Composable
fun EchoTheme(preferences: UserPreferences, content: @Composable () -> Unit) {
    val dark = when (preferences.theme) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }
    val primary = Color(preferences.primaryColor)
    val secondary = Color(preferences.secondaryColor)
    val tertiary = Color(preferences.tertiaryColor)
    val colors = if (dark) {
        DefaultDark.copy(primary = primary, secondary = secondary, tertiary = tertiary)
    } else {
        DefaultLight.copy(primary = primary, secondary = secondary, tertiary = tertiary)
    }
    val buttonShape: Shape = when (preferences.buttonShape) {
        "Pill" -> CircleShape
        "Square" -> RoundedCornerShape(0.dp)
        else -> RoundedCornerShape(12.dp)
    }
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(
            LocalButtonCustomization provides ButtonCustomization(
                shape = buttonShape,
                fillColor = preferences.buttonFillColor?.let(::Color),
                textColor = preferences.buttonTextColor?.let(::Color),
                borderColor = preferences.buttonBorderColor?.let(::Color),
            ),
            content = content,
        )
    }
}
