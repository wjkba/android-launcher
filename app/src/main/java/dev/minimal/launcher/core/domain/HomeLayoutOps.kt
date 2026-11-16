package dev.minimal.launcher.core.domain

import dev.minimal.launcher.core.model.HomeItem
import dev.minimal.launcher.core.model.HomeLayout

// Pure edits of the home layout. Each returns a new layout and keeps the invariant that an app appears at
// most once: putting an app somewhere first takes it out of wherever it was.

/** Every app on the home screen, including those inside folders. These open without the breathing pause. */
fun HomeLayout.appIdsOnHome(): Set<String> = items.flatMapTo(LinkedHashSet()) { item ->
    when (item) {
        is HomeItem.App -> listOf(item.appId)
        is HomeItem.Folder -> item.appIds
    }
}

fun HomeLayout.folderOf(appId: String): HomeItem.Folder? =
    items.firstOrNull { it is HomeItem.Folder && appId in it.appIds } as HomeItem.Folder?

/** Appends [appId] as a single app, unless it is already on the home screen (also inside a folder). */
fun HomeLayout.addApp(appId: String): HomeLayout =
    if (appId in appIdsOnHome()) this else HomeLayout(items + HomeItem.App(appId))

/** Removes [appId] from the home screen, wherever it is. A folder left empty stays. */
fun HomeLayout.removeApp(appId: String): HomeLayout = HomeLayout(items.withoutApps(setOf(appId)))

/** Moves the item with [key] by [delta] places, clamped to the list. */
fun HomeLayout.moveItem(key: String, delta: Int): HomeLayout {
    val from = items.indexOfFirst { it.key == key }
    if (from < 0) return this
    val to = (from + delta).coerceIn(0, items.lastIndex)
    if (to == from) return this
    return HomeLayout(items.toMutableList().apply { add(to, removeAt(from)) })
}

/** Puts [appId] into folder [folderId], taking it from the home screen or another folder. */
fun HomeLayout.moveAppToFolder(appId: String, folderId: String): HomeLayout {
    if (items.none { it is HomeItem.Folder && it.id == folderId }) return this
    return HomeLayout(
        items.withoutApps(setOf(appId)).map { item ->
            if (item is HomeItem.Folder && item.id == folderId) item.copy(appIds = item.appIds + appId) else item
        },
    )
}

/** Takes [appId] out of its folder and places it right below the folder. */
fun HomeLayout.takeOutOfFolder(appId: String): HomeLayout {
    val folder = folderOf(appId) ?: return this
    return HomeLayout(
        items.flatMap { item ->
            if (item == folder) {
                listOf(folder.copy(appIds = folder.appIds - appId), HomeItem.App(appId))
            } else {
                listOf(item)
            }
        },
    )
}

/**
 * Creates or updates [folder] (matched by id). Its apps are taken from wherever they were.
 * A new folder takes the place of the first of its apps that was a single app on home, or goes to the end.
 * Apps removed from an existing folder are placed right below it, so nothing silently leaves the home screen.
 */
fun HomeLayout.saveFolder(folder: HomeItem.Folder): HomeLayout {
    val saved = folder.copy(appIds = folder.appIds.distinct())
    val selected = saved.appIds.toSet()
    val existing = items.firstOrNull { it is HomeItem.Folder && it.id == saved.id } as HomeItem.Folder?
    if (existing != null) {
        val released = (existing.appIds - selected).map(HomeItem::App)
        return HomeLayout(
            items.flatMap { item ->
                when {
                    item == existing -> listOf(saved) + released
                    item is HomeItem.App && item.appId in selected -> emptyList()
                    item is HomeItem.Folder -> listOf(item.copy(appIds = item.appIds - selected))
                    else -> listOf(item)
                }
            },
        )
    }

    var placed = false
    val result = items.flatMap { item ->
        when {
            item is HomeItem.App && item.appId in selected -> {
                if (placed) {
                    emptyList()
                } else {
                    placed = true
                    listOf(saved)
                }
            }

            item is HomeItem.Folder -> listOf(item.copy(appIds = item.appIds - selected))

            else -> listOf(item)
        }
    }
    return HomeLayout(if (placed) result else result + saved)
}

/** Deletes folder [folderId]; its apps take its place as single apps, ordered by [order]. */
fun HomeLayout.deleteFolder(folderId: String, order: Comparator<String>): HomeLayout = HomeLayout(
    items.flatMap { item ->
        if (item is HomeItem.Folder && item.id == folderId) {
            item.appIds.sortedWith(order).map(HomeItem::App)
        } else {
            listOf(item)
        }
    },
)

/** Replaces app ids by [renames] (old → new), e.g. after an app changed its main activity in an update. */
fun HomeLayout.renameApps(renames: Map<String, String>): HomeLayout {
    if (renames.isEmpty()) return this
    fun rename(id: String) = renames[id] ?: id
    return HomeLayout(
        items.map { item ->
            when (item) {
                is HomeItem.App -> HomeItem.App(rename(item.appId))
                is HomeItem.Folder -> item.copy(appIds = item.appIds.map(::rename))
            }
        },
    )
}

private fun List<HomeItem>.withoutApps(appIds: Set<String>): List<HomeItem> = mapNotNull { item ->
    when (item) {
        is HomeItem.App -> item.takeUnless { it.appId in appIds }
        is HomeItem.Folder -> item.copy(appIds = item.appIds - appIds)
    }
}
