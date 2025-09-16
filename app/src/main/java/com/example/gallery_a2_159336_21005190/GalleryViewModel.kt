package com.example.gallery_a2_159336_21005190

import android.content.ContentResolver
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import androidx.collection.LruCache
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryViewModel : ViewModel(){

    lateinit var memoryCache: LruCache<String, Bitmap>

    var photos = mutableStateOf<List<PhotoData>>(emptyList())
        private set

    init {
        initCache()
    }

    // cache functions
    private fun initCache(){
        // initialize cache
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        memoryCache = object  : LruCache<String, Bitmap>(cacheSize){
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
    }

    fun cacheKey(id: Long, w: Int, h: Int, orientation: Int): String = "$id-${w}x$h-rot$orientation"

    fun loadImages(contentResolver: ContentResolver){
        viewModelScope.launch(Dispatchers.IO){
            photos.value = getImagesData(contentResolver)
            println("Found ${photos.value.size} images")
        }
    }

    private fun getImagesData(contentResolver: ContentResolver): List<PhotoData> {
        val photoData = mutableListOf<PhotoData>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val orientationColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            while (cursor.moveToNext()) {
                photoData.add(
                    PhotoData(
                        id = cursor.getLong(idColumn),
                        orientation = cursor.getInt(orientationColumn),
                        width = cursor.getInt(widthColumn),
                        height = cursor.getInt(heightColumn)
                    )
                )
            }
        }

        return photoData
    }

    fun getFromCache(key: String): Bitmap? {
        return memoryCache.get(key)
    }

    fun putInCache(key: String, bmp: Bitmap) {
        memoryCache.put(key, bmp)
    }

    fun refresh(contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryCache.evictAll()
            val newPhotos = getImagesData(contentResolver)
            withContext(Dispatchers.Main){
                photos.value = newPhotos
            }
        }
    }
}