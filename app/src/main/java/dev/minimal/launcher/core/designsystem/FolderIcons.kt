package dev.minimal.launcher.core.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.minimal.launcher.R

/** Monochrome folder icons (Material Symbols, thin rounded outline), looked up by the key saved in a folder. */
object FolderIcons {
    const val DEFAULT = "folder"

    /** Keys in picker order. */
    val keys: List<String> = folderIconSet.map { it.first }

    private val drawables = folderIconSet.toMap()

    /** Unknown keys (e.g. an icon dropped from the set) fall back to the plain folder. */
    @DrawableRes
    fun drawable(key: String): Int = drawables[key] ?: R.drawable.folder_icon_folder
}

/** Always drawn in a single color (the text color by default), whatever the source file says. */
@Composable
fun FolderIcon(
    key: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(FolderIcons.drawable(key)),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}
