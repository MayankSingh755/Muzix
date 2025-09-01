package com.ionic.muzix.utils

import android.content.Context
import android.provider.MediaStore
import com.ionic.muzix.data.Muzix

fun getMuzix(context: Context): List<Muzix> {
    val muzix = mutableListOf<Muzix>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val selection = MediaStore.Audio.Media.IS_MUSIC + "!=0"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.ALBUM_ID
    )

    val cursor = context.contentResolver.query(
        uri,
        projection,
        selection,
        null,
        sortOrder
    )

    cursor?.use {
        val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

        while (it.moveToNext()) {
            val id = it.getLong(idCol)
            val title = it.getString(titleCol)
            val artist = it.getString(artistCol)
            val data = it.getString(dataCol)
            val albumId = it.getLong(albumCol)

            muzix.add(Muzix(id, title, artist, data, albumId))
        }
    }

    return muzix
}


fun getMuzixByIds(context: Context, ids: List<Long>): List<Muzix> {
    if (ids.isEmpty()) return emptyList()

    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.ALBUM_ID
    )
    val selection = "${MediaStore.Audio.Media._ID} IN (${ids.joinToString { "?" }})"
    val selectionArgs = ids.map { it.toString() }.toTypedArray()

    val cursor = context.contentResolver.query(
        collection,
        projection,
        selection,
        selectionArgs,
        null
    )

    val muzixMap = mutableMapOf<Long, Muzix>()
    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            muzixMap[id] = Muzix(
                id = id,
                title = it.getString(titleColumn),
                artist = it.getString(artistColumn) ?: "Unknown Artist",
                data = it.getString(dataColumn),
                albumId = it.getLong(albumIdColumn)
            )
        }
    }
    // Preserve order based on ids
    return ids.mapNotNull { muzixMap[it] }
}