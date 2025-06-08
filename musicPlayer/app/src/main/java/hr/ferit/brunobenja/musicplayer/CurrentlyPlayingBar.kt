package hr.ferit.brunobenja.musicplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import hr.ferit.brunobenja.musicplayer.ui.theme.FavColor
import hr.ferit.brunobenja.musicplayer.ui.theme.Gray2
import hr.ferit.brunobenja.musicplayer.ui.theme.SecondaryButtonSize
import hr.ferit.brunobenja.musicplayer.ui.theme.White2
import hr.ferit.brunobenja.musicplayer.ui.theme.White3

@Composable
fun CurrentlyPlayingBar(
    vm: PlayerViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val queue = vm.currentQueue
    val index = vm.currentQueueIndex
    val current = queue.getOrNull(index)
    if (current == null) return // Don't show if nothing is playing

    val isPlaying = vm.isPlaying
    val isFavourite by remember(vm.favouriteIds.size, index) {
        derivedStateOf { vm.isCurrentTrackFavourite() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Gray2
            )
            .clickable { navController.navigate("playing") }
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PLAY/PAUSE
            IconButton(
                onClick = {
                    vm.playPause()
                }
            ) {
                Icon(
                    painter = if (vm.isPlaying)
                        painterResource(id = R.drawable.pause)
                    else
                        painterResource(id = R.drawable.play),
                    contentDescription = "Pause/Play",
                    tint = White2,
                    modifier = Modifier.size(SecondaryButtonSize)
                )
            }

            Spacer(Modifier.width(8.dp))

            // TRACK INFO
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (current.title.isNotBlank())
                        current.title
                    else
                        current.filePath.substringAfterLast('/'),
                    style = MaterialTheme.typography.titleMedium,
                    color = White2,
                    maxLines = 1
                )
                Text(
                    text = if (current.artist.isBlank() || current.artist == "<unknown>") "Unknown Artist" else current.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = White3,
                    maxLines = 1
                )
            }

            // FAV BUTTON
            IconButton(
                onClick = {
                    vm.toggleFavourite()
                }
            ) {
                Icon(
                    imageVector =
                        if (isFavourite)
                            Icons.Default.Favorite
                        else
                            Icons.Default.FavoriteBorder,
                    contentDescription = "Favourite",
                    tint = if (isFavourite) FavColor else White2,
                    modifier = Modifier.size(SecondaryButtonSize)
                )
            }
        }
    }
}