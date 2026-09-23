package com.shadman.emojiarena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadman.emojiarena.data.Message

private val ScoreCardMaxWidth = 260.dp
private val ScoreCardShape = RoundedCornerShape(18.dp)
private val ScoreCardEmojiSize = 32.sp

/**
 * The score-card bubble: the emoji played with, a line of context text
 * ChatRepository already phrased for who sent it — "Scored 12" for the
 * user's own, "I scored 10 — can you beat it?" for a contact's — and a
 * "Beat this score" button. Tappable regardless of who sent it: the
 * contact's own counter-card challenges the user right back, exactly like
 * theirs challenged the contact.
 */
@Composable
fun ScoreCardBubble(message: Message, onBeatScore: () -> Unit, modifier: Modifier = Modifier) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = ScoreCardMaxWidth)
                .background(MaterialTheme.colorScheme.surfaceVariant, ScoreCardShape)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = message.scoreCardEmoji.orEmpty(), fontSize = ScoreCardEmojiSize)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = onBeatScore, modifier = Modifier.fillMaxWidth()) {
                Text("Beat this score")
            }
            if (message.isFromUser) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    MessageStatusTicks(status = message.status)
                }
            }
        }
    }
}
