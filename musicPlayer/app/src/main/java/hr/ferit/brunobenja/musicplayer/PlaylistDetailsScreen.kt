package hr.ferit.brunobenja.musicplayer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Edit
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
import hr.ferit.brunobenja.musicplayer.ui.theme.FavColor
import hr.ferit.brunobenja.musicplayer.ui.theme.Gray1
import hr.ferit.brunobenja.musicplayer.ui.theme.ToggleColor
import hr.ferit.brunobenja.musicplayer.ui.theme.White1
import hr.ferit.brunobenja.musicplayer.ui.theme.White2
import hr.ferit.brunobenja.musicplayer.ui.theme.White3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsScreen(
    navController: NavHostController,
    playlist: Playlist,
    vm: PlayerViewModel,
    onBack: () -> Unit
) {
    var showPlaylistMenu by remember { mutableStateOf(false) }
    var showTrackMenuFor by remember { mutableStateOf<AudioFile?>(null) }
    var shuffleOn by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showEmptyFavouritesDialog by remember { mutableStateOf(false) }
    var showRenameDialogFor by remember { mutableStateOf<Playlist?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var showDeleteDialogFor by remember { mutableStateOf<Playlist?>(null) }

    val tracks = playlist.songs

    // CUSTOM COVER SYNC
    var customCoverUri by remember { mutableStateOf(playlist.customCoverUri) }
    val context = LocalContext.current
    var coverBitmap = remember(customCoverUri) {
        customCoverUri?.let { path ->
            try {
                BitmapFactory.decodeFile(path)
            } catch (e: Exception) {
                null
            }
        }
    }

    // CUSTOM COVER
    LaunchedEffect(customCoverUri) {
        coverBitmap = customCoverUri?.let { uriString ->
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(uriString)
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } catch (e: Exception) {
                    null
                }
            }
        } ?: playlist.coverArt
    }
    //COVER FROM GALLERY
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val filename = "playlist_cover_${playlist.name.hashCode()}.jpg"
            val copiedPath = copyUriToInternalStorage(context, uri, filename)
            if (copiedPath != null) {
                playlist.customCoverUri = copiedPath
                customCoverUri = copiedPath
                vm.savePlaylists()
            }
        }
    }

    //DURATION CALC
    val totalDurationSeconds = remember(tracks) {
        tracks.sumOf { getAudioDurationSeconds(it.filePath) }
    }
    val totalDurationFormatted = String.format("%d:%02d", totalDurationSeconds / 60, totalDurationSeconds % 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )
    {
        //TOP BAR
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 0.dp)
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                playlist.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                color=White2
            )
            Box {
                IconButton(onClick = { showPlaylistMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Playlist Options")
                }
                DropdownMenu(
                    expanded = showPlaylistMenu,
                    onDismissRequest = { showPlaylistMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Reorder", color = White2) },
                        onClick = {
                            showPlaylistMenu = false
                            showEditDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.reorder),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename", color = White2) },
                        onClick = {
                            showPlaylistMenu = false
                            showRenameDialogFor = playlist
                            renameValue = playlist.name
                        },
                        leadingIcon = { Icon(
                            Icons.Default.Edit,
                            contentDescription = null) }
                    )
                    if (playlist.name.equals("Favourites", ignoreCase = true)) {
                        DropdownMenuItem(
                            text = { Text("Empty", color = White2) },
                            onClick = {
                                showPlaylistMenu = false
                                showEmptyFavouritesDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Delete", color = White2) },
                            onClick = {
                                showPlaylistMenu = false
                                showDeleteDialogFor = playlist
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        }
        // Playlist Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Playlist image (clickable for gallery selection)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePickerLauncher.launch("image/*") }
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            // Playlist Name
            Column {
                Text(
                    playlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    color=White2
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Playlist • ${tracks.size} Tracks • $totalDurationFormatted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Playlist Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { shuffleOn = !shuffleOn }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.shuffle),
                    contentDescription = "Shuffle",
                    tint = if (shuffleOn) ToggleColor else White2,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(8.dp))

            val isThisPlaylistPlaying = vm.isPlaying && vm.currentPlaylist?.name == playlist.name

            IconButton(
                onClick = {
                    if (isThisPlaylistPlaying) {
                        vm.playPause()
                    } else if (tracks.isNotEmpty()) {
                        val playList = if (shuffleOn) playlist.copy(songs = mutableStateListOf<AudioFile>().apply { addAll(tracks.shuffled()) }) else playlist
                        vm.playSongFromPlaylist(playList, 0)
                    }
                }
            ) {
                if (isThisPlaylistPlaying) {
                    Icon(painter=painterResource(R.drawable.pause),
                        contentDescription = "Pause",
                        tint = White2,
                        modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        painter=painterResource(R.drawable.play),
                        contentDescription = "Play",
                        tint = White2,
                        modifier = Modifier.size(24.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Track List
        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(tracks) { track ->
                // Inline track item (not using TrackListItemWithMenu)
                var localShowMenu by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val index = tracks.indexOf(track)
                            vm.playSongFromPlaylist(playlist, index)
                        }
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.Center)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = White2
                        )
                        Text(
                            track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            color=White3
                        )
                        val duration = getAudioDurationFormatted(track.filePath)
                        Text(
                            duration,
                            style = MaterialTheme.typography.bodySmall,
                            color = White3,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { vm.toggleFavouriteFromList(track) }) {
                        Icon(
                            if (vm.favouriteIds.contains(track.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (vm.favouriteIds.contains(track.id)) FavColor else White2
                        )
                    }
                    Box {
                        IconButton(onClick = { localShowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Track Options")
                        }
                        DropdownMenu(
                            expanded = localShowMenu,
                            onDismissRequest = { localShowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Remove from playlist",color=White2) },
                                onClick = {
                                    playlist.songs.remove(track)
                                    vm.savePlaylists()
                                    localShowMenu = false
                                },

                            )
                        }
                    }
                }
                Divider()
            }
        }
        // RENAME DIALOG
        if (showRenameDialogFor != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialogFor = null },
                title = { Text("Rename Playlist", color = White2) },
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
                                if (renameValue.isNotBlank() && vm.playlists.none { it.name == renameValue }) {
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
                    ) { Text("Rename", color = White2) }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showRenameDialogFor = null },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,
                            contentColor = White1
                        )
                    ) { Text("Cancel", color = White2) }
                }
            )
        }
        // DELETE DIALOG
        if (showDeleteDialogFor != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialogFor = null },
                title = { Text("Delete", color = White2) },
                text = { Text("Are you sure you want to delete this playlist?", color = White2) },
                confirmButton = {
                    Button(
                        onClick = {
                            vm.playlists.remove(showDeleteDialogFor)
                            vm.savePlaylists()
                            showDeleteDialogFor = null
                            navController.popBackStack("playlists", inclusive = false)
                        }
                    ) { Text("Delete", color = White2) }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteDialogFor = null }
                    ) { Text("Cancel", color = White2) }
                }
            )
        }
        // EDIT and FAVOURITES DIALOGS unchanged
        if (showEditDialog) {
            val editSongs = remember { mutableStateListOf<AudioFile>().apply { addAll(playlist.songs) } }
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Order",color=White2) },
                text = {
                    Column {
                        if (editSongs.isEmpty()) {
                            Text("No songs in this playlist.",color=White2)
                        } else {
                            editSongs.forEachIndexed { i, song ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (i > 0) {
                                                editSongs.add(i - 1, editSongs.removeAt(i))
                                            }
                                        },
                                        enabled = i != 0
                                    ) {
                                        Icon(
                                            painter = painterResource(id = android.R.drawable.arrow_up_float),
                                            contentDescription = "Move up",
                                            tint =White2
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (i < editSongs.lastIndex) {
                                                editSongs.add(i + 1, editSongs.removeAt(i))
                                            }
                                        },
                                        enabled = i != editSongs.lastIndex
                                    ) {
                                        Icon(
                                            painter = painterResource(id = android.R.drawable.arrow_down_float),
                                            contentDescription = "Move down",
                                            tint=White2
                                        )
                                    }
                                    Text(
                                        song.title,
                                        modifier = Modifier.weight(1f),
                                        color=White2
                                    )
                                    IconButton(
                                        onClick = {
                                            if (i in editSongs.indices) {
                                                editSongs.removeAt(i)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                            contentDescription = "Remove",
                                            tint=White2
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            playlist.songs.clear()
                            playlist.songs.addAll(editSongs)
                            vm.savePlaylists()
                            showEditDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,
                            contentColor = White1
                        )
                    ) { Text("Save") }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showEditDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,
                            contentColor = White1
                        )
                    ) { Text("Cancel",color=White2) }
                }
            )
        }
        if (showEmptyFavouritesDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyFavouritesDialog = false },
                title = { Text("Empty Favourites",color=White2) },
                text = { Text("Are you sure you want to remove all songs from Favourites? This will also reset all likes.",color=White2) },
                confirmButton = {
                    Button(onClick = {
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
                        onClick = { showEmptyFavouritesDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,
                            contentColor = White1
                        )
                    )
                    { Text("Cancel") }
                }
            )
        }
    }
}

// Helper: get duration for a single file (in seconds)
fun getAudioDurationSeconds(filePath: String): Int {
    val mmr = MediaMetadataRetriever()
    return try {
        mmr.setDataSource(filePath)
        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()?.div(1000)?.toInt() ?: 0
    } catch (e: Exception) {
        0
    } finally {
        mmr.release()
    }
}

// Helper: get formatted duration (m:ss)
fun getAudioDurationFormatted(filePath: String): String {
    val seconds = getAudioDurationSeconds(filePath)
    return String.format("%d:%02d", seconds / 60, seconds % 60)
}