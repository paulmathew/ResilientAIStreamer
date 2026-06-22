package com.paulmathew.resilientaistreamer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = White,
    primaryContainer = OrangeLight,
    onPrimaryContainer = OrangeDark,

    secondary = Blue,
    onSecondary = White,
    secondaryContainer = BlueLight,
    onSecondaryContainer = BlueDark,

    tertiary = Red,
    onTertiary = White,
    tertiaryContainer = RedLight,
    onTertiaryContainer = Red,

    background = Canvas,
    onBackground = Navy,

    surface = White,
    onSurface = Navy,
    surfaceVariant = Color(0xFFF0F4F8),
    onSurfaceVariant = Slate,

    outline = Color(0xFFCBD5E1),
    outlineVariant = LightBorder,

    error = Red,
    onError = White,
    errorContainer = RedLight,
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFA15C),
    onPrimary = Color(0xFF4A1A00),
    primaryContainer = Color(0xFF703000),
    onPrimaryContainer = OrangeLight,

    secondary = Color(0xFF86AFFF),
    onSecondary = Color(0xFF002B6F),
    secondaryContainer = Color(0xFF173B72),
    onSecondaryContainer = BlueLight,

    tertiary = Color(0xFFFF8A8A),
    onTertiary = Color(0xFF5F0005),

    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),

    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),

    error = Color(0xFFFF8A8A),
    errorContainer = Color(0xFF68151B),
    onErrorContainer = RedLight,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun ResilientAIStreamerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current

            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}