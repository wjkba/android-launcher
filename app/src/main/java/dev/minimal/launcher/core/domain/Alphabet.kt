package dev.minimal.launcher.core.domain

import java.text.Normalizer

private val combiningMarks = Regex("\\p{Mn}+")

/**
 * Letter used to group an app in the alphabetical list; "#" for names that start with a digit or symbol.
 * Accents are dropped ("Żabka" → "Z") to match the list order, which comes from a Collator that ignores
 * them. Letters that are distinct characters rather than accented ones, like "Ł", keep their own group.
 */
fun indexLetter(label: String): String {
    val first = label.firstOrNull { it.isLetterOrDigit() } ?: return "#"
    if (!first.isLetter()) return "#"
    return Normalizer.normalize(first.toString(), Normalizer.Form.NFD).replace(combiningMarks, "").uppercase()
}

/** A run of consecutive list rows that share [letter], starting at row [firstIndex]. */
data class LetterSection(val letter: String, val firstIndex: Int)

/**
 * Sections in list order. [letters] holds one group letter per row of the (already sorted) list.
 * A letter that shows up again later (a collation quirk) keeps only its first section.
 */
fun letterSections(letters: List<String>): List<LetterSection> = letters
    .mapIndexedNotNull { index, letter ->
        LetterSection(letter, index).takeIf { index == 0 || letters[index - 1] != letter }
    }.distinctBy { it.letter }

/** Section under a touch at [y] on a strip of [height] that splits its height evenly between [count] sections. */
fun sectionIndexAt(y: Float, height: Float, count: Int): Int {
    if (count <= 0 || height <= 0f) return -1
    return (y / height * count).toInt().coerceIn(0, count - 1)
}
