package com.ilsecondodasinistra.majon.ui.theme

import androidx.compose.ui.graphics.Color
import com.ilsecondodasinistra.majon.domain.model.ProjectColor

// Craft palette — warm yarn-inspired tones
val Terracotta = Color(0xFFBF6B4F)
val TerracottaDeep = Color(0xFF8C4A35)
val TerracottaContainer = Color(0xFFFFDBCF)
val OnTerracottaContainer = Color(0xFF3A0B00)

val Sage = Color(0xFF7C8B6F)
val SageDeep = Color(0xFF4C5B42)
val SageContainer = Color(0xFFDEE8D0)
val OnSageContainer = Color(0xFF131F0B)

val Honey = Color(0xFFC9A227)
val HoneyContainer = Color(0xFFF7E5A8)
val OnHoneyContainer = Color(0xFF251A00)

val Cream = Color(0xFFF8F2EC)
val CreamSurface = Color(0xFFFFFBF7)
val WarmGray = Color(0xFF53433D)
val InkBrown = Color(0xFF221A16)

val DarkSurface = Color(0xFF1A1311)
val DarkSurfaceHigh = Color(0xFF2A211D)
val DarkCream = Color(0xFFF0E4DC)

// Selectable project colors
fun ProjectColor.toColor(): Color = when (this) {
    ProjectColor.TERRACOTTA -> Color(0xFFBF6B4F)
    ProjectColor.SAGE -> Color(0xFF7C8B6F)
    ProjectColor.OCEAN -> Color(0xFF4F7E9E)
    ProjectColor.SUNFLOWER -> Color(0xFFD9A82E)
    ProjectColor.FUCHSIA -> Color(0xFFC2548F)
    ProjectColor.LAVENDER -> Color(0xFF8B7BB8)
    ProjectColor.MOSS -> Color(0xFF5E7D3C)
    ProjectColor.CORAL -> Color(0xFFE07856)
    ProjectColor.SLATE -> Color(0xFF6B7680)
    ProjectColor.COCOA -> Color(0xFF7A5647)
}
