package dev.minimal.launcher.core.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AttemptsTest {
    private val today = LocalDate.of(2026, 11, 16)

    @Test fun `first attempt ever counts as one`() {
        assertEquals(Attempts.Recorded("2026-11-16|1", 1), Attempts.record(null, today))
    }

    @Test fun `counts up within the same day`() {
        assertEquals(3, Attempts.record("2026-11-16|2", today).count)
    }

    @Test fun `resets on a new day`() {
        assertEquals(1, Attempts.record("2026-11-15|9", today).count)
    }

    @Test fun `treats corrupted data as no history`() {
        assertEquals(1, Attempts.record("garbage", today).count)
        assertEquals(1, Attempts.record("2026-11-16|x", today).count)
    }
}
