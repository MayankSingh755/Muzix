package com.ionic.muzix.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ionic.muzix.data.Playlist
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Insert
    suspend fun insertCrossRef(crossRef: PlaylistMuzixCrossRef)

    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: Long): Playlist

    @Query("SELECT muzixId FROM PlaylistMuzixCrossRef WHERE playlistId = :playlistId ORDER BY position")
    suspend fun getMuzixIdsForPlaylist(playlistId: Long): List<Long>

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM PlaylistMuzixCrossRef WHERE playlistId = :id")
    suspend fun deleteCrossRefsForPlaylist(id: Long)

    @Query("DELETE FROM PlaylistMuzixCrossRef WHERE playlistId = :playlistId AND muzixId = :muzixId")
    suspend fun removeMuzixFromPlaylist(playlistId: Long, muzixId: Long)

    @Query("DELETE FROM PlaylistMuzixCrossRef WHERE playlistId = :playlistId")
    suspend fun deleteAllCrossForPlaylist(playlistId: Long)

    @Transaction
    suspend fun addTrackToPlaylist(playlist: Playlist, muzix: Muzix, position: Int) {
        val updatedPlaylist = playlist.copy(
            coverAlbumId = playlist.coverAlbumId ?: muzix.albumId,
            trackCount = playlist.trackCount + 1
        )
        insertCrossRef(PlaylistMuzixCrossRef(playlist.id, muzix.id, position))
        updatePlaylist(updatedPlaylist)
    }

    @Transaction
    suspend fun deletePlaylistAndCrossRefs(id: Long) {
        deleteCrossRefsForPlaylist(id)
        deletePlaylist(id)
    }

    @Transaction
    suspend fun reorderPlaylist(playlistId: Long, orderedIds: List<Long>) {
        deleteAllCrossForPlaylist(playlistId)
        orderedIds.forEachIndexed { index, muzixId ->
            insertCrossRef(PlaylistMuzixCrossRef(playlistId, muzixId, index))
        }
    }
}
