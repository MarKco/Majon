package com.ilsecondodasinistra.majon.ui.components

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads a picture from a local file, shrunk so its longest side is about [maxPx] pixels, off the main thread.
 * Null while loading or when the file is missing or unreadable.
 */
@Composable
fun rememberLocalImage(path: String?, maxPx: Int = 512): ImageBitmap? {
    val image by produceState<ImageBitmap?>(initialValue = null, path, maxPx) {
        value = if (path == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(path, bounds)
                    var sample = 1
                    while (bounds.outWidth / (sample * 2) >= maxPx || bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
                    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
                        ?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return image
}
