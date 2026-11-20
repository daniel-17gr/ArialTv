package com.example.arialtv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.graphics.SolidColor
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
import com.example.arialtv.data.model.AppSettings
import com.example.arialtv.data.model.AspectRatioMode
import com.example.arialtv.data.model.CatalogLanguage
import com.example.arialtv.data.model.ConnectionMode
import com.example.arialtv.data.model.PreferredCodec
import com.example.arialtv.data.model.PreferredQuality
import com.example.arialtv.data.model.SubtitleBackground
import com.example.arialtv.data.model.SubtitleColor
import com.example.arialtv.data.model.SubtitlePosition
import com.example.arialtv.data.model.SubtitleSize
import com.example.arialtv.data.model.SubtitleStyle
import com.example.arialtv.ui.theme.CanvasBlack
import com.example.arialtv.ui.theme.DividerHairline
import com.example.arialtv.ui.theme.FocusBorderWhite
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.SelectedBorderRed
import com.example.arialtv.ui.theme.SignalRed
import com.example.arialtv.ui.theme.StatusGreen
import com.example.arialtv.ui.theme.SurfaceElevated
import com.example.arialtv.ui.theme.SurfaceSelected
import com.example.arialtv.ui.theme.SurfaceTonal
import com.example.arialtv.ui.viewmodel.SettingsUiState

private enum class SettingsSection(val label: String, val icon: ImageVector) {
    PLAYBACK("Playback", Icons.Rounded.PlayCircleOutline),
    SUBTITLES("Subtitles", Icons.Rounded.ClosedCaption),
    TIMERS("Timers", Icons.Rounded.Bedtime),
    LANGUAGE("Language", Icons.Rounded.Language),
    PRIVACY("History", Icons.Rounded.DeleteOutline),
    ADVANCED("Advanced", Icons.Rounded.Tune),
    ABOUT("About", Icons.Rounded.Info),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    settingsUiState: SettingsUiState,
    watchHistoryCount: Int,
    searchHistoryCount: Int,
    watchlistCount: Int,
    sleepTimerMinutes: Int,
    onModeChange: (ConnectionMode) -> Unit,
    onBridgeUrlChange: (String) -> Unit,
    onLanguageChange: (CatalogLanguage) -> Unit,
    onQualityChange: (PreferredQuality) -> Unit,
    onCodecChange: (PreferredCodec) -> Unit,
    onAutoPlayNextChange: (Boolean) -> Unit,
    onDefaultSpeedChange: (Float) -> Unit,
    onAspectRatioChange: (AspectRatioMode) -> Unit,
    onControlsTimeoutChange: (Int) -> Unit,
    onNetworkTimeoutChange: (Int) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onUpdateSubtitleStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onTestConnection: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onClearWatchHistory: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onClearWatchlist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeSection by remember { mutableStateOf(SettingsSection.PLAYBACK) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBlack),
    ) {
        val isWide = maxWidth >= 660.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isWide) 32.dp else 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (isWide) {
                // Clean YouTube TV Two-Pane Settings Layout (no redundant profile card)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .width(214.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        items(SettingsSection.entries) { section ->
                            val selected = section == activeSection
                            val shape = RoundedCornerShape(14.dp)
                            Surface(
                                onClick = { activeSection = section },
                                shape = ClickableSurfaceDefaults.shape(shape),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (selected) SurfaceSelected else Color.Transparent,
                                    contentColor = if (selected) PureWhite else InkSecondary,
                                    focusedContainerColor = PureWhite,
                                    focusedContentColor = InkOnWhite,
                                ),
                                border = ClickableSurfaceDefaults.border(
                                    border = if (selected) Border(BorderStroke(1.5.dp, SelectedBorderRed), shape = shape) else Border.None,
                                    focusedBorder = Border(BorderStroke(2.dp, SignalRed), shape = shape),
                                ),
                                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(section.icon, contentDescription = null, modifier = Modifier.size(19.dp))
                                    Text(section.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    SettingsSectionDetail(
                        section = activeSection,
                        settings = settings,
                        settingsUiState = settingsUiState,
                        watchHistoryCount = watchHistoryCount,
                        searchHistoryCount = searchHistoryCount,
                        watchlistCount = watchlistCount,
                        sleepTimerMinutes = sleepTimerMinutes,
                        onModeChange = onModeChange,
                        onBridgeUrlChange = onBridgeUrlChange,
                        onLanguageChange = onLanguageChange,
                        onQualityChange = onQualityChange,
                        onCodecChange = onCodecChange,
                        onAutoPlayNextChange = onAutoPlayNextChange,
                        onDefaultSpeedChange = onDefaultSpeedChange,
                        onAspectRatioChange = onAspectRatioChange,
                        onControlsTimeoutChange = onControlsTimeoutChange,
                        onNetworkTimeoutChange = onNetworkTimeoutChange,
                        onSetSleepTimer = onSetSleepTimer,
                        onUpdateSubtitleStyle = onUpdateSubtitleStyle,
                        onTestConnection = onTestConnection,
                        onCheckForUpdates = onCheckForUpdates,
                        onClearWatchHistory = onClearWatchHistory,
                        onClearSearchHistory = onClearSearchHistory,
                        onClearWatchlist = onClearWatchlist,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(SettingsSection.entries) { section ->
                        SettingsPill(
                            label = section.label,
                            selected = section == activeSection,
                            onClick = { activeSection = section },
                        )
                    }
                }
                SettingsSectionDetail(
                    section = activeSection,
                    settings = settings,
                    settingsUiState = settingsUiState,
                    watchHistoryCount = watchHistoryCount,
                    searchHistoryCount = searchHistoryCount,
                    watchlistCount = watchlistCount,
                    sleepTimerMinutes = sleepTimerMinutes,
                    onModeChange = onModeChange,
                    onBridgeUrlChange = onBridgeUrlChange,
                    onLanguageChange = onLanguageChange,
                    onQualityChange = onQualityChange,
                    onCodecChange = onCodecChange,
                    onAutoPlayNextChange = onAutoPlayNextChange,
                    onDefaultSpeedChange = onDefaultSpeedChange,
                    onAspectRatioChange = onAspectRatioChange,
                    onControlsTimeoutChange = onControlsTimeoutChange,
                    onNetworkTimeoutChange = onNetworkTimeoutChange,
                    onSetSleepTimer = onSetSleepTimer,
                    onUpdateSubtitleStyle = onUpdateSubtitleStyle,
                    onTestConnection = onTestConnection,
                    onCheckForUpdates = onCheckForUpdates,
                    onClearWatchHistory = onClearWatchHistory,
                    onClearSearchHistory = onClearSearchHistory,
                    onClearWatchlist = onClearWatchlist,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSectionDetail(
    section: SettingsSection,
    settings: AppSettings,
    settingsUiState: SettingsUiState,
    watchHistoryCount: Int,
    searchHistoryCount: Int,
    watchlistCount: Int,
    sleepTimerMinutes: Int,
    onModeChange: (ConnectionMode) -> Unit,
    onBridgeUrlChange: (String) -> Unit,
    onLanguageChange: (CatalogLanguage) -> Unit,
    onQualityChange: (PreferredQuality) -> Unit,
    onCodecChange: (PreferredCodec) -> Unit,
    onAutoPlayNextChange: (Boolean) -> Unit,
    onDefaultSpeedChange: (Float) -> Unit,
    onAspectRatioChange: (AspectRatioMode) -> Unit,
    onControlsTimeoutChange: (Int) -> Unit,
    onNetworkTimeoutChange: (Int) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onUpdateSubtitleStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onTestConnection: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onClearWatchHistory: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onClearWatchlist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var urlInputFocused by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (section) {
            SettingsSection.PLAYBACK -> {
                item {
                    SettingGroup(title = "Video Quality") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PreferredQuality.entries.forEach { q ->
                                SettingsPill(q.label, settings.preferredQuality == q) { onQualityChange(q) }
                            }
                        }
                    }
                }
                item {
                    SettingGroup(title = "Autoplay Next Episode") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsPill("On", settings.autoPlayNextEpisode) { onAutoPlayNextChange(true) }
                            SettingsPill("Off", !settings.autoPlayNextEpisode) { onAutoPlayNextChange(false) }
                        }
                    }
                }
                item {
                    SettingGroup(title = "Default Aspect Ratio") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AspectRatioMode.entries.forEach { m ->
                                SettingsPill(m.label, settings.aspectRatioMode == m) { onAspectRatioChange(m) }
                            }
                        }
                    }
                }
                item {
                    SettingGroup(title = "Default Speed") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { sp ->
                                SettingsPill(if (sp == 1.0f) "Normal" else "${sp}x", settings.defaultPlaybackSpeed == sp) {
                                    onDefaultSpeedChange(sp)
                                }
                            }
                        }
                    }
                }
            }

            SettingsSection.SUBTITLES -> {
                item {
                    val st = settings.subtitleStyle
                    val previewFontSp = when (st.size) {
                        SubtitleSize.SMALL -> 14.sp
                        SubtitleSize.MEDIUM -> 18.sp
                        SubtitleSize.LARGE -> 22.sp
                        SubtitleSize.EXTRA_LARGE -> 26.sp
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF050505))
                            .padding(18.dp),
                        contentAlignment = if (st.position == SubtitlePosition.RAISED) Alignment.Center else Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(st.background.argb))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "The quick brown fox jumps over the lazy dog.",
                                color = Color(st.color.argb),
                                fontSize = previewFontSp,
                            )
                        }
                    }
                }
                item {
                    SettingGroup(title = "Subtitle Size") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubtitleSize.entries.forEach { sz ->
                                SettingsPill(sz.label, settings.subtitleStyle.size == sz) {
                                    onUpdateSubtitleStyle { it.copy(size = sz) }
                                }
                            }
                        }
                    }
                }
                item {
                    SettingGroup(title = "Text Color") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubtitleColor.entries.forEach { col ->
                                SettingsPill(col.label, settings.subtitleStyle.color == col) {
                                    onUpdateSubtitleStyle { it.copy(color = col) }
                                }
                            }
                        }
                    }
                }
                item {
                    SettingGroup(title = "Background Style") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubtitleBackground.entries.forEach { bg ->
                                SettingsPill(bg.label, settings.subtitleStyle.background == bg) {
                                    onUpdateSubtitleStyle { it.copy(background = bg) }
                                }
                            }
                        }
                    }
                }
                item {
                    SettingGroup(title = "Position") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubtitlePosition.entries.forEach { pos ->
                                SettingsPill(pos.label, settings.subtitleStyle.position == pos) {
                                    onUpdateSubtitleStyle { it.copy(position = pos) }
                                }
                            }
                        }
                    }
                }
            }

            SettingsSection.TIMERS -> {
                item {
                    SettingGroup(
                        title = "Sleep Timer",
                        subtitle = "Automatically pause playback after a set duration (up to 4 hours).",
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppMetadata.SLEEP_TIMER_OPTIONS.forEach { mins ->
                                SettingsPill(
                                    label = AppMetadata.formatSleepLongLabel(mins),
                                    selected = sleepTimerMinutes == mins,
                                ) { onSetSleepTimer(mins) }
                            }
                        }
                    }
                }
                item {
                    SettingGroup(title = "Player Controls Auto-Hide") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3 to "3s", 4 to "4s", 7 to "7s", 0 to "Never").forEach { (sec, label) ->
                                SettingsPill(label, settings.controlsTimeoutSeconds == sec) {
                                    onControlsTimeoutChange(sec)
                                }
                            }
                        }
                    }
                }
                item {
                    SettingGroup(title = "Network Request Timeout") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 25, 40).forEach { sec ->
                                SettingsPill("${sec}s", settings.networkTimeoutSeconds == sec) {
                                    onNetworkTimeoutChange(sec)
                                }
                            }
                        }
                    }
                }
            }

            SettingsSection.LANGUAGE -> {
                item {
                    SettingGroup(title = "Catalog & Preferred Subtitles") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CatalogLanguage.entries.forEach { lang ->
                                SettingsPill(lang.label, settings.language == lang) { onLanguageChange(lang) }
                            }
                        }
                    }
                }
            }

            SettingsSection.PRIVACY -> {
                item {
                    SettingGroup(title = "Clear Saved Data") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsPill("Clear Watch History ($watchHistoryCount)", false, destructive = true) {
                                onClearWatchHistory()
                            }
                            SettingsPill("Clear Search History ($searchHistoryCount)", false, destructive = true) {
                                onClearSearchHistory()
                            }
                            SettingsPill("Clear My List ($watchlistCount)", false, destructive = true) {
                                onClearWatchlist()
                            }
                        }
                    }
                }
            }

            SettingsSection.ADVANCED -> {
                item {
                    SettingGroup(
                        title = "Video Decoder Preference",
                        subtitle = "Includes automatic FFmpeg audio decoders and software video fallback.",
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PreferredCodec.entries.forEach { codec ->
                                SettingsPill(codec.label, settings.preferredCodec == codec) { onCodecChange(codec) }
                            }
                        }
                    }
                }
                item {
                    SettingGroup(title = "Source Mode") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ConnectionMode.entries.forEach { mode ->
                                    SettingsPill(mode.label, settings.connectionMode == mode) { onModeChange(mode) }
                                }
                            }
                            if (settings.connectionMode == ConnectionMode.CUSTOM_BRIDGE) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CanvasBlack)
                                        .then(
                                            if (urlInputFocused) Modifier.border(2.dp, SignalRed, RoundedCornerShape(12.dp))
                                            else Modifier,
                                        )
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    BasicTextField(
                                        value = settings.bridgeBaseUrl,
                                        onValueChange = onBridgeUrlChange,
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureWhite),
                                        cursorBrush = SolidColor(SignalRed),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { urlInputFocused = it.isFocused },
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    SettingGroup(title = "Connection Check") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsPill(
                                label = if (settingsUiState.isTesting) "Checking..." else "Check Connection",
                                selected = false,
                                onClick = onTestConnection,
                            )
                            val res = settingsUiState.testResult
                            if (res != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (res.success) StatusGreen else SignalRed),
                                    )
                                    Text(
                                        text = if (res.success) "Online · ${res.latencyMs}ms" else "Offline · Try again",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SettingsSection.ABOUT -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceTonal)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.arial_tv_logo_dark),
                                contentDescription = "arial tv.",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.height(28.dp),
                            )
                            Text(
                                text = "VERSION ${AppMetadata.VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary,
                            )
                        }

                        Text(
                            text = "A fast, minimalist Android TV streaming experience inspired by Nothing OS and YouTube TV — featuring live catalog filtering, custom subtitles, FFmpeg multi-codec playback, and intuitive remote navigation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary,
                        )

                        Box(Modifier.fillMaxWidth().height(1.dp).background(DividerHairline))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AboutMetaRow(label = "DEVELOPED BY", value = AppMetadata.DEVELOPER_NAME)
                            AboutMetaRow(label = "GITHUB", value = AppMetadata.GITHUB_REPO_URL)
                            AboutMetaRow(label = "PLATFORM", value = "Android TV · Jetpack Compose · Media3 + FFmpeg")
                        }

                        Box(Modifier.fillMaxWidth().height(1.dp).background(DividerHairline))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsPill(
                                label = if (settingsUiState.isCheckingUpdate) "Checking Updates..." else "Check for Updates",
                                selected = false,
                                onClick = onCheckForUpdates,
                            )
                            val updateMsg = settingsUiState.updateStatusMessage
                            if (!updateMsg.isNullOrBlank()) {
                                Text(
                                    text = updateMsg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = PureWhite,
        )
    }
}

@Composable
private fun SettingGroup(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceTonal)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = PureWhite)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
        }
        content()
    }
}

@Composable
private fun SettingsPill(
    label: String,
    selected: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) SurfaceSelected else SurfaceElevated,
            contentColor = when {
                selected -> PureWhite
                destructive -> InkPrimary
                else -> InkSecondary
            },
            focusedContainerColor = if (destructive) SignalRed else PureWhite,
            focusedContentColor = if (destructive) PureWhite else InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (selected) Border(BorderStroke(1.5.dp, SelectedBorderRed), shape = shape) else Border.None,
            focusedBorder = Border(
                BorderStroke(2.dp, if (destructive) FocusBorderWhite else SignalRed),
                shape = shape,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}
