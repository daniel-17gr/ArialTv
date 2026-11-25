package com.example.arialtv.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.KeyEvent
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.arialtv.data.model.AppMetadata
import com.example.arialtv.data.model.AppSettings
import com.example.arialtv.data.model.AspectRatioMode
import com.example.arialtv.data.model.EpisodeProgress
import com.example.arialtv.data.model.SubtitleBackground
import com.example.arialtv.data.model.SubtitleColor
import com.example.arialtv.data.model.SubtitlePosition
import com.example.arialtv.data.model.SubtitleSize
import com.example.arialtv.data.model.SubtitleStyle
import com.example.arialtv.data.moviebox.MovieBoxConfig
import com.example.arialtv.data.moviebox.SubtitleShiftHelper
import com.example.arialtv.data.storage.AppPreferences
import com.example.arialtv.ui.theme.InkMuted
import com.example.arialtv.ui.theme.InkOnSelected
import com.example.arialtv.ui.theme.InkOnWhite
import com.example.arialtv.ui.theme.InkPrimary
import com.example.arialtv.ui.theme.InkSecondary
import com.example.arialtv.ui.theme.PureWhite
import com.example.arialtv.ui.theme.SignalRed
import com.example.arialtv.ui.theme.SignalRedSoft
import com.example.arialtv.ui.theme.StatusGreen
import com.example.arialtv.ui.theme.SurfaceElevated
import com.example.arialtv.ui.theme.SurfaceSelected
import com.example.arialtv.ui.theme.SurfaceTonal
import com.example.arialtv.ui.theme.TrueBlack
import com.example.arialtv.ui.viewmodel.AppDestination
import com.example.arialtv.ui.viewmodel.PlayerUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import java.util.Locale
import java.util.concurrent.TimeUnit

private enum class PlayerDrawer { NONE, EPISODES, AUDIO, SUBTITLES, VOLUME, QUALITY, SPEED, SLEEP }

private data class EmbeddedAudioTrack(
    val trackGroup: TrackGroup,
    val trackIndex: Int,
    val label: String,
    val detail: String,
    val isSelected: Boolean,
)

private fun extractAudioTracks(tracks: Tracks): List<EmbeddedAudioTrack> {
    val result = mutableListOf<EmbeddedAudioTrack>()
    var counter = 1
    for (group in tracks.groups) {
        if (group.type != C.TRACK_TYPE_AUDIO) continue
        for (i in 0 until group.length) {
            if (!group.isTrackSupported(i)) continue
            val fmt = group.getTrackFormat(i)
            val langTag = fmt.language.orEmpty()
            val langName = when {
                !fmt.label.isNullOrBlank() -> fmt.label!!
                langTag.isNotBlank() && !langTag.equals("und", ignoreCase = true) -> {
                    Locale.forLanguageTag(langTag).getDisplayLanguage(Locale.ENGLISH)
                        .replaceFirstChar { it.uppercase() }
                        .ifBlank { langTag.uppercase() }
                }
                else -> "Audio Track $counter"
            }
            val channels = when (fmt.channelCount) {
                1 -> "Mono"
                2 -> "Stereo"
                6 -> "5.1"
                8 -> "7.1"
                else -> if (fmt.channelCount > 0) "${fmt.channelCount}ch" else ""
            }
            val detail = listOfNotNull(
                langTag.takeIf { it.isNotBlank() && !it.equals("und", ignoreCase = true) }?.uppercase(),
                channels.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            result.add(
                EmbeddedAudioTrack(
                    trackGroup = group.mediaTrackGroup,
                    trackIndex = i,
                    label = langName,
                    detail = detail,
                    isSelected = group.isTrackSelected(i),
                ),
            )
            counter++
        }
    }
    return result
}

private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@OptIn(UnstableApi::class)
private fun createSafeRenderersFactory(
    context: Context,
    preferSoftwareVideo: Boolean,
): DefaultRenderersFactory {
    return DefaultRenderersFactory(context)
        // Enable Jellyfin Media3 FFmpeg audio extension decoders (AC3, E-AC3, DTS, TrueHD, FLAC, etc.)
        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        // Automatically fall back to secondary/software codecs when a hardware codec fails
        .setEnableDecoderFallback(true)
        .apply {
            if (preferSoftwareVideo) {
                setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                    val all = MediaCodecSelector.DEFAULT.getDecoderInfos(
                        mimeType,
                        requiresSecureDecoder,
                        requiresTunnelingDecoder,
                    )
                    if (mimeType.startsWith("video/", ignoreCase = true) && all.size > 1) {
                        val (sw, hw) = all.partition { isSoftwareDecoder(it) }
                        if (sw.isNotEmpty()) sw + hw else all
                    } else {
                        all
                    }
                }
            }
        }
}

@OptIn(UnstableApi::class)
private fun isSoftwareDecoder(info: MediaCodecInfo): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info.softwareOnly) return true
    val name = info.name.lowercase(Locale.US)
    return name.startsWith("omx.google.") ||
        name.startsWith("c2.android.") ||
        name.contains(".sw.") ||
        name.contains("ffmpeg")
}

private fun isDecoderError(error: PlaybackException): Boolean {
    val code = error.errorCode
    if (code == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
        code == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ||
        code == PlaybackException.ERROR_CODE_DECODING_FAILED ||
        code == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES ||
        code == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ||
        code == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
        code == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED
    ) {
        return true
    }
    val msg = (error.message.orEmpty() + " " + error.cause?.message.orEmpty()).lowercase(Locale.US)
    return msg.contains("decoder") || msg.contains("codec") || msg.contains("mediacodec")
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    destination: AppDestination.Player,
    playerState: PlayerUiState,
    settings: AppSettings,
    episodeProgress: Map<String, EpisodeProgress>,
    sleepTimerMinutes: Int,
    sleepTimerRemainingSecFlow: StateFlow<Int>,
    sleepTimerExpired: Boolean,
    onSelectQuality: (Int) -> Unit,
    onSelectAudioDub: (String, Long) -> Unit,
    onSelectSubtitle: (String?) -> Unit,
    onUpdateSubtitleStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onUpdateAspectRatio: (AspectRatioMode) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onDismissSleepExpired: () -> Unit,
    onPlayEpisode: (Int, Int) -> Unit,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onRetry: () -> Unit,
    onSaveProgress: (Long, Long) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember(context) {
        runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull()
    }

    val episodeKey = "${destination.item.id}_${destination.season}_${destination.episode}"

    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isEnded by remember { mutableStateOf(false) }
    var upNextCountdown by remember { mutableIntStateOf(10) }
    var upNextCancelled by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var recoveryToast by remember { mutableStateOf<String?>(null) }
    var embeddedAudioTracks by remember(episodeKey) { mutableStateOf<List<EmbeddedAudioTrack>>(emptyList()) }

    // Software decoder fallback state (rebuilds ExoPlayer cleanly without crashing)
    var preferSoftwareDecoder by remember(episodeKey) { mutableStateOf(false) }
    var autoSoftwareTried by remember(episodeKey) { mutableStateOf(false) }
    val triedStreamUrls = remember(episodeKey) { mutableSetOf<String>() }

    var currentPositionMs by remember { mutableLongStateOf(destination.startPositionMs) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(settings.defaultPlaybackSpeed) }
    var aspectMode by remember { mutableStateOf(settings.aspectRatioMode) }

    // Live Volume State (works both via Android AudioManager & in-player gain so HDMI-CEC TVs work too)
    val initialSysVol = remember(audioManager) {
        runCatching {
            val am = audioManager ?: return@runCatching 1.0f
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            (cur.toFloat() / max.toFloat()).coerceIn(0f, 1f)
        }.getOrDefault(1.0f)
    }
    var volumeLevel by remember { mutableFloatStateOf(initialSysVol.coerceAtLeast(0.1f)) }
    var isMuted by remember { mutableStateOf(initialSysVol == 0f) }
    var preMuteVolume by remember { mutableFloatStateOf(initialSysVol.coerceAtLeast(0.5f)) }

    var loadedEpisodeKey by remember { mutableStateOf("") }
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var activeDrawer by remember { mutableStateOf(PlayerDrawer.NONE) }
    var drawerSeason by remember(destination.season) {
        mutableIntStateOf(destination.season.takeIf { it > 0 } ?: 1)
    }

    // YouTube-style seek ripple & brightness/volume swipe gauges
    var seekDeltaAccumSec by remember { mutableIntStateOf(0) }
    var seekIsForward by remember { mutableStateOf(true) }
    var seekRippleTick by remember { mutableIntStateOf(0) }
    var gestureHudLabel by remember { mutableStateOf<String?>(null) }
    var gestureHudFraction by remember { mutableFloatStateOf(0f) }
    var gestureHudIsVolume by remember { mutableStateOf(true) }
    var stageSize by remember { mutableStateOf(IntSize(1280, 720)) }
    var scrubBarFocused by remember { mutableStateOf(false) }
    var scrubBarWidthPx by remember { mutableFloatStateOf(1f) }

    val stageFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val drawerFocusRequester = remember { FocusRequester() }
    val endedFocusRequester = remember { FocusRequester() }
    val errorFocusRequester = remember { FocusRequester() }
    val sleepFocusRequester = remember { FocusRequester() }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    val latestPlayerState by rememberUpdatedState(playerState)
    val latestOnSelectQuality by rememberUpdatedState(onSelectQuality)

    // Keep screen awake during playback
    DisposableEffect(activity) {
        runCatching { activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        onDispose {
            runCatching { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
    }

    val streamOkHttp = remember {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    val okHttpDataSourceFactory = remember(streamOkHttp) {
        OkHttpDataSource.Factory(streamOkHttp)
            .setUserAgent(MovieBoxConfig.CDN_USER_AGENT)
    }

    val mediaDataSourceFactory = remember(context, okHttpDataSourceFactory) {
        DefaultDataSource.Factory(context, okHttpDataSourceFactory)
    }

    // Build resilient ExoPlayer with FFmpeg audio decoders + decoder fallback + optional software video mode
    val exoPlayer = remember(context, mediaDataSourceFactory, preferSoftwareDecoder) {
        val renderersFactory = createSafeRenderersFactory(
            context = context,
            preferSoftwareVideo = preferSoftwareDecoder,
        )
        runCatching {
            ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(DefaultMediaSourceFactory(mediaDataSourceFactory))
                .setSeekBackIncrementMs(10_000L)
                .setSeekForwardIncrementMs(10_000L)
                .build()
                .apply {
                    playWhenReady = true
                }
        }.getOrElse {
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(mediaDataSourceFactory))
                .build()
                .apply {
                    playWhenReady = true
                }
        }
    }

    fun applyVolume(newLevel: Float, muted: Boolean = false, showHud: Boolean = false) {
        val clamped = newLevel.coerceIn(0f, 1f)
        isMuted = muted || clamped <= 0.001f
        volumeLevel = if (isMuted) 0f else clamped
        if (!isMuted) {
            preMuteVolume = clamped
        }
        runCatching {
            exoPlayer.volume = if (isMuted) 0f else volumeLevel
        }
        runCatching {
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val targetVol = if (isMuted) 0 else (volumeLevel * maxVol).toInt().coerceIn(0, maxVol)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            }
        }
        if (showHud) {
            gestureHudIsVolume = true
            gestureHudFraction = if (isMuted) 0f else volumeLevel
            gestureHudLabel = if (isMuted) "Muted" else "${(volumeLevel * 100).toInt()}%"
        }
    }

    // Keep ExoPlayer gain synced when rebuilt
    LaunchedEffect(exoPlayer, isMuted, volumeLevel) {
        runCatching { exoPlayer.volume = if (isMuted) 0f else volumeLevel }
    }

    // Pause cleanly if sleep timer expires
    LaunchedEffect(sleepTimerExpired, exoPlayer) {
        if (sleepTimerExpired) {
            runCatching { exoPlayer.pause() }
            controlsVisible = true
        }
    }

    // Apply playback speed live
    LaunchedEffect(playbackSpeed, exoPlayer) {
        runCatching { exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed) }
    }

    // Clear transient recovery notice after 3s
    LaunchedEffect(recoveryToast) {
        if (recoveryToast != null) {
            delay(3_000L)
            recoveryToast = null
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: Tracks) {
                embeddedAudioTracks = runCatching { extractAudioTracks(tracks) }.getOrDefault(emptyList())
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                isEnded = playbackState == Player.STATE_ENDED
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                    durationMs = runCatching { exoPlayer.duration.coerceAtLeast(0L) }.getOrDefault(0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    upNextCountdown = 10
                    upNextCancelled = false
                    controlsVisible = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                runCatching {
                    isBuffering = false
                    isEnded = false
                    val pb = latestPlayerState.playback
                    val curUrl = pb?.selectedStream?.url
                    if (!curUrl.isNullOrBlank()) {
                        triedStreamUrls.add(curUrl)
                    }

                    val codecProblem = isDecoderError(error)

                    // 1. Automatic failover to software video decoder on codec failure
                    if (codecProblem && !preferSoftwareDecoder && !autoSoftwareTried) {
                        autoSoftwareTried = true
                        recoveryToast = "Switching to compatibility decoder..."
                        playbackError = null
                        preferSoftwareDecoder = true
                        return@runCatching
                    }

                    // 2. Automatic failover to next untried stream (preferring H.264 first)
                    val streams = pb?.availableStreams.orEmpty()
                    if (streams.size > 1) {
                        val untriedIndexed = streams.mapIndexedNotNull { idx, stream ->
                            if (stream.url !in triedStreamUrls) idx to stream else null
                        }
                        val preferredFallback = untriedIndexed.firstOrNull { (_, s) ->
                            val c = s.codec.lowercase(Locale.US)
                            c == "h264" || c == "avc"
                        } ?: untriedIndexed.firstOrNull()

                        if (preferredFallback != null) {
                            val (nextIdx, nextStream) = preferredFallback
                            recoveryToast = "Switching stream (${nextStream.qualityLabel} ${nextStream.codecLabel})..."
                            playbackError = null
                            latestOnSelectQuality(nextIdx)
                            return@runCatching
                        }
                    }

                    // 3. Exhausted automatic recovery — surface focusable recovery dialog
                    playbackError = if (codecProblem) {
                        "This video format couldn't be decoded on your TV. Try another quality or compatibility mode."
                    } else {
                        "Playback interrupted. Try retrying or switching quality."
                    }
                    controlsVisible = true
                }.onFailure {
                    playbackError = "Playback interrupted. Try retrying."
                    controlsVisible = true
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            runCatching {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                if (pos > 3_000L && dur > 0L) {
                    onSaveProgress(pos, dur)
                }
            }
            if (playerViewRef?.player === exoPlayer) {
                runCatching { playerViewRef?.player = null }
            }
            runCatching { exoPlayer.removeListener(listener) }
            runCatching { exoPlayer.stop() }
            runCatching { exoPlayer.release() }
        }
    }

    // Load stream + subtitle track cleanly (supports live subtitle sync offset & software decoder rebuilds)
    val playback = playerState.playback
    val selectedStream = playback?.selectedStream
    val selectedSubtitle = playback?.subtitles?.firstOrNull { it.id == playerState.selectedSubtitleId }
    var subtitleOffsetMs by remember(episodeKey, selectedSubtitle?.id) { mutableLongStateOf(0L) }

    LaunchedEffect(exoPlayer, episodeKey, selectedStream?.url, selectedSubtitle?.url, subtitleOffsetMs) {
        val stream = selectedStream ?: return@LaunchedEffect
        playbackError = null
        isEnded = false
        upNextCancelled = false
        isBuffering = true

        runCatching {
            okHttpDataSourceFactory.setDefaultRequestProperties(stream.headers)

            val resumeAt = when {
                loadedEpisodeKey == episodeKey && currentPositionMs > 3_000L -> currentPositionMs
                loadedEpisodeKey == episodeKey && exoPlayer.currentPosition > 3_000L -> exoPlayer.currentPosition
                else -> destination.startPositionMs
            }
            loadedEpisodeKey = episodeKey
            currentPositionMs = resumeAt

            val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(stream.url))
            if (stream.url.contains(".m3u8", ignoreCase = true)) {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            if (selectedSubtitle != null && selectedSubtitle.url.isNotBlank()) {
                val shiftedFile = if (subtitleOffsetMs != 0L) {
                    SubtitleShiftHelper.getShiftedSubtitleFile(
                        context = context,
                        url = selectedSubtitle.url,
                        offsetMs = subtitleOffsetMs,
                        httpClient = streamOkHttp,
                    )
                } else {
                    null
                }
                val subUri = if (shiftedFile != null) Uri.fromFile(shiftedFile) else Uri.parse(selectedSubtitle.url)
                val subMime = if (subUri.toString().contains(".vtt", ignoreCase = true)) {
                    MimeTypes.TEXT_VTT
                } else {
                    MimeTypes.APPLICATION_SUBRIP
                }
                val subConfig = MediaItem.SubtitleConfiguration.Builder(subUri)
                    .setMimeType(subMime)
                    .setLanguage(selectedSubtitle.languageCode.ifBlank { "en" })
                    .setLabel(selectedSubtitle.languageName)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
            }

            exoPlayer.setMediaItem(mediaItemBuilder.build(), resumeAt)
            exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
            exoPlayer.volume = if (isMuted) 0f else volumeLevel
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }.onFailure {
            isBuffering = false
            playbackError = "Couldn't open this video stream. Try retrying or switching quality."
            controlsVisible = true
        }
    }

    // Periodic position + watch progress save
    LaunchedEffect(exoPlayer) {
        var tick = 0
        while (true) {
            runCatching {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                if (dur > 0L || pos > 0L) {
                    currentPositionMs = pos
                    durationMs = dur
                    bufferedPositionMs = buf
                }
            }
            tick++
            if (tick % 20 == 0 && currentPositionMs > 3_000L && durationMs > 0L) {
                onSaveProgress(currentPositionMs, durationMs)
            }
            delay(400L)
        }
    }

    // Auto-play next episode 10-second YouTube TV countdown
    LaunchedEffect(isEnded, upNextCancelled, settings.autoPlayNextEpisode) {
        if (isEnded && destination.item.isSeries && settings.autoPlayNextEpisode && !upNextCancelled) {
            upNextCountdown = 10
            while (upNextCountdown > 0 && !upNextCancelled) {
                delay(1_000L)
                upNextCountdown--
            }
            if (!upNextCancelled) {
                onNextEpisode()
            }
        }
    }

    val effectiveError = playerState.error ?: playbackError

    // Auto-hide OSD based on user's configured timeout
    LaunchedEffect(controlsVisible, lastInteractionMs, isPlaying, activeDrawer, effectiveError, isEnded, sleepTimerExpired, settings.controlsTimeoutSeconds) {
        val timeoutSec = settings.controlsTimeoutSeconds
        if (timeoutSec > 0 && controlsVisible && isPlaying && activeDrawer == PlayerDrawer.NONE && effectiveError == null && !isEnded && !sleepTimerExpired) {
            val timeoutMs = timeoutSec * 1_000L
            delay(timeoutMs)
            if (System.currentTimeMillis() - lastInteractionMs >= timeoutMs - 200L) {
                controlsVisible = false
                runCatching { stageFocusRequester.requestFocus() }
            }
        }
    }

    // Clear seek ripple after 950ms
    LaunchedEffect(seekRippleTick) {
        if (seekDeltaAccumSec != 0) {
            delay(950L)
            seekDeltaAccumSec = 0
        }
    }

    // Clear brightness/volume swipe HUD
    LaunchedEffect(gestureHudLabel, gestureHudFraction) {
        if (gestureHudLabel != null) {
            delay(1_100L)
            gestureHudLabel = null
        }
    }

    // D-pad focus choreography — guarantees Error Dialog, Sleep Dialog, Ended Dialog, Drawers, and Controls ALWAYS receive focus!
    LaunchedEffect(controlsVisible, activeDrawer, isEnded, effectiveError, sleepTimerExpired) {
        delay(80)
        when {
            effectiveError != null -> runCatching { errorFocusRequester.requestFocus() }
            sleepTimerExpired -> runCatching { sleepFocusRequester.requestFocus() }
            isEnded -> runCatching { endedFocusRequester.requestFocus() }
            activeDrawer != PlayerDrawer.NONE -> runCatching { drawerFocusRequester.requestFocus() }
            controlsVisible -> runCatching { playPauseFocusRequester.requestFocus() }
        }
    }

    fun wakeControls() {
        lastInteractionMs = System.currentTimeMillis()
        controlsVisible = true
    }

    fun seekBySeconds(deltaSec: Int) {
        runCatching {
            val deltaMs = deltaSec * 1_000L
            val cur = exoPlayer.currentPosition.coerceAtLeast(0L)
            val maxDur = exoPlayer.duration.coerceAtLeast(cur + deltaMs).coerceAtLeast(0L)
            val target = (cur + deltaMs).coerceIn(0L, maxDur)
            exoPlayer.seekTo(target)
            currentPositionMs = target
            val forward = deltaSec > 0
            seekDeltaAccumSec = if (seekIsForward == forward) seekDeltaAccumSec + kotlin.math.abs(deltaSec) else kotlin.math.abs(deltaSec)
            seekIsForward = forward
            seekRippleTick++
            lastInteractionMs = System.currentTimeMillis()
        }
    }

    val modalBlockingStage = effectiveError != null || isEnded || sleepTimerExpired

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TrueBlack)
            .onSizeChanged { stageSize = it }
            .focusRequester(stageFocusRequester)
            .focusable()
            // YouTube touch gestures: single tap = toggle OSD, double-tap L/R = seek -10/+10s
            .pointerInput(modalBlockingStage) {
                detectTapGestures(
                    onTap = {
                        if (modalBlockingStage) return@detectTapGestures
                        if (activeDrawer != PlayerDrawer.NONE) {
                            activeDrawer = PlayerDrawer.NONE
                            wakeControls()
                        } else if (controlsVisible) {
                            controlsVisible = false
                        } else {
                            wakeControls()
                        }
                    },
                    onDoubleTap = { offset ->
                        if (modalBlockingStage) return@detectTapGestures
                        val width = stageSize.width.coerceAtLeast(1)
                        if (offset.x < width * 0.45f) {
                            seekBySeconds(-10)
                        } else if (offset.x > width * 0.55f) {
                            seekBySeconds(10)
                        } else {
                            runCatching {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                            wakeControls()
                        }
                    },
                )
            }
            // YouTube vertical swipe gestures: left edge = brightness, right edge = volume
            .pointerInput(activity, audioManager, modalBlockingStage) {
                var dragStartX = 0f
                var brightnessLevel = runCatching {
                    activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f }
                }.getOrNull() ?: 0.5f
                detectVerticalDragGestures(
                    onDragStart = { start ->
                        dragStartX = start.x
                        val curB = runCatching { activity?.window?.attributes?.screenBrightness }.getOrNull() ?: -1f
                        if (curB >= 0f) brightnessLevel = curB
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (modalBlockingStage) return@detectVerticalDragGestures
                        change.consume()
                        val width = stageSize.width.coerceAtLeast(1)
                        val height = stageSize.height.coerceAtLeast(1).toFloat()
                        val deltaFraction = -dragAmount / (height * 0.65f)

                        if (dragStartX < width * 0.5f && activity != null) {
                            runCatching {
                                brightnessLevel = (brightnessLevel + deltaFraction).coerceIn(0.05f, 1.0f)
                                val lp = activity.window.attributes
                                lp.screenBrightness = brightnessLevel
                                activity.window.attributes = lp
                                gestureHudIsVolume = false
                                gestureHudFraction = brightnessLevel
                                gestureHudLabel = "${(brightnessLevel * 100).toInt()}%"
                            }
                        } else {
                            val base = if (isMuted) 0f else volumeLevel
                            applyVolume(base + deltaFraction, muted = false, showHud = true)
                        }
                    },
                )
            }
            // YouTube TV D-pad remote controls
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_BACK -> {
                        if (activeDrawer != PlayerDrawer.NONE) {
                            activeDrawer = PlayerDrawer.NONE
                            wakeControls()
                            true
                        } else if (controlsVisible && isPlaying && !modalBlockingStage) {
                            controlsVisible = false
                            true
                        } else {
                            runCatching { onSaveProgress(exoPlayer.currentPosition, exoPlayer.duration) }
                            onExit()
                            true
                        }
                    }
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        val base = if (isMuted) 0f else volumeLevel
                        applyVolume(base + 0.1f, muted = false, showHud = true)
                        true
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        val base = if (isMuted) 0f else volumeLevel
                        applyVolume(base - 0.1f, muted = false, showHud = true)
                        true
                    }
                    KeyEvent.KEYCODE_VOLUME_MUTE -> {
                        if (isMuted) applyVolume(preMuteVolume.coerceAtLeast(0.4f), muted = false, showHud = true)
                        else applyVolume(0f, muted = true, showHud = true)
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (!modalBlockingStage) {
                            runCatching { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }
                            wakeControls()
                            true
                        } else {
                            false
                        }
                    }
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        if (!modalBlockingStage) {
                            seekBySeconds(10)
                            true
                        } else {
                            false
                        }
                    }
                    KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        if (!modalBlockingStage) {
                            seekBySeconds(-10)
                            true
                        } else {
                            false
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (!modalBlockingStage && (!controlsVisible || scrubBarFocused)) {
                            seekBySeconds(-10)
                            true
                        } else {
                            lastInteractionMs = System.currentTimeMillis()
                            false
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (!modalBlockingStage && (!controlsVisible || scrubBarFocused)) {
                            seekBySeconds(10)
                            true
                        } else {
                            lastInteractionMs = System.currentTimeMillis()
                            false
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE -> {
                        if (!modalBlockingStage && !controlsVisible) {
                            runCatching { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }
                            wakeControls()
                            true
                        } else {
                            lastInteractionMs = System.currentTimeMillis()
                            false
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (!modalBlockingStage && !controlsVisible) {
                            if (destination.item.isSeries && destination.seasonsMeta.isNotEmpty()) {
                                activeDrawer = PlayerDrawer.EPISODES
                            }
                            wakeControls()
                            true
                        } else {
                            lastInteractionMs = System.currentTimeMillis()
                            false
                        }
                    }
                    KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO -> {
                        if (!modalBlockingStage) {
                            wakeControls()
                            activeDrawer = if (activeDrawer != PlayerDrawer.NONE) {
                                PlayerDrawer.NONE
                            } else if (destination.item.isSeries && destination.seasonsMeta.isNotEmpty()) {
                                PlayerDrawer.EPISODES
                            } else {
                                PlayerDrawer.SUBTITLES
                            }
                            true
                        } else {
                            false
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (!modalBlockingStage && !controlsVisible) {
                            wakeControls()
                            true
                        } else {
                            lastInteractionMs = System.currentTimeMillis()
                            false
                        }
                    }
                    else -> false
                }
            },
    ) {
        // Media3 Video Surface + Live Customized Subtitles
        val subStyle = settings.subtitleStyle
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    playerViewRef = this
                }
            },
            update = { view ->
                playerViewRef = view
                runCatching {
                    if (view.player !== exoPlayer) view.player = exoPlayer
                    view.resizeMode = when (aspectMode) {
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                    view.subtitleView?.apply {
                        setApplyEmbeddedStyles(false)
                        setApplyEmbeddedFontSizes(false)
                        setFractionalTextSize(subStyle.size.fraction)
                        setBottomPaddingFraction(subStyle.position.bottomPaddingFraction)
                        val edgeType = if (subStyle.background == SubtitleBackground.NONE) {
                            CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
                        } else {
                            CaptionStyleCompat.EDGE_TYPE_NONE
                        }
                        setStyle(
                            CaptionStyleCompat(
                                subStyle.color.argb,
                                subStyle.background.argb,
                                android.graphics.Color.TRANSPARENT,
                                edgeType,
                                android.graphics.Color.BLACK,
                                Typeface.DEFAULT_BOLD,
                            ),
                        )
                    }
                }
            },
            onRelease = { view ->
                runCatching { view.player = null }
                if (playerViewRef === view) playerViewRef = null
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Quiet Buffering / Auto-Recovery Indicator
        if ((playerState.isLoading || isBuffering || recoveryToast != null) && !modalBlockingStage) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xC80A0A0A))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = recoveryToast?.uppercase()
                        ?: if (playerState.isLoading) "LOADING" else "BUFFERING",
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite,
                )
            }
        }

        // YouTube-style Side Seek Ripple Overlay (« 10s / 10s »)
        AnimatedVisibility(
            visible = seekDeltaAccumSec > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(if (seekIsForward) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 42.dp),
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xA6000000))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = if (seekIsForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = if (seekIsForward) "+${seekDeltaAccumSec}s" else "-${seekDeltaAccumSec}s",
                    style = MaterialTheme.typography.labelMedium,
                    color = PureWhite,
                )
            }
        }

        // Brightness / Volume HUD Pill
        AnimatedVisibility(
            visible = gestureHudLabel != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xD40A0A0A))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val hudIcon = when {
                    !gestureHudIsVolume -> Icons.Rounded.Brightness6
                    gestureHudFraction <= 0.001f -> Icons.AutoMirrored.Rounded.VolumeOff
                    gestureHudFraction < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                    else -> Icons.AutoMirrored.Rounded.VolumeUp
                }
                Icon(
                    imageVector = hudIcon,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp),
                )
                Box(
                    modifier = Modifier
                        .width(104.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x38FFFFFF)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(gestureHudFraction.coerceIn(0f, 1f))
                            .height(5.dp)
                            .background(if (gestureHudIsVolume) SignalRed else PureWhite),
                    )
                }
                Text(
                    text = gestureHudLabel.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite,
                )
            }
        }

        // Sleep Timer Reached Modal (auto-focused!)
        if (sleepTimerExpired) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(SurfaceTonal)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = SignalRed, modifier = Modifier.size(30.dp))
                    Text("SLEEP TIMER", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
                    Text("Playback paused.", style = MaterialTheme.typography.headlineMedium, color = PureWhite)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 6.dp)) {
                        PlayerPillButton(
                            label = "+30 min",
                            icon = Icons.Rounded.PlayArrow,
                            primary = true,
                            modifier = Modifier.focusRequester(sleepFocusRequester),
                            onClick = {
                                onSetSleepTimer(30)
                                runCatching { exoPlayer.play() }
                            },
                        )
                        PlayerPillButton(
                            label = "Exit",
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onClick = {
                                onDismissSleepExpired()
                                onExit()
                            },
                        )
                    }
                }
            }
        }

        // YouTube TV "Up Next" / Playback Ended Overlay
        if (isEnded && effectiveError == null && !sleepTimerExpired) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xC8000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(SurfaceTonal)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val kicker = if (destination.item.isSeries && settings.autoPlayNextEpisode && !upNextCancelled) {
                        "UP NEXT IN ${String.format(Locale.US, "%02d", upNextCountdown)}s"
                    } else if (destination.item.isSeries) {
                        "EPISODE COMPLETE"
                    } else {
                        "PLAYBACK COMPLETE"
                    }
                    Text(kicker, style = MaterialTheme.typography.labelMedium, color = InkSecondary)

                    Text(
                        text = playback?.title?.ifBlank { destination.item.title } ?: destination.item.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (destination.item.isSeries) {
                            PlayerPillButton(
                                label = "Next Episode",
                                icon = Icons.Rounded.SkipNext,
                                primary = true,
                                modifier = Modifier.focusRequester(endedFocusRequester),
                                onClick = {
                                    isEnded = false
                                    onNextEpisode()
                                },
                            )
                            PlayerPillButton(
                                label = "Replay",
                                icon = Icons.Rounded.Replay,
                                onClick = {
                                    upNextCancelled = true
                                    isEnded = false
                                    runCatching {
                                        exoPlayer.seekTo(0L)
                                        exoPlayer.play()
                                    }
                                },
                            )
                        } else {
                            PlayerPillButton(
                                label = "Replay",
                                icon = Icons.Rounded.Replay,
                                primary = true,
                                modifier = Modifier.focusRequester(endedFocusRequester),
                                onClick = {
                                    isEnded = false
                                    runCatching {
                                        exoPlayer.seekTo(0L)
                                        exoPlayer.play()
                                    }
                                },
                            )
                        }
                        PlayerPillButton(
                            label = "Exit",
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onClick = {
                                upNextCancelled = true
                                onExit()
                            },
                        )
                    }
                }
            }
        }

        // Auto-Focused Error Recovery Panel (fully navigable with TV remote!)
        if (effectiveError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDC000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(SurfaceTonal)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SignalRedSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = SignalRed, modifier = Modifier.size(24.dp))
                    }
                    Text("PLAYBACK ISSUE", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
                    Text(effectiveError, style = MaterialTheme.typography.bodyLarge, color = InkPrimary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        PlayerPillButton(
                            label = "Retry",
                            icon = Icons.Rounded.Refresh,
                            primary = true,
                            modifier = Modifier.focusRequester(errorFocusRequester),
                            onClick = {
                                triedStreamUrls.clear()
                                playbackError = null
                                onRetry()
                            },
                        )
                        if (!preferSoftwareDecoder) {
                            PlayerPillButton(
                                label = "Soft Decoder",
                                icon = Icons.Rounded.Tune,
                                onClick = {
                                    playbackError = null
                                    preferSoftwareDecoder = true
                                },
                            )
                        }
                        val streams = playback?.availableStreams.orEmpty()
                        if (streams.size > 1) {
                            val nextIdx = (playerState.activeStreamIndex + 1) % streams.size
                            PlayerPillButton(
                                label = streams[nextIdx].qualityLabel,
                                icon = Icons.Rounded.HighQuality,
                                onClick = {
                                    playbackError = null
                                    onSelectQuality(nextIdx)
                                },
                            )
                        }
                        PlayerPillButton(
                            label = "Back",
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onClick = onExit,
                        )
                    }
                }
            }
        }

        // YouTube TV Matte OSD Overlay
        AnimatedVisibility(
            visible = controlsVisible && !modalBlockingStage,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color(0xD8000000), Color.Transparent)))
                        .statusBarsPadding()
                        .padding(horizontal = 32.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        PlayerCircleButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            onClick = {
                                runCatching { onSaveProgress(exoPlayer.currentPosition, exoPlayer.duration) }
                                onExit()
                            },
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = playback?.title?.ifBlank { destination.item.title } ?: destination.item.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = PureWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val subParts = buildList {
                                if (destination.season > 0 && destination.episode > 0) {
                                    add(String.format(Locale.US, "S%02d · E%02d", destination.season, destination.episode))
                                }
                                selectedStream?.qualityLabel?.let { add(it) }
                                playback?.activeAudioLabel?.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
                                if (preferSoftwareDecoder) add("SW")
                                if (playbackSpeed != 1.0f) add("${playbackSpeed}x")
                                if (selectedSubtitle != null) add("CC ${selectedSubtitle.languageCode.uppercase()}")
                            }
                            if (subParts.isNotEmpty()) {
                                Text(
                                    text = subParts.joinToString("  ·  "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSecondary,
                                )
                            }
                        }
                    }

                    PlayerSleepBadge(sleepTimerRemainingSecFlow = sleepTimerRemainingSecFlow)
                }

                // Bottom YouTube TV Transport Stack
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEC000000))))
                        .padding(horizontal = 32.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${formatPlayerTime(currentPositionMs)}  /  ${formatPlayerTime(durationMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite,
                        )
                        if (scrubBarFocused) {
                            Text(
                                text = "←  SCRUB 10s  →",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary,
                            )
                        }
                    }

                    // Interactive Signal Red Scrub Bar
                    val fraction = if (durationMs > 0L) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                    val bufFraction = if (durationMs > 0L) (bufferedPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .onSizeChanged { scrubBarWidthPx = it.width.coerceAtLeast(1).toFloat() }
                            .onFocusChanged { scrubBarFocused = it.isFocused }
                            .focusable()
                            .pointerInput(durationMs) {
                                detectTapGestures { offset ->
                                    if (durationMs > 0L) {
                                        val target = ((offset.x / scrubBarWidthPx).coerceIn(0f, 1f) * durationMs).toLong()
                                        runCatching { exoPlayer.seekTo(target) }
                                        currentPositionMs = target
                                        wakeControls()
                                    }
                                }
                            }
                            .pointerInput(durationMs) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    if (durationMs > 0L) {
                                        val target = ((change.position.x / scrubBarWidthPx).coerceIn(0f, 1f) * durationMs).toLong()
                                        runCatching { exoPlayer.seekTo(target) }
                                        currentPositionMs = target
                                        wakeControls()
                                    }
                                }
                            },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        val barH = if (scrubBarFocused) 7.dp else 4.5.dp
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barH)
                                .clip(RoundedCornerShape(50))
                                .background(if (scrubBarFocused) Color(0x66FFFFFF) else Color(0x3DFFFFFF)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(bufFraction)
                                    .height(barH)
                                    .background(Color(0x55FFFFFF)),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(barH)
                                    .background(SignalRed),
                            )
                        }
                    }

                    // Action Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left Transport
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlayerCircleButton(
                                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                primary = true,
                                modifier = Modifier.focusRequester(playPauseFocusRequester),
                                onClick = {
                                    runCatching { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }
                                    wakeControls()
                                },
                            )

                            if (destination.item.isSeries) {
                                if (destination.episode > 1 || destination.season > 1) {
                                    PlayerCircleButton(
                                        icon = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Previous Episode",
                                        onClick = {
                                            runCatching { onSaveProgress(exoPlayer.currentPosition, exoPlayer.duration) }
                                            onPreviousEpisode()
                                        },
                                    )
                                }
                                PlayerCircleButton(
                                    icon = Icons.Rounded.SkipNext,
                                    contentDescription = "Next Episode",
                                    onClick = {
                                        runCatching { onSaveProgress(exoPlayer.currentPosition, exoPlayer.duration) }
                                        onNextEpisode()
                                    },
                                )
                                if (destination.seasonsMeta.isNotEmpty()) {
                                    PlayerPillButton(
                                        label = "Episodes",
                                        icon = Icons.Rounded.VideoLibrary,
                                        active = activeDrawer == PlayerDrawer.EPISODES,
                                        onClick = {
                                            wakeControls()
                                            activeDrawer = if (activeDrawer == PlayerDrawer.EPISODES) PlayerDrawer.NONE else PlayerDrawer.EPISODES
                                        },
                                    )
                                }
                            }
                        }

                        // Right Utility Controls (Volume, CC + Style, Speed, Quality, Aspect, Sleep Timer)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val volIcon = when {
                                isMuted || volumeLevel <= 0.001f -> Icons.AutoMirrored.Rounded.VolumeOff
                                volumeLevel < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                                else -> Icons.AutoMirrored.Rounded.VolumeUp
                            }
                            PlayerCircleButton(
                                icon = volIcon,
                                contentDescription = "Volume",
                                active = isMuted || activeDrawer == PlayerDrawer.VOLUME,
                                onClick = {
                                    wakeControls()
                                    activeDrawer = if (activeDrawer == PlayerDrawer.VOLUME) PlayerDrawer.NONE else PlayerDrawer.VOLUME
                                },
                            )
                            val hasMultipleAudio = (playback?.dubs?.size ?: 0) > 1 || embeddedAudioTracks.size > 1
                            PlayerCircleButton(
                                icon = Icons.Rounded.RecordVoiceOver,
                                contentDescription = "Audio Language",
                                active = activeDrawer == PlayerDrawer.AUDIO || hasMultipleAudio,
                                onClick = {
                                    wakeControls()
                                    activeDrawer = if (activeDrawer == PlayerDrawer.AUDIO) PlayerDrawer.NONE else PlayerDrawer.AUDIO
                                },
                            )
                            PlayerCircleButton(
                                icon = Icons.Rounded.ClosedCaption,
                                contentDescription = "Subtitles",
                                active = selectedSubtitle != null || activeDrawer == PlayerDrawer.SUBTITLES,
                                onClick = {
                                    wakeControls()
                                    activeDrawer = if (activeDrawer == PlayerDrawer.SUBTITLES) PlayerDrawer.NONE else PlayerDrawer.SUBTITLES
                                },
                            )
                            PlayerCircleButton(
                                icon = Icons.Rounded.Speed,
                                contentDescription = "Playback Speed",
                                active = playbackSpeed != 1.0f || activeDrawer == PlayerDrawer.SPEED,
                                onClick = {
                                    wakeControls()
                                    activeDrawer = if (activeDrawer == PlayerDrawer.SPEED) PlayerDrawer.NONE else PlayerDrawer.SPEED
                                },
                            )
                            if ((playback?.availableStreams?.size ?: 0) > 1) {
                                PlayerCircleButton(
                                    icon = Icons.Rounded.HighQuality,
                                    contentDescription = "Quality",
                                    active = activeDrawer == PlayerDrawer.QUALITY,
                                    onClick = {
                                        wakeControls()
                                        activeDrawer = if (activeDrawer == PlayerDrawer.QUALITY) PlayerDrawer.NONE else PlayerDrawer.QUALITY
                                    },
                                )
                            }
                            PlayerCircleButton(
                                icon = Icons.Rounded.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                active = aspectMode != AspectRatioMode.FIT,
                                onClick = {
                                    wakeControls()
                                    val next = AspectRatioMode.entries[(aspectMode.ordinal + 1) % AspectRatioMode.entries.size]
                                    aspectMode = next
                                    onUpdateAspectRatio(next)
                                },
                            )
                            PlayerCircleButton(
                                icon = Icons.Rounded.Bedtime,
                                contentDescription = "Sleep Timer",
                                active = sleepTimerMinutes > 0 || activeDrawer == PlayerDrawer.SLEEP,
                                onClick = {
                                    wakeControls()
                                    activeDrawer = if (activeDrawer == PlayerDrawer.SLEEP) PlayerDrawer.NONE else PlayerDrawer.SLEEP
                                },
                            )
                        }
                    }
                }
            }
        }

        // Early "Next Episode" Floating Pill during final 45 seconds of a TV series episode
        val remainingMs = (durationMs - currentPositionMs).coerceAtLeast(0L)
        val showEarlyNextEpisode = destination.item.isSeries &&
            durationMs > 120_000L &&
            remainingMs in 2_000L..45_000L &&
            !controlsVisible &&
            activeDrawer == PlayerDrawer.NONE &&
            !modalBlockingStage

        AnimatedVisibility(
            visible = showEarlyNextEpisode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 36.dp, bottom = 36.dp),
        ) {
            PlayerPillButton(
                label = "Next Episode",
                icon = Icons.Rounded.SkipNext,
                primary = true,
                onClick = {
                    runCatching { onSaveProgress(exoPlayer.currentPosition, exoPlayer.duration) }
                    onNextEpisode()
                },
            )
        }

        // Right Slide-Over Drawer (fast 110ms fade for low-RAM TVs)
        AnimatedVisibility(
            visible = activeDrawer != PlayerDrawer.NONE && !modalBlockingStage,
            enter = fadeIn(tween(110)),
            exit = fadeOut(tween(90)),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(344.dp)
                    .background(Color(0xF5121212))
                    .statusBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = when (activeDrawer) {
                        PlayerDrawer.EPISODES -> "EPISODES"
                        PlayerDrawer.AUDIO -> "AUDIO LANGUAGE"
                        PlayerDrawer.SUBTITLES -> "SUBTITLES & STYLE"
                        PlayerDrawer.VOLUME -> "VOLUME"
                        PlayerDrawer.QUALITY -> "QUALITY & DECODER"
                        PlayerDrawer.SPEED -> "PLAYBACK SPEED"
                        PlayerDrawer.SLEEP -> "SLEEP TIMER"
                        PlayerDrawer.NONE -> ""
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = PureWhite,
                )

                when (activeDrawer) {
                    PlayerDrawer.AUDIO -> {
                        val dubs = playback?.dubs.orEmpty()
                        val currentSubjectId = playback?.subjectId ?: destination.item.id
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (dubs.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "DUBBED VERSIONS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkMuted,
                                    )
                                }
                                itemsIndexed(dubs, key = { idx, d -> "dub_${idx}_${d.subjectId}" }) { idx, dub ->
                                    val selected = dub.subjectId == currentSubjectId
                                    DrawerRow(
                                        title = dub.languageName,
                                        trailing = when {
                                            selected -> "ACTIVE"
                                            dub.isOriginal -> "ORIGINAL"
                                            dub.languageCode.isNotBlank() -> dub.languageCode.uppercase()
                                            else -> ""
                                        },
                                        selected = selected,
                                        modifier = if (idx == 0) Modifier.focusRequester(drawerFocusRequester) else Modifier,
                                        onClick = {
                                            activeDrawer = PlayerDrawer.NONE
                                            val curPos = runCatching { exoPlayer.currentPosition }.getOrDefault(currentPositionMs)
                                            onSelectAudioDub(dub.subjectId, curPos)
                                            wakeControls()
                                        },
                                    )
                                }
                            }

                            if (embeddedAudioTracks.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "STREAM AUDIO TRACKS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkMuted,
                                        modifier = Modifier.padding(top = if (dubs.isNotEmpty()) 10.dp else 0.dp),
                                    )
                                }
                                itemsIndexed(embeddedAudioTracks, key = { idx, t -> "eat_${idx}_${t.label}" }) { idx, track ->
                                    DrawerRow(
                                        title = track.label,
                                        trailing = if (track.isSelected) {
                                            listOf("ACTIVE", track.detail).filter { it.isNotBlank() }.joinToString(" · ")
                                        } else {
                                            track.detail
                                        },
                                        selected = track.isSelected,
                                        modifier = if (dubs.isEmpty() && idx == 0) Modifier.focusRequester(drawerFocusRequester) else Modifier,
                                        onClick = {
                                            runCatching {
                                                val override = TrackSelectionOverride(track.trackGroup, listOf(track.trackIndex))
                                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                    .buildUpon()
                                                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                                    .addOverride(override)
                                                    .build()
                                            }
                                            activeDrawer = PlayerDrawer.NONE
                                            wakeControls()
                                        },
                                    )
                                }
                            }

                            if (dubs.isEmpty() && embeddedAudioTracks.isEmpty()) {
                                item {
                                    DrawerRow(
                                        title = playback?.activeAudioLabel?.ifBlank { "Original Audio" } ?: "Original Audio",
                                        trailing = "ACTIVE",
                                        selected = true,
                                        modifier = Modifier.focusRequester(drawerFocusRequester),
                                        onClick = {
                                            activeDrawer = PlayerDrawer.NONE
                                            wakeControls()
                                        },
                                    )
                                }
                            }
                        }
                    }

                    PlayerDrawer.VOLUME -> {
                        val pct = if (isMuted) 0 else (volumeLevel * 100).toInt()
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Live Visual Volume Meter
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceTonal)
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = null,
                                        tint = if (isMuted) SignalRed else PureWhite,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Text(
                                        text = if (isMuted) "MUTED" else "$pct%",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = PureWhite,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0x2EFFFFFF)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(if (isMuted) 0f else volumeLevel.coerceIn(0f, 1f))
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(SignalRed),
                                    )
                                }
                            }

                            // Step Controls (-10%, +10%, Mute)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                PlayerPillButton(
                                    label = "-10%",
                                    icon = Icons.Rounded.Remove,
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(drawerFocusRequester),
                                    onClick = {
                                        val base = if (isMuted) 0f else volumeLevel
                                        applyVolume(base - 0.10f, muted = false)
                                    },
                                )
                                PlayerPillButton(
                                    label = "+10%",
                                    icon = Icons.Rounded.Add,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val base = if (isMuted) 0f else volumeLevel
                                        applyVolume(base + 0.10f, muted = false)
                                    },
                                )
                            }

                            DrawerRow(
                                title = if (isMuted) "Unmute Audio" else "Mute Audio",
                                trailing = if (isMuted) "MUTED" else "ACTIVE",
                                selected = isMuted,
                                onClick = {
                                    if (isMuted) applyVolume(preMuteVolume.coerceAtLeast(0.4f), muted = false)
                                    else applyVolume(0f, muted = true)
                                },
                            )

                            Text("QUICK LEVELS", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(25, 50, 75, 100).forEach { level ->
                                    val selected = !isMuted && kotlin.math.abs(pct - level) <= 5
                                    DrawerSmallChip(
                                        label = "$level%",
                                        selected = selected,
                                        onClick = { applyVolume(level / 100f, muted = false) },
                                    )
                                }
                            }
                        }
                    }

                    PlayerDrawer.EPISODES -> {
                        val seasons = destination.seasonsMeta
                        val curSe = seasons.firstOrNull { it.seasonNumber == drawerSeason } ?: seasons.firstOrNull()
                        if (seasons.size > 1) {
                            LazyRow(
                                contentPadding = PaddingValues(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                itemsIndexed(seasons, key = { idx, se -> "pse_${idx}_${se.seasonNumber}" }) { _, se ->
                                    val sel = se.seasonNumber == drawerSeason
                                    DrawerSmallChip(
                                        label = String.format(Locale.US, "S%02d", se.seasonNumber),
                                        selected = sel,
                                        onClick = { drawerSeason = se.seasonNumber },
                                    )
                                }
                            }
                        }
                        if (curSe != null && curSe.episodeCount > 0) {
                            val safeEpCount = curSe.episodeCount.coerceIn(1, 500)
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                itemsIndexed(
                                    items = (1..safeEpCount).toList(),
                                    key = { idx, ep -> "pe_${curSe.seasonNumber}_${idx}_$ep" },
                                ) { _, ep ->
                                    val isPlayingThis = curSe.seasonNumber == destination.season && ep == destination.episode
                                    val prog = episodeProgress[AppPreferences.episodeKey(destination.item.id, curSe.seasonNumber, ep)]
                                    DrawerRow(
                                        title = "Episode $ep",
                                        trailing = when {
                                            isPlayingThis -> "PLAYING"
                                            prog?.isWatched == true -> "WATCHED"
                                            else -> String.format(Locale.US, "S%02d E%02d", curSe.seasonNumber, ep)
                                        },
                                        selected = isPlayingThis,
                                        modifier = if (ep == 1) Modifier.focusRequester(drawerFocusRequester) else Modifier,
                                        onClick = {
                                            activeDrawer = PlayerDrawer.NONE
                                            runCatching { onSaveProgress(exoPlayer.currentPosition, exoPlayer.duration) }
                                            onPlayEpisode(curSe.seasonNumber, ep)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    PlayerDrawer.SUBTITLES -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("TEXT SIZE", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                                    LazyRow(
                                        contentPadding = PaddingValues(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        itemsIndexed(SubtitleSize.entries, key = { idx, sz -> "ssz_${idx}_${sz.id}" }) { _, sz ->
                                            DrawerSmallChip(
                                                label = sz.label,
                                                selected = settings.subtitleStyle.size == sz,
                                                modifier = if (sz == SubtitleSize.SMALL) Modifier.focusRequester(drawerFocusRequester) else Modifier,
                                                onClick = { onUpdateSubtitleStyle { it.copy(size = sz) } },
                                            )
                                        }
                                    }
                                    Text("COLOR", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                                    LazyRow(
                                        contentPadding = PaddingValues(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        itemsIndexed(SubtitleColor.entries, key = { idx, col -> "sco_${idx}_${col.id}" }) { _, col ->
                                            DrawerSmallChip(
                                                label = col.label,
                                                selected = settings.subtitleStyle.color == col,
                                                onClick = { onUpdateSubtitleStyle { it.copy(color = col) } },
                                            )
                                        }
                                    }
                                    Text("BACKGROUND", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                                    LazyRow(
                                        contentPadding = PaddingValues(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        itemsIndexed(SubtitleBackground.entries, key = { idx, bg -> "sbg_${idx}_${bg.id}" }) { _, bg ->
                                            DrawerSmallChip(
                                                label = bg.label,
                                                selected = settings.subtitleStyle.background == bg,
                                                onClick = { onUpdateSubtitleStyle { it.copy(background = bg) } },
                                            )
                                        }
                                    }
                                    Text("POSITION", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SubtitlePosition.entries.forEach { pos ->
                                            DrawerSmallChip(
                                                label = pos.label,
                                                selected = settings.subtitleStyle.position == pos,
                                                onClick = { onUpdateSubtitleStyle { it.copy(position = pos) } },
                                            )
                                        }
                                    }
                                    if (selectedSubtitle != null) {
                                        val offsetSec = subtitleOffsetMs / 1000.0
                                        val offsetLabel = if (subtitleOffsetMs == 0L) "0.0s"
                                        else String.format(Locale.US, "%+.1fs", offsetSec)
                                        Text(
                                            text = "SYNC OFFSET  ·  $offsetLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = InkMuted,
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            DrawerSmallChip(
                                                label = "-0.5s",
                                                selected = false,
                                                onClick = { subtitleOffsetMs -= 500L },
                                            )
                                            DrawerSmallChip(
                                                label = "Reset",
                                                selected = subtitleOffsetMs == 0L,
                                                onClick = { subtitleOffsetMs = 0L },
                                            )
                                            DrawerSmallChip(
                                                label = "+0.5s",
                                                selected = false,
                                                onClick = { subtitleOffsetMs += 500L },
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "LANGUAGES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkMuted,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                            item {
                                DrawerRow(
                                    title = "Off",
                                    trailing = "",
                                    selected = playerState.selectedSubtitleId == null,
                                    onClick = {
                                        onSelectSubtitle(null)
                                        activeDrawer = PlayerDrawer.NONE
                                        wakeControls()
                                    },
                                )
                            }
                            itemsIndexed(playback?.subtitles.orEmpty(), key = { idx, sub -> "psub_${idx}_${sub.id}" }) { _, sub ->
                                DrawerRow(
                                    title = sub.languageName,
                                    trailing = sub.languageCode.uppercase(),
                                    selected = sub.id == playerState.selectedSubtitleId,
                                    onClick = {
                                        onSelectSubtitle(sub.id)
                                        activeDrawer = PlayerDrawer.NONE
                                        wakeControls()
                                    },
                                )
                            }
                        }
                    }

                    PlayerDrawer.QUALITY -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            itemsIndexed(
                                items = playback?.availableStreams.orEmpty(),
                                key = { idx, s -> "pq_${idx}_${s.resourceId}_${s.resolution}" },
                            ) { idx, stream ->
                                DrawerRow(
                                    title = "${stream.qualityLabel} · ${stream.codecLabel}",
                                    trailing = if (stream.sizeBytes > 0) "${stream.sizeBytes / (1024 * 1024)} MB" else "",
                                    selected = idx == playerState.activeStreamIndex,
                                    modifier = if (idx == 0) Modifier.focusRequester(drawerFocusRequester) else Modifier,
                                    onClick = {
                                        onSelectQuality(idx)
                                        activeDrawer = PlayerDrawer.NONE
                                        wakeControls()
                                    },
                                )
                            }
                            item {
                                Spacer(Modifier.height(6.dp))
                                Text("DECODER MODE", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                            }
                            item {
                                DrawerRow(
                                    title = "Hardware + FFmpeg",
                                    trailing = "AUTO",
                                    selected = !preferSoftwareDecoder,
                                    onClick = {
                                        preferSoftwareDecoder = false
                                        activeDrawer = PlayerDrawer.NONE
                                        wakeControls()
                                    },
                                )
                            }
                            item {
                                DrawerRow(
                                    title = "Software Compatibility",
                                    trailing = "SAFE",
                                    selected = preferSoftwareDecoder,
                                    onClick = {
                                        preferSoftwareDecoder = true
                                        activeDrawer = PlayerDrawer.NONE
                                        wakeControls()
                                    },
                                )
                            }
                        }
                    }

                    PlayerDrawer.SPEED -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(SPEED_OPTIONS, key = { idx, sp -> "psp_${idx}_$sp" }) { idx, sp ->
                                DrawerRow(
                                    title = if (sp == 1.0f) "Normal (1.0x)" else "${sp}x",
                                    trailing = "",
                                    selected = playbackSpeed == sp,
                                    modifier = if (idx == 0) Modifier.focusRequester(drawerFocusRequester) else Modifier,
                                    onClick = {
                                        playbackSpeed = sp
                                        activeDrawer = PlayerDrawer.NONE
                                        wakeControls()
                                    },
                                )
                            }
                        }
                    }

                    PlayerDrawer.SLEEP -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            itemsIndexed(AppMetadata.SLEEP_TIMER_OPTIONS, key = { idx, mins -> "psl_${idx}_$mins" }) { idx, mins ->
                                DrawerRow(
                                    title = AppMetadata.formatSleepLongLabel(mins),
                                    trailing = if (mins > 0 && sleepTimerMinutes == mins) "ACTIVE" else "",
                                    selected = sleepTimerMinutes == mins,
                                    modifier = if (idx == 0) Modifier.focusRequester(drawerFocusRequester) else Modifier,
                                    onClick = {
                                        onSetSleepTimer(mins)
                                        activeDrawer = PlayerDrawer.NONE
                                        wakeControls()
                                    },
                                )
                            }
                        }
                    }

                    PlayerDrawer.NONE -> Unit
                }
            }
        }
    }
}

@Composable
private fun PlayerSleepBadge(sleepTimerRemainingSecFlow: StateFlow<Int>) {
    val sec by sleepTimerRemainingSecFlow.collectAsStateWithLifecycle()
    if (sec > 0) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xC8141414))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "SLEEP ${formatCountdown(sec)}",
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
            )
        }
    }
}

@Composable
private fun PlayerCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    active: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = when {
                primary -> Color(0x45FFFFFF)
                active -> SurfaceSelected
                else -> Color(0x28FFFFFF)
            },
            contentColor = if (active && !primary) InkOnSelected else PureWhite,
            focusedContainerColor = PureWhite,
            focusedContentColor = InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
    ) {
        Box(
            modifier = Modifier.size(if (primary) 48.dp else 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(if (primary) 24.dp else 19.dp),
            )
        }
    }
}

@Composable
private fun PlayerPillButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    active: Boolean = false,
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = when {
                primary -> Color(0x40FFFFFF)
                active -> SurfaceSelected
                else -> Color(0x28FFFFFF)
            },
            contentColor = if (active && !primary) InkOnSelected else PureWhite,
            focusedContainerColor = PureWhite,
            focusedContentColor = InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun DrawerRow(
    title: String,
    trailing: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) SurfaceSelected else Color.Transparent,
            contentColor = if (selected) InkOnSelected else InkPrimary,
            focusedContainerColor = PureWhite,
            focusedContentColor = InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            if (trailing.isNotBlank()) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        selected -> InkOnSelected.copy(alpha = 0.72f)
                        trailing == "WATCHED" -> StatusGreen
                        else -> InkMuted
                    },
                )
            }
        }
    }
}

@Composable
private fun DrawerSmallChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) SurfaceSelected else SurfaceElevated,
            contentColor = if (selected) InkOnSelected else InkSecondary,
            focusedContainerColor = PureWhite,
            focusedContentColor = InkOnWhite,
        ),
        border = ClickableSurfaceDefaults.border(border = Border.None, focusedBorder = Border.None),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

private fun formatPlayerTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}

private fun formatCountdown(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}
