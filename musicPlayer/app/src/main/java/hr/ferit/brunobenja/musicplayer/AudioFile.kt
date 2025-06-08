package hr.ferit.brunobenja.musicplayer

import java.io.File

data class AudioFile(
    val id: String,
    val title: String,
    val artist: String,
    val filePath: String,
) {
    val fileName: String
        get() = File(filePath).name
}