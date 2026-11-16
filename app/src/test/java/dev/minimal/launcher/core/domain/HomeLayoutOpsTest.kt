package dev.minimal.launcher.core.domain

import dev.minimal.launcher.core.model.HomeItem
import dev.minimal.launcher.core.model.HomeItem.App
import dev.minimal.launcher.core.model.HomeLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HomeLayoutOpsTest {
    private fun folder(id: String, vararg appIds: String) =
        HomeItem.Folder(id, name = id, iconKey = "star", appIds = appIds.toList())

    private fun layout(vararg items: HomeItem) = HomeLayout(items.toList())

    private val social = folder("social", "signal", "telegram")
    private val sample = layout(App("phone"), social, App("maps"))

    @Test fun `apps on home include folder contents`() {
        assertEquals(setOf("phone", "signal", "telegram", "maps"), sample.appIdsOnHome())
    }

    @Test fun `addApp appends a new app and ignores one already on home`() {
        assertEquals(layout(App("phone"), social, App("maps"), App("clock")), sample.addApp("clock"))
        assertSame(sample, sample.addApp("maps"))
        assertSame(sample, sample.addApp("signal"))
    }

    @Test fun `removeApp works for single apps and folder contents, leaving empty folders`() {
        assertEquals(layout(social, App("maps")), sample.removeApp("phone"))
        assertEquals(layout(App("phone"), folder("social", "telegram"), App("maps")), sample.removeApp("signal"))
        val emptied = sample.removeApp("signal").removeApp("telegram")
        assertEquals(layout(App("phone"), folder("social"), App("maps")), emptied)
    }

    @Test fun `moveItem shifts apps and folders within bounds`() {
        assertEquals(layout(social, App("phone"), App("maps")), sample.moveItem(social.key, -1))
        assertEquals(layout(App("phone"), App("maps"), social), sample.moveItem(social.key, 1))
        assertSame(sample, sample.moveItem(App("phone").key, -1))
        assertSame(sample, sample.moveItem("missing", 1))
    }

    @Test fun `moveAppToFolder takes the app from home or another folder`() {
        assertEquals(
            layout(folder("social", "signal", "telegram", "phone"), App("maps")),
            sample.moveAppToFolder("phone", "social"),
        )
        val two = layout(social, folder("work", "mail"))
        assertEquals(
            layout(folder("social", "telegram"), folder("work", "mail", "signal")),
            two.moveAppToFolder("signal", "work"),
        )
        assertSame(sample, sample.moveAppToFolder("phone", "missing"))
    }

    @Test fun `takeOutOfFolder puts the app right below its folder`() {
        assertEquals(
            layout(App("phone"), folder("social", "telegram"), App("signal"), App("maps")),
            sample.takeOutOfFolder("signal"),
        )
        assertSame(sample, sample.takeOutOfFolder("phone"))
    }

    @Test fun `a new folder takes the place of its first single app`() {
        val saved = sample.saveFolder(folder("daily", "maps", "phone"))
        assertEquals(layout(folder("daily", "maps", "phone"), social), saved)
    }

    @Test fun `a new folder of apps not on home goes to the end, taking apps from other folders`() {
        val saved = sample.saveFolder(folder("chat", "signal", "discord"))
        assertEquals(
            layout(App("phone"), folder("social", "telegram"), App("maps"), folder("chat", "signal", "discord")),
            saved,
        )
    }

    @Test fun `updating a folder releases removed apps below it and takes added ones`() {
        val saved = sample.saveFolder(folder("social", "telegram", "maps").copy(name = "Chat", iconKey = "forum"))
        assertEquals(
            layout(
                App("phone"),
                folder("social", "telegram", "maps").copy(name = "Chat", iconKey = "forum"),
                App("signal"),
            ),
            saved,
        )
    }

    @Test fun `saveFolder drops duplicate ids`() {
        assertEquals(
            layout(App("phone"), folder("social", "signal"), App("telegram"), App("maps")),
            sample.saveFolder(folder("social", "signal", "signal")),
        )
    }

    @Test fun `deleteFolder puts its apps in its place in the given order`() {
        val alphabetical = compareBy<String> { it }
        assertEquals(
            layout(App("phone"), App("signal"), App("telegram"), App("maps")),
            layout(
                App("phone"),
                folder("social", "telegram", "signal"),
                App("maps"),
            ).deleteFolder("social", alphabetical),
        )
        assertEquals(
            layout(App("phone"), App("maps")),
            layout(App("phone"), folder("empty"), App("maps")).deleteFolder("empty", alphabetical),
        )
    }

    @Test fun `renameApps updates single apps and folder contents`() {
        assertEquals(
            layout(App("dialer"), folder("social", "signal2", "telegram"), App("maps")),
            sample.renameApps(mapOf("phone" to "dialer", "signal" to "signal2")),
        )
    }
}

class AppIdsTest {
    @Test fun `packageKey keeps package and profile`() {
        assertEquals("com.example#0", packageKey("com.example/.MainActivity#0"))
        assertEquals("com.example#10", packageKey("com.example/com.example.ui.Start#10"))
    }

    @Test fun `relinks a stored id whose activity was renamed`() {
        val stored = setOf("com.a/.Old#0", "com.b/.Main#0")
        val installed = listOf("com.a/.New#0", "com.b/.Main#0", "com.c/.Main#0")
        assertEquals(mapOf("com.a/.Old#0" to "com.a/.New#0"), relinkedAppIds(stored, installed))
    }

    @Test fun `does not relink to another profile, a missing package or an app already stored`() {
        assertEquals(emptyMap<String, String>(), relinkedAppIds(setOf("com.a/.Old#0"), listOf("com.a/.New#10")))
        assertEquals(emptyMap<String, String>(), relinkedAppIds(setOf("com.a/.Old#0"), emptyList()))
        assertEquals(
            emptyMap<String, String>(),
            relinkedAppIds(setOf("com.a/.Old#0", "com.a/.Other#0"), listOf("com.a/.Other#0")),
        )
    }
}
