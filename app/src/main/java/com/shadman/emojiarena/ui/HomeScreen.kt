package com.shadman.emojiarena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadman.emojiarena.data.Contact
import com.shadman.emojiarena.data.Message
import com.shadman.emojiarena.ui.components.MessageStatusTicks
import com.shadman.emojiarena.ui.theme.AvatarPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onContactClick: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Chats") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
        ) {
            items(uiState.previews, key = { it.contact.id }) { preview ->
                ContactRow(
                    preview = preview,
                    onClick = { onContactClick(preview.contact.id) }
                )
            }
        }
    }
}

@Composable
private fun ContactRow(
    preview: ContactPreview,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(contact = preview.contact)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preview.contact.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(2.dp))
            LastMessagePreview(message = preview.lastMessage)
        }
    }
}

@Composable
private fun AvatarCircle(contact: Contact) {
    val colorIndex = (contact.id.toIntOrNull() ?: 0).mod(AvatarPalette.size)
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(AvatarPalette[colorIndex], CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialsFor(contact.name),
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun initialsFor(name: String): String =
    name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }

@Composable
private fun LastMessagePreview(message: Message?) {
    if (message == null) {
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (message.isFromUser) {
            MessageStatusTicks(status = message.status)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
