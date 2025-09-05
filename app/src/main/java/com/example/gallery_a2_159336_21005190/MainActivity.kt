package com.example.gallery_a2_159336_21005190

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.gallery_a2_159336_21005190.ui.theme.Gallery_A2_159336_21005190Theme

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
            GalleryGrid()
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
    fun GalleryGrid(){
        Text("this is a gallery grid")
    }
}

