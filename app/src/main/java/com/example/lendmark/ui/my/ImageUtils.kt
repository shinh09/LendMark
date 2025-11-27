package com.example.lendmark.ui.my

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
