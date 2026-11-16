package dev.minimal.launcher.feature.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
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
    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    val openDrawer by rememberUpdatedState(onOpenDrawer)

    Column(
        modifier
            .fillMaxSize()
            .systemBarsPadding()
            .pointerInput(swipeThresholdPx) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onVerticalDrag = { change, dy ->
                        dragged += dy
                        change.consume()
                    },
                    onDragEnd = { if (dragged < -swipeThresholdPx) openDrawer() },
                )
            }
            .padding(horizontal = 24.dp),
    ) {
        Clock(now, Modifier.padding(top = 16.dp))

        // Apps and folders sit at the bottom, where the thumb rests. Row height shrinks
        // (down to a 40dp touch target) so the whole list fits without scrolling.
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds(),
        ) {
            if (loaded && rows.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_empty_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 16.dp),
                )
            } else if (rows.isNotEmpty()) {
                val rowHeight = (maxHeight / rows.size).coerceIn(40.dp, 56.dp)
                Column(Modifier.align(Alignment.BottomStart)) {
                    rows.forEach { row ->
                        key(row.key) {
                            when (row) {
                                is HomeRow.App -> HomeRowText(
                                    text = row.app.label,
                                    height = rowHeight,
                                    onClick = { onAppClick(row.app) },
                                    onLongClick = { onAppLongClick(row.app) },
                                )

                                is HomeRow.Folder -> HomeRowText(
                                    text = row.name,
                                    height = rowHeight,
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
        }

        SheetHandle(label = stringResource(R.string.open_drawer), onClick = onOpenDrawer)
    }
}

/** One home row: an app name, or a folder name after its icon. [description] replaces the text for screen readers. */
@Composable
private fun HomeRowText(
    text: String,
    height: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .then(
                if (description !=
                    null
                ) {
                    Modifier.clearAndSetSemantics { contentDescription = description }
                } else {
                    Modifier
                },
            ),
    ) {
        icon?.invoke()
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
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
