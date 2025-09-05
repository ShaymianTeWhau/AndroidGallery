package com.example.gallery_a2_159336_21005190

import android.net.Uri
import android.provider.MediaStore

fun uriForImageId(imageId: Long): Uri {
    return Uri.withAppendedPath(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        imageId.toString()
    )
}