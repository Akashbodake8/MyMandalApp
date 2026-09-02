package com.example.mymandalapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class LocalBrandingRepository(context: Context) {
    private val appContext = context.applicationContext
    private val logoFile = File(appContext.filesDir, "mandal_logo_local.png")
    private val stampFile = File(appContext.filesDir, "mandal_stamp_local.png")

    fun saveBranding(uri: Uri, type: String): Boolean {
        val targetFile = if (type == "logo") logoFile else stampFile
        return try {
            val inputStream: InputStream? = appContext.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                if (bitmap != null) {
                    val outputStream = FileOutputStream(targetFile)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                    outputStream.close()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getBrandingBitmap(type: String): Bitmap? {
        val file = if (type == "logo") logoFile else stampFile
        return if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    }

    fun getBrandingPath(type: String): String? {
        val file = if (type == "logo") logoFile else stampFile
        return if (file.exists()) file.absolutePath else null
    }

    fun hasBranding(type: String): Boolean {
        val file = if (type == "logo") logoFile else stampFile
        return file.exists()
    }

    fun deleteBranding(type: String): Boolean {
        val file = if (type == "logo") logoFile else stampFile
        return if (file.exists()) {
            file.delete()
        } else {
            true
        }
    }
}
