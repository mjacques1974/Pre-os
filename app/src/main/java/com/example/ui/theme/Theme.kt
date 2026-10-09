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

private val LightColorScheme = lightColorScheme(
    primary = BasilGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = BasilContainer,
    onPrimaryContainer = OnBasilContainer,
    secondary = OliveGold,
    onSecondary = Color.White,
    secondaryContainer = OliveGoldContainer,
    onSecondaryContainer = OnOliveGoldContainer,
    tertiary = Terracotta,
    onTertiary = Color.White,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = OnTerracottaContainer,
    background = CreamBackground,
    onBackground = Color(0xFF191C19),
    surface = WhiteSurface,
    onSurface = Color(0xFF191C19),
    surfaceVariant = SurfaceVariantWarm,
    onSurfaceVariant = Color(0xFF424940),
    outline = OutlineSoft,
    outlineVariant = Color(0xFFC2C9BD)
)

private val DarkColorScheme = darkColorScheme(
    primary = BasilGreenLight,
    onPrimary = Color(0xFF00390E),
    primaryContainer = BasilGreenDark,
    onPrimaryContainer = BasilContainer,
    secondary = OliveGoldContainer,
    onSecondary = Color(0xFF383200),
    secondaryContainer = Color(0xFF514A00),
    onSecondaryContainer = OliveGoldContainer,
    tertiary = Color(0xFFFFB59F),
    onTertiary = Color(0xFF601400),
    tertiaryContainer = Terracotta,
    onTertiaryContainer = TerracottaContainer,
    background = Color(0xFF111411),
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF111411),
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),
    outline = OutlineSoft
)

@Composable
fun PestoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinctive brand basil identity
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
