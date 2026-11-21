package com.example.arialtv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.arialtv.R
import com.example.arialtv.data.model.AppMetadata
import com.example.arialtv.data.model.CatalogLanguage
import com.example.arialtv.ui.theme.DividerHairline
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.RailOverlayBg
import com.example.arialtv.ui.theme.SignalRed
import com.example.arialtv.ui.theme.SurfaceSelected
import com.example.arialtv.ui.viewmodel.TvNavTab
import kotlinx.coroutines.delay

private val DrawerCardShape = RoundedCornerShape(14.dp)
private val DrawerRowShape = RoundedCornerShape(10.dp)
private val DrawerArtBottomFade = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color.Transparent,
        0.55f to Color(0x350F0F0F),
        1.0f to Color(0xEE121212),
    ),
)

/**
 * Lightweight, snappy remote MENU Drawer:
 * - Features the user's framed portrait artwork as an editorial top showcase card (NOT stretched in bg).
 * - Fast 110ms fade (no heavy dual slide+fade lag on low-RAM TVs).
 * - Pure YouTube TV focus (solid white pill fill + dark ink, zero colored outlines).
 */
@Composable
fun QuickMenuOverlay(
    visible: Boolean,
    activeTab: TvNavTab,
    language: CatalogLanguage,
    sleepTimerMinutes: Int,
    onDismiss: () -> Unit,
    onSelectTab: (TvNavTab) -> Unit,
    onRefresh: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onToggleLanguage: () -> Unit,
    onRequestExit: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(visible) {
        if (visible) {
            delay(50)
            runCatching { firstFocus.requestFocus() }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(110)),
        exit = fadeOut(tween(90)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(324.dp)
                    .background(RailOverlayBg)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Editorial Showcase Card featuring the user's portrait artwork cleanly framed
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(DrawerCardShape),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.menu_drawer_art),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DrawerArtBottomFade),
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.arial_tv_logo_dark),
                            contentDescription = "arial tv.",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(19.dp),
                        )
                        Text(
                            text = "v${AppMetadata.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary,
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item {
                        QuickMenuItem(
                            label = "Home",
                            icon = Icons.Rounded.Home,
                            active = activeTab == TvNavTab.HOME,
                            modifier = Modifier.focusRequester(firstFocus),
                            onClick = { onDismiss(); onSelectTab(TvNavTab.HOME) },
                        )
                    }
                    item {
                        QuickMenuItem(
                            label = "Search",
                            icon = Icons.Rounded.Search,
                            active = activeTab == TvNavTab.SEARCH,
                            onClick = { onDismiss(); onSelectTab(TvNavTab.SEARCH) },
                        )
                    }
                    item {
                        QuickMenuItem(
                            label = "Films",
                            icon = Icons.Rounded.Movie,
                            active = activeTab == TvNavTab.MOVIES,
                            onClick = { onDismiss(); onSelectTab(TvNavTab.MOVIES) },
                        )
                    }
                    item {
                        QuickMenuItem(
                            label = "Series",
                            icon = Icons.Rounded.LiveTv,
                            active = activeTab == TvNavTab.SERIES,
                            onClick = { onDismiss(); onSelectTab(TvNavTab.SERIES) },
                        )
                    }
                    item {
                        QuickMenuItem(
                            label = "My List",
                            icon = Icons.Rounded.Bookmark,
                            active = activeTab == TvNavTab.LIBRARY,
                            onClick = { onDismiss(); onSelectTab(TvNavTab.LIBRARY) },
                        )
                    }

                    item {
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(DividerHairline))
                        Spacer(Modifier.height(6.dp))
                    }

                    item {
                        QuickMenuItem(
                            label = "Refresh Catalog",
                            icon = Icons.Rounded.Refresh,
                            onClick = { onDismiss(); onRefresh() },
                        )
                    }
                    item {
                        QuickMenuItem(
                            label = "Sleep Timer",
                            trailing = AppMetadata.formatSleepLabel(sleepTimerMinutes).uppercase(),
                            icon = Icons.Rounded.Bedtime,
                            active = sleepTimerMinutes > 0,
                            onClick = onCycleSleepTimer,
                        )
                    }
                    item {
                        QuickMenuItem(
                            label = "Language",
                            trailing = language.code.uppercase(),
                            icon = Icons.Rounded.Language,
                            onClick = onToggleLanguage,
                        )
                    }
                    item {
                        QuickMenuItem(
                            label = "Settings & About",
                            icon = Icons.Rounded.Settings,
                            active = activeTab == TvNavTab.SETTINGS,
                            onClick = { onDismiss(); onSelectTab(TvNavTab.SETTINGS) },
                        )
                    }
                    item {
                        Spacer(Modifier.height(4.dp))
                        QuickMenuItem(
                            label = "Exit Arial TV",
                            icon = Icons.AutoMirrored.Rounded.ExitToApp,
                            destructive = true,
                            onClick = { onDismiss(); onRequestExit() },
                        )
                    }
                }

                // Clean Bottom Developer Credit & GitHub Footer
                Box(Modifier.fillMaxWidth().height(1.dp).background(DividerHairline))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Developed by ${AppMetadata.DEVELOPER_NAME}",
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary,
                    )
                    Text(
                        text = AppMetadata.GITHUB_HANDLE,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String = "",
    active: Boolean = false,
    destructive: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(DrawerRowShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (active) SurfaceSelected else Color.Transparent,
            contentColor = when {
                destructive -> SignalRed
                active -> PureWhite
                else -> InkPrimary
            },
            focusedContainerColor = if (destructive) SignalRed else PureWhite,
            focusedContentColor = if (destructive) PureWhite else InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
            if (trailing.isNotBlank()) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
