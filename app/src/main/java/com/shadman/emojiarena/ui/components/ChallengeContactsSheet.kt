package com.shadman.emojiarena.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shadman.emojiarena.data.Contact

/**
 * Multi-select contact picker for the game-over screen's "Challenge
 * others" action. Dumb: holds only its own in-progress checkbox
 * selection, and reports the confirmed id list back up — GameViewModel
 * decides what actually happens with them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeContactsSheet(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    // Opens straight to fully expanded — the default partially-expanded
    // peek was cutting the confirm button off below the fold for a list
    // this long, forcing a manual drag just to see it.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(text = "Challenge others", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(contacts, key = { it.id }) { contact ->
                    val checked = contact.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (checked) selectedIds - contact.id else selectedIds + contact.id
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // onCheckedChange is null deliberately: the Row above
                        // already toggles on tap, so a checkbox that also
                        // handled its own tap would fire both on one touch —
                        // toggling twice and net-canceling. Null makes it
                        // purely a state readout, sole ownership stays with
                        // the Row's clickable.
                        Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = contact.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onConfirm(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Challenge (${selectedIds.size})")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
