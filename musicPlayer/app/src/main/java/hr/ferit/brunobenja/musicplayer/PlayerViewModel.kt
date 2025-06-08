package hr.ferit.brunobenja.musicplayer

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
// --- Playlist fix for stateful songs ---
import androidx.compose.runtime.mutableStateListOf

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val context get() = getApplication<Application>().applicationContext
    val audioFiles = mutableStateListOf<AudioFile>()
    var currentIndex by mutableStateOf(0) // Used only if not in playlist

    // --- Playback state ---
    var isPlaying by mutableStateOf(false)
    var progress by mutableStateOf(0f)
    var duration by mutableStateOf(1f)
    var coverArt by mutableStateOf<Bitmap?>(null)
    var player: ExoPlayer? = null

    // --- Persistence ---
    private val prefs: SharedPreferences = app.getSharedPreferences("playlists_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val LAST_INDEX_KEY = "last_played_index"
    private val LAST_POSITION_KEY = "last_played_position"
    private val LAST_PLAYLIST_KEY = "last_playlist_name"
    private val LAST_PLAYLIST_INDEX_KEY = "last_playlist_index"

    // --- Playlists ---
    var playlists = mutableStateListOf<Playlist>()
        private set
    // --- Favourites (reactive) ---
    var favouriteIds = mutableStateListOf<String>()
        private set

    // --- Queue logic for playlist or all songs ---
    var currentPlaylist: Playlist? by mutableStateOf(null)
    var currentPlaylistIndex by mutableStateOf(0)

    val currentQueue: List<AudioFile>
        get() = currentPlaylist?.songs ?: audioFiles.sortedBy { it.filePath }
    val currentQueueIndex: Int
        get() = currentPlaylist?.let { currentPlaylistIndex }
            ?: currentQueue.indexOfFirst { it.id == audioFiles.getOrNull(currentIndex)?.id }
    // --- All songs as convenience, for likes reset ---
    val allSongs: List<AudioFile>
        get() = audioFiles

    fun resetAllLikesAndEmptyFavourites() {
        // Remove all favourite ids
        favouriteIds.clear()
        // Remove all songs from Favourites playlist if it exists
        playlists.find { it.name.equals("Favourites", ignoreCase = true) }?.songs?.clear()
        saveFavourites()
        savePlaylists()
    }

    // --- INIT ---
    init {
        loadPlaylists()
        loadFavourites()
        loadAudioFiles()
        restoreLastPlayedOrFirst()
    }

    // --- Persistence ---
    private fun saveLastPlayed() {
        if (currentPlaylist != null) {
            prefs.edit()
                .putString(LAST_PLAYLIST_KEY, currentPlaylist?.name)
                .putInt(LAST_PLAYLIST_INDEX_KEY, currentPlaylistIndex)
                .apply()
        } else {
            prefs.edit()
                .putString(LAST_PLAYLIST_KEY, "")
                .putInt(LAST_INDEX_KEY, currentIndex)
                .apply()
        }
        val pos = player?.currentPosition ?: 0L
        prefs.edit().putLong(LAST_POSITION_KEY, pos).apply()
    }

    private fun loadLastPlayed() {
        val playlistName = prefs.getString(LAST_PLAYLIST_KEY, "") ?: ""
        if (playlistName.isNotEmpty()) {
            currentPlaylist = playlists.find { it.name == playlistName }
            currentPlaylistIndex = prefs.getInt(LAST_PLAYLIST_INDEX_KEY, 0)
            currentIndex = 0 // Not used in playlist mode
        } else {
            currentPlaylist = null
            currentPlaylistIndex = 0
            currentIndex = prefs.getInt(LAST_INDEX_KEY, 0)
        }
    }

    // --- Playlist storage ---
    fun savePlaylists() {
        val json = gson.toJson(playlists)
        prefs.edit().putString("playlists", json).apply()
        saveFavourites() // Always keep favourites in sync!
    }
    fun loadPlaylists() {
        val json = prefs.getString("playlists", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Playlist>>() {}.type
            val loaded: MutableList<Playlist> = gson.fromJson(json, type)
            playlists.clear()
            playlists.addAll(loaded.map { it.toStateful() }) // convert to stateful!
        }
    }

    // --- Favourites storage (ids only for reactivity) ---
    private fun saveFavourites() {
        prefs.edit().putString("favourite_ids", gson.toJson(favouriteIds)).apply()
    }
    private fun loadFavourites() {
        val json = prefs.getString("favourite_ids", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<String>>() {}.type
            val loaded: MutableList<String> = gson.fromJson(json, type)
            favouriteIds.clear()
            favouriteIds.addAll(loaded)
        } else {
            // If first run, populate from playlist if any
            playlists.find { it.name == "Favourites" }?.let { fav ->
                favouriteIds.clear()
                favouriteIds.addAll(fav.songs.map { it.id })
            }
        }
    }

    // --- Music loading ---
    fun loadAudioFiles() {
        audioFiles.clear()
        audioFiles.addAll(MusicRepository.getAllAudioFiles(context))
    }

    // --- Restore ---
    fun restoreLastPlayedOrFirst() {
        loadLastPlayed()
        val queue = currentQueue
        val idx = currentQueueIndex.takeIf { it in queue.indices } ?: 0
        preparePlayer(idx, resumeLastPosition = true)
    }

    // --- Playback ---
    fun preparePlayer(index: Int, resumeLastPosition: Boolean = false) {
        player?.release()
        player = ExoPlayer.Builder(context).build()
        val queue = currentQueue
        val file = queue.getOrNull(index) ?: return
        player?.setMediaItem(MediaItem.fromUri(file.filePath))
        player?.prepare()
        player?.addListener(PlayerEventListener(this))
        updateCoverArt(file.filePath)
        duration = player?.duration?.toFloat() ?: 1f

        if (currentPlaylist != null) {
            currentPlaylistIndex = index
        } else {
            currentIndex = audioFiles.indexOfFirst { it.id == file.id }
        }

        saveLastPlayed()
        if (resumeLastPosition) {
            val lastPos = prefs.getLong(LAST_POSITION_KEY, 0L)
            if (lastPos > 0) {
                player?.seekTo(lastPos)
            }
        }
    }

    // --- Start playback from playlist ---
    fun playSongFromPlaylist(playlist: Playlist, indexInPlaylist: Int) {
        currentPlaylist = playlist
        currentPlaylistIndex = indexInPlaylist
        preparePlayer(indexInPlaylist)
        player?.play()
        isPlaying = true
    }

    // --- Start playback from all songs ---
    fun playSongFromAllSongs(indexInAudioFiles: Int) {
        currentPlaylist = null
        currentIndex = indexInAudioFiles
        preparePlayer(currentQueue.indexOfFirst { it.id == audioFiles.getOrNull(indexInAudioFiles)?.id })
    }

    // --- Controls ---
    fun playPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
            } else {
                it.play()
                isPlaying = true
            }
        }
    }

    fun next() {
        val queue = currentQueue
        val idx = currentQueueIndex
        if (queue.isNotEmpty()) {
            val wasPlaying = isPlaying
            clearRepeat()
            val nextIndex = if (idx + 1 < queue.size) idx + 1 else 0
            preparePlayer(nextIndex)
            if (wasPlaying) player?.play() else player?.pause()
            isPlaying = wasPlaying
        }
    }

    fun prev() {
        val queue = currentQueue
        val idx = currentQueueIndex
        if (queue.isNotEmpty()) {
            val wasPlaying = isPlaying
            val prevIndex = if (idx - 1 < 0) queue.lastIndex else idx - 1
            preparePlayer(prevIndex)
            if (wasPlaying) player?.play() else player?.pause()
            isPlaying = wasPlaying
        }
    }

    fun seekTo(position: Float) {
        player?.seekTo((position * (player?.duration ?: 1)).toLong())
    }

    // --- Repeat ---
    enum class RepeatMode { OFF, ONCE, FOREVER }
    var repeatMode by mutableStateOf(RepeatMode.OFF)
        private set
    fun toggleRepeat() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ONCE
            RepeatMode.ONCE -> RepeatMode.FOREVER
            RepeatMode.FOREVER -> RepeatMode.OFF
        }
    }
    fun clearRepeat() {
        repeatMode = RepeatMode.OFF
    }
    fun onTrackEnded() {
        when (repeatMode) {
            RepeatMode.ONCE -> {
                player?.seekTo(0)
                player?.play()
                repeatMode = RepeatMode.OFF
            }
            RepeatMode.FOREVER -> {
                player?.seekTo(0)
                player?.play()
            }
            RepeatMode.OFF -> {
                next()
            }
        }
    }

    // --- Favourites ---
    fun getFavouritesPlaylist(): Playlist {
        // Always reflect the actual favouriteIds!
        val favSongs = audioFiles.filter { favouriteIds.contains(it.id) }
        val fav = playlists.find { it.name == "Favourites" }
        return if (fav != null) {
            fav.songs.clear()
            fav.songs.addAll(favSongs)
            fav
        } else {
            val newFav = Playlist(name = "Favourites", songs = mutableStateListOf<AudioFile>().apply { addAll(favSongs) })
            playlists.add(0, newFav)
            newFav
        }
    }

    fun isCurrentTrackFavourite(): Boolean {
        val file = currentQueue.getOrNull(currentQueueIndex) ?: return false
        return favouriteIds.contains(file.id)
    }

    fun toggleFavourite() {
        val file = currentQueue.getOrNull(currentQueueIndex) ?: return
        if (favouriteIds.contains(file.id)) {
            favouriteIds.remove(file.id)
        } else {
            favouriteIds.add(file.id)
        }
        // Keep Favourites playlist in sync
        getFavouritesPlaylist()
        saveFavourites()
        savePlaylists()
    }

    // --- Playlists add ---
    fun addSongToPlaylist(song: AudioFile, playlist: Playlist) {
        val index = playlists.indexOfFirst { it.name == playlist.name }
        if (index != -1) {
            if (!playlists[index].songs.any { it.id == song.id }) {
                playlists[index].songs.add(song)
                if (playlist.name == "Favourites" && !favouriteIds.contains(song.id)) {
                    favouriteIds.add(song.id)
                    saveFavourites()
                }
                savePlaylists()
            }
        }
    }
    fun addSongToPlaylist(song: AudioFile, playlistName: String) {
        val index = playlists.indexOfFirst { it.name == playlistName }
        if (index != -1) {
            if (!playlists[index].songs.any { it.id == song.id }) {
                playlists[index].songs.add(song)
                if (playlistName == "Favourites" && !favouriteIds.contains(song.id)) {
                    favouriteIds.add(song.id)
                    saveFavourites()
                }
                savePlaylists()
            }
        } else {
            playlists.add(Playlist(name = playlistName, songs = mutableStateListOf(song)))
            if (playlistName == "Favourites" && !favouriteIds.contains(song.id)) {
                favouriteIds.add(song.id)
                saveFavourites()
            }
            savePlaylists()
        }
    }
    fun toggleFavouriteFromList(track: AudioFile) {
        if (favouriteIds.contains(track.id)) {
            favouriteIds.remove(track.id)
        } else {
            favouriteIds.add(track.id)
        }
        // Keep Favourites playlist in sync
        getFavouritesPlaylist()
        saveFavourites()
        savePlaylists()
    }
    val currentFile: AudioFile?
        get() = currentPlaylist?.songs?.getOrNull(currentPlaylistIndex)
            ?: audioFiles.getOrNull(currentIndex)

    // --- Cover art ---
    private fun updateCoverArt(filePath: String) {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(filePath)
            val art = mmr.embeddedPicture
            coverArt = art?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (e: Exception) {
            coverArt = null
        }
        mmr.release()
    }

    override fun onCleared() {
        saveLastPlayed()
        player?.release()
        super.onCleared()
    }
}


fun Playlist.toStateful(): Playlist {
    // Convert songs to mutableStateListOf for Compose reactivity
    return this.copy(songs = mutableStateListOf<AudioFile>().apply { addAll(songs) })
}