package com.shoecycle.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
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
     * Saves an image and returns the image key and thumbnail data.
     * Applies EXIF rotation to thumbnails but saves original display image.
     */
    suspend fun saveImage(bitmap: Bitmap, uri: Uri): Pair<String, ByteArray> = withContext(Dispatchers.IO) {
        val imageKey = UUID.randomUUID().toString()

        // Get EXIF orientation
        val orientation = getExifOrientation(uri)

        // Create display image (original, unrotated)
        val displayBitmap = resizeBitmap(bitmap, DISPLAY_WIDTH, DISPLAY_HEIGHT)
        saveImageToFile(imageKey, displayBitmap)

        // Save EXIF orientation metadata separately
        saveExifOrientation(imageKey, orientation)

        // Cache display image with rotation applied
        val rotatedDisplay = applyExifRotation(displayBitmap, orientation)
        memoryCache.put(imageKey, rotatedDisplay)

        // Create thumbnail with rotation applied
        val thumbnailBitmap = resizeBitmap(bitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
        val rotatedThumbnail = applyExifRotation(thumbnailBitmap, orientation)
        val thumbnailData = bitmapToByteArray(rotatedThumbnail)

        return@withContext Pair(imageKey, thumbnailData)
    }
    
    /**
     * Loads a display image from cache or file, applying EXIF rotation
     */
    suspend fun loadImage(imageKey: String): Bitmap? = withContext(Dispatchers.IO) {
        // Check memory cache first
        memoryCache.get(imageKey)?.let { return@withContext it }

        // Load from file
        val file = File(imageDirectory, "$imageKey.jpg")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                // Apply EXIF rotation from saved metadata
                val orientation = loadExifOrientation(imageKey)
                val rotatedBitmap = applyExifRotation(bitmap, orientation)
                memoryCache.put(imageKey, rotatedBitmap)
                return@withContext rotatedBitmap
            }
        }

        return@withContext null
    }
    
    /**
     * Deletes an image from storage
     */
    suspend fun deleteImage(imageKey: String) = withContext(Dispatchers.IO) {
        // Remove from cache
        memoryCache.remove(imageKey)

        // Delete image file
        val file = File(imageDirectory, "$imageKey.jpg")
        if (file.exists()) {
            file.delete()
        }

        // Delete EXIF metadata file
        val exifFile = File(imageDirectory, "$imageKey.exif")
        if (exifFile.exists()) {
            exifFile.delete()
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

    /**
     * Reads EXIF orientation from a URI
     */
    private fun getExifOrientation(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /**
     * Saves EXIF orientation metadata to a file
     */
    private fun saveExifOrientation(imageKey: String, orientation: Int) {
        val exifFile = File(imageDirectory, "$imageKey.exif")
        exifFile.writeText(orientation.toString())
    }

    /**
     * Loads EXIF orientation metadata from a file
     */
    private fun loadExifOrientation(imageKey: String): Int {
        val exifFile = File(imageDirectory, "$imageKey.exif")
        return if (exifFile.exists()) {
            try {
                exifFile.readText().toInt()
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
        } else {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /**
     * Applies EXIF rotation to a bitmap
     */
    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap // No rotation needed
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}