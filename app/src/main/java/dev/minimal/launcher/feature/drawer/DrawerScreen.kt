package dev.minimal.launcher.feature.drawer

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import dev.minimal.launcher.R
import dev.minimal.launcher.core.designsystem.AlphabetIndexedList
import dev.minimal.launcher.core.designsystem.SheetHandle
import dev.minimal.launcher.core.model.AppEntry

/** All apps, alphabetically. [onHomeIds] marks apps on the home screen, including those in folders. */
@Composable
fun DrawerScreen(
    apps: List<AppEntry>,
    onHomeIds: Set<String>,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
    val close by rememberUpdatedState(onClose)
    val pullDownToClose = remember(closeThresholdPx) { PullDownToClose(closeThresholdPx) { close() } }
    val labels = remember(apps) { apps.map { it.label } }
    val favoriteMarker = stringResource(R.string.favorite_marker)

    Column(
        modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        SheetHandle(label = stringResource(R.string.close_drawer), onClick = onClose)
        AlphabetIndexedList(
            labels = labels,
            key = { apps[it].id },
            scrollerDescription = stringResource(R.string.alphabet_index),
            modifier = Modifier.weight(1f),
            listModifier = Modifier.nestedScroll(pullDownToClose),
        ) { index, letter ->
            val app = apps[index]
            DrawerRow(
                app = app,
                // The letter is shown only on the first app of each group: a quiet index on a narrow screen.
                letter = letter,
                isOnHome = app.id in onHomeIds,
                favoriteMarker = favoriteMarker,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) },
            )
        }
    }
}

@Composable
private fun DrawerRow(
    app: AppEntry,
    letter: String?,
    isOnHome: Boolean,
    favoriteMarker: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Text(
            text = letter.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(32.dp),
        )
        Text(
            text = app.label,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isOnHome) {
            Text(
                text = "•",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .semantics { contentDescription = favoriteMarker },
            )
        }
    }
}

/** Closes the list when the user keeps pulling down after it is already scrolled to the top. */
private class PullDownToClose(private val thresholdPx: Float, private val onClose: () -> Unit) :
    NestedScrollConnection {
    private var pulled = 0f

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        if (available.y > 0f) {
            pulled += available.y
            if (pulled >= thresholdPx) {
                pulled = 0f
                onClose()
            }
            return Offset(0f, available.y)
        }
        if (consumed.y < 0f) pulled = 0f
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        pulled = 0f
        return Velocity.Zero
    }
}
