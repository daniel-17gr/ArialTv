package com.example.arialtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.model.WatchProgress
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.InkPrimary
import java.util.Locale

@Composable
fun TvMediaRail(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onItemFocused: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    indexPrefix: Int? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (indexPrefix != null) {
                Text(
                    text = String.format(Locale.US, "%02d", indexPrefix),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary,
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    items = items,
                    key = { "${title}_${it.id}" },
                    contentType = { "media_card" },
                ) { mediaItem ->
                    TvMediaCard(
                        item = mediaItem,
                        onClick = { onItemClick(mediaItem) },
                        onFocused = { onItemFocused(mediaItem) },
                    )
                }
            }

            // Subtle right-edge fade so overflowing posters melt smoothly instead of hard-clipping
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(42.dp)
                    .height(248.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, CanvasBlack.copy(alpha = 0.88f)),
                        ),
                    ),
            )
        }
    }
}

@Composable
fun ContinueWatchingRail(
    entries: List<WatchProgress>,
    onEntryClick: (WatchProgress) -> Unit,
    onEntryFocused: (WatchProgress) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "RESUME",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
            )
            Text(
                text = "Continue Watching",
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary,
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    items = entries,
                    key = { "cw_${it.item.id}" },
                    contentType = { "cw_card" },
                ) { progress ->
                    TvMediaCard(
                        item = progress.item,
                        onClick = { onEntryClick(progress) },
                        onFocused = { onEntryFocused(progress) },
                        badgeOverride = progress.episodeTag.ifEmpty { "RESUME" },
                        progressFraction = progress.progressFraction,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(42.dp)
                    .height(248.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, CanvasBlack.copy(alpha = 0.88f)),
                        ),
                    ),
            )
        }
    }
}
