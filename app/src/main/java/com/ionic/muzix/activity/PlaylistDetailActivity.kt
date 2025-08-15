package com.ionic.muzix.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ionic.muzix.screens.PlaylistDetailScreen
import com.ionic.muzix.ui.theme.MuzixTheme

class PlaylistDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val playlistId = intent.getLongExtra("playlistId", -1)
        if (playlistId == -1L) {
            finish()
            return
        }

        setContent {
            MuzixTheme {
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    onBack = { finish() },
                    onPlay = { muzixList, position ->
                        val intent = Intent(this, MuzixPlayerActivity::class.java)
                        intent.putParcelableArrayListExtra("muzixList", ArrayList(muzixList))
                        intent.putExtra("initialIndex", position)
                        intent.putExtra("shouldStartPlayback", true)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}