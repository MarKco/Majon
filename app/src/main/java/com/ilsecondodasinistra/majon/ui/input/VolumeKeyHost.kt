package com.ilsecondodasinistra.majon.ui.input

/**
 * Implemented by the activity so that a screen can claim the hardware volume keys while it is
 * visible. [volumeKeyHandler] receives `true` for volume up and `false` for volume down.
 */
interface VolumeKeyHost {
    var volumeKeyHandler: ((up: Boolean) -> Unit)?
}
