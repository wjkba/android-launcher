package dev.minimal.launcher.core.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.minimal.launcher.core.model.HomeItem
import dev.minimal.launcher.core.model.HomeLayout
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HomeLayoutSerializerTest {
    private val layout = HomeLayout(
        listOf(HomeItem.App("com.a/.Main#0"), HomeItem.Folder("f1", "Social", "chat", listOf("com.b/.Main#0"))),
    )

    @Test fun `round-trips through JSON`() = runBlocking {
        val bytes = ByteArrayOutputStream().also { HomeLayoutSerializer.writeTo(layout, it) }.toByteArray()
        assertEquals(layout, HomeLayoutSerializer.readFrom(ByteArrayInputStream(bytes)))
    }

    @Test fun `keeps the stored format stable`() = runBlocking {
        val json = """
            {"items":[{"type":"app","appId":"a"},
            {"type":"folder","id":"f","name":"N","iconKey":"star","appIds":["b"],"extra":1}]}
        """.trimIndent()
        assertEquals(
            HomeLayout(listOf(HomeItem.App("a"), HomeItem.Folder("f", "N", "star", listOf("b")))),
            HomeLayoutSerializer.readFrom(ByteArrayInputStream(json.toByteArray())),
        )
    }

    @Test(expected = CorruptionException::class)
    fun `reports garbage as corruption`(): Unit = runBlocking {
        HomeLayoutSerializer.readFrom(ByteArrayInputStream("not json".toByteArray()))
    }
}

class FavoritesMigrationTest {
    @get:Rule val folder = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    @After fun tearDown() = scope.cancel()

    @Test fun `moves old favorites into the layout and removes the old key`() = runBlocking {
        val favorites = stringPreferencesKey("favorites")
        val attempts = stringPreferencesKey("attempts:a")
        val preferences = PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("launcher.preferences_pb") }
        preferences.edit {
            it[favorites] = "a\nb\n\na"
            it[attempts] = "2026-11-16|2"
        }
        val layout = DataStoreFactory.create(
            serializer = HomeLayoutSerializer,
            migrations = listOf(FavoritesMigration(preferences)),
            scope = scope,
        ) { folder.root.resolve("home_layout.json") }

        assertEquals(HomeLayout(listOf(HomeItem.App("a"), HomeItem.App("b"))), layout.data.first())
        val remaining = preferences.data.first()
        assertNull(remaining[favorites])
        assertEquals("2026-11-16|2", remaining[attempts])
    }
}
