package com.example.dogapp.data.local

import android.content.Context
import android.net.Uri
import java.io.File

/** Локальное хранение фото питомца (файл во внутренней памяти + uri в SharedPreferences). */
class DogPhotoStorage(private val context: Context) {
    private val prefs = context.getSharedPreferences("dog_photos", Context.MODE_PRIVATE)

    fun uriMap(): Map<String, String> =
        prefs.all.mapNotNull { (k, v) ->
            if (k != null && v is String) k to v else null
        }.toMap()

    fun savePhotoFromPicker(dogId: String, sourceUri: Uri): Uri? {
        return try {
            val dir = File(context.filesDir, "dog_photos").apply { mkdirs() }
            val dest = File(dir, "$dogId.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            val fileUri = Uri.fromFile(dest)
            prefs.edit().putString(dogId, fileUri.toString()).apply()
            fileUri
        } catch (_: Exception) {
            null
        }
    }

    fun clear(dogId: String) {
        File(File(context.filesDir, "dog_photos"), "$dogId.jpg").delete()
        prefs.edit().remove(dogId).apply()
    }
}
