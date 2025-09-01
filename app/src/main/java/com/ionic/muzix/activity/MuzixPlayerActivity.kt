package com.ionic.muzix.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ionic.muzix.data.model.Muzix
import com.ionic.muzix.screens.MuzixPlayerScreen
import com.ionic.muzix.ui.theme.MuzixTheme
import com.ionic.muzix.data.model.SharedMuzixData

class MuzixPlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get data from intent first
        var muzixList = intent.getParcelableArrayListExtra<Muzix>("muzixList") ?: emptyList()
        var initialIndex = intent.getIntExtra("initialIndex", 0)
        val shouldStartPlayback = intent.getBooleanExtra("shouldStartPlayback", true)
        val fromMiniPlayer = intent.getBooleanExtra("fromMiniPlayer", false)

        // If coming from mini player and no data in intent, use shared data
        if (fromMiniPlayer && muzixList.isEmpty() && SharedMuzixData.hasData()) {
            muzixList = SharedMuzixData.muzixList
            initialIndex = SharedMuzixData.currentIndex
        }

        setContent {
            MuzixTheme {
                if (muzixList.isNotEmpty()) {
                    MuzixPlayerScreen(
                        muzixList = muzixList,
                        initialIndex = initialIndex,
                        shouldStartPlayback = shouldStartPlayback,
                        onBack = {
                            finish()
                        }
                    )
                } else {
                    finish()
                }
            }
        }
    }
}