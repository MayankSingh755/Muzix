package com.ionic.muzix.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import android.widget.Toast
import kotlinx.coroutines.launch
import android.content.ContentUris
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.ionic.muzix.R
//import androidx.compose.material.icons.filled.MusicNote
import com.ionic.muzix.data.model.Muzix
import com.ionic.muzix.data.database.MyApplication
import androidx.core.net.toUri
import com.ionic.muzix.data.database.Playlist

@Composable
fun PlaylistAddDialog(
    muzix: Muzix,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val dao = app.database.playlistDao()
    val playlists by dao.getAllPlaylists().collectAsState(initial = emptyList())
    var newPlaylistName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.background(Color.Black),
        title = { Text(stringResource(R.string.save_to_playlist)) },
        text = {
            Column {
                // Create New
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text(stringResource(R.string.new_playlist_name)) },
                        modifier = Modifier.weight(1f)
                    )

                }


                Spacer(Modifier.height(16.dp))

                // Existing Playlists
                if (playlists.isNotEmpty()) {
                    Text(stringResource(R.string.add_to_existing_playlist))
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(playlists.size) { index ->
                            val playlist = playlists[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            val currentIds = dao.getMuzixIdsForPlaylist(playlist.id)
                                            dao.addTrackToPlaylist(playlist, muzix, currentIds.size)
                                            Toast.makeText(
                                                context,
                                                "Added to ${playlist.name}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onDismiss()
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (playlist.coverAlbumId != null) {
                                    AsyncImage(
                                        model = ContentUris.withAppendedId(
                                            stringResource(R.string.album_art_uri).toUri(),
                                            playlist.coverAlbumId
                                        ),
                                        contentDescription = "Cover",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Gray),
                                    )
                                } else {
                                    Icon(painterResource(R.drawable.baseline_music_note_24), contentDescription = "Default", modifier = Modifier.size(40.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(playlist.name)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    stringResource(
                                        R.string.tracksPlayAddDialog,
                                        playlist.trackCount
                                    ))
                            }
                        }
                    }
                }
            }

        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        coroutineScope.launch {
                            val playlist = Playlist(name = newPlaylistName)
                            val id = dao.insertPlaylist(playlist)
                            val insertedPlaylist = dao.getPlaylist(id)
                            dao.addTrackToPlaylist(insertedPlaylist, muzix, 0)
                            Toast.makeText(context, "Created and added to $newPlaylistName", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.create_add),fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold) }
        }
    )
}