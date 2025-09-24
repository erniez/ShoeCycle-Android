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
        const val THUMBNAIL_WIDTH = 300
        const val THUMBNAIL_HEIGHT = 200
        const val DISPLAY_WIDTH = 1200
        const val DISPLAY_HEIGHT = 800
        const val JPEG_QUALITY = 85 // Higher quality
        
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
        val sourceWidth = bitmap.width
        val sourceHeight = bitmap.height
        val isLandscape = sourceWidth > sourceHeight

        if (isLandscape) {
            // Landscape: Use aspect fill (center crop)
            val widthScale = targetWidth.toFloat() / sourceWidth
            val heightScale = targetHeight.toFloat() / sourceHeight
            val scale = maxOf(widthScale, heightScale)

            // Calculate the actual dimensions after scaling
            val scaledWidth = (sourceWidth * scale).toInt()
            val scaledHeight = (sourceHeight * scale).toInt()

            // Scale the bitmap maintaining aspect ratio
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

            // Calculate crop position to center the image
            val xOffset = (scaledWidth - targetWidth) / 2
            val yOffset = (scaledHeight - targetHeight) / 2

            // Crop to target dimensions
            return Bitmap.createBitmap(scaledBitmap, xOffset, yOffset, targetWidth, targetHeight)
        } else {
            // Portrait: Use aspect fit (letterbox)
            val widthScale = targetWidth.toFloat() / sourceWidth
            val heightScale = targetHeight.toFloat() / sourceHeight
            val scale = minOf(widthScale, heightScale)

            // Calculate the actual dimensions after scaling
            val scaledWidth = (sourceWidth * scale).toInt()
            val scaledHeight = (sourceHeight * scale).toInt()

            // Scale the bitmap maintaining aspect ratio
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

            // Create a bitmap with the target dimensions and center the scaled image
            val finalBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(finalBitmap)

            // Fill with black background
            canvas.drawColor(android.graphics.Color.BLACK)

            // Calculate position to center the image
            val left = (targetWidth - scaledWidth) / 2f
            val top = (targetHeight - scaledHeight) / 2f

            // Draw the scaled bitmap centered
            canvas.drawBitmap(scaledBitmap, left, top, null)

            return finalBitmap
        }
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