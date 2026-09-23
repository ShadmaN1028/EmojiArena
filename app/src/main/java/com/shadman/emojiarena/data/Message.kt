package com.shadman.emojiarena.data

/**
 * Where a message sits in the fake send pipeline.
 * Sending -> Sent -> Delivered, advanced by ChatRepository on a timer.
 * Only meaningful for messages the user sent (isFromUser = true) — an
 * incoming message's status is never shown, so it's set to DELIVERED
 * and ignored.
 */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED
}

/**
 * Text renders as a normal bubble; EmojiTrigger renders large and tappable;
 * ScoreCard renders the emoji/score/"Beat this score" card.
 */
enum class MessageType {
    TEXT,
    EMOJI_TRIGGER,
    SCORE_CARD
}

data class Message(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val status: MessageStatus,
    val timestamp: Long,
    val type: MessageType = MessageType.TEXT,
    // Only meaningful for SCORE_CARD messages — which emoji was played and
    // what the final score was, so "Beat this score" knows what to relaunch.
    val scoreCardEmoji: String? = null,
    val scoreCardScore: Int? = null
)
