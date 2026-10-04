package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = QuantCyan,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = QuantSky,
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = QuantEmerald,
    onTertiary = Color(0xFF022C22),
    background = LabDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = LabDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = LabDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = LabDarkBorder,
    error = QuantRose
)

private val LightColorScheme = lightColorScheme(
    primary = QuantCyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF115E59),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = LabLightBg,
    onBackground = Color(0xFF0F172A),
    surface = LabLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LabLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = LabLightBorder,
    error = QuantRose
)

@Composable
fun FootyResearchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent branding colors for research platform
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
