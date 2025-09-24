package com.example.gallery_a2_159336_21005190

import android.content.ContentResolver
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import com.example.gallery_a2_159336_21005190.ui.theme.Gallery_A2_159336_21005190Theme

class PhotoActivity : ComponentActivity() {
    private val viewModel: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get id from intent
        val photoId = intent.getLongExtra("PHOTO_ID", -1L)

        setContent {
            Gallery_A2_159336_21005190Theme {
                val resolver = contentResolver
                var photo by remember { mutableStateOf<PhotoData?>(null) }

                // fetch photo details
                LaunchedEffect(photoId) {
                    val all = viewModel.getImagesData(resolver)
                    photo = all.find { it.id == photoId }
                }

                // show photo
                photo?.let {
                    PhotoScreen(it, viewModel, resolver)
                }
            }
        }
    }

    @Composable
    fun PhotoScreen(
        photo: PhotoData,
        viewModel: GalleryViewModel,
        resolver: ContentResolver
    ) {
        var bmp by remember(photo.id) { mutableStateOf<Bitmap?>(null) }

        // target resolution
        val targetW = 1080
        val targetH = 1620

        // load bitmap
        LaunchedEffect(photo.id) {
            val result = viewModel.loadPhoto(photo, targetW, targetH, resolver)
            bmp = result.bitmap
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // show progress spinner while image is loading
            if (bmp == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@BoxWithConstraints
            }

            // zoom min and max
            val minScale = 1f
            val maxScale = 6f

            // state for zoom and pan
            var scale by rememberSaveable(photo.id) { mutableFloatStateOf(1f) }
            var offsetX by rememberSaveable(photo.id) { mutableFloatStateOf(0f) }
            var offsetY by rememberSaveable(photo.id) { mutableFloatStateOf(0f) }

            fun currentOffset() = Offset(offsetX, offsetY)

            // get container dimensions
            val density = LocalDensity.current
            val containerW = with(density) { maxWidth.toPx() }
            val containerH = with(density) { maxHeight.toPx() }

            val imgW = bmp!!.width.toFloat()
            val imgH = bmp!!.height.toFloat()

            // scale image to fit container
            val containerRatio = containerW / containerH
            val imageRatio = imgW / imgH
            val baseContentW: Float
            val baseContentH: Float
            if (imageRatio > containerRatio) {
                baseContentW = containerW
                baseContentH = containerW / imageRatio
            } else {
                baseContentH = containerH
                baseContentW = containerH * imageRatio
            }

            // clamp offset so image can't be dragged outside bounds
            fun clampOffset(raw: Offset, sc: Float): Offset {
                val scaledW = baseContentW * sc
                val scaledH = baseContentH * sc
                val maxX = ((scaledW - containerW) / 2f).coerceAtLeast(0f)
                val maxY = ((scaledH - containerH) / 2f).coerceAtLeast(0f)
                val x = if (maxX == 0f) 0f else raw.x.coerceIn(-maxX, maxX)
                val y = if (maxY == 0f) 0f else raw.y.coerceIn(-maxY, maxY)
                return Offset(x, y)
            }
            fun setClampedOffset(newOffset: Offset, sc: Float) {
                val clamped = clampOffset(newOffset, sc)
                offsetX = clamped.x
                offsetY = clamped.y
            }

            // handle zoom and pan gestures
            val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)

                val scaledOffset = currentOffset() * (newScale / scale)
                val newOffsetUnclamped = if (newScale > 1f) scaledOffset + panChange else Offset.Zero

                scale = newScale
                setClampedOffset(newOffsetUnclamped, newScale)
            }

            Image(
                bitmap = bmp!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(scale, baseContentW, baseContentH) {
                        // drag gestures if zoomed in
                        if (scale > 1f) {
                            detectDragGestures(
                                onDrag = { change, drag ->
                                    change.consume()
                                    setClampedOffset(currentOffset() + drag, scale)
                                }
                            )
                        }
                    }
                    .transformable(transformableState)
                    .graphicsLayer {
                        clip = true
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}
