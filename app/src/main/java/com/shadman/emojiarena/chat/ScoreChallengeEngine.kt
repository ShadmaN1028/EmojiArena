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
 */
object ScoreChallengeEngine {

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
