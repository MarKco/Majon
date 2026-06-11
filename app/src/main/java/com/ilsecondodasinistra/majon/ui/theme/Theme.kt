package com.ilsecondodasinistra.majon.ui.theme

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
import com.ilsecondodasinistra.majon.data.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = CreamSurface,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = Sage,
    onSecondary = CreamSurface,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,
    tertiary = Honey,
    onTertiary = InkBrown,
    tertiaryContainer = HoneyContainer,
    onTertiaryContainer = OnHoneyContainer,
    background = Cream,
    onBackground = InkBrown,
    surface = CreamSurface,
    onSurface = InkBrown,
    surfaceVariant = Color(0xFFF2E5DD),
    onSurfaceVariant = WarmGray,
    outline = Color(0xFF85736B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = Color(0xFF55200E),
    primaryContainer = TerracottaDeep,
    onPrimaryContainer = TerracottaContainer,
    secondary = Color(0xFFC2D2B0),
    onSecondary = Color(0xFF2D3A22),
    secondaryContainer = SageDeep,
    onSecondaryContainer = SageContainer,
    tertiary = Color(0xFFE8C95C),
    onTertiary = Color(0xFF3D2E00),
    background = DarkSurface,
    onBackground = DarkCream,
    surface = DarkSurfaceHigh,
    onSurface = DarkCream,
    surfaceVariant = Color(0xFF3C322D),
    onSurfaceVariant = Color(0xFFD8C2B8),
    outline = Color(0xFFA08D84),
)

@Composable
fun MajonTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MajonTypography,
        content = content,
    )
}
