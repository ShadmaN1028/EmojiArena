package com.shadman.emojiarena

/**
 * Every tunable number/string that isn't a one-off UI value lives here.
 * Later sections (scripted replies, the game, score cards) add their
 * constants to this same object rather than scattering literals around.
 */
object Constants {

    // How long a message sits in each status before advancing.
    // ChatRepository uses these to fake a real send pipeline with no server.
    const val MESSAGE_SENT_DELAY_MS = 300L
    const val MESSAGE_DELIVERED_DELAY_MS = 1200L

    // How long a contact "thinks" before replying. Randomized within this
    // range (rather than one fixed number) so replies don't all land at
    // an identical, obviously-scripted tempo.
    const val SCRIPTED_REPLY_DELAY_MIN_MS = 1500L
    const val SCRIPTED_REPLY_DELAY_MAX_MS = 2500L

    // The basketball mini-game has no timer — lives are the only end
    // condition. Detailed physics tuning (gravity, bounce damping, ball
    // size, hoop positions) lives in game/GamePhysics.kt instead of here:
    // it's a dense, self-contained set of numbers specific to that one
    // simulation, not general app config.
    const val GAME_LIVES_COUNT = 3

    // A flick's raw speed (drag distance / duration) is clamped into this
    // range before launch — both expressed as canvasHeight * this, same as
    // gravity in GamePhysics.kt. Without a floor, a lazy swipe barely lobs
    // the ball; without a ceiling, a fast flick clears the screen before
    // gravity gets a say.
    const val MIN_LAUNCH_SPEED_PER_HEIGHT = 0.75f
    const val MAX_LAUNCH_SPEED_PER_HEIGHT = 1.2f

    // How long the ball keeps falling (under gravity, no more collision
    // checks — it's already through) and "GOAL!!!" stays on screen before
    // the next throw resets it.
    const val SCORE_CELEBRATION_DELAY_MS = 550L
    const val GOAL_CELEBRATION_TEXT = "GOAL!!!"

    // Any single emoji sent in chat becomes the ball's glyph in the game
    // (see data/EmojiDetection.kt for what counts as "a single emoji").
    // This is what the game falls back to when a screen is entered
    // without a real one — normally only possible via a bug, since the
    // real entry point (tapping a sent emoji) always has one.
    const val DEFAULT_BALL_EMOJI = "🏀"

    // A contact's counter-score always beats the user's by a random amount
    // in this range — never a tie, never a landslide, just enough to feel
    // like a real "I got you" reply.
    const val SCORE_CHALLENGE_MIN_BEAT = 1
    const val SCORE_CHALLENGE_MAX_BEAT = 3

    // The one pre-seeded incoming challenge — Tanvir's opening "beat this"
    // card, present in his thread from the very first cold start.
    const val SEEDED_CHALLENGE_CONTACT_ID = "2"
    const val SEEDED_CHALLENGE_SCORE = 10
}
