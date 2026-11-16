package dev.minimal.launcher.screenshots

import android.content.ComponentName
import android.os.Process
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import com.github.takahirom.roborazzi.captureRoboImage
import dev.minimal.launcher.core.designsystem.LauncherTheme
import dev.minimal.launcher.core.model.AppEntry
import dev.minimal.launcher.core.model.HomeRow
import dev.minimal.launcher.feature.drawer.DrawerScreen
import dev.minimal.launcher.feature.folders.AppPickerScreen
import dev.minimal.launcher.feature.folders.FolderDraft
import dev.minimal.launcher.feature.folders.FolderEditorScreen
import dev.minimal.launcher.feature.folders.FolderSheetContent
import dev.minimal.launcher.feature.home.HomeScreen
import dev.minimal.launcher.feature.pause.PauseScreen
import java.time.LocalDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders screens at the Unihertz Jelly Star's real size (480x854 px at 220 dpi = 349x621 dp)
 * and the owner's 1.3 font scale. Record: ./gradlew recordRoborazziDebug; check: verifyRoborazziDebug.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w349dp-h621dp-port-220dpi", fontScale = 1.3f)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun home_withFavorites() = capture("home_favorites") { HomeSample(apps.take(5).map(HomeRow::App)) }

    @Test
    fun home_withFolders() = capture("home_folders") {
        HomeSample(listOf(socialFolder, workFolder) + listOf(apps[21], apps[18], apps[7]).map(HomeRow::App))
    }

    @Test
    fun home_empty() = capture("home_empty") { HomeSample(emptyList()) }

    @Test
    fun folder_sheet() = capture("folder_sheet") {
        // The sheet's content in place of the ModalBottomSheet window, which the screenshot would not include.
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            FolderSheetContent(socialFolder, onAppClick = {}, onAppLongClick = {}, onEdit = {})
        }
    }

    @Test
    fun folder_editor() = capture("folder_editor") {
        FolderEditorScreen(
            draft = FolderDraft(
                name = socialFolder.name,
                iconKey = "chat",
                appIds = socialFolder.apps.map {
                    it.id
                }.toSet(),
            ),
            isNew = false,
            apps = apps,
            onDraftChange = {},
            onPickApps = {},
            onSave = {},
            onBack = {},
        )
    }

    @Test
    fun folder_appPicker() = capture("folder_app_picker") {
        AppPickerScreen(
            apps = apps,
            selected = socialFolder.apps.map { it.id }.toSet(),
            folderOf = { id -> workFolder.name.takeIf { workFolder.apps.any { it.id == id } } },
            onToggle = {},
            onDone = {},
        )
    }

    @Test
    fun drawer() = capture("drawer") { DrawerSample() }

    @Test
    fun drawer_scrubbingAlphabet() {
        setContent { DrawerSample() }
        compose.onNodeWithContentDescription("Alphabet index", substring = true).performTouchInput {
            down(percentOffset(0.5f, 0.55f))
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/drawer_scrubbing.png")
    }

    @Test
    fun pause_breathing() {
        compose.mainClock.autoAdvance = false
        setContent { PauseSample() }
        compose.mainClock.advanceTimeBy(2_000)
        compose.onRoot().captureRoboImage("src/test/screenshots/pause_breathing.png")
    }

    @Test
    fun pause_decision() {
        compose.mainClock.autoAdvance = false
        setContent { PauseSample() }
        compose.mainClock.advanceTimeBy(9_000)
        compose.onRoot().captureRoboImage("src/test/screenshots/pause_decision.png")
    }

    @Composable
    private fun HomeSample(rows: List<HomeRow>) {
        HomeScreen(
            loaded = true,
            rows = rows,
            onAppClick = {},
            onAppLongClick = {},
            onFolderClick = {},
            onFolderLongClick = {},
            onOpenDrawer = {},
            now = fixedNow,
        )
    }

    @Composable
    private fun DrawerSample() {
        DrawerScreen(
            apps = apps,
            onHomeIds = setOf(apps[2].id, apps[7].id),
            onAppClick = {},
            onAppLongClick = {},
            onClose = {},
        )
    }

    @Composable
    private fun PauseSample() {
        PauseScreen(app = apps[4], attemptsToday = 3, onOpen = {}, onCancel = {})
    }

    private fun capture(name: String, content: @Composable () -> Unit) {
        setContent(content)
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private fun setContent(content: @Composable () -> Unit) {
        compose.setContent {
            LauncherTheme(dynamicColor = false) {
                // Mirrors LauncherApp, which hosts every screen in a Surface (black background, light text).
                Surface(color = MaterialTheme.colorScheme.background, content = content)
            }
        }
    }

    private companion object {
        val fixedNow = { LocalDateTime.of(2026, 11, 16, 17, 19) }

        val apps = listOf(
            "2FAS Auth", "Airbnb", "AnkiDroid", "AntennaPod", "Bitwarden", "Bolt", "Calendar", "Camera",
            "Chrome", "Clock", "Contacts", "Discord", "Drive", "Files", "Gmail", "Google Authenticator",
            "Keep Notes", "Maps", "Messages", "Mobilny Bank Polski z bardzo długą nazwą", "Netflix", "Phone",
            "Photos", "Revolut", "Settings", "Signal", "Slack", "Spotify", "Strava", "Telegram", "Uber",
            "Weather", "WhatsApp", "YouTube", "Zoom", "Łódź Transit",
        ).sortedWith(java.text.Collator.getInstance(java.util.Locale.forLanguageTag("pl")))
            .mapIndexed { index, label ->
                AppEntry(
                    id = "app$index",
                    label = label,
                    component = ComponentName("pkg$index", "Main"),
                    user = Process.myUserHandle(),
                )
            }

        fun named(vararg labels: String) = labels.map { label -> apps.first { it.label == label } }

        val socialFolder = HomeRow.Folder(
            "social",
            "Social",
            "chat",
            named("Discord", "Signal", "Telegram", "WhatsApp"),
        )
        val workFolder = HomeRow.Folder("work", "Work", "work", named("Gmail", "Slack", "Zoom"))
    }
}
