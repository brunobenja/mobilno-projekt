package hr.ferit.brunobenja.musicplayer

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import hr.ferit.brunobenja.musicplayer.ui.theme.Gray1
import hr.ferit.brunobenja.musicplayer.ui.theme.Gray2
import hr.ferit.brunobenja.musicplayer.ui.theme.Gray3
import hr.ferit.brunobenja.musicplayer.ui.theme.ToggleColor
import hr.ferit.brunobenja.musicplayer.ui.theme.White1
import hr.ferit.brunobenja.musicplayer.ui.theme.White2
import hr.ferit.brunobenja.musicplayer.ui.theme.White3

@Composable
fun PlaylistsScreen(
    navController: NavHostController,
    vm: PlayerViewModel
) {
    var search by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showEmptyFavouritesDialog by remember { mutableStateOf(false) }
    var showMenuForPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showRenameDialogFor by remember { mutableStateOf<Playlist?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var showDeleteDialogFor by remember { mutableStateOf<Playlist?>(null) }

    val playlists = vm.playlists
    val filteredPlaylists = playlists.filter { it.name.contains(search, ignoreCase = true) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // TITLE
        Text(
            "Playlists",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = White1
        )

        Spacer(modifier = Modifier.height(8.dp))

        // SEARCH + ADD
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = {
                    Text("Search", color = White3, modifier = Modifier.padding(top = 4.dp))
                },
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .fillMaxWidth(),

            )
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Gray2)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Playlist", tint = White2)
            }
        }

        // PLAYLIST LIST
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(filteredPlaylists) { playlist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (vm.currentPlaylist?.name == playlist.name)
                                ToggleColor.copy(alpha = 0.25f)
                            else
                                Color.Transparent
                        )
                        .clickable {
                            navController.navigate("playlistDetails/${playlist.name}")
                        }
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    // CUSTOM COVER ART
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Gray3)
                    ) {
                        val customCoverUri = playlist.customCoverUri
                        val coverBitmap = remember(customCoverUri, playlist.coverArt) {
                            if (customCoverUri != null) {
                                try {
                                    BitmapFactory.decodeFile(customCoverUri)
                                } catch (e: Exception) {
                                    null
                                }
                            } else {
                                playlist.coverArt
                            }
                        }
                        if (coverBitmap != null) {
                            Image(
                                bitmap = coverBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center),
                                tint = White3
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    // Info
                    Column(Modifier.weight(1f)) {
                        Text(
                            playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = White2
                        )

                        Text(
                            "Playlist • ${playlist.songs.size} Tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = White3
                        )
                    }

                    // THREE DOT MENU
                    Box {
                        IconButton(onClick = { showMenuForPlaylist = playlist }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Playlist Options", tint = White2)
                        }
                        DropdownMenu(
                            expanded = showMenuForPlaylist == playlist,
                            onDismissRequest = { showMenuForPlaylist = null }
                        ) {
                            if (!playlist.name.equals("Favourites", ignoreCase = true)) {
                                DropdownMenuItem(
                                    text = { Text("Rename",color = White2) },
                                    onClick = {
                                        showMenuForPlaylist = null
                                        showRenameDialogFor = playlist
                                        renameValue = playlist.name
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                            }
                            if (playlist.name.equals("Favourites", ignoreCase = true)) {
                                DropdownMenuItem(
                                    text = { Text("Empty",color = White2) },
                                    onClick = {
                                        showMenuForPlaylist = null
                                        showEmptyFavouritesDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Delete",color = White2) },
                                    onClick = {
                                        showMenuForPlaylist = null
                                        showDeleteDialogFor = playlist
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
                Divider()
            }
        }

        // EMPTY FAV DIALOG
        if (showEmptyFavouritesDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyFavouritesDialog = false },
                title = { Text("Empty Favourites",color = White2) },
                text = { Text("Are you sure you want to remove all songs from Favourites? This will also reset all likes.",color = White2) },
                confirmButton = {
                    Button(
                        onClick = {
                            vm.resetAllLikesAndEmptyFavourites()
                            showEmptyFavouritesDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,
                            contentColor = White1
                        )
                    ) { Text("Empty") }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showEmptyFavouritesDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,
                            contentColor = White1
                        )
                    ) { Text("Cancel") }
                }
            )
        }

        // CREATE PLAYLIST DIALOG
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create Playlist",color=White2) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Playlist name", color = White3) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = White3,
                            unfocusedBorderColor = White3,
                            cursorColor = White1,
                            focusedTextColor = White1,
                            unfocusedTextColor = White1,
                            disabledTextColor = White3,
                            focusedPlaceholderColor = White3,
                            unfocusedPlaceholderColor = White3
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank() && playlists.none { it.name == newPlaylistName }) {
                                vm.playlists.add(Playlist(newPlaylistName))
                                vm.savePlaylists()
                            }
                            newPlaylistName = ""
                            showCreateDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,
                            contentColor = White1
                        )
                    ) { Text("Create") }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showCreateDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,
                            contentColor = White1
                        )
                    ) { Text("Cancel") }
                }
            )
        }

        // RENAME DIALOG
        if (showRenameDialogFor != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialogFor = null },
                title = { Text("Rename Playlist",color=White2) },
                text = {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = White3,
                            unfocusedBorderColor = White3,
                            cursorColor = White1,
                            focusedTextColor = White1,
                            unfocusedTextColor = White1,
                            disabledTextColor = White3,
                            focusedPlaceholderColor = White3,
                            unfocusedPlaceholderColor = White3
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRenameDialogFor?.let { pl ->
                                if (renameValue.isNotBlank() && playlists.none { it.name == renameValue }) {
                                    pl.name = renameValue
                                    vm.savePlaylists()
                                }
                            }
                            showRenameDialogFor = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,
                            contentColor = White1
                        )
                    ) { Text("Rename",color=White2) }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showRenameDialogFor = null },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,
                            contentColor = White1
                        )
                    ) { Text("Cancel",color=White2) }
                }
            )
        }

        // DELETE CONFIRMATION
        if (showDeleteDialogFor != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialogFor = null },
                title = { Text("Delete",color=White2) },
                text = { Text("Are you sure you want to delete this playlist?",color=White2) },
                confirmButton = {
                    Button(
                        onClick = {
                            vm.playlists.remove(showDeleteDialogFor)
                            vm.savePlaylists()
                            showDeleteDialogFor = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,
                            contentColor = White1
                        )
                    ) { Text("Delete",color=White2) }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteDialogFor = null },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,
                            contentColor = White1
                        )
                    ) { Text("Cancel",color=White2) }
                }
            )
        }
    }
}