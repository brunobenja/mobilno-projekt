package hr.ferit.brunobenja.musicplayer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

data class Playlist(
    var name: String,
    val songs: SnapshotStateList<AudioFile> = mutableStateListOf(),
    var coverArt: android.graphics.Bitmap? = null,  // auto/embedded, not persisted
    var customCoverUri: String? = null             // selected by user, persisted as string
)