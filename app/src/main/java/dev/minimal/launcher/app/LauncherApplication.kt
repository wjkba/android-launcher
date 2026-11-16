package dev.minimal.launcher.app

import android.app.Application
import android.content.Context
import dev.minimal.launcher.core.data.AppsRepository
import dev.minimal.launcher.core.data.AttemptsRepository
import dev.minimal.launcher.core.data.HomeLayoutRepository

class LauncherApplication : Application() {
    val container by lazy { AppContainer(this) }
}

/** Hand-rolled dependency container; small enough that a DI framework would only add weight. */
class AppContainer(context: Context) {
    val apps = AppsRepository(context)
    val homeLayout = HomeLayoutRepository(context)
    val attempts = AttemptsRepository(context)
}
