package dev.minimal.launcher.core.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import dev.minimal.launcher.core.model.AppEntry
import java.text.Collator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AppsRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    /** All launchable apps, sorted alphabetically; re-emits on install, removal and update. */
    val apps: Flow<List<AppEntry>> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) {
                trySend(Unit)
            }

            override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) {
                trySend(Unit)
            }
        }
        trySend(Unit)
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }
        .conflate()
        .map { loadApps() }
        .flowOn(Dispatchers.IO)

    fun launch(app: AppEntry): Boolean = runCatching {
        launcherApps.startMainActivity(app.component, app.user, null, null)
    }.isSuccess

    fun openAppInfo(app: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }

    private fun loadApps(): List<AppEntry> {
        // Locale-aware ordering, so e.g. "Ł" sorts right after "L" in Polish.
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        return launcherApps.profiles
            .flatMap { user ->
                val serial = userManager.getSerialNumberForUser(user)
                launcherApps.getActivityList(null, user).map { info ->
                    AppEntry(
                        id = "${info.componentName.flattenToShortString()}#$serial",
                        label = info.label.toString().trim(),
                        component = info.componentName,
                        user = user,
                    )
                }
            }
            .filterNot { it.component.packageName == context.packageName }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }
}
