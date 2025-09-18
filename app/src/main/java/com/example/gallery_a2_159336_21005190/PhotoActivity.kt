package com.example.gallery_a2_159336_21005190

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

class PhotoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val photoId = intent.getLongExtra("PHOTO_ID", -1L)
        setContent{

            if(photoId != -1L){
                FullImageScreen(photoId)
            } else {
                Text("No photo id provided")
            }
        }
    }

    @Composable
    fun FullImageScreen(photoId: Long){
        Text("Id=$photoId")
    }

}