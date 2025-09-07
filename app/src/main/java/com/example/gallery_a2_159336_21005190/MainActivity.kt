package com.example.gallery_a2_159336_21005190

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.gallery_a2_159336_21005190.ui.theme.Gallery_A2_159336_21005190Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Gallery_A2_159336_21005190Theme {
                Scaffold (
                    topBar = {GalleryAppBar()}
                ){ innerPadding ->
                    ImagesPermissionGate(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GalleryAppBar(){
        TopAppBar(
            title = {Text("Gallery")},
            actions = {
                IconButton(onClick = {}){
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
        )
    }

    // Permission Gate - only works for READ_MEDIA_IMAGES
    @Composable
    fun ImagesPermissionGate(modifier: Modifier){
        val context = LocalContext.current
        val permission = Manifest.permission.READ_MEDIA_IMAGES

        // current permission state
        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            )
        }

        // launcher to request the permission
        val requestPermission = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
        }

        // if permission UI
        if (hasPermission) {
            GalleryScreen(modifier = modifier)
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "To show your photos, allow access to images.",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = { requestPermission.launch(permission) }
                ) {
                    Text("Allow images access")
                }
            }
        }
    }

    @Composable
    fun GalleryScreen(modifier: Modifier){
        var ids by remember { mutableStateOf<List<Long>>(emptyList()) }
        LaunchedEffect(Unit){
            withContext(Dispatchers.IO){
                ids = getImageIds(contentResolver)
                println("Found ${ids.size} images, first ID = ${ids.firstOrNull()}")
            }
        }

        if(ids.isEmpty()){
            Text("No Images Found")
        } else{
            GalleryGrid(modifier = modifier, ids = ids)
        }

    }



    @Composable
    fun GalleryGrid(
        modifier: Modifier,
        ids: List<Long>,
        onImageClick: (Long) -> Unit = {}
    ){
       Log.d("Gallery", "this is a gallery grid with ${ids.size} images")
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp) ,
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = ids,
                key = { it } // stable key = imageId
            ) { id ->
                // Keep a 4:3 box since your decode uses 240x180
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clickable { onImageClick(id) }
                ) {
                    Thumbnail(imageId = id)
                }
            }
        }
    }

    @Composable
    fun Thumbnail(imageId: Long) {
        val context = LocalContext.current
        val bmp = remember(imageId) {
            val uri = uriForImageId(imageId)
            val bounds = decodeBounds(context.contentResolver, uri)
            val sample = calculateInSampleSize(bounds, 240, 180)
            decodeWithSampleSize(context.contentResolver, uri, sample)
        }
        bmp?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }

    fun getImageIds(contentResolver: ContentResolver): List<Long> {
        val ids = mutableListOf<Long>()

        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(idColumn))
            }
        }

        return ids
    }
}

