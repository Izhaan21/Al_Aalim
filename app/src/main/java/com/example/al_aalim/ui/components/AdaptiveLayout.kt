package com.example.al_aalim.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive breakpoints mirroring Material3 WindowSizeClass.
 *
 * | Device              | Width      | Max content width |
 * |---------------------|------------|-------------------|
 * | Compact phone       | < 600 dp   | full width        |
 * | Large phone / 7"    | 600–839 dp | 560 dp            |
 * | 8–10" tablet        | 840–1199dp | 720 dp            |
 * | 12"+ tablet / fold  | ≥ 1200 dp  | 900 dp            |
 */
@Composable
fun AdaptiveContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val maxContentWidth: Dp = when {
            maxWidth < 600.dp  -> maxWidth          // compact phone → full bleed
            maxWidth < 840.dp  -> 560.dp            // large phone / 7" tablet
            maxWidth < 1200.dp -> 720.dp            // 8–10" tablet
            else               -> 900.dp            // 12"+ / foldable unfolded
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxContentWidth)
                .wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            content()
        }
    }
}
