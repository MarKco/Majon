package com.ilsecondodasinistra.majon.ui.stash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.formatMillimeters
import com.ilsecondodasinistra.majon.domain.model.formatSkeins
import java.text.DecimalFormatSymbols
import kotlin.math.roundToInt

/** The decimal separator of the user's language, for numbers shown in the stash. */
@Composable
fun decimalSeparator(): Char = DecimalFormatSymbols.getInstance(LocalLocale.current.platformLocale).decimalSeparator

/** "2½ gomitoli" / "½ gomitolo" / "Finita". */
@Composable
fun skeinsText(value: Double): String {
    if (value <= 0.0) return stringResource(R.string.yarn_used_up)
    val number = formatSkeins(value, decimalSeparator())
    return stringResource(if (value <= 1.0) R.string.skein_one else R.string.skein_many, number)
}

/** "2½ gomitoli · 250 g · 875 m", leaving out what the skein size does not allow. */
@Composable
fun stockText(yarn: Yarn): String {
    val parts = buildList {
        add(skeinsText(yarn.stockSkeins))
        if (yarn.stockSkeins > 0) {
            yarn.stockGrams?.let { add("${it.roundToInt()} g") }
            yarn.stockMeters?.let { add("${it.roundToInt()} m") }
        }
    }
    return parts.joinToString(" · ")
}

/** "Merino 70% · Seta 30%". */
@Composable
fun compositionText(yarn: Yarn): String = yarn.materials.map { share ->
    // map is inline, so the string resource can be read here (joinToString's lambda is not composable)
    val name = share.customName ?: stringResource(share.material.labelRes())
    if (share.percent != null) "$name ${share.percent}%" else name
}.joinToString(" · ")

@Composable
fun needleSizeText(needle: Needle): String =
    needle.sizeMm?.let { stringResource(R.string.diagram_needles, formatMillimeters(it, decimalSeparator())) }
        ?: stringResource(R.string.needle_unknown_size)

/**
 * The yarn's colour: a little ball of yarn in that colour (a neutral one when no colour is set), or, with [flat],
 * a plain round swatch (used for the choices in the colour picker).
 */
@Composable
fun ColorDot(argb: Int?, size: Dp = 40.dp, modifier: Modifier = Modifier, flat: Boolean = false) {
    if (flat && argb != null) {
        Box(
            modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(argb))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        )
    } else {
        // Without a colour: a warm neutral, so the ball still reads as yarn
        com.ilsecondodasinistra.majon.ui.components.YarnBall(
            color = if (argb != null) Color(argb) else Color(0xFFD9B8A4),
            modifier = modifier,
            size = size,
        )
    }
}

/** Colours offered in the yarn editor. */
val YarnColorChoices: List<Int> = listOf(
    0xFFF5F0E6, 0xFFE8D9B5, 0xFFE0A800, 0xFFF08A24, 0xFFD64545, 0xFFC2185B, 0xFF8E4585, 0xFF5B6FD6,
    0xFF2A8F9E, 0xFF3C9D5B, 0xFF7A8450, 0xFF8B5E3C, 0xFF9E9E9E, 0xFF2B2B2B,
).map { it.toInt() }

/** A wrapping row of single-choice chips; tapping the selected one clears it when [clearable]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipChoices(
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    clearable: Boolean = true,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(if (option == selected && clearable) null else option) },
                label = { Text(label(option)) },
            )
        }
    }
}

/** Makes any row tappable as a whole (used for list cards without their own Card onClick). */
fun Modifier.tappable(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
