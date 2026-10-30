package com.imhcode.hargatetangga

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.imhcode.hargatetangga.ui.AppRoot
import com.imhcode.hargatetangga.ui.HargaTetanggaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HargaTetanggaTheme {
                Surface(Modifier.fillMaxSize()) { AppRoot() }
            }
        }
    }
}
