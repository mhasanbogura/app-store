package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PolishPrimary,
    onPrimary = PolishOnPrimary,
    secondary = PolishSecondary,
    onSecondary = PolishOnSecondary,
    tertiary = PolishTertiary,
    onTertiary = PolishOnTertiary,
    background = PolishBackground,
    onBackground = PolishTextPrimary,
    surface = PolishSurface,
    onSurface = PolishTextPrimary,
    surfaceVariant = PolishSurfaceVariant,
    onSurfaceVariant = PolishTextSecondary,
    outline = PolishBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PolishLightPrimary,
    onPrimary = PolishLightOnPrimary,
    secondary = PolishLightSecondary,
    onSecondary = PolishLightOnSecondary,
    tertiary = PolishLightTertiary,
    onTertiary = PolishLightOnTertiary,
    background = PolishLightBackground,
    onBackground = PolishLightTextPrimary,
    surface = PolishLightSurface,
    onSurface = PolishLightTextPrimary,
    surfaceVariant = PolishLightSurfaceVariant,
    onSurfaceVariant = PolishLightTextSecondary,
    outline = PolishLightBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
