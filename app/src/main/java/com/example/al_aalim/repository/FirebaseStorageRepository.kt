package com.example.al_aalim.repository

import android.content.Context
import android.net.Uri
import com.example.al_aalim.config.FirebaseConfig
import com.example.al_aalim.models.Attachment
import com.example.al_aalim.utils.FileUtils
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Repository for Firebase Storage operations
 */
class FirebaseStorageRepository(private val context: Context) {
    
    private val storage: FirebaseStorage = FirebaseStorage.getInstance().apply {
        if (FirebaseConfig.USE_EMULATOR) {
            useEmulator(FirebaseConfig.EMULATOR_HOST, FirebaseConfig.STORAGE_EMULATOR_PORT)
        }
    }
    
    /**
     * Upload file to Firebase Storage
     * @param uri File URI to upload
     * @param onProgress Callback for upload progress (0-100)
     * @return Attachment object with download URL
     */
    suspend fun uploadFile(
        uri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): Result<Attachment> = suspendCoroutine { continuation ->
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid
            
            if (userId == null) {
                continuation.resume(Result.failure(Exception("User not authenticated. Please log in to send photos.")))
                return@suspendCoroutine
            }

            val mimeType = FileUtils.getMimeType(context, uri)
            val fileType = FileUtils.getFileTypeCategory(mimeType)
            val fileName = FileUtils.getFileName(context, uri)
            val fileSize = FileUtils.getFileSize(context, uri)
            
            // Determine storage path based on file type, scoped to user
            val storagePath = when (fileType) {
                "image" -> FirebaseConfig.IMAGES_PATH
                "video" -> FirebaseConfig.VIDEOS_PATH
                else -> FirebaseConfig.DOCUMENTS_PATH
            }
            
            // Generate unique file name — include user UID in path for security rules
            val uniqueFileName = "${System.currentTimeMillis()}_$fileName"
            val storageRef: StorageReference = storage.reference
                .child("users")
                .child(userId)
                .child(storagePath)
                .child(uniqueFileName)
            
            // For images, compress before uploading
            val uploadUri = if (fileType == "image") {
                val compressedFile = FileUtils.compressImage(context, uri)
                compressedFile?.let { Uri.fromFile(it) } ?: uri
            } else {
                uri
            }
            
            // Start upload
            val uploadTask = storageRef.putFile(uploadUri)
            
            // Track progress
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                onProgress?.invoke(progress)
            }
            
            // Handle completion
            uploadTask.addOnSuccessListener {
                // Get download URL
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val attachment = Attachment(
                        type = fileType,
                        url = downloadUri.toString(),
                        fileName = fileName,
                        fileSize = fileSize,
                        mimeType = mimeType,
                        uploadedAt = System.currentTimeMillis()
                    )
                    continuation.resume(Result.success(attachment))
                }.addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
            }.addOnFailureListener { e ->
                android.util.Log.e("StorageRepo", "Upload failed: ${e.message}", e)
                continuation.resume(Result.failure(Exception("Upload failed: ${e.message}")))
            }
            
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }
    
    /**
     * Upload multiple files
     */
    suspend fun uploadMultipleFiles(
        uris: List<Uri>,
        onProgress: ((Int, Int) -> Unit)? = null // (current, total)
    ): Result<List<Attachment>> {
        val attachments = mutableListOf<Attachment>()
        
        uris.forEachIndexed { index, uri ->
            onProgress?.invoke(index + 1, uris.size)
            
            val result = uploadFile(uri)
            if (result.isSuccess) {
                result.getOrNull()?.let { attachments.add(it) }
            } else {
                return Result.failure(result.exceptionOrNull() ?: Exception("Upload failed"))
            }
        }
        
        return Result.success(attachments)
    }
    
    /**
     * Delete file from Firebase Storage
     */
    suspend fun deleteFile(fileUrl: String): Result<Unit> {
        return try {
            val fileRef = storage.getReferenceFromUrl(fileUrl)
            fileRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
