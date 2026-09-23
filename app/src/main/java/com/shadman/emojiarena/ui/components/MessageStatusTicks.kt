package com.shadman.emojiarena.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.shadman.emojiarena.data.MessageStatus
import com.shadman.emojiarena.ui.theme.TickDelivered
import com.shadman.emojiarena.ui.theme.TickSent

/**
 * Single tick for Sent, double tick for Delivered, nothing for Sending
 * (the message hasn't left the device yet). Shared by the home screen's
 * last-message preview and by outgoing chat bubbles — one place owns what
 * a status looks like. Colors are parameters (not hardcoded) because a
 * tick sitting on the neutral home-row background needs different colors
 * than one sitting on the user's own accent-colored bubble.
 */
@Composable
fun MessageStatusTicks(
    status: MessageStatus,
    sentColor: Color = TickSent,
    deliveredColor: Color = TickDelivered
) {
    when (status) {
        MessageStatus.SENDING -> Unit
        MessageStatus.SENT -> Text(
            text = "✓",
            color = sentColor,
            style = MaterialTheme.typography.bodySmall
        )
        MessageStatus.DELIVERED -> Text(
            text = "✓✓",
            color = deliveredColor,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
