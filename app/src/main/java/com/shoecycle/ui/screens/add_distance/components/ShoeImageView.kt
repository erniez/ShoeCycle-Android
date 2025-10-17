package com.shoecycle.ui.screens.add_distance.components

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.shoecycle.R
import com.shoecycle.data.repository.ImageRepository
import com.shoecycle.domain.models.Shoe
import com.shoecycle.ui.components.PhotoSelectionDialog
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private const val SHOE_IMAGE_ASPECT_RATIO = 1.5f

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ShoeImageView(
    shoe: Shoe?,
    imageRepository: ImageRepository,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onImageUpdated: ((String, ByteArray) -> Unit)? = null,
    imageSize: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var displayBitmap by remember(shoe?.imageKey) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(shoe?.imageKey) { mutableStateOf(false) }
    var swipeOffset by remember { mutableStateOf(0f) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Camera permission
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // Load shoe image
    LaunchedEffect(shoe?.imageKey) {
        shoe?.imageKey?.let { key ->
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
                            val (imageKey, thumbnailData) = imageRepository.saveImage(it, uri)
                            displayBitmap = imageRepository.loadImage(imageKey)
                            onImageUpdated?.invoke(imageKey, thumbnailData)
                        }
                    }
                    isLoading = false
                }
            }
        }
    }
    
    // Photo picker launcher - Uses system photo picker (no permission required on Android 13+)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    bitmap?.let {
                        val (imageKey, thumbnailData) = imageRepository.saveImage(it, uri)
                        displayBitmap = imageRepository.loadImage(imageKey)
                        onImageUpdated?.invoke(imageKey, thumbnailData)
                    }
                }
                isLoading = false
            }
        }
    }
    
    Card(
        modifier = modifier
            .width(imageSize * SHOE_IMAGE_ASPECT_RATIO)
            .height(imageSize)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (abs(swipeOffset) > 50) {
                            if (swipeOffset < 0) {
                                onSwipeUp()
                            } else {
                                onSwipeDown()
                            }
                        }
                        swipeOffset = 0f
                    }
                ) { _, dragAmount ->
                    swipeOffset += dragAmount
                }
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                }
                displayBitmap != null -> {
                    Image(
                        bitmap = displayBitmap!!.asImageBitmap(),
                        contentDescription = shoe?.displayName ?: "Shoe image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                shoe?.thumbnailData != null -> {
                    val thumbnailBitmap = remember(shoe.thumbnailData) {
                        imageRepository.createThumbnailFromBytes(shoe.thumbnailData)
                    }
                    thumbnailBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = shoe.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                shoe != null -> {
                    // Placeholder for shoe without image - same as ShoeImage component
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showPhotoDialog = true }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add photo",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap to add photo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    // No shoe selected
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(8.dp)
                            .alpha(0.5f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "No shoe",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No shoe",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Photo selection dialog
    if (showPhotoDialog && shoe != null) {
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
                    showPhotoDialog = false
                    cameraLauncher.launch(imageUri)
                } else {
                    cameraPermissionState.launchPermissionRequest()
                }
            },
            onSelectFromGallery = {
                showPhotoDialog = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
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