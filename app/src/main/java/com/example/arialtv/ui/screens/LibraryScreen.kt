package com.example.arialtv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.model.WatchProgress
import com.example.arialtv.ui.components.ContinueWatchingRail
import com.example.arialtv.ui.components.TvMediaCard
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.SignalRed
import com.example.arialtv.ui.theme.SurfaceElevated
import com.example.arialtv.ui.theme.SurfaceTonal

@Composable
fun LibraryScreen(
    watchHistory: List<WatchProgress>,
    watchlist: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onResumeProgress: (WatchProgress) -> Unit,
    onClearHistory: () -> Unit,
    onClearWatchlist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (watchHistory.isEmpty() && watchlist.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CanvasBlack),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SurfaceTonal),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BookmarkBorder,
                        contentDescription = null,
                        tint = InkSecondary,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text("YOUR LIBRARY IS EMPTY", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
                Text(
                    text = "Save films and series with + or start watching to see them here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBlack),
        contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        if (watchHistory.isNotEmpty()) {
            item(key = "lib_history") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "RECENTLY WATCHED",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSecondary,
                        )
                        QuietActionPill(label = "Clear History", onClick = onClearHistory)
                    }
                    ContinueWatchingRail(
                        entries = watchHistory,
                        onEntryClick = onResumeProgress,
                        onEntryFocused = {},
                    )
                }
            }
        }

        if (watchlist.isNotEmpty()) {
            item(key = "lib_watchlist") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "MY LIST  ·  ${watchlist.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkPrimary,
                        )
                        QuietActionPill(label = "Clear List", onClick = onClearWatchlist)
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        itemsIndexed(watchlist, key = { idx, it -> "wl_${idx}_${it.id}" }) { _, item ->
                            TvMediaCard(
                                item = item,
                                onClick = { onItemClick(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuietActionPill(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceTonal,
            contentColor = InkSecondary,
            focusedContainerColor = PureWhite,
            focusedContentColor = InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
