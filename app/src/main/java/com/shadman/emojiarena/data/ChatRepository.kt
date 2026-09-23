package com.shadman.emojiarena.data

import com.shadman.emojiarena.Constants
import com.shadman.emojiarena.chat.ScoreChallengeEngine
import com.shadman.emojiarena.chat.ScriptedReplyEngine
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The single in-memory source of truth for every contact's conversation.
 *
 * This is what makes the app feel "live" without a backend: it's one object,
 * shared by every screen, holding a Map<contactId, List<Message>> behind a
 * StateFlow. The home screen and a chat screen both read from [conversations]
 * — neither owns its own copy of the messages — so a change made from
 * anywhere (a new message, a status update) is visible everywhere at once.
 *
 * A plain `object` is enough here: no DI framework, no repository interface
 * with a fake/real pair, because there's no real backend to swap in later.
 */
object ChatRepository {

    val contacts: List<Contact> = sampleContacts

    // Owns the delayed status-transition coroutines below. This lives as long
    // as the process does, which matches the repository's own lifetime — it
    // isn't tied to any one screen's ViewModel.
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _conversations = MutableStateFlow(seedConversations())
    val conversations: StateFlow<Map<String, List<Message>>> = _conversations.asStateFlow()

    // How many scripted replies are currently "in flight" per contact.
    // A count rather than a plain flag: if the user fires off several
    // messages in a row, each schedules its own reply, and the indicator
    // should only clear once the LAST of them has landed — not the first.
    private val _typingCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val typingContactIds: StateFlow<Set<String>> = _typingCounts
        .map { counts -> counts.filterValues { it > 0 }.keys }
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptySet())

    /**
     * Adds a user-sent message, advances it Sending -> Sent -> Delivered,
     * and schedules the contact's scripted reply. The reply goes through
     * the exact same [appendMessage] path a real send does, so it drives
     * the same home-screen reordering and preview update.
     */
    fun sendMessage(contactId: String, text: String) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            text = text,
            isFromUser = true,
            status = MessageStatus.SENDING,
            timestamp = System.currentTimeMillis(),
            type = if (isSingleEmoji(text)) MessageType.EMOJI_TRIGGER else MessageType.TEXT
        )
        appendMessage(contactId, message)
        advanceStatus(contactId, message.id)
        ScriptedReplyEngine.scheduleReply(
            scope = repositoryScope,
            contactId = contactId,
            onTypingChanged = { isTyping -> setTyping(contactId, isTyping) },
            onReply = { replyText -> appendIncomingMessage(contactId, replyText) }
        )
    }

    /**
     * Posts the user's own score card into [contactId]'s thread — same
     * delivery-tick pipeline as [sendMessage] — then schedules that
     * contact's counter-card. Reused for both the "Send score" action
     * (one contact, the thread the game was launched from) and "Challenge
     * others" (called once per selected contact), which is why this takes
     * a plain contactId rather than assuming "the current thread".
     */
    fun postScoreCard(contactId: String, emoji: String, score: Int) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            text = "$emoji Scored $score",
            isFromUser = true,
            status = MessageStatus.SENDING,
            timestamp = System.currentTimeMillis(),
            type = MessageType.SCORE_CARD,
            scoreCardEmoji = emoji,
            scoreCardScore = score
        )
        appendMessage(contactId, message)
        advanceStatus(contactId, message.id)
        ScoreChallengeEngine.scheduleCounterChallenge(
            scope = repositoryScope,
            userScore = score,
            onTypingChanged = { isTyping -> setTyping(contactId, isTyping) },
            onCounterScore = { counterScore -> appendIncomingScoreCard(contactId, emoji, counterScore) }
        )
    }

    private fun appendIncomingScoreCard(contactId: String, emoji: String, score: Int) {
        appendMessage(
            contactId,
            Message(
                id = UUID.randomUUID().toString(),
                text = "$emoji I scored $score — can you beat it?",
                isFromUser = false,
                status = MessageStatus.DELIVERED,
                timestamp = System.currentTimeMillis(),
                type = MessageType.SCORE_CARD,
                scoreCardEmoji = emoji,
                scoreCardScore = score
            )
        )
    }

    private fun advanceStatus(contactId: String, messageId: String) {
        repositoryScope.launch {
            delay(Constants.MESSAGE_SENT_DELAY_MS)
            setStatus(contactId, messageId, MessageStatus.SENT)
            delay(Constants.MESSAGE_DELIVERED_DELAY_MS)
            setStatus(contactId, messageId, MessageStatus.DELIVERED)
        }
    }

    private fun appendIncomingMessage(contactId: String, text: String) {
        appendMessage(
            contactId,
            Message(
                id = UUID.randomUUID().toString(),
                text = text,
                isFromUser = false,
                status = MessageStatus.DELIVERED,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun appendMessage(contactId: String, message: Message) {
        _conversations.update { conversations ->
            val thread = conversations[contactId].orEmpty() + message
            conversations + (contactId to thread)
        }
    }

    private fun setStatus(contactId: String, messageId: String, status: MessageStatus) {
        _conversations.update { conversations ->
            val thread = conversations[contactId] ?: return@update conversations
            val updatedThread = thread.map { if (it.id == messageId) it.copy(status = status) else it }
            conversations + (contactId to updatedThread)
        }
    }

    private fun setTyping(contactId: String, isTyping: Boolean) {
        _typingCounts.update { counts ->
            val current = counts[contactId] ?: 0
            val updated = (current + if (isTyping) 1 else -1).coerceAtLeast(0)
            counts + (contactId to updated)
        }
    }
}

/**
 * Seed data so the home screen has real conversations to preview from the
 * first frame — not just 8 empty rows. Deliberately mixes endings (their
 * message last / delivered / sent) so the home screen shows every tick
 * state without anyone having to send a message first.
 */
private fun seedConversations(): Map<String, List<Message>> {
    val now = System.currentTimeMillis()

    fun incoming(text: String) =
        Message(UUID.randomUUID().toString(), text, isFromUser = false, status = MessageStatus.DELIVERED, timestamp = now)

    fun outgoing(text: String, status: MessageStatus) =
        Message(UUID.randomUUID().toString(), text, isFromUser = true, status = status, timestamp = now)

    val base = mapOf(
        "1" to listOf(incoming("Hey! Are you free this weekend?")),
        "2" to listOf(incoming("Don't forget the meeting at 10"), outgoing("Got it, see you there", MessageStatus.DELIVERED)),
        "3" to listOf(incoming("Happy birthday! 🎉")),
        "4" to listOf(outgoing("Can you send the notes?", MessageStatus.SENT)),
        "5" to listOf(incoming("Lunch tomorrow?"), outgoing("Sounds good", MessageStatus.DELIVERED)),
        "6" to listOf(incoming("Did you watch the match last night?")),
        "7" to listOf(outgoing("On my way", MessageStatus.DELIVERED)),
        "8" to listOf(incoming("Long time no chat 👋"))
    )

    // Live from the very first cold start: Tanvir already challenged the
    // user before they've touched anything. Timestamped a few minutes
    // before "now" rather than sharing it exactly, so it reads as a
    // message that arrived a bit ago, not one born at the same instant as
    // the app itself.
    val challengeContactId = Constants.SEEDED_CHALLENGE_CONTACT_ID
    val challenge = Message(
        id = UUID.randomUUID().toString(),
        text = "${Constants.DEFAULT_BALL_EMOJI} I scored ${Constants.SEEDED_CHALLENGE_SCORE} — can you beat it?",
        isFromUser = false,
        status = MessageStatus.DELIVERED,
        timestamp = now - 5 * 60_000L,
        type = MessageType.SCORE_CARD,
        scoreCardEmoji = Constants.DEFAULT_BALL_EMOJI,
        scoreCardScore = Constants.SEEDED_CHALLENGE_SCORE
    )
    return base + (challengeContactId to (base.getValue(challengeContactId) + challenge))
}
