package com.example.al_aalim.config

/**
 * Firebase configuration constants
 */
object FirebaseConfig {
    
    // Firebase Realtime Database paths
    const val MESSAGES_PATH = "messages"
    const val CONVERSATIONS_PATH = "conversations"
    const val USERS_PATH = "users"
    
    // Firebase Storage paths
    const val IMAGES_PATH = "images"
    const val VIDEOS_PATH = "videos"
    const val DOCUMENTS_PATH = "documents"
    
    // File size limits (in bytes)
    const val MAX_IMAGE_SIZE = 10 * 1024 * 1024 // 10 MB
    const val MAX_VIDEO_SIZE = 100 * 1024 * 1024 // 100 MB
    const val MAX_DOCUMENT_SIZE = 20 * 1024 * 1024 // 20 MB
    
    // Image compression settings
    const val IMAGE_COMPRESSION_QUALITY = 75 // 0-100
    const val MAX_IMAGE_WIDTH = 1920
    const val MAX_IMAGE_HEIGHT = 1920
    
    // Development/Testing flags
    const val USE_EMULATOR = false // Set to true to use Firebase Emulators
    const val EMULATOR_HOST = "10.0.2.2" // Android emulator host
    const val DATABASE_EMULATOR_PORT = 9000
    const val STORAGE_EMULATOR_PORT = 9199
}
