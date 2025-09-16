package com.example.gallery_a2_159336_21005190

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.collection.LruCache
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.gallery_a2_159336_21005190.ui.theme.Gallery_A2_159336_21005190Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val viewModel: GalleryViewModel by viewModels()

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
        viewModel.loadImages(contentResolver)
        if(viewModel.photos.value.isEmpty()){
            Text("No Images Found")
        } else{
            GalleryGrid(modifier = modifier, photosData = viewModel.photos.value)
        }
    }

    @Composable
    fun GalleryGrid(
        modifier: Modifier = Modifier,
        photosData: List<PhotoData>,
        onImageClick: (Long) -> Unit = {}
    ) {
        val minCols = 1
        val maxCols = 3

        var cols by rememberSaveable { mutableIntStateOf(2) }
        var zoom by remember { mutableFloatStateOf(1f) }
        var visualZoom by remember { mutableFloatStateOf(1f) }

        val animatedZoom by animateFloatAsState(
            targetValue = visualZoom,
            animationSpec = SpringSpec(
                stiffness = Spring.StiffnessMedium,
                dampingRatio = Spring.DampingRatioNoBouncy
            ),
            label = "gridZoom"
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedZoom
                    scaleY = animatedZoom
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val zoomChange = event.calculateZoom()
                            if (zoomChange != 1f) {
                                zoom *= zoomChange

                                visualZoom = (visualZoom * zoomChange).coerceIn(0.8f, 1.20f)

                                // Pinch out
                                if (zoom >= 1.5f && cols > minCols) {
                                    cols = (cols - 1).coerceAtLeast(minCols)
                                    zoom = 1f
                                    visualZoom = 1f
                                }
                                // Pinch in
                                if (zoom <= 0.7f && cols < maxCols) {
                                    cols = (cols + 1).coerceAtMost(maxCols)
                                    zoom = 1f
                                    visualZoom = 1f
                                }

                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        // Reset
                        zoom = 1f
                        visualZoom = 1f
                    }
                }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = photosData,
                    key = { it.id }
                ) { photo ->
                    Box(
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = null, fadeOutSpec = null, placementSpec = spring(
                                    stiffness = Spring.StiffnessMedium,
                                    dampingRatio = Spring.DampingRatioNoBouncy
                                )
                            )
                            .padding(0.dp)
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clickable { onImageClick(photo.id) }
                    ) {
                        Thumbnail(imageData = photo)
                    }
                }
            }
        }
    }

    @Composable
    fun Thumbnail(imageData: PhotoData) {
        val context = LocalContext.current

        val bmpState = remember(imageData.id) { mutableStateOf<Bitmap?>(null) }
        var fromCache by remember(imageData.id){ mutableStateOf(false) }

        val targetW = 240
        val targetH = 180
        val cacheKey = remember(key1 = imageData.id, key2 = imageData.orientation){
            viewModel.cacheKey(imageData.id, targetW, targetH, imageData.orientation)
        }
        // Decode off the main thread
        LaunchedEffect(imageData.id) {
            bmpState.value = withContext(Dispatchers.IO) {

                // Try get bitmap from cache first
                viewModel.getFromCache(cacheKey)?.let{
                    fromCache = true
                    return@withContext it
                }

                val uri = uriForImageId(imageData.id)
                val bounds = decodeBounds(context.contentResolver, uri)
                val sample = calculateInSampleSize(bounds, targetW, targetH)
                val decoded = decodeWithSampleSize(context.contentResolver, uri, sample)

                val orientation = imageData.orientation.toFloat()
                val result = decoded?.let { bitmap ->
                    if (orientation != 0f) {
                        val matrix = Matrix().apply { postRotate(orientation) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    } else bitmap
                }

                // put processed bitmap into cache
                result?.let{bmp -> viewModel.putInCache(cacheKey, bmp)}

                return@withContext result
            }
        }

        // launch animations when bitmap is loaded
        var visible by remember(imageData.id) { mutableStateOf(false) }
        LaunchedEffect(bmpState.value) {
            if (bmpState.value != null) {
                if (fromCache){
                    Log.d("Gallery", "Loading ${imageData.id} from cache")
                    delay((imageData.id % 16) * 40L)
                }

                visible = true
            }
        }

        // Animations
        // animate scale
        val scale =
            if(!fromCache)
                animateFloatAsState(
                    targetValue = if (visible) 1f else 0.1f,
                    animationSpec = tween(900, easing = LinearOutSlowInEasing),
                    label = "thumb-scale").value
            else 1f

        // animate alpha
        val alpha =
            if(!fromCache)
                animateFloatAsState(
                    targetValue = if(visible) 1f else 0.1f,
                    animationSpec = tween(900),
                    label = "fade-in"
                ).value
            else 1f

        // animate greyscale
        val saturation by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(900),
            label = "thumb-saturation"
        )

        val bmp = bmpState.value
        bmp?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToSaturation(saturation) }
                ),
                alpha = alpha
            )
        }
    }

}

