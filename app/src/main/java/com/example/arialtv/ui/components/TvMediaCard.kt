package com.example.arialtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.RatingGold
import com.example.arialtv.ui.theme.SignalRed
import com.example.arialtv.ui.theme.SurfaceElevated
import com.example.arialtv.ui.theme.SurfaceTonal

private val PosterCardShape = RoundedCornerShape(10.dp)
private val PosterBadgeShape = RoundedCornerShape(4.dp)
private val PosterFocusBorder = Border(BorderStroke(2.dp, PureWhite), shape = PosterCardShape)
private val PosterBadgeScrim = Color(0xC20F0F0F)
private val ProgressTrackBg = Color(0x66FFFFFF)

/**
 * High-performance YouTube TV × Nothing OS media card:
 * - Uses direct AsyncImage (zero Subcompose overhead) + exact 280x420 target decode size.
 * - Hoisted shapes/borders eliminate per-frame allocations during fast D-pad rail scrolling.
 * - Zero borders when unfocused; crisp 2.5dp Pure White focus ring + subtle lift on focus.
 * - Signal Red progress indicator along the bottom edge for Continue Watching.
 */
@Composable
fun TvMediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    onFocused: () -> Unit = {},
    modifier: Modifier = Modifier,
    cardWidth: Dp = 128.dp,
    posterHeight: Dp = 186.dp,
    badgeOverride: String? = null,
    progressFraction: Float? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier.width(cardWidth),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .width(cardWidth)
                .height(posterHeight)
                .onFocusChanged { state ->
                    if (isFocused != state.isFocused) {
                        isFocused = state.isFocused
                        if (state.isFocused) onFocused()
                    }
                },
            shape = CardDefaults.shape(shape = PosterCardShape),
            scale = CardDefaults.scale(focusedScale = 1.04f),
            border = CardDefaults.border(
                border = Border.None,
                focusedBorder = PosterFocusBorder,
            ),
            glow = CardDefaults.glow(),
            colors = CardDefaults.colors(containerColor = SurfaceTonal),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceElevated),
            ) {
                if (item.posterUrl.isNotBlank()) {
                    val request = remember(item.posterUrl) {
                        ImageRequest.Builder(context)
                            .data(item.posterUrl)
                            .size(180, 270)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = request,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = InkMuted,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center),
                    )
                }

                // Content-type indicator badge (MOVIE / SERIES / DOC / MUSIC or episode resume tag)
                val topTag = badgeOverride ?: item.contentTypeBadge
                if (topTag.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(7.dp)
                            .clip(PosterBadgeShape)
                            .background(PosterBadgeScrim)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = topTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite,
                        )
                    }
                }

                if (item.rating.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(7.dp)
                            .clip(PosterBadgeShape)
                            .background(PosterBadgeScrim)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = item.rating,
                            style = MaterialTheme.typography.labelSmall,
                            color = RatingGold,
                        )
                    }
                }

                // Signal Red progress line along bottom edge
                if (progressFraction != null && progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .align(Alignment.BottomCenter)
                            .background(ProgressTrackBg),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceIn(0.05f, 1f))
                                .height(3.5.dp)
                                .background(SignalRed),
                        )
                    }
                }
            }
        }

        // Clean title & year beneath artwork
        Column(
            modifier = Modifier.padding(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isFocused) PureWhite else InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.year.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.year,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFocused) InkSecondary else InkMuted,
                    )
                }
            }
        }
    }
}
