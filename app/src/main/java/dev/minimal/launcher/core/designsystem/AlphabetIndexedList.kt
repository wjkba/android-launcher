package dev.minimal.launcher.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.minimal.launcher.core.domain.indexLetter
import dev.minimal.launcher.core.domain.letterSections
import kotlinx.coroutines.launch

/**
 * An alphabetically sorted list with the letter strip along its right edge (see [AlphabetScroller]).
 * [itemContent] gets the row index and the group letter, which is non-null only on the first row of a group.
 */
@Composable
fun AlphabetIndexedList(
    labels: List<String>,
    key: (index: Int) -> Any,
    scrollerDescription: String,
    modifier: Modifier = Modifier,
    listModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 8.dp, end = 4.dp, bottom = 16.dp),
    itemContent: @Composable (index: Int, letter: String?) -> Unit,
) {
    val letters = remember(labels) { labels.map(::indexLetter) }
    val sections = remember(letters) { letterSections(letters) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var activeSection by remember { mutableIntStateOf(-1) }

    Box(modifier) {
        Row(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = listModifier.weight(1f),
                contentPadding = contentPadding,
            ) {
                items(count = labels.size, key = key) { index ->
                    val letter = letters[index]
                    itemContent(index, letter.takeIf { index == 0 || letters[index - 1] != letter })
                }
            }
            if (sections.size > 1) {
                AlphabetScroller(
                    sections = sections,
                    activeIndex = activeSection,
                    onActiveIndexChange = { index ->
                        activeSection = index
                        sections.getOrNull(index)?.let { section ->
                            scope.launch { listState.scrollToItem(section.firstIndex) }
                        }
                    },
                    contentDescription = scrollerDescription,
                    modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
                )
            }
        }
        sections.getOrNull(activeSection)?.let { section ->
            LetterBubble(section.letter, Modifier.align(Alignment.Center))
        }
    }
}

/** Big letter shown mid-screen while scrubbing, since the finger covers the strip itself. */
@Composable
private fun LetterBubble(letter: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(88.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
