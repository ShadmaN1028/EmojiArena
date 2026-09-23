package com.shadman.emojiarena.chat

import com.shadman.emojiarena.Constants
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Decides the contact's counter-score and when it lands — the score-card
 * equivalent of ScriptedReplyEngine: the same "typing" -> delay -> reply
 * shape, just producing a beaten score instead of a line of dialogue.
 * Always beats the user's score by a small margin, never ties or blows it
 * out, so it reads as "close enough to be a real reply."
 *
 * Not every contact replies to a challenge. A real group of people
 * wouldn't all fire back the instant they're sent something, and a demo
 * where four contacts each answer within two seconds reads as fake. The
 * ones left out of [respondingContactIds] still receive the card and its
 * delivery ticks completely normally — they just never send anything
 * back, so "sent" stays visible instead of being instantly buried under a
 * reply.
 */
object ScoreChallengeEngine {

    // Ayesha Rahman, Meherun Nesa, Sabbir Ahmed reply.
    // Tanvir Chowdhury, Farhan Kabir, Nusrat Jahan, Rafiul Islam, Priya Das don't.
    private val respondingContactIds = setOf("1", "6", "7")

    fun respondsToChallenge(contactId: String): Boolean = contactId in respondingContactIds

    fun scheduleCounterChallenge(
        scope: CoroutineScope,
        userScore: Int,
        onTypingChanged: (Boolean) -> Unit,
        onCounterScore: (Int) -> Unit
    ) {
        scope.launch {
            onTypingChanged(true)
            delay(randomDelayMs())
            onTypingChanged(false)
            onCounterScore(counterScoreFor(userScore))
        }
    }

    private fun counterScoreFor(userScore: Int): Int =
        userScore + Random.nextInt(Constants.SCORE_CHALLENGE_MIN_BEAT, Constants.SCORE_CHALLENGE_MAX_BEAT + 1)

    private fun randomDelayMs(): Long =
        Random.nextLong(Constants.SCRIPTED_REPLY_DELAY_MIN_MS, Constants.SCRIPTED_REPLY_DELAY_MAX_MS + 1)
}
