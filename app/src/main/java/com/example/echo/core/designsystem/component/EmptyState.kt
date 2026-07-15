package com.example.echo.core.designsystem.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.echo.core.designsystem.theme.EchoTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Empty screens are an invitation, not an apology: a small orbiting signal, a plain statement
 * of what's missing, and one concrete next step. Drawn with Canvas — no Lottie.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val transition = rememberInfiniteTransition(label = "emptyOrbit")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "angle",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val accent = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(EchoTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.size(112.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.34f
            drawCircle(
                color = accent.copy(alpha = 0.10f),
                radius = radius * 1.75f * pulse,
                center = center,
            )
            drawCircle(
                color = accent.copy(alpha = 0.22f),
                radius = radius * pulse,
                center = center,
            )
            drawCircle(color = accent, radius = radius * 0.48f, center = center)

            repeat(3) { index ->
                val orbitAngle = angle + index * (Math.PI * 2.0 / 3.0).toFloat()
                val orbitCenter = Offset(
                    x = center.x + cos(orbitAngle) * radius * 1.65f,
                    y = center.y + sin(orbitAngle) * radius * 1.65f,
                )
                drawCircle(
                    color = if (index == 1) secondary else accent,
                    radius = if (index == 1) 6f else 4f,
                    center = orbitCenter,
                )
            }
        }
        Spacer(Modifier.height(EchoTheme.spacing.lg))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(EchoTheme.spacing.sm))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(EchoTheme.spacing.lg))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}
