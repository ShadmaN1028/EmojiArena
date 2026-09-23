package com.shadman.emojiarena.chat

import com.shadman.emojiarena.Constants
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Decides what a contact says back and when — owns the reply pool, each
 * contact's position in it, and the "thinking" delay. Knows nothing about
 * ChatRepository or Message; it's handed plain callbacks instead, so it
 * has no dependency on where the reply actually ends up.
 */
object ScriptedReplyEngine {

    // One shared, generic pool — the lines don't need to relate to what
    // the user sent, so there's no need to maintain a separate list per
    // contact.
    private val replyPool = listOf(
        "Haha true",
        "Let me check and get back to you",
        "Sounds good!",
        "No way, really?"
    )

    // How far each contact has rotated through the pool, tracked
    // separately per contact so two contacts don't land on the same line
    // at the same time.
    private val nextIndexByContact = mutableMapOf<String, Int>()

    /**
     * Runs the "typing" -> delay -> reply sequence on [scope]. Call once
     * per user message sent; overlapping calls for the same contact are
     * fine; [onTypingChanged] fires true/false in matching pairs, so the
     * caller can just count them if several are in flight at once.
     */
    fun scheduleReply(
        scope: CoroutineScope,
        contactId: String,
        onTypingChanged: (Boolean) -> Unit,
        onReply: (String) -> Unit
    ) {
        scope.launch {
            onTypingChanged(true)
            delay(randomDelayMs())
            onTypingChanged(false)
            onReply(nextReplyFor(contactId))
        }
    }

    private fun nextReplyFor(contactId: String): String {
        val index = nextIndexByContact.getOrDefault(contactId, 0)
        nextIndexByContact[contactId] = index + 1
        return replyPool[index % replyPool.size]
    }

    private fun randomDelayMs(): Long =
        Random.nextLong(Constants.SCRIPTED_REPLY_DELAY_MIN_MS, Constants.SCRIPTED_REPLY_DELAY_MAX_MS + 1)
}
