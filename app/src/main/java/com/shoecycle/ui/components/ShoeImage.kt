package com.shoecycle.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.shoecycle.data.repository.ImageRepository
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ShoeImage(
    shoe: Shoe,
    imageRepository: ImageRepository,
    onImageUpdated: (String, ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showPhotoDialog by remember { mutableStateOf(false) }
    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Camera permission
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // Load existing image
    LaunchedEffect(shoe.imageKey) {
        shoe.imageKey?.let { key ->
            isLoading = true
            displayBitmap = imageRepository.loadImage(key)
            isLoading = false
        }
    }
    
    // Create file for camera capture
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    // Fix image orientation based on EXIF data
    fun fixImageOrientation(bitmap: Bitmap, uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            val exif = ExifInterface(stream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        }
        return bitmap
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri?.let { uri ->
                scope.launch {
                    isLoading = true
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        bitmap?.let {
                            val correctedBitmap = fixImageOrientation(it, uri)
                            val (imageKey, thumbnailData) = imageRepository.saveImage(correctedBitmap)
                            displayBitmap = correctedBitmap
                            onImageUpdated(imageKey, thumbnailData)
                        }
                    }
                    isLoading = false
                }
            }
        }
    }
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    bitmap?.let {
                        val correctedBitmap = fixImageOrientation(it, uri)
                        val (imageKey, thumbnailData) = imageRepository.saveImage(correctedBitmap)
                        displayBitmap = correctedBitmap
                        onImageUpdated(imageKey, thumbnailData)
                    }
                }
                isLoading = false
            }
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 32.dp)
            .clickable { showPhotoDialog = true },
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp)
                    )
                }
                displayBitmap != null -> {
                    Image(
                        bitmap = displayBitmap!!.asImageBitmap(),
                        contentDescription = "${shoe.displayName} image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                shoe.thumbnailData != null -> {
                    // Show thumbnail if available but display image isn't loaded
                    val thumbnailBitmap = remember(shoe.thumbnailData) {
                        imageRepository.createThumbnailFromBytes(shoe.thumbnailData)
                    }
                    thumbnailBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "${shoe.displayName} thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                else -> {
                    // Placeholder
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add photo",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap to add photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Photo selection dialog
    if (showPhotoDialog) {
        PhotoSelectionDialog(
            onDismiss = { showPhotoDialog = false },
            onTakePhoto = {
                if (cameraPermissionState.status.isGranted) {
                    val photoFile = createImageFile()
                    imageUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
                    showPhotoDialog = false // Dismiss dialog before launching camera
                    cameraLauncher.launch(imageUri)
                } else {
                    // Keep dialog open while requesting permission
                    cameraPermissionState.launchPermissionRequest()
                }
            },
            onSelectFromGallery = {
                showPhotoDialog = false // Dismiss dialog before launching gallery
                galleryLauncher.launch("image/*")
            }
        )
    }
    
    // Handle permission result
    LaunchedEffect(cameraPermissionState.status) {
        if (showPhotoDialog && cameraPermissionState.status.isGranted) {
            // Permission was just granted, automatically launch camera
            val photoFile = createImageFile()
            imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            showPhotoDialog = false
            cameraLauncher.launch(imageUri)
        }
    }
}