package com.ilsecondodasinistra.majon.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ilsecondodasinistra.majon.data.settings.ColorPalette
import com.ilsecondodasinistra.majon.data.settings.ThemeMode

private val ClassicLight = lightColorScheme(
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
    outlineVariant = Color(0xFFD8C2B8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCF5EF),
    surfaceContainer = Color(0xFFF7EEE7),
    surfaceContainerHigh = Color(0xFFF1E7DF),
    surfaceContainerHighest = Color(0xFFEBE0D8),
)

private val ClassicDark = darkColorScheme(
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
    surface = DarkSurface,
    onSurface = DarkCream,
    surfaceVariant = Color(0xFF3C322D),
    onSurfaceVariant = Color(0xFFD8C2B8),
    outline = Color(0xFFA08D84),
    outlineVariant = Color(0xFF53433D),
    surfaceContainerLowest = Color(0xFF140E0C),
    surfaceContainerLow = Color(0xFF221A17),
    surfaceContainer = DarkSurfaceHigh,
    surfaceContainerHigh = Color(0xFF352B26),
    surfaceContainerHighest = Color(0xFF403531),
)

// Cheerful — bright coral, fresh green and sunshine yellow on a luminous cream.
private val CheerfulLight = lightColorScheme(
    primary = Color(0xFFD2664F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF8DFD6),
    onPrimaryContainer = Color(0xFF3F1709),
    secondary = Color(0xFF4F8A69),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9EBDF),
    onSecondaryContainer = Color(0xFF12291B),
    tertiary = Color(0xFFE0B454),
    onTertiary = Color(0xFF3A2A00),
    tertiaryContainer = Color(0xFFF6E8BF),
    onTertiaryContainer = Color(0xFF231B00),
    background = Color(0xFFFFF8EE),
    onBackground = Color(0xFF2B1D14),
    surface = Color(0xFFFFFBF4),
    onSurface = Color(0xFF2B1D14),
    surfaceVariant = Color(0xFFF5E9DD),
    onSurfaceVariant = Color(0xFF5A4636),
    outline = Color(0xFF8A7566),
    outlineVariant = Color(0xFFE6D6C6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCF4E8),
    surfaceContainer = Color(0xFFF9EEDF),
    surfaceContainerHigh = Color(0xFFF7EADB),
    surfaceContainerHighest = Color(0xFFF3E4D2),
)

private val CheerfulDark = darkColorScheme(
    primary = Color(0xFFEDB3A4),
    onPrimary = Color(0xFF601500),
    primaryContainer = Color(0xFF9C4C3B),
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = Color(0xFF8FC9A5),
    onSecondary = Color(0xFF00391C),
    secondaryContainer = Color(0xFF2F624A),
    onSecondaryContainer = Color(0xFFC8F2D4),
    tertiary = Color(0xFFE3C87E),
    onTertiary = Color(0xFF3A2A00),
    tertiaryContainer = Color(0xFF574A22),
    onTertiaryContainer = Color(0xFFFFEDA6),
    background = Color(0xFF1F1512),
    onBackground = Color(0xFFF8E6DC),
    surface = Color(0xFF1F1512),
    onSurface = Color(0xFFF8E6DC),
    surfaceVariant = Color(0xFF45342D),
    onSurfaceVariant = Color(0xFFE2C8BC),
    outline = Color(0xFFAA9084),
    outlineVariant = Color(0xFF5A4639),
    surfaceContainerLowest = Color(0xFF190F0C),
    surfaceContainerLow = Color(0xFF281C18),
    surfaceContainer = Color(0xFF30221D),
    surfaceContainerHigh = Color(0xFF3B2C26),
    surfaceContainerHighest = Color(0xFF473731),
)

// Forest — emerald and teal with a lime accent on pale mint.
private val ForestLight = lightColorScheme(
    primary = Color(0xFF1F7A5A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F0D8),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF2B7A8A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBFEAF2),
    onSecondaryContainer = Color(0xFF001F25),
    tertiary = Color(0xFFB5C927),
    onTertiary = Color(0xFF1E2400),
    tertiaryContainer = Color(0xFFE4F28A),
    onTertiaryContainer = Color(0xFF1A1F00),
    background = Color(0xFFF1FAF5),
    onBackground = Color(0xFF13201A),
    surface = Color(0xFFF7FDF9),
    onSurface = Color(0xFF13201A),
    surfaceVariant = Color(0xFFDDEBE3),
    onSurfaceVariant = Color(0xFF3F4F46),
    outline = Color(0xFF6F8077),
    outlineVariant = Color(0xFFBFD0C6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2FAF6),
    surfaceContainer = Color(0xFFECF5F0),
    surfaceContainerHigh = Color(0xFFE5EFEA),
    surfaceContainerHighest = Color(0xFFDFEAE4),
)

private val ForestDark = darkColorScheme(
    primary = Color(0xFF74DBB4),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF0F5A42),
    onPrimaryContainer = Color(0xFFB7F0D8),
    secondary = Color(0xFF7FD3E3),
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF1A5966),
    onSecondaryContainer = Color(0xFFBFEAF2),
    tertiary = Color(0xFFCADF4F),
    onTertiary = Color(0xFF2E3600),
    tertiaryContainer = Color(0xFF444F00),
    onTertiaryContainer = Color(0xFFE4F28A),
    background = Color(0xFF0E1814),
    onBackground = Color(0xFFDCEAE2),
    surface = Color(0xFF0E1814),
    onSurface = Color(0xFFDCEAE2),
    surfaceVariant = Color(0xFF2C3B34),
    onSurfaceVariant = Color(0xFFBFD0C6),
    outline = Color(0xFF899A91),
    outlineVariant = Color(0xFF3F4F46),
    surfaceContainerLowest = Color(0xFF09120F),
    surfaceContainerLow = Color(0xFF121E19),
    surfaceContainer = Color(0xFF17251F),
    surfaceContainerHigh = Color(0xFF22302A),
    surfaceContainerHighest = Color(0xFF2C3B35),
)

private fun paletteScheme(palette: ColorPalette, dark: Boolean) = when (palette) {
    ColorPalette.CHEERFUL -> if (dark) CheerfulDark else CheerfulLight
    ColorPalette.CLASSIC -> if (dark) ClassicDark else ClassicLight
    ColorPalette.FOREST -> if (dark) ForestDark else ForestLight
}

private val MajonShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MajonTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: ColorPalette = ColorPalette.CHEERFUL,
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
        else -> paletteScheme(palette, darkTheme)
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = MajonTypography,
        shapes = MajonShapes,
        content = content,
    )
}
