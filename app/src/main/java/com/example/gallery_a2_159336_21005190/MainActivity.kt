package com.example.gallery_a2_159336_21005190

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                ImagesPermissionGate()
            }
        }
    }

    // Permission Gate - only works for READ_MEDIA_IMAGES
    @Composable
    fun ImagesPermissionGate(){
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
            GalleryScreen()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
    fun GalleryScreen(){
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
            LaunchedEffect(ids) {
                withContext(Dispatchers.IO){
                    //GalleryGrid(ids)
                    val uri = uriForImageId(ids[0])
                    val bounds = decodeBounds(contentResolver, uri)
                    Log.d("Gallery", "uri=$uri")
                    Log.d("Gallery", "mime=${contentResolver.getType(uri)}")
                    Log.d("Gallery", "bounds=${bounds.outWidth} x ${bounds.outHeight}")
                    contentResolver.openFileDescriptor(uri, "r")?.use { Log.d("Gallery", "pfd OK") }
                }

            }

            //Text("uri $uri\nH:${bounds.outHeight} W:${bounds.outWidth}")
        }

    }

    @Composable
    fun GalleryGrid(ids: List<Long>){
       Text("this is a gallery grid with ${ids.size} images")
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

