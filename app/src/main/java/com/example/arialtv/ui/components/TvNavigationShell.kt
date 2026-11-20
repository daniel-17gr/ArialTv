package com.example.arialtv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.arialtv.R
import com.example.arialtv.data.model.AppMetadata
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.DotMatrixFont
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.RailSurface
import com.example.arialtv.ui.theme.SelectedBorderRed
import com.example.arialtv.ui.theme.SignalRed
import com.example.arialtv.ui.theme.SurfaceSelected
import com.example.arialtv.ui.theme.SurfaceTonal
import com.example.arialtv.ui.viewmodel.TvNavTab
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adaptive YouTube TV × Nothing OS Navigation Shell:
 * - On TV / Wide screens (>= 680dp): Collapsible Left Navigation Rail + subtle top header
 *   backed by the user's banner at low opacity fading smoothly into CanvasBlack.
 * - On Compact screens (< 680dp): Banner-backed Top Header + Bottom Bar.
 * - Zero redundant tab titles, zero decorative red dots, zero profile clutter.
 */
@Composable
fun TvNavigationShell(
    activeTab: TvNavTab,
    sleepTimerRemainingSec: Int,
    onSelectTab: (TvNavTab) -> Unit,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBlack),
    ) {
        val isWideTv = maxWidth >= 680.dp

        if (isWideTv) {
            var railFocused by remember { mutableStateOf(false) }
            val railWidth by animateDpAsState(
                targetValue = if (railFocused) 212.dp else 74.dp,
                animationSpec = tween(durationMillis = 180),
                label = "railWidth",
            )

            Row(modifier = Modifier.fillMaxSize()) {
                // Left Collapsible YouTube TV Rail
                Column(
                    modifier = Modifier
                        .width(railWidth)
                        .fillMaxHeight()
                        .background(RailSurface)
                        .onFocusChanged { railFocused = it.hasFocus }
                        .padding(vertical = 20.dp, horizontal = 12.dp),
                    horizontalAlignment = if (railFocused) Alignment.Start else Alignment.CenterHorizontally,
                ) {
                    // Brand Logo (compact `tv.` when collapsed, full `arial tv.` when expanded)
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .padding(horizontal = if (railFocused) 8.dp else 0.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (railFocused) {
                            Image(
                                painter = painterResource(id = R.drawable.arial_tv_logo_dark),
                                contentDescription = "arial tv.",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.height(21.dp),
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.arial_tv_mark),
                                contentDescription = "tv.",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.height(17.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // Primary Navigation Icons / Rows
                    Column(
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        val primaryTabs = remember {
                            listOf(
                                TvNavTab.SEARCH to Icons.Rounded.Search,
                                TvNavTab.HOME to Icons.Rounded.Home,
                                TvNavTab.MOVIES to Icons.Rounded.Movie,
                                TvNavTab.SERIES to Icons.Rounded.LiveTv,
                                TvNavTab.LIBRARY to Icons.Rounded.Bookmark,
                            )
                        }
                        primaryTabs.forEach { (tab, icon) ->
                            RailNavItem(
                                label = tab.label,
                                icon = icon,
                                selected = activeTab == tab,
                                expanded = railFocused,
                                onClick = { onSelectTab(tab) },
                            )
                        }
                    }

                    RailNavItem(
                        label = TvNavTab.SETTINGS.label,
                        icon = Icons.Rounded.Settings,
                        selected = activeTab == TvNavTab.SETTINGS,
                        expanded = railFocused,
                        onClick = { onSelectTab(TvNavTab.SETTINGS) },
                    )

                    AnimatedVisibility(
                        visible = railFocused,
                        enter = fadeIn(tween(140)),
                        exit = fadeOut(tween(80)),
                    ) {
                        Text(
                            text = "BY ${AppMetadata.DEVELOPER_NAME.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted,
                            modifier = Modifier.padding(start = 10.dp, top = 12.dp),
                            maxLines = 1,
                        )
                    }
                }

                // Right Main Stage with Banner-Backed Top Header
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(CanvasBlack),
                ) {
                    BannerTopHeader(
                        sleepTimerRemainingSec = sleepTimerRemainingSec,
                        showLogo = !railFocused,
                        isCompact = false,
                        onRefresh = onRefresh,
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        content()
                    }
                }
            }
        } else {
            // Compact Layout
            Column(modifier = Modifier.fillMaxSize()) {
                BannerTopHeader(
                    sleepTimerRemainingSec = sleepTimerRemainingSec,
                    showLogo = true,
                    isCompact = true,
                    onRefresh = onRefresh,
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    content()
                }
                CompactBottomBar(
                    activeTab = activeTab,
                    onSelectTab = onSelectTab,
                )
            }
        }
    }
}

@Composable
private fun RailNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val pillShape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(pillShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) SurfaceSelected else Color.Transparent,
            contentColor = if (selected) PureWhite else InkSecondary,
            focusedContainerColor = PureWhite,
            focusedContentColor = InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (selected) Border(BorderStroke(1.5.dp, SelectedBorderRed), shape = pillShape) else Border.None,
            focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = pillShape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        modifier = if (expanded) Modifier.fillMaxWidth() else Modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(21.dp),
            )
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(140)),
                exit = fadeOut(tween(80)),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Top App Bar featuring the user's shared banner artwork at low opacity
 * with a smooth vertical fade down into CanvasBlack.
 */
@Composable
private fun BannerTopHeader(
    sleepTimerRemainingSec: Int,
    showLogo: Boolean,
    isCompact: Boolean,
    onRefresh: () -> Unit,
) {
    val headerHeight = if (isCompact) 58.dp else 64.dp
    val hPad = if (isCompact) 16.dp else 32.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(headerHeight)
            .background(CanvasBlack),
    ) {
        // User's shared AurisVerse banner at low opacity across the top app bar
        Image(
            painter = painterResource(id = R.drawable.auris_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.24f,
            modifier = Modifier.fillMaxSize(),
        )

        // Smooth bottom fade into CanvasBlack + soft edge vignette for legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0x380A0A0A),
                            0.45f to Color(0x780A0A0A),
                            0.85f to Color(0xE00A0A0A),
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
                            0.0f to Color(0xB80A0A0A),
                            0.25f to Color(0x280A0A0A),
                            0.75f to Color(0x280A0A0A),
                            1.0f to Color(0xB80A0A0A),
                        ),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = hPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: `arial tv.` logo (hidden only while expanded rail already displays it)
            Box(contentAlignment = Alignment.CenterStart) {
                if (showLogo) {
                    Image(
                        painter = painterResource(id = R.drawable.arial_tv_logo_dark),
                        contentDescription = "arial tv.",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(if (isCompact) 20.dp else 22.dp),
                    )
                }
            }

            // Right: Sleep timer pill (only if active) + Refresh + Dot-Matrix Clock
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp),
            ) {
                if (sleepTimerRemainingSec > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xCC1A1A1A))
                            .padding(horizontal = 11.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = "SLEEP ${formatTimerRemaining(sleepTimerRemainingSec)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkPrimary,
                        )
                    }
                }

                Surface(
                    onClick = onRefresh,
                    shape = ClickableSurfaceDefaults.shape(CircleShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color(0xB01A1A1A),
                        contentColor = InkPrimary,
                        focusedContainerColor = PureWhite,
                        focusedContentColor = InkOnWhite,
                    ),
                    border = ClickableSurfaceDefaults.border(
                        border = Border.None,
                        focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = CircleShape),
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
                ) {
                    Box(
                        modifier = Modifier.size(if (isCompact) 32.dp else 34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }

                LiveClockText(isCompact = isCompact)
            }
        }
    }
}

/**
 * Isolated leaf composable so clock updates never recompose parent screens.
 */
@Composable
private fun LiveClockText(isCompact: Boolean) {
    var timeText by remember { mutableStateOf(currentClockString()) }
    LaunchedEffect(Unit) {
        while (true) {
            timeText = currentClockString()
            delay(15_000L)
        }
    }
    Text(
        text = timeText,
        fontFamily = DotMatrixFont,
        fontSize = if (isCompact) 13.sp else 15.sp,
        color = InkPrimary,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun CompactBottomBar(
    activeTab: TvNavTab,
    onSelectTab: (TvNavTab) -> Unit,
) {
    val tabs = remember {
        listOf(
            TvNavTab.HOME to Icons.Rounded.Home,
            TvNavTab.MOVIES to Icons.Rounded.Movie,
            TvNavTab.SERIES to Icons.Rounded.LiveTv,
            TvNavTab.SEARCH to Icons.Rounded.Search,
            TvNavTab.LIBRARY to Icons.Rounded.Bookmark,
            TvNavTab.SETTINGS to Icons.Rounded.Settings,
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RailSurface)
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { (tab, icon) ->
            val selected = activeTab == tab
            val shape = RoundedCornerShape(12.dp)
            Surface(
                onClick = { onSelectTab(tab) },
                shape = ClickableSurfaceDefaults.shape(shape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (selected) SurfaceSelected else Color.Transparent,
                    contentColor = if (selected) PureWhite else InkMuted,
                    focusedContainerColor = PureWhite,
                    focusedContentColor = InkOnWhite,
                ),
                border = ClickableSurfaceDefaults.border(
                    border = if (selected) Border(BorderStroke(1.5.dp, SelectedBorderRed), shape = shape) else Border.None,
                    focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = shape),
                ),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = tab.label, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private fun currentClockString(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun formatTimerRemaining(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}
