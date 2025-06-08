package hr.ferit.brunobenja.musicplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SongOptionsDialog(
    show: Boolean,
    songTitle: String?,
    onDismiss: () -> Unit,
    onAddToFavourites: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(songTitle ?: "Song Options") },
            text = {
                Column {
                    Text(
                        "Add to Favourites",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddToFavourites(); onDismiss() }
                            .padding(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Add to Playlist",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddToPlaylist(); onDismiss() }
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}