package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GlassBorder

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = DarkSurface.copy(alpha = 0.75f),
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding),
        content = content
    )
}
