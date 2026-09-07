package com.example.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.AssistantState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PulseMagenta
import com.example.ui.theme.WarningAmber
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlowingOrb(
    state: AssistantState,
    inputAmplitude: Float,
    outputAmplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransitions")

    // Slow breathing for IDLE
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdleBreathing"
    )

    // Fast pulse for THINKING
    val thinkingPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ThinkingPulse"
    )

    // Continuous rotation for CONNECTING & SPEAKING
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = size.toPx() * 0.32f

            when (state) {
                AssistantState.IDLE -> {
                    val radius = baseRadius * idleScale
                    // Outer glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.35f), Color.Transparent),
                            center = center,
                            radius = radius * 1.5f
                        ),
                        radius = radius * 1.5f,
                        center = center
                    )
                    // Inner Core
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan, ElectricViolet, Color(0xFF070B19)),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                    // Halo Ring
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.7f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                }

                AssistantState.CONNECTING -> {
                    val radius = baseRadius
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(NeonCyan, ElectricViolet, PulseMagenta, NeonCyan),
                            center = center
                        ),
                        radius = radius * 1.25f,
                        center = center,
                        style = Stroke(width = 4f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ElectricViolet, NeonCyan, Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }

                AssistantState.LISTENING -> {
                    // Amplitude reactivity (waveform ring)
                    val dynamicFactor = 1f + (inputAmplitude * 0.5f)
                    val outerRadius = baseRadius * 1.4f * dynamicFactor
                    val midRadius = baseRadius * 1.15f * dynamicFactor

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(EmeraldGreen.copy(alpha = 0.3f), Color.Transparent),
                            center = center,
                            radius = outerRadius
                        ),
                        radius = outerRadius,
                        center = center
                    )

                    drawCircle(
                        color = EmeraldGreen.copy(alpha = 0.8f),
                        radius = midRadius,
                        center = center,
                        style = Stroke(width = 3.5f)
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(EmeraldGreen, NeonCyan, Color(0xFF041812)),
                            center = center,
                            radius = baseRadius
                        ),
                        radius = baseRadius,
                        center = center
                    )

                    // Audio ripple spikes
                    val spikes = 12
                    for (i in 0 until spikes) {
                        val angle = (i * (360f / spikes)) * (Math.PI / 180f)
                        val spikeLen = 8f + (inputAmplitude * 40f * (1f + (i % 3) * 0.3f))
                        val start = Offset(
                            (center.x + cos(angle) * midRadius).toFloat(),
                            (center.y + sin(angle) * midRadius).toFloat()
                        )
                        val end = Offset(
                            (center.x + cos(angle) * (midRadius + spikeLen)).toFloat(),
                            (center.y + sin(angle) * (midRadius + spikeLen)).toFloat()
                        )
                        drawLine(
                            color = EmeraldGreen,
                            start = start,
                            end = end,
                            strokeWidth = 3f
                        )
                    }
                }

                AssistantState.THINKING -> {
                    val radius = baseRadius * thinkingPulse
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(PulseMagenta.copy(alpha = 0.45f), Color.Transparent),
                            center = center,
                            radius = radius * 1.4f
                        ),
                        radius = radius * 1.4f,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(PulseMagenta, ElectricViolet, Color(0xFF1B0314)),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = PulseMagenta,
                        radius = radius * 1.15f,
                        center = center,
                        style = Stroke(width = 3f)
                    )
                }

                AssistantState.SPEAKING -> {
                    val dynamicRadius = baseRadius * (1f + (outputAmplitude * 0.4f))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.5f), Color.Transparent),
                            center = center,
                            radius = dynamicRadius * 1.6f
                        ),
                        radius = dynamicRadius * 1.6f,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan, ElectricViolet, PulseMagenta),
                            center = center,
                            radius = dynamicRadius
                        ),
                        radius = dynamicRadius,
                        center = center
                    )
                    // Rotating outer ring
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(NeonCyan, PulseMagenta, ElectricViolet, NeonCyan),
                            center = center
                        ),
                        radius = dynamicRadius * 1.25f,
                        center = center,
                        style = Stroke(width = 4f)
                    )
                }

                AssistantState.ERROR -> {
                    val radius = baseRadius * idleScale
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AlertRed.copy(alpha = 0.5f), Color.Transparent),
                            center = center,
                            radius = radius * 1.5f
                        ),
                        radius = radius * 1.5f,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AlertRed, WarningAmber, Color(0xFF200303)),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = AlertRed,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3f)
                    )
                }

                AssistantState.OFFLINE -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
                            center = center,
                            radius = baseRadius
                        ),
                        radius = baseRadius,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF64748B),
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}
