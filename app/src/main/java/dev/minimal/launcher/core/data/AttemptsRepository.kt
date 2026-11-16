package dev.minimal.launcher.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.minimal.launcher.core.domain.Attempts
import java.time.LocalDate

/** Key/value settings. Until the home layout got its own file, favorites lived here too (see FavoritesMigration). */
internal val Context.launcherPreferences by preferencesDataStore(name = "launcher")

class AttemptsRepository(context: Context) {
    private val dataStore = context.launcherPreferences

    /** Counts one more attempt to open [id] and returns how many there were today, this one included. */
    suspend fun recordAttempt(id: String, today: LocalDate): Int {
        val key = stringPreferencesKey("attempts:$id")
        var count = 0
        dataStore.edit { prefs ->
            val recorded = Attempts.record(prefs[key], today)
            prefs[key] = recorded.encoded
            count = recorded.count
        }
        return count
    }
}
