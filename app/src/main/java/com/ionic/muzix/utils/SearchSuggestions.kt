package com.ionic.muzix.utils

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ionic.muzix.R

@Composable
fun SearchSuggestions(
    suggestions: List<String>,
    recentSearches: List<String>,
    onSuggestionClick: (String) -> Unit,
    onClearRecentSearches: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty() && recentSearches.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 12.dp,
            bottomEnd = 12.dp
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        LazyColumn(
            modifier = Modifier.heightIn(max = 300.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Recent searches
            if (recentSearches.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = onClearRecentSearches,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                items(recentSearches) { search ->
                    SearchSuggestionItem(
                        text = search,
                        icon = painterResource(R.drawable.outline_music_history_24),
                        onClick = { onSuggestionClick(search) }
                    )
                }

                if (suggestions.isNotEmpty()) {
                    item {
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }

            // Suggestions
            if (suggestions.isNotEmpty()) {
                if (recentSearches.isNotEmpty()) {
                    item {
                        Text(
                            text = "Suggestions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                items(suggestions) { suggestion ->
                    SearchSuggestionItem(
                        text = suggestion,
                        icon = painterResource(R.drawable.outline_search_24),
                        onClick = { onSuggestionClick(suggestion) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestionItem(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}