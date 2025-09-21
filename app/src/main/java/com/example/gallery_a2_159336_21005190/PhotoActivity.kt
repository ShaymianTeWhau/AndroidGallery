package com.example.gallery_a2_159336_21005190

import android.content.ContentResolver
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.gallery_a2_159336_21005190.ui.theme.Gallery_A2_159336_21005190Theme

class PhotoActivity : ComponentActivity() {
    private val viewModel: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val photoId = intent.getLongExtra("PHOTO_ID", -1L)

        setContent {
            Gallery_A2_159336_21005190Theme {
                val resolver = contentResolver
                var photo by remember { mutableStateOf<PhotoData?>(null) }

                LaunchedEffect(photoId) {
                    val all = viewModel.getImagesData(resolver)
                    photo = all.find { it.id == photoId }
                }

                photo?.let {
                    PhotoScreen(it, viewModel, resolver)
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
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
        var fromCache by remember(photo.id) { mutableStateOf(false) }

        val targetW = 1080
        val targetH = 1620

        LaunchedEffect(photo.id) {
            val result = viewModel.loadPhoto(photo, targetW, targetH, resolver)
            bmp = result.bitmap
            fromCache = result.fromCache
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (bmp != null) {
                Image(
                    bitmap = bmp!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                CircularProgressIndicator()
            }
        }
    }
}
