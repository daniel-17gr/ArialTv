package com.example.arialtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.model.WatchProgress
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.SurfaceElevated
import com.example.arialtv.ui.theme.SurfaceTonal
import java.util.Locale

private val RailContentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
private val SeeAllCardShape = RoundedCornerShape(10.dp)
private val SeeAllPillShape = RoundedCornerShape(50)

@Composable
fun TvMediaRail(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onItemFocused: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    indexPrefix: Int? = null,
    previewLimit: Int? = null,
    onExpandCategory: (() -> Unit)? = null,
) {
    val displayedItems = if (previewLimit != null && onExpandCategory != null) {
        items.take(previewLimit)
    } else {
        items
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
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

            if (onExpandCategory != null) {
                Surface(
                    onClick = onExpandCategory,
                    shape = ClickableSurfaceDefaults.shape(SeeAllPillShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = SurfaceTonal,
                        contentColor = InkSecondary,
                        focusedContainerColor = PureWhite,
                        focusedContentColor = InkOnWhite,
                    ),
                    border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Expand $title",
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = RailContentPadding,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(
                items = displayedItems,
                key = { idx, it -> "${title}_${idx}_${it.id}" },
                contentType = { _, _ -> "media_card" },
            ) { _, mediaItem ->
                TvMediaCard(
                    item = mediaItem,
                    onClick = { onItemClick(mediaItem) },
                    onFocused = { onItemFocused(mediaItem) },
                )
            }

            if (onExpandCategory != null) {
                item(key = "${title}_see_all", contentType = "see_all_card") {
                    SeeAllCategoryCard(onClick = onExpandCategory)
                }
            }
        }
    }
}

@Composable
private fun SeeAllCategoryCard(
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(128.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .width(128.dp)
                .height(186.dp),
            shape = CardDefaults.shape(shape = SeeAllCardShape),
            scale = CardDefaults.scale(focusedScale = 1.04f),
            border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
            glow = CardDefaults.glow(),
            colors = CardDefaults.colors(
                containerColor = SurfaceTonal,
                contentColor = InkPrimary,
                focusedContainerColor = PureWhite,
                focusedContentColor = InkOnWhite,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "See All",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "See All",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Explore more",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted,
                )
            }
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

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = RailContentPadding,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(
                items = entries,
                key = { idx, it -> "cw_${idx}_${it.item.id}" },
                contentType = { _, _ -> "cw_card" },
            ) { _, progress ->
                TvMediaCard(
                    item = progress.item,
                    onClick = { onEntryClick(progress) },
                    onFocused = { onEntryFocused(progress) },
                    badgeOverride = progress.episodeTag.ifEmpty { "RESUME" },
                    progressFraction = progress.progressFraction,
                )
            }
        }
    }
}
