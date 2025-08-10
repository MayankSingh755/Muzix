package com.ionic.muzix.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Muzix(
    val id: Long,
    val title: String?,
    val artist: String,
    val data: String,
    val albumId: Long
) : Parcelable
