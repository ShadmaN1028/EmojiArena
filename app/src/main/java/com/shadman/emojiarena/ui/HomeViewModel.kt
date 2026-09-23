package com.shadman.emojiarena.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadman.emojiarena.data.ChatRepository
import com.shadman.emojiarena.data.Contact
import com.shadman.emojiarena.data.Message
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One contact paired with the latest message in their thread, for the list row. */
data class ContactPreview(
    val contact: Contact,
    val lastMessage: Message?
)

data class HomeUiState(
    val previews: List<ContactPreview> = emptyList()
)

/**
 * Doesn't hold any state of its own — it just re-shapes ChatRepository's
 * shared [ChatRepository.conversations] into what the home screen wants to
 * render. ChatViewModel (built in the next section) reads the exact same
 * flow, filtered to one contact, which is what keeps the two screens in
 * lockstep without either one owning the data.
 */
class HomeViewModel : ViewModel() {

    val uiState: StateFlow<HomeUiState> = ChatRepository.conversations
        .map { conversations -> buildUiState(conversations) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = buildUiState(ChatRepository.conversations.value)
        )

    private fun buildUiState(conversations: Map<String, List<Message>>): HomeUiState =
        HomeUiState(
            previews = ChatRepository.contacts
                .map { contact ->
                    ContactPreview(
                        contact = contact,
                        lastMessage = conversations[contact.id]?.lastOrNull()
                    )
                }
                // Most recent activity first — a contact you just messaged
                // moves to the top, same as any real messaging app. A
                // contact with no messages at all sinks to the bottom.
                .sortedByDescending { it.lastMessage?.timestamp ?: Long.MIN_VALUE }
        )
}
