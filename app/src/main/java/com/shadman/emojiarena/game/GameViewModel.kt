package com.shadman.emojiarena.game

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadman.emojiarena.Constants
import com.shadman.emojiarena.data.ChatRepository
import com.shadman.emojiarena.data.Contact
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GamePhase { AIMING, FLYING, GAME_OVER }

data class GameUiState(
    val canvasWidth: Float = 0f,
    val canvasHeight: Float = 0f,
    val ballPosition: Vec2 = Vec2(0f, 0f),
    val phase: GamePhase = GamePhase.AIMING,
    val score: Int = 0,
    val livesRemaining: Int = Constants.GAME_LIVES_COUNT,
    val hoopPreset: HoopPreset = HOOP_PRESETS.first(),
    // Ball -> current finger position, while aiming. Null when not dragging.
    val aimDragVector: Vec2? = null,
    val showGoalCelebration: Boolean = false,
    val ballEmoji: String = Constants.DEFAULT_BALL_EMOJI,
    val scoreSentToOrigin: Boolean = false
)

/**
 * Owns the whole game: state machine (aiming/flying/game over), the drag
 * gesture that becomes a launch velocity, and the per-frame physics loop.
 * The actual physics math lives in GamePhysics.kt as pure functions — this
 * class is the orchestration around it (when a flight starts/ends, what a
 * score or a miss does to lives/score/the next hoop).
 *
 * [ballEmoji] is whatever the player actually sent — resolved once by the
 * caller (see Factory), not re-derived here — and is carried through every
 * state rebuild, including [restart], since it doesn't change mid-game.
 * [originContactId] is the thread the game was launched from, for posting
 * a score card back to (section 6) — this class talks to ChatRepository
 * directly for that, the same way ChatViewModel does, rather than routing
 * chat writes through some extra layer.
 */
class GameViewModel(
    private val ballEmoji: String = Constants.DEFAULT_BALL_EMOJI,
    private val originContactId: String = ""
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState(ballEmoji = ballEmoji))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Static reference data for the "Challenge others" sheet — never
    // changes mid-game, so a plain val is enough; no need to route it
    // through the reactive uiState.
    val otherContacts: List<Contact> = ChatRepository.contacts.filter { it.id != originContactId }

    private var dragStartPosition: Vec2? = null
    private var dragStartTimeNanos: Long = 0L
    private var flightVelocity = Vec2(0f, 0f)
    private var flightJob: Job? = null

    /** Called once the Canvas reports its measured size (and again on resize). */
    fun onCanvasSizeChanged(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        val firstTime = _uiState.value.canvasWidth <= 0f
        _uiState.update {
            it.copy(
                canvasWidth = width,
                canvasHeight = height,
                ballPosition = if (firstTime) ballStartPosition(width, height) else it.ballPosition
            )
        }
    }

    fun onDragStart(position: Offset) {
        val state = _uiState.value
        if (state.phase != GamePhase.AIMING) return
        val pos = Vec2(position.x, position.y)
        val grabRadius = state.canvasWidth * GameTuning.GRAB_RADIUS_FRACTION
        if ((pos - state.ballPosition).length() > grabRadius) return
        dragStartPosition = pos
        dragStartTimeNanos = System.nanoTime()
    }

    fun onDrag(position: Offset) {
        if (dragStartPosition == null) return
        val pos = Vec2(position.x, position.y)
        _uiState.update { it.copy(aimDragVector = pos - it.ballPosition) }
    }

    fun onDragEnd(position: Offset) {
        val start = dragStartPosition
        dragStartPosition = null
        _uiState.update { it.copy(aimDragVector = null) }
        if (start == null) return

        val state = _uiState.value
        val delta = Vec2(position.x, position.y) - start
        val minDistance = state.canvasWidth * GameTuning.MIN_DRAG_DISTANCE_FRACTION
        if (delta.length() < minDistance) return // accidental tap, not a shot

        val elapsedSeconds = (System.nanoTime() - dragStartTimeNanos)
            .coerceAtLeast(1_000_000L) / 1_000_000_000f
        val rawVelocity = delta * (GameTuning.LAUNCH_VELOCITY_SCALE / elapsedSeconds)
        flightVelocity = clampSpeed(rawVelocity, state.canvasHeight)
        _uiState.update { it.copy(phase = GamePhase.FLYING) }
        startFlightLoop()
    }

    /** A gesture the system interrupted — clear aim, don't launch anything. */
    fun onDragCancel() {
        dragStartPosition = null
        _uiState.update { it.copy(aimDragVector = null) }
    }

    /**
     * Posts the final score back into the thread the game was launched
     * from. One-shot per game — posting your own score twice into the
     * same thread wouldn't mean anything, so this no-ops once
     * [GameUiState.scoreSentToOrigin] is already true.
     */
    fun sendScoreToOriginThread() {
        val state = _uiState.value
        if (state.scoreSentToOrigin || originContactId.isBlank()) return
        ChatRepository.postScoreCard(originContactId, ballEmoji, state.score)
        _uiState.update { it.copy(scoreSentToOrigin = true) }
    }

    /** Posts the same score card into each selected contact's thread. */
    fun challengeContacts(contactIds: List<String>) {
        val score = _uiState.value.score
        contactIds.forEach { contactId -> ChatRepository.postScoreCard(contactId, ballEmoji, score) }
    }

    fun restart() {
        flightJob?.cancel()
        val width = _uiState.value.canvasWidth
        val height = _uiState.value.canvasHeight
        _uiState.value = GameUiState(
            canvasWidth = width,
            canvasHeight = height,
            ballPosition = ballStartPosition(width, height),
            hoopPreset = HOOP_PRESETS.random(),
            ballEmoji = ballEmoji
        )
    }

    private fun startFlightLoop() {
        flightJob?.cancel()
        val state = _uiState.value
        val ballRadius = state.canvasWidth * GameTuning.BALL_RADIUS_FRACTION
        val hoop = HoopGeometry(state.canvasWidth, state.canvasHeight, state.hoopPreset, ballRadius)
        val gravity = state.canvasHeight * GameTuning.GRAVITY_PER_HEIGHT
        flightJob = viewModelScope.launch {
            runFlight(hoop, ballRadius, gravity, state.canvasWidth, state.canvasHeight)
        }
    }

    private suspend fun runFlight(
        hoop: HoopGeometry,
        ballRadius: Float,
        gravity: Float,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        var position = _uiState.value.ballPosition
        var velocity = flightVelocity
        val subDt = (GameTuning.FRAME_DELAY_MS / 1000f) / GameTuning.PHYSICS_SUBSTEPS

        while (_uiState.value.phase == GamePhase.FLYING) {
            delay(GameTuning.FRAME_DELAY_MS)
            var outcome = SubstepOutcome.CONTINUE

            for (i in 0 until GameTuning.PHYSICS_SUBSTEPS) {
                val step = simulateSubstep(position, velocity, subDt, gravity, ballRadius, canvasWidth, canvasHeight, hoop)
                position = step.position
                velocity = step.velocity
                outcome = step.outcome
                if (outcome != SubstepOutcome.CONTINUE) break
            }

            _uiState.update { it.copy(ballPosition = position) }

            when (outcome) {
                SubstepOutcome.SCORED -> {
                    _uiState.update { it.copy(score = it.score + 1, showGoalCelebration = true) }
                    fallThroughAfterScore(position, velocity, gravity)
                    finishScoredShot()
                    return
                }
                SubstepOutcome.MISSED -> {
                    onMissed()
                    return
                }
                SubstepOutcome.CONTINUE -> Unit
            }
        }
    }

    /**
     * The ball already passed through the gap — there's nothing left for it
     * to hit, so this just keeps applying gravity (same integration as
     * simulateSubstep, minus the collision/scoring checks) for the
     * celebration window, so it visibly falls away instead of freezing in
     * place. "GOAL!!!" is up on screen the whole time.
     */
    private suspend fun fallThroughAfterScore(startPosition: Vec2, startVelocity: Vec2, gravity: Float) {
        var position = startPosition
        var velocity = startVelocity
        val subDt = (GameTuning.FRAME_DELAY_MS / 1000f) / GameTuning.PHYSICS_SUBSTEPS
        val frames = (Constants.SCORE_CELEBRATION_DELAY_MS / GameTuning.FRAME_DELAY_MS).toInt()

        repeat(frames) {
            delay(GameTuning.FRAME_DELAY_MS)
            repeat(GameTuning.PHYSICS_SUBSTEPS) {
                velocity = velocity + Vec2(0f, gravity * subDt)
                position = position + velocity * subDt
            }
            _uiState.update { it.copy(ballPosition = position) }
        }
    }

    /** Resets for the next throw once the post-score fall has played out. */
    private fun finishScoredShot() {
        val state = _uiState.value
        val nextHoop = HOOP_PRESETS.filter { it != state.hoopPreset }.random()
        _uiState.update {
            it.copy(
                phase = GamePhase.AIMING,
                hoopPreset = nextHoop,
                ballPosition = ballStartPosition(state.canvasWidth, state.canvasHeight),
                showGoalCelebration = false
            )
        }
    }

    /** A miss resets immediately — no fall-through, no celebration. */
    private fun onMissed() {
        val state = _uiState.value
        val livesRemaining = state.livesRemaining - 1
        val gameOver = livesRemaining <= 0
        val nextHoop = HOOP_PRESETS.filter { it != state.hoopPreset }.random()

        _uiState.update {
            it.copy(
                livesRemaining = livesRemaining,
                phase = if (gameOver) GamePhase.GAME_OVER else GamePhase.AIMING,
                hoopPreset = if (gameOver) it.hoopPreset else nextHoop,
                ballPosition = ballStartPosition(state.canvasWidth, state.canvasHeight)
            )
        }
    }

    /** Keeps a flick's speed in a controllable range without touching its direction. */
    private fun clampSpeed(velocity: Vec2, canvasHeight: Float): Vec2 {
        val speed = velocity.length()
        if (speed < 0.0001f) return velocity
        val minSpeed = canvasHeight * Constants.MIN_LAUNCH_SPEED_PER_HEIGHT
        val maxSpeed = canvasHeight * Constants.MAX_LAUNCH_SPEED_PER_HEIGHT
        val clampedSpeed = speed.coerceIn(minSpeed, maxSpeed)
        return velocity * (clampedSpeed / speed)
    }

    private fun ballStartPosition(width: Float, height: Float) = Vec2(width * 0.5f, height * 0.86f)

    /** GameViewModel takes constructor args, so it needs its own factory. */
    class Factory(
        private val ballEmoji: String,
        private val originContactId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameViewModel(ballEmoji, originContactId) as T
        }
    }
}
