package com.shadman.emojiarena.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadman.emojiarena.data.ChatRepository
import com.shadman.emojiarena.data.Contact
import com.shadman.emojiarena.data.Message
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ChatUiState(
    val contact: Contact? = null,
    val messages: List<Message> = emptyList(),
    val isTyping: Boolean = false
)

/**
 * Reads the one contact's thread — and whether they're currently
 * "typing" a scripted reply — out of ChatRepository, and forwards sends
 * back into it. ScriptedReplyEngine (triggered from ChatRepository, not
 * from here) is what actually decides the reply's text and timing.
 */
class ChatViewModel(
    private val contactId: String
) : ViewModel() {

    val uiState: StateFlow<ChatUiState> = combine(
        ChatRepository.conversations,
        ChatRepository.typingContactIds
    ) { conversations, typingContactIds ->
        buildUiState(conversations, typingContactIds)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildUiState(ChatRepository.conversations.value, ChatRepository.typingContactIds.value)
    )

    /** Ignores blank input rather than letting an empty bubble through. */
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        ChatRepository.sendMessage(contactId, trimmed)
    }

    private fun buildUiState(
        conversations: Map<String, List<Message>>,
        typingContactIds: Set<String>
    ): ChatUiState =
        ChatUiState(
            contact = ChatRepository.contacts.find { it.id == contactId },
            messages = conversations[contactId].orEmpty(),
            isTyping = contactId in typingContactIds
        )

    /** ChatViewModel takes a constructor arg, so it needs its own factory. */
    class Factory(private val contactId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(contactId) as T
        }
    }
}
