package com.shadman.emojiarena.game

import kotlin.math.sqrt

/** Minimal 2D vector — just what the physics below needs, nothing generic. */
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)
    fun length(): Float = sqrt(x * x + y * y)
}

/** Tuning knobs for the physics simulation. All sizes are fractions of the
 * canvas so the game scales to any screen; GameViewModel multiplies these
 * by the measured canvas size once it knows it. */
object GameTuning {
    const val BALL_RADIUS_FRACTION = 0.052f
    const val GRAVITY_PER_HEIGHT = 1.1f // px/s^2 = canvasHeight * this
    const val MIN_DRAG_DISTANCE_FRACTION = 0.04f // ignore drags shorter than this (accidental taps)
    const val LAUNCH_VELOCITY_SCALE = 1.15f
    const val PHYSICS_SUBSTEPS = 4
    const val FRAME_DELAY_MS = 16L
    const val GRAB_RADIUS_FRACTION = 0.12f // how close to the ball a drag must start
}

/** One possible hoop position, as a fraction of canvas size. */
data class HoopPreset(val xFraction: Float, val yFraction: Float)

val HOOP_PRESETS = listOf(
    HoopPreset(0.28f, 0.16f),
    HoopPreset(0.50f, 0.14f),
    HoopPreset(0.72f, 0.18f),
    HoopPreset(0.38f, 0.26f),
    HoopPreset(0.65f, 0.24f)
)

/**
 * All the geometry for one hoop, resolved to real pixels for a given canvas
 * size and preset. The backboard's painted inner square is purely visual.
 *
 * The board's collision shape is not one solid rectangle — it's two panels
 * flanking a vertical corridor above the rim gap. A real ball reaches the
 * rim by passing in front of the glass, not through it; this 2D game has
 * no depth axis to express that, so the corridor is the stand-in. Without
 * it, the ball's fixed start position and the board's width mean a shot
 * aimed at the gap is, by definition, aimed at a point inside the board's
 * own silhouette — so on the way up (or the way down) it would clip the
 * board's face before ever reaching the gap. The corridor gives a shot
 * aimed at the gap a lane that's actually clear, on both legs of the arc.
 */
class HoopGeometry(canvasWidth: Float, canvasHeight: Float, preset: HoopPreset, ballRadius: Float) {
    val boardWidth = canvasWidth * 0.32f
    val boardHeight = boardWidth * 0.58f
    val boardCenterX = canvasWidth * preset.xFraction
    val boardTop = canvasHeight * preset.yFraction
    val boardBottom = boardTop + boardHeight
    val boardLeft = boardCenterX - boardWidth / 2f
    val boardRight = boardCenterX + boardWidth / 2f

    val innerWidth = boardWidth * 0.40f
    val innerHeight = boardHeight * 0.46f
    val innerBottom = boardBottom - boardHeight * 0.16f
    val innerTop = innerBottom - innerHeight
    val innerLeft = boardCenterX - innerWidth / 2f
    val innerRight = boardCenterX + innerWidth / 2f

    val rimY = boardBottom - boardHeight * 0.05f
    val rimHalfWidth = boardWidth * 0.30f
    val rimPostRadius = boardWidth * 0.045f
    val rimLeft = Vec2(boardCenterX - rimHalfWidth, rimY)
    val rimRight = Vec2(boardCenterX + rimHalfWidth, rimY)

    // A ball must clear this much from each post to be "through the gap".
    val scoreGapLeft = rimLeft.x + rimPostRadius
    val scoreGapRight = rimRight.x - rimPostRadius

    // The side panels' collision edges sit a full ball-radius further out
    // than the scoring gap, not just past the rim posts. The gap's own
    // margin from each post (rimPostRadius) is far smaller than the ball —
    // so without this, a ball whose CENTER is legitimately inside the
    // scoring gap still has more than half its body poking past the
    // panel's edge, and gets flagged as hitting the board on the way down
    // before it ever reaches rim height to be scored. This guarantees any
    // shot that stays within the scoring gap the whole descent has genuine
    // clearance to actually fall through.
    val corridorLeft = (scoreGapLeft - ballRadius).coerceAtLeast(boardLeft)
    val corridorRight = (scoreGapRight + ballRadius).coerceAtMost(boardRight)
}

/** Circle vs. axis-aligned rectangle — used for the backboard's two side panels. */
fun circleVsRect(center: Vec2, radius: Float, left: Float, top: Float, right: Float, bottom: Float): Boolean {
    val closestX = center.x.coerceIn(left, right)
    val closestY = center.y.coerceIn(top, bottom)
    val dist = Vec2(center.x - closestX, center.y - closestY).length()
    return dist < radius
}

/**
 * True if the ball crosses the rim's height this step while descending —
 * regardless of x. Crossing is the one moment that must be judged; whether
 * it lands in the gap is a separate question (see simulateSubstep), because
 * a post's own collision radius can't be used for that judgment: the post
 * is small next to the ball, so "close to the post" starts well inside the
 * gap's own x-range while the ball is still above rim height, on its way
 * down. Judging make/miss only at the actual crossing line keeps a shot
 * that's genuinely headed into the gap from being flagged early.
 */
fun crossedRim(prevY: Float, newY: Float, velocityY: Float, hoop: HoopGeometry): Boolean {
    if (velocityY <= 0f) return false
    return prevY < hoop.rimY && newY >= hoop.rimY
}

enum class SubstepOutcome { CONTINUE, SCORED, MISSED }

data class SubstepResult(val position: Vec2, val velocity: Vec2, val outcome: SubstepOutcome)

/**
 * Advances the ball by one physics substep: gravity, then a scoring check,
 * then backboard/rim contact, then an off-screen check. Pure function, no
 * coroutines/state — the ViewModel's loop just calls this once per substep
 * and applies the result.
 *
 * Contact with a side panel ends the flight as a miss on the spot, rather
 * than reflecting the velocity. A reflection version was
 * tried first (damped bounce off the surface normal) and worked for a
 * single clean hit, but landed in exactly the failure mode the spec
 * flagged up front: a shot grazing the board's edge at low speed would
 * reflect, get pulled back into the same surface by gravity next substep,
 * reflect again at ever-smaller penetration, and never resolve — a stuck
 * vibration loop that hung the flight indefinitely. Per the spec's own
 * fallback, contact now just ends the shot. "Rattled it in" off a bounce
 * is consequently not possible; every make is a direct shot through the
 * gap.
 */
fun simulateSubstep(
    position: Vec2,
    velocity: Vec2,
    dt: Float,
    gravity: Float,
    ballRadius: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    hoop: HoopGeometry
): SubstepResult {
    val prevY = position.y
    val newVelocity = velocity + Vec2(0f, gravity * dt)
    val candidate = position + newVelocity * dt

    if (crossedRim(prevY, candidate.y, newVelocity.y, hoop)) {
        val scored = candidate.x in hoop.scoreGapLeft..hoop.scoreGapRight
        return SubstepResult(candidate, newVelocity, if (scored) SubstepOutcome.SCORED else SubstepOutcome.MISSED)
    }

    // The side panels collide while descending. The corridor gives a
    // well-aimed shot a lane past the board on both legs of the arc, but
    // the ball starts well to one side of most hoops (its x is fixed; the
    // hoop's isn't) — early in the ascent, before it's drifted into that
    // lane, it can still be beside a panel at board height. Gating on
    // descent is what keeps that early transit from clipping something
    // the shot was never really aimed at. The rim posts have no separate
    // proximity check here — see crossedRim's doc comment for why — a
    // shot that crosses outside the gap is already a miss above.
    val hitPanel = newVelocity.y > 0f && (
        circleVsRect(candidate, ballRadius, hoop.boardLeft, hoop.boardTop, hoop.corridorLeft, hoop.boardBottom) ||
            circleVsRect(candidate, ballRadius, hoop.corridorRight, hoop.boardTop, hoop.boardRight, hoop.boardBottom)
        )
    if (hitPanel) {
        return SubstepResult(candidate, newVelocity, SubstepOutcome.MISSED)
    }

    val offScreen = candidate.y - ballRadius > canvasHeight ||
        candidate.x < -ballRadius * 2f ||
        candidate.x > canvasWidth + ballRadius * 2f

    return SubstepResult(
        position = candidate,
        velocity = newVelocity,
        outcome = if (offScreen) SubstepOutcome.MISSED else SubstepOutcome.CONTINUE
    )
}
