package dev.minimal.launcher.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Sizes tuned for a 3" screen: large enough to hit with a thumb,
// small enough that typical app names fit on one line.
private val LauncherTypography = Typography().let { base ->
    base.copy(
        displayMedium = base.displayMedium.copy(fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.Light),
        headlineSmall = base.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp),
        titleLarge = base.titleLarge.copy(fontSize = 19.sp, lineHeight = 24.sp),
    )
}

/** [dynamicColor] = Material You colors from the wallpaper (API 31+); off in screenshot tests for stable images. */
@Composable
fun LauncherTheme(dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val base = if (dynamicColor) dynamicDarkColorScheme(LocalContext.current) else darkColorScheme()
    val colors = base.copy(
        background = Color.Black,
        surface = Color.Black,
    )
    MaterialTheme(colorScheme = colors, typography = LauncherTypography, content = content)
}
