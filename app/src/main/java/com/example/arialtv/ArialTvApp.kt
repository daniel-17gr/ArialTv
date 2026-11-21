package com.example.arialtv

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.arialtv.ui.components.ExitConfirmationDialog
import com.example.arialtv.ui.components.QuickMenuOverlay
import com.example.arialtv.ui.components.SplashLoadingOverlay
import com.example.arialtv.ui.components.TvNavigationShell
import com.example.arialtv.ui.screens.CatalogScreen
import com.example.arialtv.ui.screens.DetailScreen
import com.example.arialtv.ui.screens.LibraryScreen
import com.example.arialtv.ui.screens.PlayerScreen
import com.example.arialtv.ui.screens.SearchScreen
import com.example.arialtv.ui.screens.SettingsScreen
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.SurfaceElevated
import com.example.arialtv.ui.viewmodel.AppDestination
import com.example.arialtv.ui.viewmodel.ArialTvViewModel
import com.example.arialtv.ui.viewmodel.TvNavTab
import kotlinx.coroutines.delay

@Composable
fun ArialTvApp(
    viewModel: ArialTvViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1_100L)
        var extraWait = 0
        while (viewModel.uiState.value.homeState.isLoading && extraWait < 600) {
            delay(150L)
            extraWait += 150
        }
        showSplash = false
    }

    // Always intercept Back so pressing Back on Home shows the clean Exit Confirmation dialog
    BackHandler(enabled = true) {
        viewModel.handleBackPress()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val dest = state.destination) {
            is AppDestination.Player -> {
                PlayerScreen(
                    destination = dest,
                    playerState = state.playerState,
                    settings = state.settings,
                    episodeProgress = state.episodeProgress,
                    sleepTimerMinutes = state.sleepTimerMinutes,
                    sleepTimerRemainingSecFlow = viewModel.sleepTimerRemainingSec,
                    sleepTimerExpired = state.sleepTimerExpired,
                    onSelectQuality = viewModel::selectStreamQuality,
                    onSelectAudioDub = viewModel::switchPlayerAudioDub,
                    onSelectSubtitle = viewModel::selectSubtitle,
                    onUpdateSubtitleStyle = viewModel::updateSubtitleStyle,
                    onUpdateAspectRatio = viewModel::updateAspectRatioMode,
                    onSetSleepTimer = viewModel::setSleepTimer,
                    onDismissSleepExpired = viewModel::dismissSleepTimerExpired,
                    onPlayEpisode = viewModel::playEpisodeFromPlayer,
                    onPreviousEpisode = viewModel::playPreviousEpisode,
                    onNextEpisode = viewModel::playNextEpisode,
                    onRetry = { viewModel.retryPlayback() },
                    onSaveProgress = viewModel::recordPlaybackProgress,
                    onExit = { viewModel.navigateBack() },
                )
            }

            is AppDestination.Details -> {
                val activeId = state.detailState.details?.item?.id ?: dest.item.id
                val historyEntry = state.watchHistory.firstOrNull { it.item.id == activeId }
                val inWatchlist = state.watchlist.any { it.id == activeId }

                DetailScreen(
                    fallbackItem = dest.item,
                    detailState = state.detailState,
                    watchProgress = historyEntry,
                    episodeProgress = state.episodeProgress,
                    isInWatchlist = inWatchlist,
                    onBack = { viewModel.navigateBack() },
                    onToggleWatchlist = viewModel::toggleWatchlist,
                    onPlay = { item, season, ep, resume, seasons, resolution ->
                        viewModel.startPlayback(
                            item = item,
                            season = season,
                            episode = ep,
                            resumeFromHistory = resume,
                            seasonsMeta = seasons,
                            requestedResolution = resolution,
                        )
                    },
                    onSwitchDub = viewModel::switchDub,
                    onSelectSeason = viewModel::selectSeason,
                    onOpenSimilar = viewModel::openDetails,
                )
            }

            AppDestination.Browse -> {
                val isFullBleedTop = state.activeTab == TvNavTab.HOME ||
                    state.activeTab == TvNavTab.MOVIES ||
                    state.activeTab == TvNavTab.SERIES
                TvNavigationShell(
                    activeTab = state.activeTab,
                    language = state.settings.language,
                    sleepTimerRemainingSecFlow = viewModel.sleepTimerRemainingSec,
                    isFullBleedTop = isFullBleedTop,
                    onSelectTab = viewModel::selectTab,
                    onToggleLanguage = viewModel::toggleLanguage,
                    onRefresh = viewModel::refreshCurrentTab,
                ) {
                    when (state.activeTab) {
                        TvNavTab.HOME -> CatalogScreen(
                            tabState = state.homeState,
                            watchHistory = state.watchHistory,
                            watchlist = state.watchlist,
                            recommendedRow = state.recommendedRow,
                            showPersonalRails = true,
                            onItemClick = viewModel::openDetails,
                            onPlayNow = { item -> viewModel.startPlayback(item) },
                            onToggleWatchlist = viewModel::toggleWatchlist,
                            onResumeProgress = { progress ->
                                viewModel.startPlayback(
                                    item = progress.item,
                                    season = progress.season,
                                    episode = progress.episode,
                                    resumeFromHistory = true,
                                )
                            },
                            onItemFocused = { item, category ->
                                viewModel.onCardFocused(TvNavTab.HOME, item, category)
                            },
                            onNextFeatured = { viewModel.cycleHeroFeatured(TvNavTab.HOME) },
                            onApplyFilter = { genre, year, sort, country ->
                                viewModel.applyCatalogFilter(TvNavTab.HOME, genre, year, sort, country)
                            },
                            onLoadMoreFiltered = { viewModel.loadMoreFilteredItems(TvNavTab.HOME) },
                            onClearFilter = { viewModel.clearCatalogFilter(TvNavTab.HOME) },
                            onRetry = { viewModel.loadCatalogForTab(TvNavTab.HOME, forceRefresh = true) },
                            onOpenSettings = { viewModel.selectTab(TvNavTab.SETTINGS) },
                        )

                        TvNavTab.MOVIES -> CatalogScreen(
                            tabState = state.moviesState,
                            watchHistory = emptyList(),
                            watchlist = state.watchlist,
                            showPersonalRails = false,
                            onItemClick = viewModel::openDetails,
                            onPlayNow = { item -> viewModel.startPlayback(item) },
                            onToggleWatchlist = viewModel::toggleWatchlist,
                            onResumeProgress = {},
                            onItemFocused = { item, category ->
                                viewModel.onCardFocused(TvNavTab.MOVIES, item, category)
                            },
                            onNextFeatured = { viewModel.cycleHeroFeatured(TvNavTab.MOVIES) },
                            onApplyFilter = { genre, year, sort, country ->
                                viewModel.applyCatalogFilter(TvNavTab.MOVIES, genre, year, sort, country)
                            },
                            onLoadMoreFiltered = { viewModel.loadMoreFilteredItems(TvNavTab.MOVIES) },
                            onClearFilter = { viewModel.clearCatalogFilter(TvNavTab.MOVIES) },
                            onRetry = { viewModel.loadCatalogForTab(TvNavTab.MOVIES, forceRefresh = true) },
                            onOpenSettings = { viewModel.selectTab(TvNavTab.SETTINGS) },
                        )

                        TvNavTab.SERIES -> CatalogScreen(
                            tabState = state.seriesState,
                            watchHistory = emptyList(),
                            watchlist = state.watchlist,
                            showPersonalRails = false,
                            onItemClick = viewModel::openDetails,
                            onPlayNow = { item -> viewModel.startPlayback(item) },
                            onToggleWatchlist = viewModel::toggleWatchlist,
                            onResumeProgress = {},
                            onItemFocused = { item, category ->
                                viewModel.onCardFocused(TvNavTab.SERIES, item, category)
                            },
                            onNextFeatured = { viewModel.cycleHeroFeatured(TvNavTab.SERIES) },
                            onApplyFilter = { genre, year, sort, country ->
                                viewModel.applyCatalogFilter(TvNavTab.SERIES, genre, year, sort, country)
                            },
                            onLoadMoreFiltered = { viewModel.loadMoreFilteredItems(TvNavTab.SERIES) },
                            onClearFilter = { viewModel.clearCatalogFilter(TvNavTab.SERIES) },
                            onRetry = { viewModel.loadCatalogForTab(TvNavTab.SERIES, forceRefresh = true) },
                            onOpenSettings = { viewModel.selectTab(TvNavTab.SETTINGS) },
                        )

                        TvNavTab.SEARCH -> SearchScreen(
                            searchState = state.searchState,
                            searchHistory = state.searchHistory,
                            onQueryChange = viewModel::updateSearchQuery,
                            onSearchSubmit = { q -> viewModel.performSearch(q) },
                            onFilterChange = viewModel::updateSearchFilter,
                            onItemClick = viewModel::openDetails,
                            onRemoveHistoryItem = viewModel::removeSearchHistoryItem,
                            onClearHistory = viewModel::clearSearchHistory,
                        )

                        TvNavTab.LIBRARY -> LibraryScreen(
                            watchHistory = state.watchHistory,
                            watchlist = state.watchlist,
                            onItemClick = viewModel::openDetails,
                            onResumeProgress = { progress ->
                                viewModel.startPlayback(
                                    item = progress.item,
                                    season = progress.season,
                                    episode = progress.episode,
                                    resumeFromHistory = true,
                                )
                            },
                            onClearHistory = viewModel::clearWatchHistory,
                            onClearWatchlist = viewModel::clearWatchlist,
                        )

                        TvNavTab.SETTINGS -> SettingsScreen(
                            settings = state.settings,
                            settingsUiState = state.settingsState,
                            watchHistoryCount = state.watchHistory.size,
                            searchHistoryCount = state.searchHistory.size,
                            watchlistCount = state.watchlist.size,
                            sleepTimerMinutes = state.sleepTimerMinutes,
                            onModeChange = viewModel::updateConnectionMode,
                            onBridgeUrlChange = viewModel::updateBridgeBaseUrl,
                            onLanguageChange = viewModel::updateLanguage,
                            onPreferredAudioChange = viewModel::updatePreferredAudioLanguage,
                            onQualityChange = viewModel::updatePreferredQuality,
                            onCodecChange = viewModel::updatePreferredCodec,
                            onAutoPlayNextChange = viewModel::updateAutoPlayNextEpisode,
                            onDefaultSpeedChange = viewModel::updateDefaultPlaybackSpeed,
                            onAspectRatioChange = viewModel::updateAspectRatioMode,
                            onControlsTimeoutChange = viewModel::updateControlsTimeout,
                            onNetworkTimeoutChange = viewModel::updateNetworkTimeout,
                            onSetSleepTimer = viewModel::setSleepTimer,
                            onUpdateSubtitleStyle = viewModel::updateSubtitleStyle,
                            onTestConnection = viewModel::runConnectionTest,
                            onCheckForUpdates = viewModel::checkForGithubUpdate,
                            onClearWatchHistory = viewModel::clearWatchHistory,
                            onClearSearchHistory = viewModel::clearSearchHistory,
                            onClearWatchlist = viewModel::clearWatchlist,
                        )
                    }
                }
            }
        }

        QuickMenuOverlay(
            visible = state.showQuickMenu && state.destination !is AppDestination.Player,
            activeTab = state.activeTab,
            language = state.settings.language,
            sleepTimerMinutes = state.sleepTimerMinutes,
            onDismiss = viewModel::dismissQuickMenu,
            onSelectTab = viewModel::selectTab,
            onRefresh = viewModel::refreshCurrentTab,
            onCycleSleepTimer = viewModel::cycleSleepTimer,
            onToggleLanguage = viewModel::toggleLanguage,
            onRequestExit = viewModel::requestExitDialog,
        )

        if (state.showExitDialog) {
            ExitConfirmationDialog(
                onStay = viewModel::dismissExitDialog,
                onConfirmExit = { activity?.finish() },
            )
        }

        // Minimalist Nothing OS Global Floating Status Toast
        AnimatedVisibility(
            visible = !state.toastMessage.isNullOrBlank() && !showSplash,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 18.dp),
        ) {
            val pillShape = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .background(SurfaceElevated)
                    .padding(horizontal = 20.dp, vertical = 9.dp),
            ) {
                Text(
                    text = state.toastMessage.orEmpty().uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite,
                )
            }
        }

        SplashLoadingOverlay(visible = showSplash)
    }
}
