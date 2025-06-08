package hr.ferit.brunobenja.musicplayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import hr.ferit.brunobenja.musicplayer.ui.theme.FavColor
import hr.ferit.brunobenja.musicplayer.ui.theme.Gray1
import hr.ferit.brunobenja.musicplayer.ui.theme.Gray3
import hr.ferit.brunobenja.musicplayer.ui.theme.ToggleColor
import hr.ferit.brunobenja.musicplayer.ui.theme.White1
import hr.ferit.brunobenja.musicplayer.ui.theme.White2
import hr.ferit.brunobenja.musicplayer.ui.theme.White3

@Composable
fun SearchScreen(
    navController: NavHostController,
    vm: PlayerViewModel
) {
    var query by remember { mutableStateOf("") }
    val filtered = vm.audioFiles.filter {
        it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
    }
    val file by remember { derivedStateOf { vm.currentFile } }

    // Currently playing id for highlight
    val currentFileId = vm.currentFile?.id

    // State for song options dialog
    var showOptionsDialog by remember { mutableStateOf(false) }
    var heldSong by remember { mutableStateOf<AudioFile?>(null) }

    // State for playlist dialog
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = {
                Text("Search", color = White3, modifier = Modifier.padding(top = 4.dp))
            },
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (filtered.isEmpty()) {
            Text(
                "No results.",
                style = MaterialTheme.typography.bodyMedium,
                color = White3,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            filtered.forEach { file ->
                val isCurrent = file.id == currentFileId

                // COVER ART PER SONG
                val coverArt by produceState<android.graphics.Bitmap?>(null, file.filePath) {
                    value = getEmbeddedCoverArt(file.filePath)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(                // Highlight if current
                            if (isCurrent)
                                ToggleColor.copy(alpha=0.25f)
                            else
                                Color.Transparent
                        )
                        .pointerInput(file) {
                            detectTapGestures(
                                onTap = {
                                    val files = vm.audioFiles.sortedBy { it.filePath }
                                    val sortedIndex = files.indexOfFirst { it.id == file.id }
                                    if (sortedIndex != -1) {
                                        vm.currentPlaylist = null
                                        vm.preparePlayer(sortedIndex)
                                        navController.navigate("playing")
                                    }
                                },

                            )
                        }
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    // COVER
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Gray3)
                    ) {
                        if (coverArt != null) {
                            Image(
                                bitmap = coverArt!!.asImageBitmap(),
                                contentDescription = "Cover Art",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.default_artwork),
                                contentDescription = "Default Cover Art",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    // TRACK INFO
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            file.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = White2
                        )
                        Text(
                            if (file.artist.isBlank() || file.artist == "<unknown>") "Unknown Artist" else file.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = White2,
                            maxLines = 1
                        )
                        // Duration
                        val duration = getAudioDurationFormatted(file.filePath)
                        Text(
                            duration,
                            style = MaterialTheme.typography.bodySmall,
                            color = White3,
                            fontSize = 12.sp
                        )
                    }
                    // FAV BUTTON
                    val isFavourite = vm.favouriteIds.contains(file.id)
                    IconButton(onClick = {
                        vm.toggleFavouriteFromList(file)
                    }) {
                        Icon(
                            if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isFavourite) FavColor else White2
                        )
                    }
                    // MENU
                    Box {
                        IconButton(onClick = {
                            heldSong = file
                            showOptionsDialog = true
                        }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Track Options")
                        }
                    }
                }
                Divider()
            }
        }
    }

    // THREE DOT DIALOG
    SongOptionsDialog(
        show = showOptionsDialog,
        songTitle = heldSong?.title?.takeIf { it.isNotBlank() } ?: heldSong?.filePath?.substringAfterLast('/') ?: "Song Options",
        onDismiss = { showOptionsDialog = false },
        onAddToFavourites = {
            heldSong?.let { song ->
                vm.toggleFavouriteFromList(song)
            }
        },
        onAddToPlaylist = {
            showOptionsDialog = false
            showPlaylistDialog = true
        }
    )

    //PLAYLIST DIALOG
    if (showPlaylistDialog && heldSong != null) {
        AlertDialog(
                onDismissRequest = {
                    showPlaylistDialog = false
                    newPlaylistName = ""
                },
                title = { Text("Add to Playlist",color=White2) },
                text = {
                    Column {
                        if (vm.playlists.isEmpty()) {
                            Text("No playlists found. Enter a name to create a new playlist.",color=White2)
                        } else {
                            Spacer(Modifier.height(8.dp))
                            vm.playlists.forEach { playlist ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .pointerInput(playlist) {
                                            detectTapGestures(
                                                onTap = {
                                                    file?.let { f ->
                                                        vm.addSongToPlaylist(f, playlist)
                                                        // auto update FAV icon
                                                        if (playlist.name == "Favourites" && !vm.favouriteIds.contains(f.id)) {
                                                            vm.favouriteIds.add(f.id)
                                                        }
                                                        showPlaylistDialog = false
                                                        newPlaylistName = ""
                                                    }
                                                },
                                                onLongPress = {
                                                    if (playlist.name != "Favourites") {
                                                        playlistToDelete = playlist
                                                        showDeleteDialog = true
                                                    }
                                                }
                                            )
                                        }
                                        .background(White2, androidx.compose.material.MaterialTheme.shapes.small)
                                        .padding(12.dp)
                                ) {
                                    Text(playlist.name, color = Gray3)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("New Playlist",color=White2) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            vm.addSongToPlaylist(heldSong!!, newPlaylistName)
                            showPlaylistDialog = false
                            newPlaylistName = ""
                        }
                    },
                    colors = androidx.compose.material.ButtonDefaults.buttonColors(
                        backgroundColor = Gray1,      // Button bg color
                        contentColor = White1        // Text color
                    )
                ) {
                    Text("Create & Add")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showPlaylistDialog = false
                        newPlaylistName = ""
                    },
                    colors = androidx.compose.material.ButtonDefaults.buttonColors(
                        backgroundColor = Gray1,      // Button bg color
                        contentColor = White1        // Text color
                    )
                ) {
                    Text("Cancel")
                }
            },

        )
    }
}