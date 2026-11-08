package com.lifeclock.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lifeclock.widget.ui.AniccaApp
import com.lifeclock.widget.widget.WidgetPreviews

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WidgetPreviews.update(applicationContext)
        setContent { AniccaApp() }
    }
}
