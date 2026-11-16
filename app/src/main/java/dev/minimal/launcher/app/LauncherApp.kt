package dev.minimal.launcher.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.minimal.launcher.core.model.HomeRow
import dev.minimal.launcher.feature.appactions.AppActionsSheet
import dev.minimal.launcher.feature.appactions.AppPlacement
import dev.minimal.launcher.feature.drawer.DrawerScreen
import dev.minimal.launcher.feature.folders.FolderActionsSheet
import dev.minimal.launcher.feature.folders.FolderDraft
import dev.minimal.launcher.feature.folders.FolderEditor
import dev.minimal.launcher.feature.folders.FolderSheet
import dev.minimal.launcher.feature.home.HomeScreen
import dev.minimal.launcher.feature.pause.PauseScreen

@Composable
fun LauncherApp(viewModel: LauncherViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()

    // A launcher never finishes on Back. NavDisplay handles Back while there is something to pop;
    // on the bare home screen this handler catches it instead of letting the activity close.
    BackHandler { viewModel.goHome() }

    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        NavDisplay(
            backStack = viewModel.backStack,
            onBack = viewModel::back,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            popTransitionSpec = { fadeIn() togetherWith fadeOut() },
            predictivePopTransitionSpec = { fadeIn() togetherWith fadeOut() },
            entryProvider = entryProvider {
                entry<Screen.Home> {
                    HomeScreen(
                        loaded = state.loaded,
                        rows = state.home,
                        onAppClick = viewModel::onAppClick,
                        onAppLongClick = { viewModel.showSheet(Sheet.AppActions(it)) },
                        onFolderClick = { viewModel.showSheet(Sheet.Folder(it.id)) },
                        onFolderLongClick = { viewModel.showSheet(Sheet.FolderActions(it.id)) },
                        onOpenDrawer = viewModel::openDrawer,
                    )
                }
                // The app list slides up from the home screen and back down, following the swipe that opens it.
                entry<Screen.Drawer>(
                    metadata = NavDisplay.transitionSpec {
                        (slideInVertically { it / 4 } + fadeIn()) togetherWith fadeOut()
                    } + NavDisplay.popTransitionSpec {
                        fadeIn() togetherWith (slideOutVertically { it / 4 } + fadeOut())
                    },
                ) {
                    DrawerScreen(
                        apps = state.allApps,
                        onHomeIds = state.onHomeIds,
                        onAppClick = viewModel::onAppClick,
                        onAppLongClick = { viewModel.showSheet(Sheet.AppActions(it)) },
                        onClose = viewModel::back,
                    )
                }
                entry<Screen.Pause> { key ->
                    PauseScreen(
                        app = key.app,
                        attemptsToday = key.attemptsToday,
                        onOpen = { viewModel.openAfterPause(key.app) },
                        onCancel = viewModel::goHome,
                    )
                }
                entry<Screen.FolderEditor> { key ->
                    val folder = key.folderId?.let(state::folder)
                    FolderEditor(
                        initial =
                            folder?.let { FolderDraft(it.name, it.iconKey, it.apps.map { app -> app.id }.toSet()) }
                                ?: FolderDraft(appIds = setOfNotNull(key.initialAppId)),
                        isNew = folder == null,
                        apps = state.allApps,
                        folderOf = { appId -> state.folderByApp[appId]?.takeIf { it.id != folder?.id }?.name },
                        onSave = { viewModel.saveFolder(folder?.id, it.name, it.iconKey, it.appIds) },
                        onBack = viewModel::back,
                    )
                }
            },
        )
    }

    when (val current = sheet) {
        null -> Unit

        is Sheet.Folder -> state.folder(current.folderId)?.let { folder ->
            FolderSheet(
                folder = folder,
                onAppClick = viewModel::onAppClick,
                onAppLongClick = { viewModel.showSheet(Sheet.AppActions(it)) },
                onEdit = { viewModel.openFolderEditor(folder.id) },
                onDismiss = { viewModel.showSheet(null) },
            )
        }

        is Sheet.FolderActions -> state.folder(current.folderId)?.let { folder ->
            val index = state.home.indexOfFirst { it.key == folder.key }
            FolderActionsSheet(
                folder = folder,
                canMoveUp = index > 0,
                canMoveDown = index < state.home.lastIndex,
                onEdit = { viewModel.openFolderEditor(folder.id) },
                onMove = { delta -> viewModel.moveItem(folder.key, delta) },
                onDelete = { viewModel.deleteFolder(folder.id) },
                onDismiss = { viewModel.showSheet(null) },
            )
        }

        is Sheet.AppActions -> {
            val app = current.app
            val folder = state.folderByApp[app.id]
            val index = state.home.indexOfFirst { it is HomeRow.App && it.app.id == app.id }
            val placement = when {
                folder != null -> AppPlacement.InFolder(folder.name)
                index >= 0 -> AppPlacement.OnHome(canMoveUp = index > 0, canMoveDown = index < state.home.lastIndex)
                else -> AppPlacement.NotOnHome
            }
            AppActionsSheet(
                app = app,
                placement = placement,
                folders = state.folders.filter { it.id != folder?.id },
                onDismiss = { viewModel.showSheet(null) },
                onAddToHome = { viewModel.addToHome(app) },
                onRemoveFromHome = { viewModel.removeFromHome(app) },
                onMove = { delta -> viewModel.moveItem(HomeRow.App(app).key, delta) },
                onMoveToFolder = { viewModel.moveAppToFolder(app, it) },
                onNewFolder = { viewModel.openFolderEditor(initialAppId = app.id) },
                onTakeOutOfFolder = { viewModel.takeOutOfFolder(app) },
                onAppInfo = { viewModel.openAppInfo(app) },
            )
        }
    }
}
