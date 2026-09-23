package com.shadman.emojiarena.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

private val BubbleShape = RoundedCornerShape(18.dp)

/**
 * The contact's "typing…" placeholder — same left-aligned, neutral-surface
 * bubble shape as an incoming message, but with three pulsing dots
 * instead of text. Shown for as long as ChatUiState.isTyping is true;
 * replaced by the real reply bubble the moment it lands.
 */
@Composable
fun TypingIndicatorBubble(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, BubbleShape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            TypingDot(delayMillis = 0)
            TypingDot(delayMillis = 150)
            TypingDot(delayMillis = 300)
        }
    }
}

@Composable
private fun TypingDot(delayMillis: Int) {
    val transition = rememberInfiniteTransition(label = "typing-dot")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typing-dot-alpha"
    )
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(6.dp)
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
    )
}
