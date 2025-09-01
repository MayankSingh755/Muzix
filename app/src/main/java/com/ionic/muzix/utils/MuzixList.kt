package com.ionic.muzix.utils

import android.annotation.SuppressLint
import android.content.ContentUris
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.ionic.muzix.R
import com.ionic.muzix.data.model.Muzix
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource

@Composable
fun MuzixList(
    muzix: List<Muzix>,
    onMuzixClick: (music: List<Muzix>, position: Int) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = ""
) {

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(muzix) { index, music ->
            MuzixItem(
                muzix = music,
                onClick = { onMuzixClick(muzix, index) },
                searchQuery = searchQuery
            )
        }
    }
}

@Composable
private fun MuzixItem(
    muzix: Muzix,
    onClick: () -> Unit,
    searchQuery: String = "",
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0x00FFFFFF)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            val albumArtUri = ContentUris.withAppendedId(
               stringResource(R.string.album_art_uri).toUri(),
                muzix.albumId
            )

            AsyncImage(
                model = albumArtUri,
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.baseline_music_note_24),
                placeholder = painterResource(R.drawable.baseline_music_note_24)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee()
            ) {
                Text(
                    text = getHighlightedText(muzix.title ?: "Unknown", searchQuery),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = getHighlightedText(muzix.artist, searchQuery),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                muzix.title?.let { album ->
                    if (album != muzix.title && album.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            modifier = Modifier.basicMarquee(),
                            text = getHighlightedText(album, searchQuery),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getHighlightedText(
    text: String,
    searchQuery: String
): AnnotatedString {
    if (searchQuery.isBlank() || text.isBlank()) {
        return AnnotatedString(text)
    }

    val lowercaseText = text.lowercase()
    val lowercaseQuery = searchQuery.lowercase().trim()
    val startIndex = lowercaseText.indexOf(lowercaseQuery)

    return if (startIndex >= 0) {
        buildAnnotatedString {
            append(text.substring(0, startIndex))

            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                )
            ) {
                append(text.substring(startIndex, startIndex + lowercaseQuery.length))
            }

            append(text.substring(startIndex + lowercaseQuery.length))
        }
    } else {
        AnnotatedString(text)
    }
}
