package dev.minimal.launcher.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What the home screen shows, top to bottom: single apps and folders. Stored as JSON (home_layout.json),
 * so field names and @SerialName values are part of the file format; rename them only with a migration.
 *
 * Invariant kept by the operations in core.domain: an app id appears at most once in the whole layout.
 */
@Serializable
data class HomeLayout(val items: List<HomeItem> = emptyList())

@Serializable
sealed interface HomeItem {
    /** Identifies the item for reordering; unique within a layout. */
    val key: String

    @Serializable
    @SerialName("app")
    data class App(val appId: String) : HomeItem {
        override val key: String get() = appKey(appId)
    }

    @Serializable
    @SerialName("folder")
    data class Folder(val id: String, val name: String, val iconKey: String, val appIds: List<String> = emptyList()) :
        HomeItem {
        override val key: String get() = folderKey(id)
    }

    companion object {
        fun appKey(appId: String) = "app:$appId"

        fun folderKey(folderId: String) = "folder:$folderId"
    }
}

/** A home screen row ready to display: stored ids resolved to installed apps. */
sealed interface HomeRow {
    val key: String

    data class App(val app: AppEntry) : HomeRow {
        override val key: String get() = HomeItem.appKey(app.id)
    }

    /** [apps] are the installed apps of the folder in alphabetical order; may be empty. */
    data class Folder(val id: String, val name: String, val iconKey: String, val apps: List<AppEntry>) : HomeRow {
        override val key: String get() = HomeItem.folderKey(id)
    }
}
