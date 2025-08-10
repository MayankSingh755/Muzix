package com.ionic.muzix.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ionic.muzix.screens.MuzixListScreen
import com.ionic.muzix.ui.theme.MuzixTheme
import com.ionic.muzix.data.SharedMuzixData

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuzixTheme {
                MuzixListScreen(
                    onMuzixClick = { music, position ->
                        // Store data in shared singleton
                        SharedMuzixData.setData(music, position)

                        val intent = Intent(this, MuzixPlayerActivity::class.java)
                        intent.putParcelableArrayListExtra("muzixList", ArrayList(music))
                        intent.putExtra("initialIndex", position)
                        intent.putExtra("shouldStartPlayback", true)
                        startActivity(intent)
                    },
                    onMiniPlayerExpand = {
                        // Use shared data when expanding from mini player
                        val intent = Intent(this, MuzixPlayerActivity::class.java)
                        intent.putExtra("shouldStartPlayback", false)
                        intent.putExtra("fromMiniPlayer", true)

                        // Only pass data if we have it stored
                        if (SharedMuzixData.hasData()) {
                            intent.putParcelableArrayListExtra("muzixList", ArrayList(SharedMuzixData.muzixList))
                            intent.putExtra("initialIndex", SharedMuzixData.currentIndex)
                        }

                        startActivity(intent)
                    }
                )
            }
        }
    }
}