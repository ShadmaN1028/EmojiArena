package com.shadman.emojiarena.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadman.emojiarena.chat.ChatViewModel
import com.shadman.emojiarena.ui.components.MessageBubble
import com.shadman.emojiarena.ui.components.TypingIndicatorBubble

/**
 * Send/receive shell: real bubbles, a working input bar, and status ticks.
 * Typing and hitting send appends to ChatRepository for this contact — the
 * same repository the home screen reads, so its preview updates the
 * moment you navigate back. Scripted replies (section 3) hook into
 * ChatViewModel next, not this composable.
 *
 * Inset handling is deliberate here, not left to defaults: the top bar is
 * a Scaffold `topBar` (which insets itself below the status bar on its
 * own), and `contentWindowInsets` is zeroed so Scaffold doesn't ALSO pad
 * the content — that padding is applied by hand below instead. The input
 * bar lives inside the content Column (not Scaffold's `bottomBar` slot)
 * with `.navigationBarsPadding().imePadding()` on that same Column, so as
 * the keyboard opens, only this Column's bottom shrinks: the message list
 * (weight 1f) compresses and the input bar rides up above the keyboard,
 * while the top bar — outside this Column entirely — never moves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactId: String,
    onBack: () -> Unit,
    onPlayEmoji: (String) -> Unit
) {
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(contactId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Keep the newest item in view as new ones arrive: a sent or received
    // message, or the typing indicator appearing/disappearing. The list
    // below is reverseLayout, so "newest" is always index 0 — no item-count
    // arithmetic needed, and nothing to get wrong when the keyboard resizes
    // the viewport out from under an in-flight scroll animation.
    LaunchedEffect(uiState.messages.size, uiState.isTyping) {
        if (uiState.messages.isNotEmpty() || uiState.isTyping) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.contact?.name ?: "Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reversed because reverseLayout puts index 0 at the visual
                // bottom — the typing indicator (newest thing, when present)
                // goes first so it lands closest to the input bar.
                if (uiState.isTyping) {
                    item(key = "typing-indicator") {
                        TypingIndicatorBubble()
                    }
                }
                items(uiState.messages.asReversed(), key = { it.id }) { message ->
                    MessageBubble(message = message, onPlayEmoji = onPlayEmoji)
                }
            }
            ChatInputBar(onSend = viewModel::sendMessage)
        }
    }
}

@Composable
private fun ChatInputBar(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    fun submit() {
        if (text.isNotBlank()) {
            onSend(text)
            text = ""
        }
    }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                singleLine = true,
                shape = CircleShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(
                onClick = { submit() },
                enabled = text.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(44.dp)
            ) {
                Text(text = "↑")
            }
        }
    }
}
