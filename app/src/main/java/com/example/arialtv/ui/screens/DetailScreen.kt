package com.example.arialtv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.arialtv.data.model.EpisodeProgress
import com.example.arialtv.data.model.MediaItem
import com.example.arialtv.data.model.SeasonInfo
import com.example.arialtv.data.model.WatchProgress
import com.example.arialtv.data.storage.AppPreferences
import com.example.arialtv.ui.components.TvMediaRail
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.RatingGold
import com.example.arialtv.ui.theme.SignalRed
import com.example.arialtv.ui.theme.StatusGreen
import com.example.arialtv.ui.theme.SurfaceElevated
import com.example.arialtv.ui.theme.SurfaceSelected
import com.example.arialtv.ui.theme.SurfaceTonal
import com.example.arialtv.ui.viewmodel.DetailUiState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    fallbackItem: MediaItem,
    detailState: DetailUiState,
    watchProgress: WatchProgress?,
    episodeProgress: Map<String, EpisodeProgress>,
    isInWatchlist: Boolean,
    onBack: () -> Unit,
    onToggleWatchlist: (MediaItem) -> Unit,
    onPlay: (MediaItem, Int, Int, Boolean, List<SeasonInfo>, Int?) -> Unit,
    onSwitchDub: (String) -> Unit,
    onSelectSeason: (Int) -> Unit,
    onOpenSimilar: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = detailState.details
    val item = details?.item ?: fallbackItem
    val seasons = details?.seasons.orEmpty()
    val activeSeasonInfo = seasons.firstOrNull { it.seasonNumber == detailState.selectedSeason }
        ?: seasons.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBlack),
    ) {
        // Full-bleed widescreen backdrop melting into CanvasBlack (lean 640x360 for low-RAM TVs)
        if (item.effectiveBackdropUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.effectiveBackdropUrl)
                    .size(640, 360)
                    .crossfade(false)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                alpha = 0.45f,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0x880A0A0A),
                            0.45f to Color(0xCC0A0A0A),
                            0.85f to CanvasBlack,
                            1.0f to CanvasBlack,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xEC0A0A0A),
                            0.55f to Color(0xAA0A0A0A),
                            1.0f to Color(0x440A0A0A),
                        ),
                    ),
                ),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Minimal Back Bar
            item(key = "detail_back") {
                Box(modifier = Modifier.padding(horizontal = 36.dp)) {
                    val backShape = RoundedCornerShape(50)
                    Surface(
                        onClick = onBack,
                        shape = ClickableSurfaceDefaults.shape(backShape),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color(0x30FFFFFF),
                            contentColor = PureWhite,
                            focusedContainerColor = PureWhite,
                            focusedContentColor = InkOnWhite,
                        ),
                        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(7.dp))
                            Text("Back", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Main Poster + Editorial Info + Actions
            item(key = "detail_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Borderless Poster
                    Box(
                        modifier = Modifier
                            .width(156.dp)
                            .height(234.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceTonal),
                    ) {
                        if (item.posterUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.posterUrl)
                                    .size(220, 330)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Nothing dot-matrix metadata strip (zero decorative dots or boxed pills)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val metaTokens = buildList {
                                add(item.mediaTypeLabel)
                                if (item.year.isNotBlank()) add(item.year)
                                val mins = (details?.durationSeconds ?: 0) / 60
                                if (mins > 0) {
                                    val h = mins / 60
                                    val m = mins % 60
                                    add(if (h > 0) "${h}H ${m}M" else "${m}M")
                                }
                                if ((details?.totalSeasons ?: 0) > 0) {
                                    add("${details?.totalSeasons} SEASONS")
                                }
                                if (item.countryName.isNotBlank()) add(item.countryName.uppercase())
                            }
                            Text(
                                text = metaTokens.joinToString("  ·  "),
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
                            style = MaterialTheme.typography.displaySmall,
                            color = PureWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        if (item.genre.isNotBlank()) {
                            Text(
                                text = item.genre,
                                style = MaterialTheme.typography.labelLarge,
                                color = InkSecondary,
                            )
                        }

                        if (item.description.isNotBlank()) {
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = InkPrimary,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        // Primary Actions (safe null handling, zero colored outlines)
                        val progress = watchProgress
                        val hasResume = progress != null && progress.positionMs > 5_000L
                        val nextUnwatched = findNextUnwatchedEpisode(item.id, seasons, episodeProgress)
                        val playSeason = when {
                            hasResume && item.isSeries && progress != null -> progress.season
                            item.isSeries && nextUnwatched != null -> nextUnwatched.first
                            item.isSeries -> activeSeasonInfo?.seasonNumber ?: 1
                            else -> 0
                        }
                        val playEp = when {
                            hasResume && item.isSeries && progress != null -> progress.episode
                            item.isSeries && nextUnwatched != null -> nextUnwatched.second
                            item.isSeries -> 1
                            else -> 0
                        }
                        val primaryLabel = when {
                            hasResume && item.isSeries && progress != null -> "Resume S${progress.season} E${progress.episode}"
                            hasResume && progress != null -> "Resume · ${formatShortDuration(progress.positionMs)}"
                            item.isSeries -> "Play S$playSeason E$playEp"
                            else -> "Play"
                        }

                        FlowRow(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val pillShape = RoundedCornerShape(50)

                            Surface(
                                onClick = { onPlay(item, playSeason, playEp, hasResume, seasons, null) },
                                shape = ClickableSurfaceDefaults.shape(pillShape),
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
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(primaryLabel, style = MaterialTheme.typography.labelLarge)
                                }
                            }

                            if (hasResume) {
                                Surface(
                                    onClick = { onPlay(item, playSeason, playEp, false, seasons, null) },
                                    shape = ClickableSurfaceDefaults.shape(pillShape),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = SurfaceElevated,
                                        contentColor = InkPrimary,
                                        focusedContainerColor = PureWhite,
                                        focusedContentColor = InkOnWhite,
                                    ),
                                    border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Start Over", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }

                            // My List Bookmark Button
                            Surface(
                                onClick = { onToggleWatchlist(item) },
                                shape = ClickableSurfaceDefaults.shape(pillShape),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (isInWatchlist) SurfaceSelected else SurfaceElevated,
                                    contentColor = PureWhite,
                                    focusedContainerColor = PureWhite,
                                    focusedContentColor = InkOnWhite,
                                ),
                                border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = if (isInWatchlist) Icons.Rounded.Check else Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isInWatchlist) "Saved" else "My List",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }

                        // Enhanced Audio Language Switcher (shows active language & all available dubs)
                        val dubs = details?.dubs.orEmpty()
                        if (dubs.isNotEmpty()) {
                            Column(
                                modifier = Modifier.padding(top = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = if (dubs.size > 1) "AUDIO LANGUAGES  ·  ${dubs.size} AVAILABLE" else "AUDIO LANGUAGE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkMuted,
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    itemsIndexed(dubs, key = { idx, d -> "dub_${idx}_${d.subjectId}" }) { _, dub ->
                                        val isCurrent = dub.subjectId == item.id
                                        val dubShape = RoundedCornerShape(50)
                                        Surface(
                                            onClick = { onSwitchDub(dub.subjectId) },
                                            shape = ClickableSurfaceDefaults.shape(dubShape),
                                            colors = ClickableSurfaceDefaults.colors(
                                                containerColor = if (isCurrent) SurfaceSelected else SurfaceTonal,
                                                contentColor = if (isCurrent) PureWhite else InkSecondary,
                                                focusedContainerColor = PureWhite,
                                                focusedContentColor = InkOnWhite,
                                            ),
                                            border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                if (isCurrent) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                }
                                                Text(
                                                    text = dub.languageName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Enhanced Seasons & Episodes Section (with per-episode watched checks & Signal Red progress bars)
            if (seasons.isNotEmpty() && activeSeasonInfo != null) {
                item(key = "detail_seasons") {
                    val watchedInSeason = (1..activeSeasonInfo.episodeCount).count { ep ->
                        episodeProgress[AppPreferences.episodeKey(item.id, activeSeasonInfo.seasonNumber, ep)]?.isWatched == true
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 36.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("Episodes", style = MaterialTheme.typography.titleLarge, color = PureWhite)
                                LazyRow(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    itemsIndexed(seasons, key = { idx, se -> "se_${idx}_${se.seasonNumber}" }) { _, seInfo ->
                                        val selected = seInfo.seasonNumber == activeSeasonInfo.seasonNumber
                                        val seShape = RoundedCornerShape(50)
                                        Surface(
                                            onClick = { onSelectSeason(seInfo.seasonNumber) },
                                            shape = ClickableSurfaceDefaults.shape(seShape),
                                            colors = ClickableSurfaceDefaults.colors(
                                                containerColor = if (selected) SurfaceSelected else SurfaceTonal,
                                                contentColor = if (selected) PureWhite else InkSecondary,
                                                focusedContainerColor = PureWhite,
                                                focusedContentColor = InkOnWhite,
                                            ),
                                            border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "S%02d", seInfo.seasonNumber),
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "$watchedInSeason / ${activeSeasonInfo.episodeCount} WATCHED",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted,
                            )
                        }

                        val bestRes = activeSeasonInfo.availableResolutions.maxOrNull() ?: 1080
                        val bestLabel = if (bestRes >= 2160) "4K" else "${bestRes}p"

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 12.dp),
                        ) {
                            itemsIndexed((1..activeSeasonInfo.episodeCount).toList(), key = { idx, ep -> "s${activeSeasonInfo.seasonNumber}_e${idx}_$ep" }) { _, epNum ->
                                val epProg = episodeProgress[AppPreferences.episodeKey(item.id, activeSeasonInfo.seasonNumber, epNum)]
                                val isWatched = epProg?.isWatched == true
                                val frac = epProg?.progressFraction ?: 0f
                                val epShape = RoundedCornerShape(12.dp)

                                Surface(
                                    onClick = {
                                        onPlay(item, activeSeasonInfo.seasonNumber, epNum, !isWatched && frac > 0f, seasons, null)
                                    },
                                    shape = ClickableSurfaceDefaults.shape(epShape),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = SurfaceTonal,
                                        contentColor = InkPrimary,
                                        focusedContainerColor = PureWhite,
                                        focusedContentColor = InkOnWhite,
                                    ),
                                    border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                                ) {
                                    Box(modifier = Modifier.width(164.dp)) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = String.format(Locale.US, "E%02d", epNum),
                                                    style = MaterialTheme.typography.labelMedium,
                                                )
                                                if (isWatched) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.CheckCircle,
                                                        contentDescription = "Watched",
                                                        tint = StatusGreen,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Rounded.PlayArrow,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(17.dp),
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Episode $epNum",
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                            )
                                            Text(
                                                text = when {
                                                    isWatched -> "Watched"
                                                    frac > 0f -> "${(frac * 100).toInt()}%  ·  $bestLabel"
                                                    else -> bestLabel
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = InkMuted,
                                            )
                                        }

                                        if (frac > 0f && !isWatched) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .align(Alignment.BottomCenter)
                                                    .background(Color(0x33FFFFFF)),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(frac.coerceIn(0.06f, 1f))
                                                        .height(3.dp)
                                                        .background(SignalRed),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Borderless Cast Strip (edge-to-edge LazyRow)
            val staff = details?.staff.orEmpty()
            if (staff.isNotEmpty()) {
                item(key = "detail_cast") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Cast",
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 36.dp),
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            itemsIndexed(staff.take(15), key = { idx, m -> "st_${idx}_${m.id}_${m.name}" }) { _, member ->
                                Column(
                                    modifier = Modifier.width(96.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceTonal),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (member.avatarUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(member.avatarUrl)
                                                    .size(96, 96)
                                                    .crossfade(false)
                                                    .build(),
                                                contentDescription = member.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Person,
                                                contentDescription = null,
                                                tint = InkMuted,
                                                modifier = Modifier.size(26.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (member.character.isNotBlank()) {
                                        Text(
                                            text = member.character,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = InkMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Live "More Like This" Recommendations Rail
            if (detailState.similarItems.isNotEmpty()) {
                item(key = "detail_similar", contentType = "media_rail") {
                    TvMediaRail(
                        title = "More Like This",
                        items = detailState.similarItems,
                        onItemClick = onOpenSimilar,
                        onItemFocused = {},
                    )
                }
            }
        }
    }
}

private fun findNextUnwatchedEpisode(
    subjectId: String,
    seasons: List<SeasonInfo>,
    progressMap: Map<String, EpisodeProgress>,
): Pair<Int, Int>? {
    for (se in seasons) {
        for (ep in 1..se.episodeCount) {
            val prog = progressMap[AppPreferences.episodeKey(subjectId, se.seasonNumber, ep)]
            if (prog == null || !prog.isWatched) {
                return se.seasonNumber to ep
            }
        }
    }
    return null
}

private fun formatShortDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}
