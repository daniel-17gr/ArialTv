package com.example.arialtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.InkOnSelected
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.RatingGold
import com.example.arialtv.ui.theme.SurfaceSelected

private val HeroPillShape = RoundedCornerShape(50)
private val HeroVerticalFade = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color(0x450F0F0F),
        0.45f to Color(0x7A0F0F0F),
        0.86f to Color(0xEB0F0F0F),
        1.0f to CanvasBlack,
    ),
)
private val HeroHorizontalFade = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.0f to CanvasBlack,
        0.42f to Color(0xCC0F0F0F),
        0.74f to Color(0x280F0F0F),
        1.0f to Color.Transparent,
    ),
)

/**
 * Full-bleed YouTube TV Hero Banner starting right from the top edge of the screen (y = 0).
 * - Uses single lean 640x360 AsyncImage (no Crossfade double-bitmap RAM spike).
 * - Pure YouTube TV focus pills: solid white fill + dark ink on focus, zero outline borders.
 */
@Composable
fun TvHeroBanner(
    item: MediaItem,
    categoryLabel: String,
    isInWatchlist: Boolean,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onNextFeatured: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val backdropUrl = item.effectiveBackdropUrl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(274.dp)
            .background(CanvasBlack),
    ) {
        if (backdropUrl.isNotBlank()) {
            val request = remember(backdropUrl) {
                ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .size(640, 360)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                alpha = 0.78f,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(modifier = Modifier.fillMaxSize().background(HeroVerticalFade))
        Box(modifier = Modifier.fillMaxSize().background(HeroHorizontalFade))

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .widthIn(max = 620.dp)
                .padding(start = 32.dp, end = 24.dp, top = 58.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val metaParts = buildList {
                    add(categoryLabel.uppercase())
                    add(item.mediaTypeLabel)
                    if (item.year.isNotBlank()) add(item.year)
                }
                Text(
                    text = metaParts.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.rating.isNotBlank()) {
                    Text(
                        text = "★ ${item.rating}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RatingGold,
                    )
                }
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineLarge,
                color = PureWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (item.genre.isNotBlank()) {
                Text(
                    text = item.genre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Pure YouTube TV Borderless Action Row
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onPlayClick,
                    shape = ClickableSurfaceDefaults.shape(HeroPillShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color(0x38FFFFFF),
                        contentColor = PureWhite,
                        focusedContainerColor = PureWhite,
                        focusedContentColor = InkOnWhite,
                    ),
                    border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Play", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Surface(
                    onClick = onDetailsClick,
                    shape = ClickableSurfaceDefaults.shape(HeroPillShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color(0x22FFFFFF),
                        contentColor = InkPrimary,
                        focusedContainerColor = PureWhite,
                        focusedContentColor = InkOnWhite,
                    ),
                    border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (item.isSeries) "Episodes" else "Details",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                Surface(
                    onClick = onToggleWatchlist,
                    shape = ClickableSurfaceDefaults.shape(CircleShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isInWatchlist) SurfaceSelected else Color(0x22FFFFFF),
                        contentColor = if (isInWatchlist) InkOnSelected else PureWhite,
                        focusedContainerColor = PureWhite,
                        focusedContentColor = InkOnWhite,
                    ),
                    border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                ) {
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Rounded.Check else Icons.Rounded.Add,
                            contentDescription = if (isInWatchlist) "Remove from My List" else "Add to My List",
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                if (onNextFeatured != null) {
                    Surface(
                        onClick = onNextFeatured,
                        shape = ClickableSurfaceDefaults.shape(CircleShape),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color(0x22FFFFFF),
                            contentColor = InkPrimary,
                            focusedContainerColor = PureWhite,
                            focusedContentColor = InkOnWhite,
                        ),
                        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Next Featured",
                                modifier = Modifier.size(21.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
