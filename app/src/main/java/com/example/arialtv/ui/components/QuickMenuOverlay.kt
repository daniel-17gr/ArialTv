package com.example.arialtv.ui.components

import android.view.KeyEvent
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.arialtv.ui.theme.InkOnSelected
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
 * Isolated native Dialog remote MENU Drawer:
 * - Hosted inside a platform Dialog so D-pad focus CANNOT escape to underlying screens and Android
 *   automatically restores focus to the prior card on dismiss.
 * - Features the user's framed portrait artwork as an editorial top showcase card.
 * - Pressing DPAD_LEFT, BACK, ESCAPE, or MENU cleanly closes the drawer; DPAD_RIGHT stays trapped.
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
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false,
        ),
    ) {
        val listState = rememberLazyListState()
        val homeFocus = remember { FocusRequester() }
        val searchFocus = remember { FocusRequester() }
        val moviesFocus = remember { FocusRequester() }
        val seriesFocus = remember { FocusRequester() }
        val libraryFocus = remember { FocusRequester() }
        val settingsFocus = remember { FocusRequester() }

        LaunchedEffect(activeTab) {
            val targetIndex = when (activeTab) {
                TvNavTab.HOME -> 0
                TvNavTab.SEARCH -> 1
                TvNavTab.MOVIES -> 2
                TvNavTab.SERIES -> 3
                TvNavTab.LIBRARY -> 4
                TvNavTab.SETTINGS -> 9
            }
            if (targetIndex > 4) {
                runCatching { listState.scrollToItem(targetIndex) }
            }
            delay(40)
            val targetRequester = when (activeTab) {
                TvNavTab.HOME -> homeFocus
                TvNavTab.SEARCH -> searchFocus
                TvNavTab.MOVIES -> moviesFocus
                TvNavTab.SERIES -> seriesFocus
                TvNavTab.LIBRARY -> libraryFocus
                TvNavTab.SETTINGS -> settingsFocus
            }
            runCatching { targetRequester.requestFocus() }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_BACK,
                        KeyEvent.KEYCODE_ESCAPE,
                        KeyEvent.KEYCODE_MENU -> {
                            onDismiss()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> true
                        else -> false
                    }
                },
        ) {
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Editorial Showcase Card featuring the user's portrait artwork cleanly framed
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(144.dp)
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

                Text(
                    text = "← OR BACK TO CLOSE",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item(key = "qm_home") {
                        QuickMenuItem(
                            label = "Home",
                            icon = Icons.Rounded.Home,
                            active = activeTab == TvNavTab.HOME,
                            modifier = Modifier.focusRequester(homeFocus),
                            onClick = { onDismiss(); onSelectTab(TvNavTab.HOME) },
                        )
                    }
                    item(key = "qm_search") {
                        QuickMenuItem(
                            label = "Search",
                            icon = Icons.Rounded.Search,
                            active = activeTab == TvNavTab.SEARCH,
                            modifier = Modifier.focusRequester(searchFocus),
                            onClick = { onDismiss(); onSelectTab(TvNavTab.SEARCH) },
                        )
                    }
                    item(key = "qm_movies") {
                        QuickMenuItem(
                            label = "Films",
                            icon = Icons.Rounded.Movie,
                            active = activeTab == TvNavTab.MOVIES,
                            modifier = Modifier.focusRequester(moviesFocus),
                            onClick = { onDismiss(); onSelectTab(TvNavTab.MOVIES) },
                        )
                    }
                    item(key = "qm_series") {
                        QuickMenuItem(
                            label = "Series",
                            icon = Icons.Rounded.LiveTv,
                            active = activeTab == TvNavTab.SERIES,
                            modifier = Modifier.focusRequester(seriesFocus),
                            onClick = { onDismiss(); onSelectTab(TvNavTab.SERIES) },
                        )
                    }
                    item(key = "qm_library") {
                        QuickMenuItem(
                            label = "My List",
                            icon = Icons.Rounded.Bookmark,
                            active = activeTab == TvNavTab.LIBRARY,
                            modifier = Modifier.focusRequester(libraryFocus),
                            onClick = { onDismiss(); onSelectTab(TvNavTab.LIBRARY) },
                        )
                    }

                    item(key = "qm_div") {
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(DividerHairline))
                        Spacer(Modifier.height(6.dp))
                    }

                    item(key = "qm_refresh") {
                        QuickMenuItem(
                            label = "Refresh Catalog",
                            icon = Icons.Rounded.Refresh,
                            onClick = { onDismiss(); onRefresh() },
                        )
                    }
                    item(key = "qm_sleep") {
                        QuickMenuItem(
                            label = "Sleep Timer",
                            trailing = AppMetadata.formatSleepLabel(sleepTimerMinutes).uppercase(),
                            icon = Icons.Rounded.Bedtime,
                            active = sleepTimerMinutes > 0,
                            onClick = onCycleSleepTimer,
                        )
                    }
                    item(key = "qm_lang") {
                        QuickMenuItem(
                            label = "Language",
                            trailing = language.code.uppercase(),
                            icon = Icons.Rounded.Language,
                            onClick = onToggleLanguage,
                        )
                    }
                    item(key = "qm_settings") {
                        QuickMenuItem(
                            label = "Settings & About",
                            icon = Icons.Rounded.Settings,
                            active = activeTab == TvNavTab.SETTINGS,
                            modifier = Modifier.focusRequester(settingsFocus),
                            onClick = { onDismiss(); onSelectTab(TvNavTab.SETTINGS) },
                        )
                    }
                    item(key = "qm_exit") {
                        Spacer(Modifier.height(4.dp))
                        QuickMenuItem(
                            label = "Exit Arial TV",
                            icon = Icons.AutoMirrored.Rounded.ExitToApp,
                            destructive = true,
                            onClick = { onDismiss(); onRequestExit() },
                        )
                    }
                }

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
                active -> InkOnSelected
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
