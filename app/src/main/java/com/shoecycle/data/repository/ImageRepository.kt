package com.shoecycle.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
class ImageRepository(
    private val context: Context
) {
    companion object {
        const val IMAGE_DIRECTORY = "shoe_images"
        const val THUMBNAIL_WIDTH = 143
        const val THUMBNAIL_HEIGHT = 96
        const val DISPLAY_WIDTH = 210
        const val DISPLAY_HEIGHT = 140
        const val JPEG_QUALITY = 50 // 0.5 compression
        
        // Memory cache size: 1/8 of available memory
        private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        private val cacheSize = maxMemory / 8
    }
    
    // LRU cache for display images
    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }
    
    private val imageDirectory: File by lazy {
        File(context.filesDir, IMAGE_DIRECTORY).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Saves an image and returns the image key and thumbnail data
     */
    suspend fun saveImage(bitmap: Bitmap): Pair<String, ByteArray> = withContext(Dispatchers.IO) {
        val imageKey = UUID.randomUUID().toString()
        
        // Create display image
        val displayBitmap = resizeBitmap(bitmap, DISPLAY_WIDTH, DISPLAY_HEIGHT)
        saveImageToFile(imageKey, displayBitmap)
        
        // Cache display image
        memoryCache.put(imageKey, displayBitmap)
        
        // Create thumbnail
        val thumbnailBitmap = resizeBitmap(bitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
        val thumbnailData = bitmapToByteArray(thumbnailBitmap)
        
        return@withContext Pair(imageKey, thumbnailData)
    }
    
    /**
     * Loads a display image from cache or file
     */
    suspend fun loadImage(imageKey: String): Bitmap? = withContext(Dispatchers.IO) {
        // Check memory cache first
        memoryCache.get(imageKey)?.let { return@withContext it }
        
        // Load from file
        val file = File(imageDirectory, "$imageKey.jpg")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            bitmap?.let { memoryCache.put(imageKey, it) }
            return@withContext bitmap
        }
        
        return@withContext null
    }
    
    /**
     * Deletes an image from storage
     */
    suspend fun deleteImage(imageKey: String) = withContext(Dispatchers.IO) {
        // Remove from cache
        memoryCache.remove(imageKey)
        
        // Delete file
        val file = File(imageDirectory, "$imageKey.jpg")
        if (file.exists()) {
            file.delete()
        }
    }
    
    /**
     * Creates a thumbnail from raw byte data
     */
    fun createThumbnailFromBytes(data: ByteArray): Bitmap? {
        return BitmapFactory.decodeByteArray(data, 0, data.size)
    }
    
    private fun resizeBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
    
    private fun saveImageToFile(imageKey: String, bitmap: Bitmap) {
        val file = File(imageDirectory, "$imageKey.jpg")
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        }
    }
    
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        return outputStream.toByteArray()
    }
}