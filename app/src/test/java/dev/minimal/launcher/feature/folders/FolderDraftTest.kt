package dev.minimal.launcher.feature.folders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderDraftTest {
    @Test fun `needs a non-blank name`() {
        assertFalse(FolderDraft().canSave)
        assertFalse(FolderDraft(name = "  ").canSave)
        assertTrue(FolderDraft(name = "Social").canSave)
    }

    @Test fun `cuts long names`() {
        assertEquals(FolderDraft.MAX_NAME_LENGTH, FolderDraft().withName("x".repeat(40)).name.length)
    }

    @Test fun `toggles apps`() {
        val draft = FolderDraft().toggleApp("a").toggleApp("b").toggleApp("a")
        assertEquals(setOf("b"), draft.appIds)
    }
}
