package dev.minimal.launcher.core.domain

import java.time.LocalDate

/** Per-app counter of open attempts that resets every calendar day. Stored as "2026-11-16|3". */
object Attempts {
    data class Recorded(val encoded: String, val count: Int)

    fun record(stored: String?, today: LocalDate): Recorded {
        val previous = parse(stored)
        val count = if (previous?.first == today) previous.second + 1 else 1
        return Recorded("$today|$count", count)
    }

    private fun parse(stored: String?): Pair<LocalDate, Int>? {
        val parts = stored?.split('|')?.takeIf { it.size == 2 } ?: return null
        return runCatching { LocalDate.parse(parts[0]) to parts[1].toInt() }.getOrNull()
    }
}
