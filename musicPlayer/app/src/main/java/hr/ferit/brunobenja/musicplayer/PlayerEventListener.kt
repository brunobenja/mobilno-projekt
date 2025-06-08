package hr.ferit.brunobenja.musicplayer

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player

class PlayerEventListener(private val viewModel: PlayerViewModel) : Player.Listener {
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        viewModel.isPlaying = isPlaying
    }

    override fun onPlaybackStateChanged(state: Int) {
        val player = viewModel.player
        viewModel.duration = player?.duration?.toFloat() ?: 1f
        viewModel.progress = player?.currentPosition?.toFloat() ?: 0f

        if (state == Player.STATE_ENDED) {
            viewModel.onTrackEnded()
        }
    }
}