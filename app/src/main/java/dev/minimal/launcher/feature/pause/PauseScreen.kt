package dev.minimal.launcher.feature.pause

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.minimal.launcher.R
import dev.minimal.launcher.core.model.AppEntry

private const val INHALE_MS = 4_000L
private const val EXHALE_MS = 4_000L

private enum class Phase { Inhale, Exhale, Decide }

/** A one-breath pause before opening an app that is not on the home screen. */
@Composable
fun PauseScreen(
    app: AppEntry,
    attemptsToday: Int,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var phase by remember(app.id) { mutableStateOf(Phase.Inhale) }
    val breath = remember(app.id) { mutableFloatStateOf(0f) }

    LaunchedEffect(app.id) {
        runForWallClock(INHALE_MS) { t -> breath.floatValue = FastOutSlowInEasing.transform(t) }
        phase = Phase.Exhale
        runForWallClock(EXHALE_MS) { t -> breath.floatValue = 1f - FastOutSlowInEasing.transform(t) }
        phase = Phase.Decide
    }

    Box(
        modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
    ) {
        Crossfade(targetState = phase == Phase.Decide, label = "pause") { deciding ->
            if (deciding) {
                Decision(app, attemptsToday, onOpen, onCancel)
            } else {
                Breathing(app, phase, progress = { breath.floatValue })
            }
        }
    }
}

@Composable
private fun Breathing(app: AppEntry, phase: Phase, progress: () -> Float) {
    val color = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.pause_opening, app.label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        // Reading progress only in the draw phase keeps the 60fps animation from recomposing.
        Canvas(Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2f * 0.9f
            val radius = maxRadius * (0.35f + 0.65f * progress())
            drawCircle(color.copy(alpha = 0.18f), radius)
            drawCircle(color, radius, style = Stroke(width = 2.dp.toPx()))
        }
        Text(
            text = stringResource(if (phase == Phase.Inhale) R.string.pause_inhale else R.string.pause_exhale),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun Decision(app: AppEntry, attemptsToday: Int, onOpen: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Text(
            text = app.label,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.pause_attempts_today, attemptsToday),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        // The easy, thumb-sized choice is to not open the app.
        Button(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.pause_cancel))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.pause_open, app.label),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Drives [onFrame] with 0..1 over [durationMs] of real time. Unlike Compose animation specs this ignores
 * the system "animation scale", so turning animations off in developer options cannot skip the pause.
 */
private suspend fun runForWallClock(durationMs: Long, onFrame: (Float) -> Unit) {
    val start = withFrameMillis { it }
    while (true) {
        val elapsed = withFrameMillis { it } - start
        val t = (elapsed.toFloat() / durationMs).coerceAtMost(1f)
        onFrame(t)
        if (t >= 1f) return
    }
}
