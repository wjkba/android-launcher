package dev.minimal.launcher.feature.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.minimal.launcher.R
import dev.minimal.launcher.core.designsystem.FolderIcon
import dev.minimal.launcher.core.designsystem.SheetHandle
import dev.minimal.launcher.core.model.AppEntry
import dev.minimal.launcher.core.model.HomeRow
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    loaded: Boolean,
    rows: List<HomeRow>,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
    onFolderClick: (HomeRow.Folder) -> Unit,
    onFolderLongClick: (HomeRow.Folder) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    now: () -> LocalDateTime = { LocalDateTime.now() },
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    // Leaving the launcher scrolls back up, so home always opens on the first (most used) rows.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { scope.launch { scrollState.scrollTo(0) } }

    Column(modifier.fillMaxSize().systemBarsPadding()) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .fadeBottomEdge(visible = scrollState.canScrollForward)
                .nestedScroll(rememberDrawerPull(onOpenDrawer)),
        ) {
            val viewportHeight = maxHeight
            // The clock starts a bit below the top, so the first rows land in the thumb's reach and the
            // rest scroll in below. A short list sits at the bottom instead.
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .heightIn(min = viewportHeight)
                    .padding(horizontal = HomeMargin),
            ) {
                Clock(now, Modifier.padding(top = viewportHeight * HEADROOM_FRACTION, bottom = 16.dp))
                if (loaded && rows.isEmpty()) {
                    Text(
                        text = stringResource(R.string.home_empty_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
                rows.forEach { row ->
                    key(row.key) {
                        when (row) {
                            is HomeRow.App -> HomeRowText(
                                text = row.app.label,
                                onClick = { onAppClick(row.app) },
                                onLongClick = { onAppLongClick(row.app) },
                            )

                            is HomeRow.Folder -> HomeRowText(
                                text = row.name,
                                onClick = { onFolderClick(row) },
                                onLongClick = { onFolderLongClick(row) },
                                description = pluralStringResource(
                                    R.plurals.folder_description,
                                    row.apps.size,
                                    row.name,
                                    row.apps.size,
                                ),
                                icon = { FolderIcon(row.iconKey, contentDescription = null, size = 28.dp) },
                            )
                        }
                    }
                }
            }
        }

        SheetHandle(label = stringResource(R.string.open_drawer), onClick = onOpenDrawer)
    }
}

/** Fades out the bottom edge, as a hint that more rows scroll in from below. */
private fun Modifier.fadeBottomEdge(visible: Boolean): Modifier = if (!visible) {
    this
} else {
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent()
        val fade = 32.dp.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Black,
                1f to Color.Transparent,
                startY = size.height - fade,
                endY = size.height,
            ),
            blendMode = BlendMode.DstIn,
        )
    }
}

/**
 * Dragging up opens the app drawer, but only a drag that did not scroll the list: at its end, or on a list
 * too short to scroll. A fast swipe down to the last rows therefore never overshoots into the drawer.
 */
@Composable
private fun rememberDrawerPull(onOpenDrawer: () -> Unit): NestedScrollConnection {
    val thresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    val openDrawer by rememberUpdatedState(onOpenDrawer)
    return remember(thresholdPx) {
        object : NestedScrollConnection {
            var pulledUp = 0f
            var scrolled = false

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    if (consumed.y != 0f) scrolled = true
                    pulledUp -= available.y
                }
                return Offset.Zero
            }

            // Called once when the finger lifts, also for a slow drag.
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!scrolled && pulledUp > thresholdPx) openDrawer()
                pulledUp = 0f
                scrolled = false
                return Velocity.Zero
            }
        }
    }
}

private const val HEADROOM_FRACTION = 0.15f

private val RowHeight = 52.dp

private val HomeMargin = 32.dp

// Every row reserves this column, so app and folder names start at the same x, with folder icons in
// line with the clock.
private val IconGutter = 44.dp

/** One home row: an app name, or a folder name after its icon. [description] replaces the text for screen readers. */
@Composable
private fun HomeRowText(
    text: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    val semantics = if (description != null) {
        Modifier.clearAndSetSemantics { contentDescription = description }
    } else {
        Modifier
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .then(semantics),
    ) {
        Box(Modifier.width(IconGutter)) { icon?.invoke() }
        Text(
            text = text,
            // A bit below headlineSmall, so long names still fit beside the icon gutter.
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, lineHeight = 28.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Clock(now: () -> LocalDateTime, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val time = rememberCurrentMinute(now)
    val is24h = DateFormat.is24HourFormat(context)
    val timeFormat = remember(locale, is24h) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, if (is24h) "Hm" else "hm"), locale)
    }
    val dateFormat = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale)
    }

    Column(modifier) {
        Text(
            text = time.format(timeFormat),
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.clickable { context.startSafely(Intent(AlarmClock.ACTION_SHOW_ALARMS)) },
        )
        Text(
            text = time.format(dateFormat).replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { context.startSafely(calendarIntent(time)) },
        )
    }
}

/** Ticks on every minute boundary while the launcher is visible, and refreshes right after it becomes visible again. */
@Composable
private fun rememberCurrentMinute(now: () -> LocalDateTime): LocalDateTime {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // The default `now` lambda is a new instance on every recomposition; don't restart the ticker for it.
    val currentNow by rememberUpdatedState(now)
    val time by produceState(now(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val current = currentNow()
                value = current
                delay(60_000L - current.second * 1_000L - current.nano / 1_000_000L)
            }
        }
    }
    return time
}

private fun calendarIntent(now: LocalDateTime): Intent {
    val millis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(millis.toString()).build()
    return Intent(Intent.ACTION_VIEW, uri)
}

private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No app handles it on this device; nothing to do.
    }
}
