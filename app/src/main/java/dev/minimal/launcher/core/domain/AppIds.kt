package dev.minimal.launcher.core.domain

// App ids look like "com.example/.MainActivity#0": launcher activity plus the user profile's serial number.

/** The package and profile part of an app id: "com.example#0". */
fun packageKey(appId: String): String = appId.substringBefore('/') + "#" + appId.substringAfterLast('#')

/**
 * For [stored] ids that match no installed app, an installed app of the same package and profile that is not
 * stored yet. This keeps an app on the home screen when an update renames its launcher activity.
 */
fun relinkedAppIds(stored: Set<String>, installed: List<String>): Map<String, String> {
    val installedSet = installed.toSet()
    val byPackage = installed.filterNot { it in stored }.groupBy(::packageKey)
    val renames = mutableMapOf<String, String>()
    for (id in stored) {
        if (id in installedSet) continue
        val candidate = byPackage[packageKey(id)]?.firstOrNull { it !in renames.values } ?: continue
        renames[id] = candidate
    }
    return renames
}
