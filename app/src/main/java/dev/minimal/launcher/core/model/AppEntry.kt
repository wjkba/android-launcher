package dev.minimal.launcher.core.model

import android.content.ComponentName
import android.os.UserHandle

/** One launchable activity. [id] is stable across restarts and is what we persist. */
data class AppEntry(val id: String, val label: String, val component: ComponentName, val user: UserHandle)
