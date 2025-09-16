package com.example.gallery_a2_159336_21005190

import android.content.ContentResolver
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GalleryViewModel : ViewModel(){

    var photos = mutableStateOf<List<PhotoData>>(emptyList())
        private set

    fun loadImages(contentResolver: ContentResolver){
        viewModelScope.launch(Dispatchers.IO){
            photos.value = getImagesData(contentResolver)
            println("Found ${photos.value.size} images")
        }
    }

    fun getImagesData(contentResolver: ContentResolver): List<PhotoData> {
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
}