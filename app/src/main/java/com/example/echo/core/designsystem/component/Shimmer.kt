package com.example.echo.core.designsystem.component

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.echo.core.designsystem.theme.EchoTheme

/**
 * Skeleton loading. A diagonal highlight sweeps across the placeholder while data is in flight.
 * Every list and card in the app shows this instead of a spinner.
 */
@Composable
fun Modifier.shimmer(shape: Shape = MaterialTheme.shapes.small): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val width = 600f
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(progress * 2 * width - width, 0f),
        end = Offset(progress * 2 * width, width),
    )
    return this.clip(shape).background(brush)
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) {
    Box(modifier.shimmer(shape))
}

/** Placeholder for a horizontal rail of album cards. */
@Composable
fun ShimmerCardRow(count: Int = 4, @SuppressLint("ModifierParameter") modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = EchoTheme.spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.gutter),
    ) {
        repeat(count) {
            Column(verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm)) {
                ShimmerBox(
                    Modifier.size(EchoTheme.sizes.artCard),
                    MaterialTheme.shapes.medium,
                )
                ShimmerBox(Modifier.width(EchoTheme.sizes.artCard).height(14.dp))
                ShimmerBox(Modifier.width(80.dp).height(12.dp))
            }
        }
    }
}

/** Placeholder for a vertical list of song rows. */
@Composable
fun ShimmerSongList(count: Int = 6, @SuppressLint("ModifierParameter") modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = EchoTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.md),
    ) {
        repeat(count) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.gutter),
                verticalAlignment = Alignment0,
            ) {
                ShimmerBox(Modifier.size(EchoTheme.sizes.artThumb), MaterialTheme.shapes.small)
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
                ) {
                    ShimmerBox(Modifier.fillMaxWidth(0.6f).height(14.dp))
                    ShimmerBox(Modifier.fillMaxWidth(0.35f).height(12.dp))
                }
            }
        }
    }
}

private val Alignment0 = Alignment.CenterVertically
