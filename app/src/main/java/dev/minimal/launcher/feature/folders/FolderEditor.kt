package dev.minimal.launcher.feature.folders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.minimal.launcher.R
import dev.minimal.launcher.core.designsystem.AlphabetIndexedList
import dev.minimal.launcher.core.designsystem.FolderIcon
import dev.minimal.launcher.core.designsystem.FolderIcons
import dev.minimal.launcher.core.model.AppEntry

/**
 * Creates or edits a folder: name, icon and apps. Holds the unsaved draft; [onSave] gets the final one.
 * [folderOf] names the other folder an app is in, if any (picking the app moves it here).
 */
@Composable
fun FolderEditor(
    initial: FolderDraft,
    isNew: Boolean,
    apps: List<AppEntry>,
    folderOf: (appId: String) -> String?,
    onSave: (FolderDraft) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf(initial) }
    var pickingApps by rememberSaveable { mutableStateOf(false) }

    // The keyboard opens for a new folder's name only once, not again after coming back from the app list.
    var visitedApps by rememberSaveable { mutableStateOf(false) }

    // Back from the app list returns to the editor, not to the home screen.
    BackHandler(enabled = pickingApps) { pickingApps = false }

    if (pickingApps) {
        AppPickerScreen(
            apps = apps,
            selected = draft.appIds,
            folderOf = folderOf,
            onToggle = { draft = draft.toggleApp(it) },
            onDone = { pickingApps = false },
            modifier = modifier,
        )
    } else {
        FolderEditorScreen(
            draft = draft,
            isNew = isNew,
            apps = apps,
            onDraftChange = { draft = it },
            onPickApps = {
                pickingApps = true
                visitedApps = true
            },
            focusName = isNew && !visitedApps,
            onSave = { onSave(draft) },
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
fun FolderEditorScreen(
    draft: FolderDraft,
    isNew: Boolean,
    apps: List<AppEntry>,
    onDraftChange: (FolderDraft) -> Unit,
    onPickApps: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    focusName: Boolean = isNew,
) {
    val focusManager = LocalFocusManager.current
    val nameFocus = remember { FocusRequester() }
    // A new folder starts with its name; editing one usually means changing apps or the icon.
    if (focusName) LaunchedEffect(Unit) { nameFocus.requestFocus() }

    // safeDrawing includes the keyboard, so the scrolling part shrinks above it and Save stays visible.
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        EditorTopBar(
            title = stringResource(if (isNew) R.string.folder_new_title else R.string.folder_edit_title),
            onBack = onBack,
        ) {
            TextButton(onClick = onSave, enabled = draft.canSave) { Text(stringResource(R.string.folder_save)) }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onDraftChange(draft.withName(it)) },
                label = { Text(stringResource(R.string.folder_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocus),
            )
            Spacer(Modifier.height(8.dp))
            SelectedApps(apps.filter { it.id in draft.appIds }, onPickApps)
            Spacer(Modifier.height(8.dp))
            SectionLabel(stringResource(R.string.folder_icon))
            IconGrid(selected = draft.iconKey, onSelect = { onDraftChange(draft.copy(iconKey = it)) })
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SelectedApps(selected: List<AppEntry>, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.folder_apps, selected.size),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = selected.joinToString { it.label }.ifEmpty { stringResource(R.string.folder_no_apps) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(R.string.folder_choose_apps),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Five icons per row: about 60dp cells on the 349dp-wide screen. */
@Composable
private fun IconGrid(selected: String, onSelect: (String) -> Unit) {
    Column {
        FolderIcons.keys.chunked(ICONS_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    val isSelected = key == selected
                    val label = key.replace('_', ' ')
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(key) })
                            .semantics { contentDescription = label },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            FolderIcon(
                                key = key,
                                contentDescription = null,
                                size = 28.dp,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
                // Keep the last row's cells the same width as the others.
                repeat(ICONS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val ICONS_PER_ROW = 5

/** Alphabetical list of all apps with a checkbox each; changes apply to the draft right away. */
@Composable
fun AppPickerScreen(
    apps: List<AppEntry>,
    selected: Set<String>,
    folderOf: (appId: String) -> String?,
    onToggle: (appId: String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = remember(apps) { apps.map { it.label } }
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        EditorTopBar(title = stringResource(R.string.folder_apps, selected.size), onBack = onDone) {
            IconButton(onClick = onDone) {
                Icon(
                    painterResource(R.drawable.ui_icon_check),
                    contentDescription = stringResource(R.string.folder_done),
                )
            }
        }
        AlphabetIndexedList(
            labels = labels,
            key = { apps[it].id },
            scrollerDescription = stringResource(R.string.alphabet_index),
            modifier = Modifier.weight(1f),
        ) { index, letter ->
            val app = apps[index]
            PickerRow(
                app = app,
                letter = letter,
                checked = app.id in selected,
                otherFolder = folderOf(app.id),
                onToggle = { onToggle(app.id) },
            )
        }
    }
}

@Composable
private fun PickerRow(app: AppEntry, letter: String?, checked: Boolean, otherFolder: String?, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
    ) {
        Text(
            text = letter.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(32.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (otherFolder != null) {
                Text(
                    text = stringResource(R.string.folder_in, otherFolder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(horizontal = 12.dp))
    }
}

@Composable
private fun EditorTopBar(title: String, onBack: () -> Unit, action: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painterResource(R.drawable.ui_icon_arrow_back),
                contentDescription = stringResource(R.string.folder_back),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        action()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}
