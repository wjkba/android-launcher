package dev.minimal.launcher.core.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.minimal.launcher.core.domain.addApp
import dev.minimal.launcher.core.model.HomeLayout
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val Context.homeLayoutStore by dataStore(
    fileName = "home_layout.json",
    serializer = HomeLayoutSerializer,
    // An unreadable file would otherwise break the home screen for good; start over with an empty one.
    corruptionHandler = ReplaceFileCorruptionHandler { HomeLayout() },
    produceMigrations = { context -> listOf(FavoritesMigration(context.launcherPreferences)) },
)

/** The home screen layout, saved as one JSON document; every change is one atomic read-modify-write. */
class HomeLayoutRepository(private val dataStore: DataStore<HomeLayout>) {
    constructor(context: Context) : this(context.homeLayoutStore)

    val layout: Flow<HomeLayout> = dataStore.data

    suspend fun update(transform: (HomeLayout) -> HomeLayout) {
        dataStore.updateData { transform(it) }
    }
}

object HomeLayoutSerializer : Serializer<HomeLayout> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue = HomeLayout()

    override suspend fun readFrom(input: InputStream): HomeLayout = try {
        json.decodeFromString(HomeLayout.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Cannot read the home layout", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Cannot read the home layout", e)
    }

    override suspend fun writeTo(t: HomeLayout, output: OutputStream) {
        output.write(json.encodeToString(HomeLayout.serializer(), t).encodeToByteArray())
    }
}

/** Moves favorites from the old Preferences key ("id1\nid2…") into the layout file, once. */
class FavoritesMigration(private val preferences: DataStore<Preferences>) : DataMigration<HomeLayout> {
    override suspend fun shouldMigrate(currentData: HomeLayout): Boolean = preferences.data.first()[FAVORITES] != null

    override suspend fun migrate(currentData: HomeLayout): HomeLayout {
        val ids = preferences.data.first()[FAVORITES].orEmpty().split('\n').filter { it.isNotBlank() }
        return ids.fold(currentData) { layout, id -> layout.addApp(id) }
    }

    override suspend fun cleanUp() {
        preferences.edit { it.remove(FAVORITES) }
    }

    private companion object {
        val FAVORITES = stringPreferencesKey("favorites")
    }
}
