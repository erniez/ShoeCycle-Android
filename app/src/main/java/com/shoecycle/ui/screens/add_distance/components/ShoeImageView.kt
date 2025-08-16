package com.shoecycle.ui.screens.add_distance.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shoecycle.R
import com.shoecycle.data.repository.ImageRepository
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ShoeImageView(
    shoe: Shoe?,
    imageRepository: ImageRepository,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    imageSize: Dp,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var displayBitmap by remember(shoe?.imageKey) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(shoe?.imageKey) { mutableStateOf(false) }
    var swipeOffset by remember { mutableStateOf(0f) }
    
    // Load shoe image
    LaunchedEffect(shoe?.imageKey) {
        shoe?.imageKey?.let { key ->
            isLoading = true
            displayBitmap = imageRepository.loadImage(key)
            isLoading = false
        }
    }
    
    Card(
        modifier = modifier
            .size(imageSize)
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
                    // Placeholder for shoe without image
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Shoe placeholder",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = shoe.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 2
                        )
                    }
                }
                else -> {
                    // No shoe selected
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "No shoe",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}