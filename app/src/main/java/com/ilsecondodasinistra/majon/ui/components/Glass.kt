package com.ilsecondodasinistra.majon.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/** Haze state of the screen content behind the glass bar (provided by the nav host). */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Frosted glass over whatever is registered as haze source. With [blur] = false it is an almost opaque
 * surface instead: blur is redrawn whenever the content behind it changes, so skip it where something
 * animates constantly under the bar.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.glass(state: HazeState? = LocalHazeState.current, blur: Boolean = true): Modifier {
    state ?: return background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
    if (!blur) return background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
    return hazeEffect(state = state, style = HazeMaterials.thin(MaterialTheme.colorScheme.surface))
}

/** Height of the bottom bar floating over the content (0 where there is none), so lists can leave room. */
val LocalBottomBarHeight = compositionLocalOf<Dp> { 0.dp }

/** Stops taps and drags from reaching what is drawn under a translucent bar. */
fun Modifier.consumeTouches(): Modifier = pointerInput(Unit) {}
