package com.example.echo.ui.player

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pulls the dominant colour out of the cover art with the Palette API so the player
 * background is a gradient of the artwork rather than a fixed slab. Falls back to the
 * theme's primary colour when the image can't be read.
 */
@Composable
fun rememberDominantColor(imageUrl: String?, fallback: Color): Color {
    val context = LocalContext.current
    var color by remember(imageUrl) { mutableStateOf(fallback) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) return@LaunchedEffect
        color = extractDominant(context, imageUrl, fallback)
    }
    return color
}

private suspend fun extractDominant(context: Context, url: String, fallback: Color): Color =
    withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)   // Palette needs to read pixels back
            .size(128)              // a thumbnail is plenty and keeps this cheap
            .build()

        val result = ImageLoader(context).execute(request)
        val bitmap = (result as? SuccessResult)?.drawable
            ?.let { (it as? android.graphics.drawable.BitmapDrawable)?.bitmap }
            ?: return@withContext fallback

        val palette = Palette.from(bitmap).clearFilters().generate()
        val argb = palette.vibrantSwatch?.rgb
            ?: palette.darkVibrantSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: fallback.toArgb()
        Color(argb)
    }
