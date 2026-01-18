package com.example.al_aalim.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.example.al_aalim.config.FirebaseConfig
import java.io.File
import java.io.FileOutputStream

/**
 * Utility functions for file operations
 */
object FileUtils {
    
    /**
     * Get file size from URI
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.available().toLong()
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get MIME type from URI
     */
    fun getMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) 
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                )
            ?: "application/octet-stream"
    }
    
    /**
     * Determine file type category from MIME type
     */
    fun getFileTypeCategory(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("video/") -> "video"
            else -> "document"
        }
    }
    
    /**
     * Compress image before upload
     * @return Compressed image file
     */
    fun compressImage(context: Context, uri: Uri): File? {
        return try {
            val bitmap = if (uri.scheme == "content") {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                BitmapFactory.decodeFile(uri.path)
            }
            
            // Calculate new dimensions maintaining aspect ratio
            val (newWidth, newHeight) = calculateScaledDimensions(
                bitmap.width,
                bitmap.height,
                FirebaseConfig.MAX_IMAGE_WIDTH,
                FirebaseConfig.MAX_IMAGE_HEIGHT
            )
            
            // Scale bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            
            // Save compressed image to cache
            val compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(compressedFile).use { out ->
                scaledBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    FirebaseConfig.IMAGE_COMPRESSION_QUALITY,
                    out
                )
            }
            
            // Clean up
            if (bitmap != scaledBitmap) {
                bitmap.recycle()
            }
            scaledBitmap.recycle()
            
            compressedFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Calculate scaled dimensions maintaining aspect ratio
     */
    private fun calculateScaledDimensions(
        width: Int,
        height: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Pair<Int, Int> {
        if (width <= maxWidth && height <= maxHeight) {
            return Pair(width, height)
        }
        
        val ratio = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )
        
        return Pair(
            (width * ratio).toInt(),
            (height * ratio).toInt()
        )
    }
    
    /**
     * Get file name from URI
     */
    fun getFileName(context: Context, uri: Uri): String {
        var fileName = "file_${System.currentTimeMillis()}"
        
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        
        return fileName
    }
}
