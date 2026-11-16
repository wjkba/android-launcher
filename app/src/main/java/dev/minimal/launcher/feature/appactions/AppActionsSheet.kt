package dev.minimal.launcher.feature.appactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.minimal.launcher.R
import dev.minimal.launcher.core.designsystem.FolderIcon
import dev.minimal.launcher.core.designsystem.SheetAction
import dev.minimal.launcher.core.designsystem.SheetTitle
import dev.minimal.launcher.core.model.AppEntry
import dev.minimal.launcher.core.model.HomeRow

/** Where an app is on the home screen; decides which actions its long-press menu offers. */
sealed interface AppPlacement {
    data object NotOnHome : AppPlacement

    data class OnHome(val canMoveUp: Boolean, val canMoveDown: Boolean) : AppPlacement

    data class InFolder(val folderName: String) : AppPlacement
}

/** Long-press menu of an app. "Add/Move to folder…" swaps the menu for a list of [folders] in the same sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    app: AppEntry,
    placement: AppPlacement,
    folders: List<HomeRow.Folder>,
    onDismiss: () -> Unit,
    onAddToHome: () -> Unit,
    onRemoveFromHome: () -> Unit,
    onMove: (delta: Int) -> Unit,
    onMoveToFolder: (folderId: String) -> Unit,
    onNewFolder: () -> Unit,
    onTakeOutOfFolder: () -> Unit,
    onAppInfo: () -> Unit,
) {
    var choosingFolder by remember { mutableStateOf(false) }

    // Actions that change where the app is close the menu; moving up/down keeps it open (see AppActions).
    fun thenDismiss(action: () -> Unit): () -> Unit = {
        action()
        onDismiss()
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (choosingFolder) {
                FolderChoices(
                    folders = folders,
                    onChoose = {
                        onMoveToFolder(it)
                        onDismiss()
                    },
                    onNewFolder = onNewFolder,
                )
            } else {
                AppActions(
                    app = app,
                    placement = placement,
                    onAddToHome = thenDismiss(onAddToHome),
                    onRemoveFromHome = thenDismiss(onRemoveFromHome),
                    onMove = onMove,
                    onChooseFolder = { choosingFolder = true },
                    onTakeOutOfFolder = thenDismiss(onTakeOutOfFolder),
                    onAppInfo = thenDismiss(onAppInfo),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AppActions(
    app: AppEntry,
    placement: AppPlacement,
    onAddToHome: () -> Unit,
    onRemoveFromHome: () -> Unit,
    onMove: (delta: Int) -> Unit,
    onChooseFolder: () -> Unit,
    onTakeOutOfFolder: () -> Unit,
    onAppInfo: () -> Unit,
) {
    SheetTitle(app.label)
    when (placement) {
        AppPlacement.NotOnHome -> {
            SheetAction(stringResource(R.string.action_add_favorite), onClick = onAddToHome)
            SheetAction(stringResource(R.string.action_add_to_folder), onClick = onChooseFolder)
        }

        is AppPlacement.OnHome -> {
            SheetAction(stringResource(R.string.action_move_to_folder), onClick = onChooseFolder)
            // Moving keeps the sheet open, so an app can be nudged several places in a row.
            if (placement.canMoveUp) SheetAction(stringResource(R.string.action_move_up), onClick = { onMove(-1) })
            if (placement.canMoveDown) SheetAction(stringResource(R.string.action_move_down), onClick = { onMove(1) })
            SheetAction(stringResource(R.string.action_remove_favorite), onClick = onRemoveFromHome)
        }

        is AppPlacement.InFolder -> {
            SheetAction(stringResource(R.string.action_take_out_of_folder, placement.folderName), onTakeOutOfFolder)
            SheetAction(stringResource(R.string.action_move_to_folder), onClick = onChooseFolder)
            SheetAction(stringResource(R.string.action_remove_favorite), onClick = onRemoveFromHome)
        }
    }
    SheetAction(stringResource(R.string.action_app_info), onClick = onAppInfo)
}

@Composable
private fun FolderChoices(
    folders: List<HomeRow.Folder>,
    onChoose: (folderId: String) -> Unit,
    onNewFolder: () -> Unit,
) {
    SheetTitle(stringResource(R.string.action_choose_folder))
    folders.forEach { folder ->
        SheetAction(
            label = folder.name,
            onClick = { onChoose(folder.id) },
            icon = { FolderIcon(folder.iconKey, contentDescription = null) },
        )
    }
    SheetAction(
        label = stringResource(R.string.folder_new_title),
        onClick = onNewFolder,
        icon = { FolderIcon("folder", contentDescription = null) },
    )
}
