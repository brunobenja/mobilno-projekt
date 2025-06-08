package hr.ferit.brunobenja.musicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import hr.ferit.brunobenja.musicplayer.ui.theme.MusicPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Shared ViewModel instanca za cijelu app
        val playerViewModel: PlayerViewModel by viewModels()

        setContent {
            MusicPlayerTheme {
                //Proslijedi vm u MusicPlayerApp
                MusicPlayerApp(playerViewModel)
            }
        }
    }
}