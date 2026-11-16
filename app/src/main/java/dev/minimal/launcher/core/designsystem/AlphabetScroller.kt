package dev.minimal.launcher.core.designsystem

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.minimal.launcher.core.domain.LetterSection
import dev.minimal.launcher.core.domain.sectionIndexAt

/**
 * Vertical strip of section letters along the list edge. Touching or dragging along it jumps the list to the
 * letter under the finger; each letter gets an equal share of the strip so the whole alphabet stays reachable
 * on a short screen, even when individual letters are too small to tap precisely.
 *
 * [activeIndex] is the section currently under the finger, or -1 when the strip is not touched.
 */
@Composable
fun AlphabetScroller(
    sections: List<LetterSection>,
    activeIndex: Int,
    onActiveIndexChange: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val currentSections by rememberUpdatedState(sections)
    val currentOnChange by rememberUpdatedState(onActiveIndexChange)

    Column(
        modifier = modifier
            .width(32.dp)
            .fillMaxHeight()
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(Unit) {
                var last = -1
                fun select(y: Float) {
                    val index = sectionIndexAt(y, size.height.toFloat(), currentSections.size)
                    if (index != last && index >= 0) {
                        last = index
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        currentOnChange(index)
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    last = -1
                    select(down.position.y)
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        select(change.position.y)
                    }
                    currentOnChange(-1)
                }
            },
    ) {
        sections.forEachIndexed { index, section ->
            val active = index == activeIndex
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = section.letter,
                    // Tight line height: with ~27 sections each letter may get only ~20dp on a 3" screen.
                    style = MaterialTheme.typography.labelMedium.copy(lineHeight = 14.sp),
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.wrapContentHeight(unbounded = true),
                )
            }
        }
    }
}
