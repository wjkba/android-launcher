package dev.minimal.launcher.feature.folders

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.minimal.launcher.R
import dev.minimal.launcher.core.designsystem.FolderIcon
import dev.minimal.launcher.core.designsystem.SheetAction
import dev.minimal.launcher.core.designsystem.SheetTitle
import dev.minimal.launcher.core.model.AppEntry
import dev.minimal.launcher.core.model.HomeRow

/** A folder's apps in a bottom panel. Apps open right away, like the ones on the home screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSheet(
    folder: HomeRow.Folder,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        FolderSheetContent(folder, onAppClick, onAppLongClick, onEdit)
    }
}

/** The panel's content without the sheet window, so screenshot tests can render it in place. */
@Composable
fun FolderSheetContent(
    folder: HomeRow.Folder,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.padding(bottom = 16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                SheetTitle(
                    text = folder.name,
                    icon = { FolderIcon(folder.iconKey, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        painterResource(R.drawable.ui_icon_edit),
                        contentDescription = stringResource(R.string.folder_edit_title),
                    )
                }
            }
        }
        if (folder.apps.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.folder_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
        items(folder.apps, key = { it.id }) { app ->
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .combinedClickable(onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) })
                    .padding(horizontal = 24.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

/** Long-press menu of a folder on the home screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderActionsSheet(
    folder: HomeRow.Folder,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMove: (delta: Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle(text = folder.name, icon = { FolderIcon(folder.iconKey, contentDescription = null) })
        SheetAction(stringResource(R.string.folder_edit_title), onClick = onEdit)
        // Moving keeps the sheet open, so a folder can be nudged several places in a row.
        if (canMoveUp) SheetAction(stringResource(R.string.action_move_up), onClick = { onMove(-1) })
        if (canMoveDown) SheetAction(stringResource(R.string.action_move_down), onClick = { onMove(1) })
        SheetAction(stringResource(R.string.folder_delete), onClick = onDelete)
        Text(
            text = stringResource(R.string.folder_delete_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}
