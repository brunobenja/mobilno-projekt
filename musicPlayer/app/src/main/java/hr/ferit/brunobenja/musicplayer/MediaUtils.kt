package hr.ferit.brunobenja.musicplayer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.io.OutputStream

fun getEmbeddedCoverArt(filePath: String): Bitmap? {
    val mmr = MediaMetadataRetriever()
    return try {
        mmr.setDataSource(filePath)
        mmr.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    } catch (e: Exception) {
        null
    } finally {
        mmr.release()
    }
}



fun copyUriToInternalStorage(context: Context, uri: Uri, filename: String): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, filename)
        val outputStream: OutputStream = file.outputStream()
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}