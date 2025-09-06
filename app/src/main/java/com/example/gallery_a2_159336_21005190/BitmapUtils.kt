package com.example.gallery_a2_159336_21005190

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore

fun uriForImageId(imageId: Long): Uri {
    return Uri.withAppendedPath(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        imageId.toString()
    )
}

fun decodeBounds(contentResolver: ContentResolver, uri: Uri): BitmapFactory.Options {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }
    return options
}

