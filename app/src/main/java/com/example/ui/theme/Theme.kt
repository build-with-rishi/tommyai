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
    primary = TealPrimary,
    secondary = TealSecondary,
    tertiary = CosmicSurfaceVariant,
    background = CosmicBackground, // Pure Black (0xFF000000)
    surface = CosmicSurface,       // Sleek card base (0xFF121318)
    surfaceVariant = CosmicSurfaceVariant,
    onPrimary = Color(0xFF000000), 
    onSecondary = Color(0xFF000000),
    onBackground = Color(0xFFF1F1F4), // Clean soft-white text
    onSurface = Color(0xFFF1F1F4),
    onSurfaceVariant = Color(0xFFC4C6D0)
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    secondary = TealSecondary,
    tertiary = CosmicSurfaceVariant,
    background = Color(0xFFF8FAFC), 
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun TommyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Set true to prioritize Material You dynamic coloring
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val baseScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme) {
                // Enforce black background with dynamic primary accents
                baseScheme.copy(
                    background = Color(0xFF000000),
                    surface = Color(0xFF121318), // Pitch-black card base
                    surfaceVariant = Color(0xFF1B1D23), // Border/secondary container
                    onBackground = Color(0xFFF1F1F4),
                    onSurface = Color(0xFFF1F1F4),
                    onSurfaceVariant = Color(0xFFC4C6D0)
                )
            } else {
                baseScheme
            }
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
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    TommyTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
