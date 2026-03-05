package com.studio.plantspot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.studio.plantspot.data.local.UserPreferences
import com.studio.plantspot.ui.main.MainScreen
import com.studio.plantspot.ui.theme.PlantSpotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 로컬 UUID 초기화 (OAuth 미구현 임시 처리)
        UserPreferences.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            PlantSpotTheme {
                MainScreen()
            }
        }
    }
}
