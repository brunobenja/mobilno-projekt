package hr.ferit.brunobenja.musicplayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun QueueScreen(navController: NavHostController, vm: PlayerViewModel) {
    val inPlaylist = vm.currentPlaylist != null
    val queue: List<AudioFile>
    val highlightIndex: Int

    if (inPlaylist) {
        val playlist = vm.currentPlaylist!!
        val idx = vm.currentPlaylistIndex
        queue = if (idx in playlist.songs.indices) playlist.songs.subList(idx, playlist.songs.size) else emptyList()
        highlightIndex = 0
    } else {
        val files = vm.audioFiles.sortedBy { it.filePath }
        val current = vm.audioFiles.getOrNull(vm.currentIndex)
        val currentInSortedIdx = files.indexOfFirst { it.id == current?.id }
        val rotated = if (files.isNotEmpty() && currentInSortedIdx != -1) {
            files.drop(currentInSortedIdx) + files.take(currentInSortedIdx)
        } else {
            files
        }
        queue = rotated
        highlightIndex = 0
    }

    // For options dialog/like menu
    var showOptionsDialog by remember { mutableStateOf(false) }
    var heldSong by remember { mutableStateOf<AudioFile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Queue",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = White1
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (queue.isEmpty()) {
            Text(
                "No tracks in queue.",
                modifier = Modifier.padding(8.dp),
                color = White1
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(queue) { index, song ->
                    val isCurrent = index == highlightIndex

                    val coverArt by produceState<android.graphics.Bitmap?>(null, song.filePath) {
                        value = getEmbeddedCoverArt(song.filePath)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isCurrent) ToggleColor.copy(alpha = 0.25f) else Color.Transparent
                            )
                            .pointerInput(song) {
                                detectTapGestures(
                                    onTap = {
                                        val playIndex = if (inPlaylist) {
                                            vm.currentPlaylistIndex + index
                                        } else {
                                            val files = vm.audioFiles.sortedBy { it.filePath }
                                            files.indexOfFirst { it.id == song.id }
                                        }
                                        vm.preparePlayer(playIndex)
                                        navController.navigate("playing")
                                    },
                                    onLongPress = {
                                        heldSong = song
                                        showOptionsDialog = true
                                    }
                                )
                            }
                            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                    ) {
                        // Track cover
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
                        // Track info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                song.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = White2
                            )
                            Text(
                                if (song.artist.isBlank() || song.artist == "<unknown>") "Unknown Artist" else song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = White2,
                                maxLines = 1
                            )
                            val duration = getAudioDurationFormatted(song.filePath)
                            Text(
                                duration,
                                style = MaterialTheme.typography.bodySmall,
                                color = White3,
                                fontSize = 12.sp
                            )
                        }
                        // Like button
                        val isFavourite = vm.favouriteIds.contains(song.id)
                        IconButton(onClick = {
                            vm.toggleFavouriteFromList(song)
                        }) {
                            Icon(
                                if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isFavourite) FavColor else White2
                            )
                        }
                        // Track menu
                        Box {
                            IconButton(onClick = {
                                heldSong = song
                                showOptionsDialog = true
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Track Options", tint = White2)
                            }
                        }
                    }
                    if (index < queue.lastIndex) {
                        Divider()
                    }
                }
            }
        }
    }

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
            // Implement playlist dialog if desired
        }
    )
}