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

private val DarkColorScheme =
  darkColorScheme(
    primary = YemenGold,
    secondary = YemenGoldLight,
    tertiary = SoftEmerald,
    background = SlateBg,
    surface = SlateCard,
    onPrimary = SlateBg,
    onSecondary = SlateBg,
    onBackground = OffWhite,
    onSurface = OffWhite
  )

private val LightColorScheme =
  darkColorScheme( // Keep same premium luxury dark theme for consistency and luxury branding
    primary = YemenGold,
    secondary = YemenGoldLight,
    tertiary = SoftEmerald,
    background = SlateBg,
    surface = SlateCard,
    onPrimary = SlateBg,
    onSecondary = SlateBg,
    onBackground = OffWhite,
    onSurface = OffWhite
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color parameter left for signature compatibility
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme // Always use our luxury gold/slate theme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
