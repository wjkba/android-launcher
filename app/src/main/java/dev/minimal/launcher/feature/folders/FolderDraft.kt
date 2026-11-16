package dev.minimal.launcher.feature.folders

import dev.minimal.launcher.core.designsystem.FolderIcons

/** A folder being created or edited; nothing is saved until the user taps Save. */
data class FolderDraft(
    val name: String = "",
    val iconKey: String = FolderIcons.DEFAULT,
    val appIds: Set<String> = emptySet(),
) {
    val canSave: Boolean get() = name.isNotBlank()

    /** Typed names are cut at [MAX_NAME_LENGTH]: longer ones would not fit a home row on a 3" screen anyway. */
    fun withName(input: String): FolderDraft = copy(name = input.take(MAX_NAME_LENGTH))

    fun toggleApp(appId: String): FolderDraft = copy(appIds = if (appId in appIds) appIds - appId else appIds + appId)

    companion object {
        const val MAX_NAME_LENGTH = 24
    }
}
