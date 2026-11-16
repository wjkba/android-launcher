package dev.minimal.launcher.app

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.minimal.launcher.core.data.AppsRepository
import dev.minimal.launcher.core.data.AttemptsRepository
import dev.minimal.launcher.core.data.HomeLayoutRepository
import dev.minimal.launcher.core.domain.addApp
import dev.minimal.launcher.core.domain.appIdsOnHome
import dev.minimal.launcher.core.domain.deleteFolder
import dev.minimal.launcher.core.domain.moveAppToFolder
import dev.minimal.launcher.core.domain.moveItem
import dev.minimal.launcher.core.domain.relinkedAppIds
import dev.minimal.launcher.core.domain.removeApp
import dev.minimal.launcher.core.domain.renameApps
import dev.minimal.launcher.core.domain.saveFolder
import dev.minimal.launcher.core.domain.takeOutOfFolder
import dev.minimal.launcher.core.model.AppEntry
import dev.minimal.launcher.core.model.HomeItem
import dev.minimal.launcher.core.model.HomeLayout
import dev.minimal.launcher.core.model.HomeRow
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LauncherUiState(
    val loaded: Boolean = false,
    val allApps: List<AppEntry> = emptyList(),
    val home: List<HomeRow> = emptyList(),
    /** Apps that open without the pause: on the home screen, directly or in a folder. */
    val onHomeIds: Set<String> = emptySet(),
    val folderByApp: Map<String, HomeRow.Folder> = emptyMap(),
) {
    val folders: List<HomeRow.Folder> get() = home.filterIsInstance<HomeRow.Folder>()

    fun folder(id: String): HomeRow.Folder? = folders.firstOrNull { it.id == id }
}

/** Navigation 3 back stack entries. Home is always at the bottom. */
sealed interface Screen {
    data object Home : Screen

    data object Drawer : Screen

    data class Pause(val app: AppEntry, val attemptsToday: Int) : Screen

    /** [folderId] null = new folder, optionally starting with [initialAppId] in it. */
    data class FolderEditor(val folderId: String? = null, val initialAppId: String? = null) : Screen
}

/** Bottom sheets over the current screen; at most one at a time. */
sealed interface Sheet {
    data class Folder(val folderId: String) : Sheet

    data class FolderActions(val folderId: String) : Sheet

    data class AppActions(val app: AppEntry) : Sheet
}

class LauncherViewModel(
    private val apps: AppsRepository,
    private val homeLayout: HomeLayoutRepository,
    private val attempts: AttemptsRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    // Shared so the system app list is loaded (and LauncherApps watched) once for both collectors below.
    private val installedApps = apps.apps.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    val uiState: StateFlow<LauncherUiState> = combine(installedApps, homeLayout.layout, ::buildUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherUiState())

    /** Read by NavDisplay; a snapshot list, so changes recompose it directly. */
    val backStack = mutableStateListOf<Screen>(Screen.Home)

    private val _sheet = MutableStateFlow<Sheet?>(null)
    val sheet: StateFlow<Sheet?> = _sheet.asStateFlow()

    init {
        // An app update can rename its launcher activity, which changes its id; keep it on the home screen.
        viewModelScope.launch {
            combine(installedApps, homeLayout.layout) { installed, layout ->
                relinkedAppIds(layout.appIdsOnHome(), installed.map { it.id })
            }.collect { renames ->
                if (renames.isNotEmpty()) homeLayout.update { it.renameApps(renames) }
            }
        }
    }

    fun openDrawer() {
        if (backStack.last() == Screen.Home) backStack.add(Screen.Drawer)
    }

    /** Back to a clean home screen: Home press, leaving the launcher, Not now. */
    fun goHome() {
        _sheet.value = null
        if (backStack.size > 1) backStack.subList(1, backStack.size).clear()
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    /** Apps on the home screen (also in folders) open right away; every other app gets the breathing pause. */
    fun onAppClick(app: AppEntry) {
        if (app.id in uiState.value.onHomeIds) {
            apps.launch(app)
            return
        }
        viewModelScope.launch {
            val attemptsToday = attempts.recordAttempt(app.id, today())
            // A quick second tap must not stack a second pause.
            if (backStack.last() !is Screen.Pause) backStack.add(Screen.Pause(app, attemptsToday))
        }
    }

    fun openAfterPause(app: AppEntry) {
        if (!apps.launch(app)) goHome()
    }

    fun openAppInfo(app: AppEntry) = apps.openAppInfo(app)

    fun showSheet(sheet: Sheet?) {
        _sheet.value = sheet
    }

    fun addToHome(app: AppEntry) = edit { it.addApp(app.id) }

    fun removeFromHome(app: AppEntry) = edit { it.removeApp(app.id) }

    fun moveItem(key: String, delta: Int) = edit { it.moveItem(key, delta) }

    fun moveAppToFolder(app: AppEntry, folderId: String) = edit { it.moveAppToFolder(app.id, folderId) }

    fun takeOutOfFolder(app: AppEntry) = edit { it.takeOutOfFolder(app.id) }

    /** The folder's apps take its place on the home screen, alphabetically. */
    fun deleteFolder(folderId: String) {
        _sheet.value = null
        val position = uiState.value.allApps.withIndex().associate { (index, app) -> app.id to index }
        edit { it.deleteFolder(folderId, compareBy { id -> position[id] ?: Int.MAX_VALUE }) }
    }

    fun openFolderEditor(folderId: String? = null, initialAppId: String? = null) {
        _sheet.value = null
        backStack.add(Screen.FolderEditor(folderId, initialAppId))
    }

    fun saveFolder(folderId: String?, name: String, iconKey: String, appIds: Set<String>) {
        val folder = HomeItem.Folder(
            id = folderId ?: UUID.randomUUID().toString(),
            name = name.trim(),
            iconKey = iconKey,
            appIds = appIds.toList(),
        )
        edit { it.saveFolder(folder) }
        back()
    }

    private fun edit(transform: (HomeLayout) -> HomeLayout) {
        viewModelScope.launch { homeLayout.update(transform) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as LauncherApplication).container
                LauncherViewModel(container.apps, container.homeLayout, container.attempts)
            }
        }
    }
}

private fun buildUiState(installed: List<AppEntry>, layout: HomeLayout): LauncherUiState {
    val byId = installed.associateBy { it.id }
    // Folder contents follow the alphabetical order of the full list.
    val order = installed.withIndex().associate { (index, app) -> app.id to index }
    // Ids of apps that are gone (uninstalled, SD card removed) stay stored but are not shown.
    val home = layout.items.mapNotNull { item ->
        when (item) {
            is HomeItem.App -> byId[item.appId]?.let(HomeRow::App)

            is HomeItem.Folder -> HomeRow.Folder(
                id = item.id,
                name = item.name,
                iconKey = item.iconKey,
                apps = item.appIds.mapNotNull(byId::get).sortedBy { order[it.id] },
            )
        }
    }
    return LauncherUiState(
        loaded = true,
        allApps = installed,
        home = home,
        onHomeIds = layout.appIdsOnHome(),
        folderByApp = home.filterIsInstance<HomeRow.Folder>()
            .flatMap { folder -> folder.apps.map { it.id to folder } }
            .toMap(),
    )
}
