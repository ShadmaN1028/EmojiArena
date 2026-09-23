package com.shadman.emojiarena.game

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.shadman.emojiarena.ui.theme.AccentCoral

/** Colors for one board look. Rim stays accent-colored in both. */
data class BoardPalette(val background: Color, val boardFill: Color, val boardBorder: Color)

val LightBoardPalette = BoardPalette(
    background = Color.White,
    boardFill = Color.White,
    boardBorder = Color.Black
)

val DarkBoardPalette = BoardPalette(
    background = Color.Black,
    boardFill = Color.Black,
    boardBorder = Color.White
)

private const val BOARD_BORDER_WIDTH = 7f
private const val INNER_BORDER_WIDTH = 5f

/** Backboard (fill + bold border), the painted inner square, net, and rim posts. */
fun DrawScope.drawHoop(hoop: HoopGeometry, palette: BoardPalette) {
    drawRect(
        color = palette.boardFill,
        topLeft = Offset(hoop.boardLeft, hoop.boardTop),
        size = Size(hoop.boardWidth, hoop.boardHeight)
    )
    drawRect(
        color = palette.boardBorder,
        topLeft = Offset(hoop.boardLeft, hoop.boardTop),
        size = Size(hoop.boardWidth, hoop.boardHeight),
        style = Stroke(width = BOARD_BORDER_WIDTH)
    )
    drawRect(
        color = palette.boardBorder,
        topLeft = Offset(hoop.innerLeft, hoop.innerTop),
        size = Size(hoop.innerWidth, hoop.innerHeight),
        style = Stroke(width = INNER_BORDER_WIDTH)
    )

    val netTopY = hoop.rimY
    val netBottomY = hoop.rimY + hoop.rimPostRadius * 3.5f
    val netStrandCount = 5
    for (i in 0 until netStrandCount) {
        val t = i / (netStrandCount - 1f)
        val topX = hoop.rimLeft.x + (hoop.rimRight.x - hoop.rimLeft.x) * t
        val bottomX = hoop.boardCenterX + (topX - hoop.boardCenterX) * 0.35f
        drawLine(
            color = palette.boardBorder,
            start = Offset(topX, netTopY),
            end = Offset(bottomX, netBottomY),
            strokeWidth = 2f
        )
    }

    drawLine(
        color = AccentCoral,
        start = Offset(hoop.rimLeft.x, hoop.rimY),
        end = Offset(hoop.rimRight.x, hoop.rimY),
        strokeWidth = hoop.rimPostRadius * 0.7f
    )
    listOf(hoop.rimLeft, hoop.rimRight).forEach { post ->
        drawCircle(color = AccentCoral, radius = hoop.rimPostRadius, center = Offset(post.x, post.y))
        drawCircle(
            color = palette.boardBorder,
            radius = hoop.rimPostRadius,
            center = Offset(post.x, post.y),
            style = Stroke(width = 2f)
        )
    }
}

// Reused across frames rather than allocated per draw call — textSize is
// the only thing that changes call to call, and that's cheap to set.
private val ballEmojiPaint = Paint().apply {
    isAntiAlias = true
    textAlign = Paint.Align.CENTER
}

/** The ball's drop shadow, plus whatever emoji the player sent as the glyph. */
fun DrawScope.drawBall(position: Vec2, radius: Float, emoji: String) {
    drawOval(
        color = Color.Black.copy(alpha = 0.2f),
        topLeft = Offset(position.x - radius * 0.9f, position.y + radius * 0.55f),
        size = Size(radius * 1.8f, radius * 0.7f)
    )
    drawContext.canvas.nativeCanvas.apply {
        ballEmojiPaint.textSize = radius * 1.7f
        // Paint.Align.CENTER handles x; drawText baselines at y, so nudge
        // up to visually center the glyph on the ball's tracked position.
        drawText(emoji, position.x, position.y + ballEmojiPaint.textSize * 0.35f, ballEmojiPaint)
    }
}

/** The pull-back aim indicator while the player is dragging from the ball. */
fun DrawScope.drawAimLine(ballPosition: Vec2, dragVector: Vec2, palette: BoardPalette) {
    drawLine(
        color = palette.boardBorder.copy(alpha = 0.5f),
        start = Offset(ballPosition.x, ballPosition.y),
        end = Offset(ballPosition.x + dragVector.x, ballPosition.y + dragVector.y),
        strokeWidth = 4f
    )
}
