package com.example.gallery_a2_159336_21005190

import android.content.ContentResolver
import android.graphics.Bitmap
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

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

fun decodeWithSampleSize(
    contentResolver: ContentResolver,
    uri: Uri,
    inSampleSize: Int
): Bitmap? {
    val opts = BitmapFactory.Options().apply {
        this.inSampleSize = if (inSampleSize > 0) inSampleSize else 1
        this.inJustDecodeBounds = false
    }

    // Open an InputStream and decode
    return contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, opts)
    }
}

