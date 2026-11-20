package com.example.arialtv

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.arialtv.ui.components.ExitConfirmationDialog
import com.example.arialtv.ui.components.QuickMenuOverlay
import com.example.arialtv.ui.components.TvNavigationShell
import com.example.arialtv.ui.screens.CatalogScreen
import com.example.arialtv.ui.screens.DetailScreen
import com.example.arialtv.ui.screens.LibraryScreen
import com.example.arialtv.ui.screens.PlayerScreen
import com.example.arialtv.ui.screens.SearchScreen
import com.example.arialtv.ui.screens.SettingsScreen
import com.example.arialtv.ui.viewmodel.AppDestination
import com.example.arialtv.ui.viewmodel.ArialTvViewModel
import com.example.arialtv.ui.viewmodel.TvNavTab

@Composable
fun ArialTvApp(
    viewModel: ArialTvViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity

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
                    sleepTimerRemainingSec = state.sleepTimerRemainingSec,
                    sleepTimerExpired = state.sleepTimerExpired,
                    onSelectQuality = viewModel::selectStreamQuality,
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
                )
            }

            AppDestination.Browse -> {
                TvNavigationShell(
                    activeTab = state.activeTab,
                    sleepTimerRemainingSec = state.sleepTimerRemainingSec,
                    onSelectTab = viewModel::selectTab,
                    onRefresh = viewModel::refreshCurrentTab,
                ) {
                    when (state.activeTab) {
                        TvNavTab.HOME -> CatalogScreen(
                            tabState = state.homeState,
                            watchHistory = state.watchHistory,
                            watchlist = state.watchlist,
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
    }
}
