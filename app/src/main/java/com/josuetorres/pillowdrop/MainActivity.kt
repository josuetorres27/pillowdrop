package com.josuetorres.pillowdrop

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.josuetorres.pillowdrop.service.NotificationHelper
import com.josuetorres.pillowdrop.ui.PillowDropApp
import com.josuetorres.pillowdrop.ui.theme.PillowDropTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        NotificationHelper.ensureChannels(this)
        setContent {
            PillowDropTheme {
                PillowDropApp()
            }
        }
    }
}
