package com.shadman.emojiarena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadman.emojiarena.data.Message
import com.shadman.emojiarena.data.MessageType
import com.shadman.emojiarena.ui.theme.TickDeliveredOnAccent
import com.shadman.emojiarena.ui.theme.TickSentOnAccent

private val BubbleMaxWidth = 280.dp
private val BubbleShape = RoundedCornerShape(18.dp)
private val EmojiTriggerFontSize = 48.sp

/**
 * Renders one message as a chat bubble. Text messages get the normal
 * right/left-aligned colored bubble; a detected emoji trigger (see
 * data/EmojiDetection.kt) or a score card both get their own large,
 * tappable rendering instead — tapping either is what (re)launches the
 * game, which is why they share [onPlayEmoji] rather than each having
 * their own callback.
 */
@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier,
    onPlayEmoji: (String) -> Unit = {}
) {
    when (message.type) {
        MessageType.EMOJI_TRIGGER -> EmojiTriggerBubble(
            message = message,
            onClick = { onPlayEmoji(message.text) },
            modifier = modifier
        )
        MessageType.SCORE_CARD -> ScoreCardBubble(
            message = message,
            onBeatScore = { onPlayEmoji(message.scoreCardEmoji.orEmpty()) },
            modifier = modifier
        )
        MessageType.TEXT -> TextMessageBubble(message = message, modifier = modifier)
    }
}

@Composable
private fun TextMessageBubble(message: Message, modifier: Modifier = Modifier) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = BubbleMaxWidth)
                .background(bubbleColor, BubbleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
            if (message.isFromUser) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    MessageStatusTicks(
                        status = message.status,
                        sentColor = TickSentOnAccent,
                        deliveredColor = TickDeliveredOnAccent
                    )
                }
            }
        }
    }
}

/**
 * No bubble chrome — just the glyph itself, big, sitting on the plain
 * background. Still aligned to the sender's side and still shows delivery
 * ticks, same as a text bubble, since those apply to every message the
 * user sends. Ticks use MessageStatusTicks' own defaults (tuned for a
 * plain background) rather than the accent-bubble colors above.
 */
@Composable
private fun EmojiTriggerBubble(message: Message, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(8.dp)
        ) {
            Text(text = message.text, fontSize = EmojiTriggerFontSize)
            if (message.isFromUser) {
                Spacer(modifier = Modifier.height(2.dp))
                MessageStatusTicks(status = message.status)
            }
        }
    }
}
