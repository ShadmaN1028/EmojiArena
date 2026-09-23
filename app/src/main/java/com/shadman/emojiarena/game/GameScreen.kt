package com.shadman.emojiarena.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadman.emojiarena.Constants
import com.shadman.emojiarena.data.Contact
import com.shadman.emojiarena.ui.components.ChallengeContactsSheet
import com.shadman.emojiarena.ui.theme.AccentCoral

// Fixed rather than wrap-content, so "Send Score" / "Challenge Others" /
// "Play Again" — three very different label lengths — read as one
// deliberate button group instead of three oddly-sized pills.
private val GameOverCardWidth = 260.dp

/**
 * The full-screen basketball mini-game, launched by tapping a sent emoji
 * or a score card in chat — [ballEmoji] is that emoji and [originContactId]
 * is the thread it came from, both resolved by MainActivity from the nav
 * route. Renders GameViewModel's state; all physics/scoring decisions
 * happen there, this file only draws and forwards gestures.
 */
@Composable
fun GameScreen(
    ballEmoji: String,
    originContactId: String,
    viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory(ballEmoji, originContactId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isDarkBoard by remember { mutableStateOf(false) }
    val palette = if (isDarkBoard) DarkBoardPalette else LightBoardPalette

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    viewModel.onCanvasSizeChanged(size.width.toFloat(), size.height.toFloat())
                }
                .pointerInput(viewModel) {
                    var lastPosition = Offset.Zero
                    detectDragGestures(
                        onDragStart = { offset ->
                            lastPosition = offset
                            viewModel.onDragStart(offset)
                        },
                        onDrag = { change, _ ->
                            lastPosition = change.position
                            viewModel.onDrag(change.position)
                        },
                        onDragEnd = { viewModel.onDragEnd(lastPosition) },
                        onDragCancel = { viewModel.onDragCancel() }
                    )
                }
        ) {
            if (uiState.canvasWidth <= 0f) return@Canvas
            val ballRadius = uiState.canvasWidth * GameTuning.BALL_RADIUS_FRACTION
            val hoop = HoopGeometry(uiState.canvasWidth, uiState.canvasHeight, uiState.hoopPreset, ballRadius)
            drawHoop(hoop, palette)
            uiState.aimDragVector?.let { drawAimLine(uiState.ballPosition, it, palette) }
            drawBall(uiState.ballPosition, ballRadius, uiState.ballEmoji)
        }

        GameHud(
            score = uiState.score,
            livesRemaining = uiState.livesRemaining,
            isDarkBoard = isDarkBoard,
            onToggleBoard = { isDarkBoard = !isDarkBoard },
            palette = palette
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GoalCelebration(visible = uiState.showGoalCelebration)
        }

        if (uiState.phase == GamePhase.GAME_OVER) {
            GameOverOverlay(
                score = uiState.score,
                scoreSent = uiState.scoreSentToOrigin,
                otherContacts = viewModel.otherContacts,
                palette = palette,
                onPlayAgain = viewModel::restart,
                onSendScore = viewModel::sendScoreToOriginThread,
                onChallengeContacts = viewModel::challengeContacts
            )
        }
    }
}

@Composable
private fun GameHud(
    score: Int,
    livesRemaining: Int,
    isDarkBoard: Boolean,
    onToggleBoard: () -> Unit,
    palette: BoardPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Score: $score",
            style = MaterialTheme.typography.titleMedium,
            color = palette.boardBorder
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(Constants.GAME_LIVES_COUNT) { index ->
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = if (index < livesRemaining) Color(0xFFFF4B4B) else palette.boardBorder.copy(alpha = 0.25f),
                    modifier = Modifier.size(22.dp)
                )
                if (index < Constants.GAME_LIVES_COUNT - 1) Spacer(modifier = Modifier.width(4.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = onToggleBoard) {
                Icon(
                    imageVector = if (isDarkBoard) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Switch board theme",
                    tint = palette.boardBorder
                )
            }
        }
    }
}

/** Purely a rendering of showGoalCelebration — the ViewModel owns when it's up and for how long. */
@Composable
private fun GoalCelebration(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.6f) + fadeIn(),
        exit = fadeOut()
    ) {
        Text(
            text = Constants.GOAL_CELEBRATION_TEXT,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = AccentCoral
        )
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    scoreSent: Boolean,
    otherContacts: List<Contact>,
    palette: BoardPalette,
    onPlayAgain: () -> Unit,
    onSendScore: () -> Unit,
    onChallengeContacts: (List<String>) -> Unit
) {
    var showChallengeSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(GameOverCardWidth)
                .background(palette.background, RoundedCornerShape(20.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Game Over", style = MaterialTheme.typography.titleLarge, color = palette.boardBorder)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Score: $score", style = MaterialTheme.typography.headlineMedium, color = AccentCoral)
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(onClick = onSendScore, enabled = !scoreSent, modifier = Modifier.fillMaxWidth()) {
                Text(if (scoreSent) "Score Sent" else "Send Score")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { showChallengeSheet = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Challenge Others")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
                Text("Play Again")
            }
        }
    }

    if (showChallengeSheet) {
        ChallengeContactsSheet(
            contacts = otherContacts,
            onDismiss = { showChallengeSheet = false },
            onConfirm = { contactIds ->
                onChallengeContacts(contactIds)
                showChallengeSheet = false
            }
        )
    }
}
