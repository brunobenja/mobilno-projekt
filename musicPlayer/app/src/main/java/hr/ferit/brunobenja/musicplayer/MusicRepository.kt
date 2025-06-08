package hr.ferit.brunobenja.musicplayer
import android.content.Context
import android.provider.MediaStore

object MusicRepository {
    fun getAllAudioFiles(context: Context): List<AudioFile> {
        val audioList = mutableListOf<AudioFile>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA
        )
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (it.moveToNext()) {
                val id = it.getLong(idCol).toString()
                val title = it.getString(titleCol)
                val artist = it.getString(artistCol)
                val data = it.getString(dataCol)
                audioList.add(AudioFile(id, title, artist, data))
            }
        }
        return audioList
    }
}