package com.ionic.muzix.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ionic.muzix.screens.MuzixListScreen
import com.ionic.muzix.ui.theme.MuzixTheme
import com.ionic.muzix.data.model.SharedMuzixData
import android.widget.Toast

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuzixTheme {
                MuzixListScreen(
                    onMuzixClick = { musicList, position ->
                        SharedMuzixData.setData(musicList, position)
                        val intent = Intent(this, MuzixPlayerActivity::class.java)
                        intent.putParcelableArrayListExtra("muzixList", ArrayList(musicList))
                        intent.putExtra("initialIndex", position)
                        intent.putExtra("shouldStartPlayback", true)
                        startActivity(intent)
                    },
                    onMiniPlayerExpand = {
                        // Use shared data when expanding from mini player
                        if (!SharedMuzixData.hasData()) {
                            Toast.makeText(this, "No track selected", Toast.LENGTH_SHORT).show()
                            return@MuzixListScreen
                        }
                        val intent = Intent(this, MuzixPlayerActivity::class.java)
                        intent.putExtra("shouldStartPlayback", false)
                        intent.putExtra("fromMiniPlayer", true)
                        intent.putParcelableArrayListExtra("muzixList", ArrayList(SharedMuzixData.muzixList))
                        intent.putExtra("initialIndex", SharedMuzixData.currentIndex)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}