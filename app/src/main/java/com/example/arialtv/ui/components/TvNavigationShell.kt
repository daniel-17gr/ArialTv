package com.example.arialtv.ui.components

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.arialtv.R
import com.example.arialtv.data.model.CatalogLanguage
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.InkOnSelected
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.RailOverlayBg
import com.example.arialtv.ui.theme.RailSurface
import com.example.arialtv.ui.theme.SurfaceSelected
import com.example.arialtv.ui.viewmodel.TvNavTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TopOverlayScrim = Brush.verticalGradient(
    colors = listOf(Color(0xB80F0F0F), Color(0x450F0F0F), Color.Transparent),
)
private val RailItemShape = RoundedCornerShape(10.dp)
private val TopPillShape = RoundedCornerShape(50)
private val PrimaryNavItems = listOf(
    TvNavTab.SEARCH to Icons.Rounded.Search,
    TvNavTab.HOME to Icons.Rounded.Home,
    TvNavTab.MOVIES to Icons.Rounded.Movie,
    TvNavTab.SERIES to Icons.Rounded.LiveTv,
    TvNavTab.LIBRARY to Icons.Rounded.Bookmark,
)

/**
 * Zero-Lag YouTube TV Navigation Shell:
 * - Uses a Box overlay for the left rail so expanding/collapsing NEVER resizes or re-lays out
 *   the Hero or poster rails behind it (fixes menu/rail toggle lag on low-RAM TVs).
 * - Content extends all the way to the top edge (y = 0) so the Hero artwork fits seamlessly
 *   behind a transparent floating top bar.
 * - Pure YouTube TV focus (solid white pill fill + dark ink, zero colored outline borders).
 */
@Composable
fun TvNavigationShell(
    activeTab: TvNavTab,
    language: CatalogLanguage,
    sleepTimerRemainingSecFlow: StateFlow<Int>,
    isFullBleedTop: Boolean,
    onSelectTab: (TvNavTab) -> Unit,
    onToggleLanguage: () -> Unit,
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

            Box(modifier = Modifier.fillMaxSize()) {
                // Main Content Stage — FIXED left inset (68dp), NEVER resizes when rail toggles!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 68.dp)
                        .background(CanvasBlack),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = if (isFullBleedTop) 0.dp else 56.dp),
                    ) {
                        content()
                    }

                    // Clean Transparent Floating Top Bar (no banner image, no boxed outline)
                    FloatingTopBar(
                        language = language,
                        sleepTimerRemainingSecFlow = sleepTimerRemainingSecFlow,
                        isCompact = false,
                        onOpenSearch = { onSelectTab(TvNavTab.SEARCH) },
                        onToggleLanguage = onToggleLanguage,
                        onRefresh = onRefresh,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }

                // Subtle dim scrim behind expanded rail only (no relayout!)
                if (railFocused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 200.dp)
                            .background(Color(0x55000000)),
                    )
                }

                // Overlay Left Navigation Rail (instant snappy snap — zero width animation thrash!)
                Column(
                    modifier = Modifier
                        .width(if (railFocused) 200.dp else 68.dp)
                        .fillMaxHeight()
                        .background(if (railFocused) RailOverlayBg else RailSurface)
                        .onFocusChanged { state ->
                            if (railFocused != state.hasFocus) railFocused = state.hasFocus
                        }
                        .padding(vertical = 18.dp, horizontal = 10.dp),
                    horizontalAlignment = if (railFocused) Alignment.Start else Alignment.CenterHorizontally,
                ) {
                    // Simple static brand mark (no laggy bitmap swap animation)
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .padding(horizontal = if (railFocused) 10.dp else 0.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.arial_tv_mark),
                            contentDescription = "tv.",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(17.dp),
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        PrimaryNavItems.forEach { (tab, icon) ->
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
                }
            }
        } else {
            // Compact layout
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = if (isFullBleedTop) 0.dp else 52.dp),
                    ) {
                        content()
                    }
                    FloatingTopBar(
                        language = language,
                        sleepTimerRemainingSecFlow = sleepTimerRemainingSecFlow,
                        isCompact = true,
                        onOpenSearch = { onSelectTab(TvNavTab.SEARCH) },
                        onToggleLanguage = onToggleLanguage,
                        onRefresh = onRefresh,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
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
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RailItemShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) SurfaceSelected else Color.Transparent,
            contentColor = if (selected) InkOnSelected else InkSecondary,
            focusedContainerColor = PureWhite,
            focusedContentColor = InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
        modifier = if (expanded) Modifier.fillMaxWidth() else Modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(21.dp),
            )
            if (expanded) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun FloatingTopBar(
    language: CatalogLanguage,
    sleepTimerRemainingSecFlow: StateFlow<Int>,
    isCompact: Boolean,
    onOpenSearch: () -> Unit,
    onToggleLanguage: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hPad = if (isCompact) 16.dp else 32.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TopOverlayScrim)
            .statusBarsPadding()
            .height(if (isCompact) 52.dp else 56.dp)
            .padding(horizontal = hPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "developed by Auris",
            style = MaterialTheme.typography.labelLarge,
            fontSize = if (isCompact) 12.sp else 14.sp,
            color = PureWhite,
            letterSpacing = 0.3.sp,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 10.dp),
        ) {
            SleepTimerTopBadge(sleepTimerRemainingSecFlow = sleepTimerRemainingSecFlow)

            if (!isCompact) {
                TopBarIconPill(
                    icon = Icons.Rounded.Search,
                    contentDescription = "Search",
                    onClick = onOpenSearch,
                )
            }

            Surface(
                onClick = onToggleLanguage,
                shape = ClickableSurfaceDefaults.shape(TopPillShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color(0x45000000),
                    contentColor = InkPrimary,
                    focusedContainerColor = PureWhite,
                    focusedContentColor = InkOnWhite,
                ),
                border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
            ) {
                Text(
                    text = language.code.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                )
            }

            TopBarIconPill(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                onClick = onRefresh,
            )

            LiveClockLeaf(isCompact = isCompact)
        }
    }
}

@Composable
private fun SleepTimerTopBadge(sleepTimerRemainingSecFlow: StateFlow<Int>) {
    val remainingSec by sleepTimerRemainingSecFlow.collectAsStateWithLifecycle()
    if (remainingSec > 0) {
        Box(
            modifier = Modifier
                .clip(TopPillShape)
                .background(Color(0x88000000))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text = "SLEEP ${formatTimerRemaining(remainingSec)}",
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
            )
        }
    }
}

@Composable
private fun TopBarIconPill(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x45000000),
            contentColor = InkPrimary,
            focusedContainerColor = PureWhite,
            focusedContentColor = InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun LiveClockLeaf(isCompact: Boolean) {
    var timeText by remember { mutableStateOf(currentClockString()) }
    LaunchedEffect(Unit) {
        while (true) {
            timeText = currentClockString()
            delay(20_000L)
        }
    }
    Text(
        text = timeText,
        style = MaterialTheme.typography.labelLarge,
        fontSize = if (isCompact) 13.sp else 14.sp,
        color = PureWhite,
        modifier = Modifier.padding(start = 4.dp),
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
            Surface(
                onClick = { onSelectTab(tab) },
                shape = ClickableSurfaceDefaults.shape(RailItemShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (selected) SurfaceSelected else Color.Transparent,
                    contentColor = if (selected) InkOnSelected else InkMuted,
                    focusedContainerColor = PureWhite,
                    focusedContentColor = InkOnWhite,
                ),
                border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
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
