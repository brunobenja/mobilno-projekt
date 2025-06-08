package hr.ferit.brunobenja.musicplayer

// Import the MediaUtils function
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import hr.ferit.brunobenja.musicplayer.ui.theme.FavColor
import hr.ferit.brunobenja.musicplayer.ui.theme.Gray1
import hr.ferit.brunobenja.musicplayer.ui.theme.Gray2
import hr.ferit.brunobenja.musicplayer.ui.theme.Gray3
import hr.ferit.brunobenja.musicplayer.ui.theme.OnceColor
import hr.ferit.brunobenja.musicplayer.ui.theme.PrimaryButtonSize
import hr.ferit.brunobenja.musicplayer.ui.theme.ToggleColor
import hr.ferit.brunobenja.musicplayer.ui.theme.White1
import hr.ferit.brunobenja.musicplayer.ui.theme.White2
import hr.ferit.brunobenja.musicplayer.ui.theme.White3
import hr.ferit.brunobenja.musicplayer.ui.theme.SecondaryButtonSize
import kotlinx.coroutines.delay
import androidx.compose.material3.*

@Composable
fun PlayingScreen(
    navController: NavHostController,
    vm: PlayerViewModel
) {
    val file by remember { derivedStateOf { vm.currentFile } }

    val playlists = vm.playlists
    // Update: isFavourite is now reactive on the actual audio file, not just index
    val isFavourite by remember(vm.currentQueueIndex, vm.favouriteIds.size) {
        derivedStateOf { vm.isCurrentTrackFavourite() }
    }
    // Playlist dialog state
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Automatsko ažuriranje vremena
    LaunchedEffect(vm.isPlaying) {
        while (vm.isPlaying) {
            vm.player?.let {
                vm.progress = it.currentPosition.toFloat()
                vm.duration = it.duration.takeIf { d -> d > 0 }?.toFloat() ?: 1f
            }
            delay(200)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            file?.let {
                // + PLAYLIST BUTTON
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { showPlaylistDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(SecondaryButtonSize),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.plus),
                            contentDescription = "Add to Playlist",
                            tint = White2
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                // COVER ART - EMBEDDED IF AVAILABLE
                val coverArt by produceState<android.graphics.Bitmap?>(null, it.filePath) {
                    value = getEmbeddedCoverArt(it.filePath)
                }
                if (coverArt != null) {
                    Image(
                        bitmap = coverArt!!.asImageBitmap(),
                        contentDescription = "Cover Art",
                        modifier = Modifier
                            .size(256.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.default_artwork),
                        contentDescription = "Default Cover Art",
                        modifier = Modifier
                            .size(256.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                // TITLE (MARQUEE)
                Text(
                    it.title,
                    style = MaterialTheme.typography.h6,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(),
                    color = White1
                )
                // ARTIST NAME / <unknown> ako je prazan tag
                val artistName =
                    if (it.artist == "<unknown>" || it.artist.isBlank()) "Unknown Artist" else it.artist
                Text(artistName, style = MaterialTheme.typography.subtitle1, color = White2)

                Spacer(modifier = Modifier.height(32.dp))

                // Vrijeme iznad progress bara
                val elapsed = formatTime(vm.progress)
                val remaining = formatTime((vm.duration - vm.progress).coerceAtLeast(0f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(elapsed, style = MaterialTheme.typography.caption, color = White3)
                    Text(remaining, style = MaterialTheme.typography.caption, color = White3)
                }
                //PROGRESS BAR
                Slider(
                    value = (vm.progress / vm.duration).coerceIn(0f, 1f),
                    onValueChange = { vm.seekTo(it) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = White1,            //boja kruzica
                        activeTrackColor = White3,   //Prosli dio pjesme
                        inactiveTrackColor = White2     //Buduci dio pjesme
                    )
                )
                Spacer(modifier = Modifier.height(32.dp))

                // CONTROLS
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.height(1.dp))
                    // REPEAT
                    IconButton(
                        onClick = { vm.toggleRepeat() },
                        modifier=Modifier.size(SecondaryButtonSize)
                    ) {
                        val color = when (vm.repeatMode) {
                            PlayerViewModel.RepeatMode.OFF -> White3
                            PlayerViewModel.RepeatMode.ONCE -> OnceColor
                            PlayerViewModel.RepeatMode.FOREVER -> ToggleColor
                        }
                        Icon(
                            painter=painterResource(id=R.drawable.repeat),
                            contentDescription = "Repeat",
                            tint = color
                        )
                    }
                    // Previous
                    IconButton(onClick = { vm.prev() }) {
                        Icon(
                            painter=painterResource(id=R.drawable.previous),
                            "Prev",
                            Modifier
                                .size(PrimaryButtonSize),
                            tint = Color.White
                        )
                    }
                    // Play/Pause
                    IconButton(onClick = { vm.playPause() }) {
                        Icon(
                            painter = if (vm.isPlaying)
                                painterResource(id = R.drawable.pause)
                            else
                                painterResource(id = R.drawable.play),
                            contentDescription = "Play/Pause",
                            tint=White1,
                            modifier=Modifier.size(PrimaryButtonSize)
                        )
                    }
                    // Next
                    IconButton(onClick = { vm.next() }) {
                        Icon(
                            painter=painterResource(id=R.drawable.next),
                            "Prev",
                            Modifier
                                .size(PrimaryButtonSize),
                            tint = White1
                        )
                    }
                    // Favourite
                    IconButton(onClick = { vm.toggleFavourite() }) {
                        if (isFavourite) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = "Unfavourite",
                                tint = FavColor,
                                modifier = Modifier.size(SecondaryButtonSize)
                            )
                        } else {
                            Icon(
                                Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favourite",
                                tint = White3,
                                modifier = Modifier.size(SecondaryButtonSize)
                            )
                        }
                    }
                }
            } ?: Text("No song selected")
        }

        // Add to Playlist Dialog
        if (showPlaylistDialog && file != null) {
            AlertDialog(
                onDismissRequest = {
                    showPlaylistDialog = false
                    newPlaylistName = ""
                },
                title = { Text("Add to Playlist", color = White2) },
                text = {
                    Column {
                        if (playlists.isEmpty()) {
                            Text("No playlists found. Enter a name to create a new playlist.", color = White2)
                        } else {
                            Spacer(Modifier.height(8.dp))
                            playlists.forEach { playlist ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .pointerInput(playlist) {
                                            detectTapGestures(
                                                onTap = {
                                                    file?.let { f ->
                                                        vm.addSongToPlaylist(f, playlist)
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
                            label = { Text("New Playlist", color = White2) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            file?.let { f ->
                                if (newPlaylistName.isNotBlank()) {
                                    vm.addSongToPlaylist(f, newPlaylistName)
                                    if (newPlaylistName == "Favourites" && !vm.favouriteIds.contains(f.id)) {
                                        vm.favouriteIds.add(f.id)
                                    }
                                    showPlaylistDialog = false
                                    newPlaylistName = ""
                                }
                            }
                        },
                        colors = androidx.compose.material.ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,
                            contentColor = White1
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
                            backgroundColor = Gray1,
                            contentColor = White1
                        )
                    ) {
                        Text("Cancel")
                    }
                },
                containerColor = Gray2
            )
        }

        // Playlist Delete Dialog
        if (showDeleteDialog && playlistToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    playlistToDelete = null
                },
                title = { Text("Delete Playlist",color=White2) },
                text = { Text("Are you sure you want to delete the playlist \"${playlistToDelete?.name}\"?",color=White2) },

                confirmButton = {
                    Button(onClick = {
                        val nameToDelete = playlistToDelete?.name
                        if (nameToDelete != null && nameToDelete != "Favourites") {
                            val idx = vm.playlists.indexOfFirst { it.name == nameToDelete }
                            if (idx != -1) {
                                vm.playlists.removeAt(idx)
                                vm.savePlaylists()
                            }
                        }
                        showDeleteDialog = false
                        playlistToDelete = null
                    },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Gray1,      // Button bg color
                            contentColor = White1        // Text color
                        )
                    )
                    {
                        Text("Delete",color=White2)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                        showDeleteDialog = false
                        playlistToDelete = null
                    },
                        colors = ButtonDefaults.buttonColors(
                        backgroundColor = Gray1,      // Button bg color
                        contentColor = White1        // Text color
                        )
                    )
                    {
                        Text("Cancel",color=White2)
                    }
                },
                containerColor = Gray2
            )
        }
    }
}

fun formatTime(millis: Float): String {
    val totalSeconds = (millis / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}