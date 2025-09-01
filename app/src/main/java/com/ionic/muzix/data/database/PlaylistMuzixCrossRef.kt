package com.ionic.muzix.data.database

import androidx.room.Entity

@Entity(primaryKeys = ["playlistId", "muzixId"])
data class PlaylistMuzixCrossRef(
    val playlistId: Long,
    val muzixId: Long,
    val position: Int
)