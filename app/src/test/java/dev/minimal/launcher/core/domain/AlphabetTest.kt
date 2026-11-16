package dev.minimal.launcher.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AlphabetTest {
    @Test fun `groups by first letter, keeping diacritics`() {
        assertEquals("M", indexLetter("mapy"))
        assertEquals("Ł", indexLetter("łódź app"))
        assertEquals("Z", indexLetter("Żabka"))
        assertEquals("A", indexLetter("ąbc"))
        assertEquals("E", indexLetter("Éclair"))
        assertEquals("#", indexLetter("1Password"))
        assertEquals("Y", indexLetter("  YouTube"))
        assertEquals("#", indexLetter(""))
    }
}

class LetterSectionsTest {
    @Test fun `one section per run of letters, pointing at its first row`() {
        assertEquals(
            listOf(LetterSection("#", 0), LetterSection("A", 1), LetterSection("B", 3)),
            letterSections(listOf("#", "A", "A", "B")),
        )
        assertEquals(emptyList<LetterSection>(), letterSections(emptyList()))
        assertEquals(listOf(LetterSection("Z", 0), LetterSection("Y", 1)), letterSections(listOf("Z", "Y", "Z")))
    }

    @Test fun `touch position maps evenly onto sections and clamps at the ends`() {
        assertEquals(0, sectionIndexAt(y = 0f, height = 100f, count = 4))
        assertEquals(1, sectionIndexAt(y = 25f, height = 100f, count = 4))
        assertEquals(3, sectionIndexAt(y = 99f, height = 100f, count = 4))
        assertEquals(3, sectionIndexAt(y = 500f, height = 100f, count = 4))
        assertEquals(0, sectionIndexAt(y = -20f, height = 100f, count = 4))
        assertEquals(-1, sectionIndexAt(y = 10f, height = 100f, count = 0))
    }
}
