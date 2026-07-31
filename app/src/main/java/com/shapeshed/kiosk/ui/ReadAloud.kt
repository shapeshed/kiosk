package com.shapeshed.kiosk.ui

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.activity.compose.LocalActivity
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shapeshed.kiosk.KioskApp
import com.shapeshed.kiosk.MainActivity
import com.shapeshed.kiosk.R
import com.shapeshed.kiosk.data.ReaderArticle
import com.shapeshed.kiosk.data.ReaderBlock
import com.shapeshed.kiosk.data.ReaderInline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ReadAloudControls(
    title: String?,
    source: String?,
    imageUrl: String?,
    segments: List<ReadAloudSegment>,
    autoPlayKey: Long,
    canSkipToPreviousArticle: Boolean,
    canSkipToNextArticle: Boolean,
    speechRate: Float,
    selectedVoiceName: String?,
    onSpeechRateChange: (Float) -> Unit,
    onVoiceNameChange: (String?) -> Unit,
    onCurrentBlockChange: (Int?) -> Unit,
    onSkipToPreviousArticle: () -> Unit,
    onSkipToNextArticle: () -> Unit,
    onDismiss: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val app = appContext as? KioskApp
    val activity = LocalActivity.current
    val window = activity?.window
    val shouldRead = remember { AtomicBoolean(false) }
    val hasAudioFocus = remember { AtomicBoolean(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val audioManager = remember(appContext) { appContext.getSystemService(AudioManager::class.java) }
    val readAloudAudioAttributes = remember {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isPlaying by rememberSaveable(segments) { mutableStateOf(false) }
    var playbackEnded by rememberSaveable(segments) { mutableStateOf(false) }
    var currentIndex by rememberSaveable(segments) { mutableIntStateOf(0) }
    var showPlayerSheet by rememberSaveable { mutableStateOf(false) }
    var draftSpeechRate by rememberSaveable(speechRate) {
        mutableFloatStateOf(speechRate.nearestReadAloudSpeechRate())
    }
    var segmentStartElapsedMillis by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var playbackProgressNowMillis by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var availableVoices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    var voiceMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var mediaSession by remember { mutableStateOf<MediaSession?>(null) }
    var mediaArtwork by remember(imageUrl) { mutableStateOf<Bitmap?>(null) }
    val latestSpeechRate by rememberUpdatedState(speechRate)
    val latestSelectedVoiceName by rememberUpdatedState(selectedVoiceName)
    val latestAvailableVoices by rememberUpdatedState(availableVoices)
    val audioFocusRequest = remember(audioManager) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(readAloudAudioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener { focusChange ->
                if (
                    focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                ) {
                    mainHandler.post {
                        shouldRead.set(false)
                        tts?.stop()
                        isPlaying = false
                        playbackEnded = false
                        onCurrentBlockChange(null)
                        hasAudioFocus.set(false)
                    }
                }
            }
            .build()
    }

    fun requestReadAloudAudioFocus(): Boolean {
        if (hasAudioFocus.get()) return true
        val granted = audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        hasAudioFocus.set(granted)
        return granted
    }

    fun releaseReadAloudAudioFocus() {
        if (hasAudioFocus.getAndSet(false)) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
    }

    DisposableEffect(window) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    DisposableEffect(appContext, segments) {
        ready = false
        error = null
        isPlaying = false
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(appContext) { status ->
            mainHandler.post {
                if (status == TextToSpeech.SUCCESS) {
                    val languageStatus = engine.setLanguage(Locale.getDefault())
                    if (
                        languageStatus == TextToSpeech.LANG_MISSING_DATA ||
                        languageStatus == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        error = appContext.getString(R.string.read_aloud_language_unavailable)
                    } else {
                        engine.setAudioAttributes(readAloudAudioAttributes)
                        val voices = engine.readAloudVoices()
                        availableVoices = voices
                        latestSelectedVoiceName?.let { voiceName ->
                            voices.firstOrNull { it.name == voiceName }?.let(engine::setVoice)
                        }
                        engine.setSpeechRate(latestSpeechRate)
                        ready = true
                    }
                } else {
                    error = appContext.getString(R.string.read_aloud_unavailable)
                }
            }
        }
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val index = utteranceId?.readAloudIndexOrNull() ?: return
                    mainHandler.post {
                        currentIndex = index
                        segmentStartElapsedMillis = SystemClock.elapsedRealtime()
                        playbackProgressNowMillis = segmentStartElapsedMillis
                        playbackEnded = false
                        onCurrentBlockChange(segments.getOrNull(index)?.blockIndex)
                        isPlaying = true
                    }
                }

                override fun onDone(utteranceId: String?) {
                    val nextIndex = (utteranceId?.readAloudIndexOrNull() ?: return) + 1
                    mainHandler.post {
                        if (shouldRead.get() && nextIndex < segments.size) {
                            currentIndex = nextIndex
                            onCurrentBlockChange(segments[nextIndex].blockIndex)
                            engine.speakReadAloudSegment(
                                text = segments[nextIndex].text,
                                index = nextIndex,
                                speechRate = latestSpeechRate,
                                voice = latestSelectedVoiceName?.let { voiceName ->
                                    latestAvailableVoices.firstOrNull { it.name == voiceName }
                                },
                            )
                        } else {
                            shouldRead.set(false)
                            isPlaying = false
                            playbackEnded = nextIndex >= segments.size
                            onCurrentBlockChange(null)
                            releaseReadAloudAudioFocus()
                        }
                    }
                }

                @Deprecated("Required by framework callback; onError(String, Int) handles modern engines.")
                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        shouldRead.set(false)
                        isPlaying = false
                        playbackEnded = false
                        onCurrentBlockChange(null)
                        releaseReadAloudAudioFocus()
                        error = appContext.getString(R.string.read_aloud_unavailable)
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    mainHandler.post {
                        shouldRead.set(false)
                        isPlaying = false
                        playbackEnded = false
                        onCurrentBlockChange(null)
                        releaseReadAloudAudioFocus()
                        error = appContext.getString(R.string.read_aloud_unavailable)
                    }
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    if (interrupted) {
                        mainHandler.post {
                            isPlaying = false
                            playbackEnded = false
                            releaseReadAloudAudioFocus()
                        }
                    }
                }
            },
        )
        tts = engine

        onDispose {
            shouldRead.set(false)
            engine.stop()
            engine.shutdown()
            releaseReadAloudAudioFocus()
            onCurrentBlockChange(null)
            tts = null
        }
    }

    LaunchedEffect(autoPlayKey, ready, segments) {
        val engine = tts
        if (autoPlayKey > 0L && ready && segments.isNotEmpty() && engine != null) {
            engine.stop()
            if (requestReadAloudAudioFocus()) {
                shouldRead.set(true)
                currentIndex = 0
                segmentStartElapsedMillis = SystemClock.elapsedRealtime()
                playbackProgressNowMillis = segmentStartElapsedMillis
                playbackEnded = false
                onCurrentBlockChange(segments[0].blockIndex)
                engine.speakReadAloudSegment(
                    text = segments[0].text,
                    index = 0,
                    speechRate = speechRate,
                    voice = selectedVoiceName?.let { voiceName -> availableVoices.firstOrNull { it.name == voiceName } },
                )
            } else {
                shouldRead.set(false)
                isPlaying = false
                playbackEnded = false
                error = appContext.getString(R.string.read_aloud_unavailable)
            }
        }
    }

    LaunchedEffect(ready, speechRate, selectedVoiceName, availableVoices) {
        val engine = tts ?: return@LaunchedEffect
        if (!ready) return@LaunchedEffect
        if (selectedVoiceName == null) {
            engine.setLanguage(Locale.getDefault())
        } else {
            availableVoices.firstOrNull { it.name == selectedVoiceName }?.let(engine::setVoice)
        }
        engine.setSpeechRate(speechRate)
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            playbackProgressNowMillis = SystemClock.elapsedRealtime()
            delay(250)
        }
    }

    val selectedVoice = selectedVoiceName?.let { voiceName ->
        availableVoices.firstOrNull { it.name == voiceName }
    }
    val selectedVoiceLabel = selectedVoice?.readAloudLabel() ?: stringResource(R.string.system_voice)
    val displayTitle = title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.read_aloud)
    val displaySource = source?.takeIf { it.isNotBlank() } ?: when {
        error != null -> error.orEmpty()
        segments.isEmpty() -> stringResource(R.string.read_aloud_empty)
        !ready -> stringResource(R.string.read_aloud_starting)
        else -> stringResource(
            R.string.read_aloud_progress,
            (currentIndex + 1).coerceAtMost(segments.size),
            segments.size,
        )
    }
    fun speakSegment(engine: TextToSpeech, index: Int) {
        if (segments.isEmpty()) return
        if (!requestReadAloudAudioFocus()) {
            shouldRead.set(false)
            isPlaying = false
            playbackEnded = false
            error = appContext.getString(R.string.read_aloud_unavailable)
            return
        }
        val safeIndex = index.coerceIn(0, segments.lastIndex)
        currentIndex = safeIndex
        segmentStartElapsedMillis = SystemClock.elapsedRealtime()
        playbackProgressNowMillis = segmentStartElapsedMillis
        playbackEnded = false
        shouldRead.set(true)
        onCurrentBlockChange(segments[safeIndex].blockIndex)
        engine.speakReadAloudSegment(
            text = segments[safeIndex].text,
            index = safeIndex,
            speechRate = draftSpeechRate,
            voice = selectedVoice,
        )
    }
    fun speakCurrentSegment(engine: TextToSpeech) {
        speakSegment(engine, currentIndex)
    }
    fun moveToSegment(index: Int) {
        if (segments.isEmpty()) return
        val safeIndex = index.coerceIn(0, segments.lastIndex)
        currentIndex = safeIndex
        segmentStartElapsedMillis = SystemClock.elapsedRealtime()
        playbackProgressNowMillis = segmentStartElapsedMillis
        playbackEnded = false
        onCurrentBlockChange(segments[safeIndex].blockIndex)
        val engine = tts
        if (engine != null && isPlaying) {
            speakSegment(engine, safeIndex)
        }
    }
    fun togglePlayback() {
        val engine = tts ?: return
        if (isPlaying) {
            shouldRead.set(false)
            playbackProgressNowMillis = SystemClock.elapsedRealtime()
            engine.stop()
            isPlaying = false
            playbackEnded = false
            releaseReadAloudAudioFocus()
        } else {
            speakCurrentSegment(engine)
        }
    }
    fun playPlayback() {
        if (!isPlaying) {
            tts?.let(::speakCurrentSegment)
        }
    }
    fun pausePlayback() {
        if (isPlaying) {
            shouldRead.set(false)
            playbackProgressNowMillis = SystemClock.elapsedRealtime()
            tts?.stop()
            isPlaying = false
            playbackEnded = false
            releaseReadAloudAudioFocus()
        }
    }
    val latestPlayPlayback by rememberUpdatedState(::playPlayback)
    val latestPausePlayback by rememberUpdatedState(::pausePlayback)
    val latestSkipToPreviousArticle by rememberUpdatedState(onSkipToPreviousArticle)
    val latestSkipToNextArticle by rememberUpdatedState(onSkipToNextArticle)
    val latestDismiss by rememberUpdatedState {
        shouldRead.set(false)
        tts?.stop()
        releaseReadAloudAudioFocus()
        playbackEnded = false
        onDismiss()
    }

    LaunchedEffect(app, imageUrl) {
        mediaArtwork = null
        val client = app?.okHttpClient ?: return@LaunchedEffect
        val url = imageUrl ?: return@LaunchedEffect
        mediaArtwork = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body.byteStream().use { stream ->
                        BitmapFactory.decodeStream(stream)?.scaledForMediaMetadata()
                    }
                }
            }.getOrNull()
        }
    }

    DisposableEffect(appContext) {
        val launchIntent = Intent(appContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = PendingIntent.getActivity(
            appContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val session = MediaSession(appContext, "KioskReadAloud").apply {
            setSessionActivity(contentIntent)
            setCallback(
                object : MediaSession.Callback() {
                    override fun onPlay() {
                        latestPlayPlayback()
                    }

                    override fun onPause() {
                        latestPausePlayback()
                    }

                    override fun onStop() {
                        latestDismiss()
                    }

                    override fun onSkipToPrevious() {
                        latestSkipToPreviousArticle()
                    }

                    override fun onSkipToNext() {
                        latestSkipToNextArticle()
                    }
                },
            )
            isActive = true
        }
        mediaSession = session
        onDispose {
            session.isActive = false
            session.release()
            mediaSession = null
            appContext.readAloudNotificationManager().cancel(ReadAloudNotificationId)
        }
    }

    LaunchedEffect(
        appContext,
        mediaSession,
        displayTitle,
        displaySource,
        mediaArtwork,
        isPlaying,
        ready,
        error,
    ) {
        val session = mediaSession ?: return@LaunchedEffect
        appContext.notifyReadAloudMediaNotification(
            appContext.buildReadAloudNotification(
                mediaSession = session,
                title = displayTitle,
                source = displaySource,
                artwork = mediaArtwork,
                isPlaying = isPlaying,
            ),
        )
    }

    LaunchedEffect(mediaSession, displayTitle, displaySource, mediaArtwork, segments) {
        mediaSession?.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, displayTitle)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, displaySource)
                .putLong(
                    MediaMetadata.METADATA_KEY_DURATION,
                    segments.estimatedDurationMillis(),
                )
                .apply {
                    mediaArtwork?.let { artwork ->
                        putBitmap(MediaMetadata.METADATA_KEY_ART, artwork)
                        putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
                    }
                }
                .build(),
        )
    }

    val playbackDurationMillis = remember(segments) { segments.estimatedDurationMillis() }
    val playbackPositionMillis = segments.readAloudPlaybackPositionMillis(
        currentIndex = currentIndex,
        segmentStartElapsedMillis = segmentStartElapsedMillis,
        nowElapsedMillis = playbackProgressNowMillis,
        speechRate = speechRate,
        playbackEnded = playbackEnded,
    )

    LaunchedEffect(mediaSession, isPlaying, ready, error, playbackEnded, playbackPositionMillis, playbackDurationMillis, speechRate) {
        val actions = (
            PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_STOP
            ) or
            (if (canSkipToPreviousArticle) PlaybackState.ACTION_SKIP_TO_PREVIOUS else 0L) or
            (if (canSkipToNextArticle) PlaybackState.ACTION_SKIP_TO_NEXT else 0L)
        mediaSession?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(
                    when {
                        !ready || error != null -> PlaybackState.STATE_NONE
                        playbackEnded -> PlaybackState.STATE_STOPPED
                        isPlaying -> PlaybackState.STATE_PLAYING
                        else -> PlaybackState.STATE_PAUSED
                    },
                    playbackPositionMillis,
                    if (isPlaying) 1f else 0f,
                )
                .build(),
        )
    }

    val miniPlayerDismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        positionalThreshold = { distance -> distance * 0.35f },
    )
    val miniPlayerProgress = if (playbackDurationMillis <= 0L) {
        0f
    } else {
        (playbackPositionMillis.toFloat() / playbackDurationMillis.toFloat()).coerceIn(0f, 1f)
    }
    val animatedPlayerProgress by animateFloatAsState(
        targetValue = miniPlayerProgress,
        animationSpec = tween(500),
        label = "readAloudProgress",
    )
    SwipeToDismissBox(
        state = miniPlayerDismissState,
        backgroundContent = { Box(Modifier.fillMaxSize().background(backgroundColor)) },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        onDismiss = {
            latestDismiss()
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
        ) {
            Column {
                LinearProgressIndicator(
                    progress = { animatedPlayerProgress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showPlayerSheet = true }
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = displaySource,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        enabled = segments.isNotEmpty() && currentIndex > 0,
                        onClick = { moveToSegment(currentIndex - 1) },
                    ) {
                        Icon(Icons.Filled.SkipPrevious, stringResource(R.string.previous_paragraph))
                    }
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .clickable(enabled = ready && segments.isNotEmpty() && error == null) {
                                togglePlayback()
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                    IconButton(
                        enabled = segments.isNotEmpty() && currentIndex < segments.lastIndex,
                        onClick = { moveToSegment(currentIndex + 1) },
                    ) {
                        Icon(Icons.Filled.SkipNext, stringResource(R.string.next_paragraph))
                    }
                }
            }
        }
    }

    if (showPlayerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPlayerSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(104.dp),
                    ) {
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = displayTitle.firstOrNull()?.uppercase().orEmpty(),
                                    style = MaterialTheme.typography.displaySmall,
                                )
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = displaySource,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                if (ready && error == null && segments.isNotEmpty()) {
                    val playerProgressModifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .height(WavyProgressIndicatorDefaults.LinearContainerHeight)
                    LinearWavyProgressIndicator(
                        progress = { animatedPlayerProgress },
                        amplitude = { if (isPlaying) 1f else 0f },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = playerProgressModifier,
                    )
                    Text(
                        text = stringResource(
                            R.string.read_aloud_progress,
                            (currentIndex + 1).coerceAtMost(segments.size),
                            segments.size,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Text(
                        text = segments.getOrNull(currentIndex)?.text.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    Text(
                        text = displaySource,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        enabled = segments.isNotEmpty() && currentIndex > 0,
                        onClick = { moveToSegment(currentIndex - 1) },
                    ) {
                        Icon(Icons.Filled.SkipPrevious, stringResource(R.string.previous_paragraph))
                    }
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(horizontal = 18.dp)
                            .size(64.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .clickable(enabled = ready && segments.isNotEmpty() && error == null) { togglePlayback() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                    IconButton(
                        enabled = segments.isNotEmpty() && currentIndex < segments.lastIndex,
                        onClick = { moveToSegment(currentIndex + 1) },
                    ) {
                        Icon(Icons.Filled.SkipNext, stringResource(R.string.next_paragraph))
                    }
                }

                Text(
                    text = stringResource(R.string.read_aloud_speed, draftSpeechRate),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Slider(
                    value = draftSpeechRate,
                    onValueChange = { value -> draftSpeechRate = value.nearestReadAloudSpeechRate() },
                    onValueChangeFinished = {
                        onSpeechRateChange(draftSpeechRate)
                        val engine = tts
                        if (engine != null) {
                            engine.setSpeechRate(draftSpeechRate)
                            if (isPlaying) speakCurrentSegment(engine)
                        }
                    },
                    valueRange = ReadAloudSpeechRates.first()..ReadAloudSpeechRates.last(),
                    steps = ReadAloudSpeechRates.size - 2,
                )

                Box {
                    TextButton(
                        enabled = ready && segments.isNotEmpty() && error == null,
                        onClick = { voiceMenuExpanded = true },
                    ) {
                        Text(stringResource(R.string.read_aloud_voice, selectedVoiceLabel))
                    }
                    DropdownMenu(
                        expanded = voiceMenuExpanded,
                        onDismissRequest = { voiceMenuExpanded = false },
                        shape = RoundedCornerShape(28.dp),
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.system_voice)) },
                            onClick = {
                                voiceMenuExpanded = false
                                onVoiceNameChange(null)
                                val engine = tts
                                if (engine != null) {
                                    engine.setLanguage(Locale.getDefault())
                                    engine.setSpeechRate(draftSpeechRate)
                                    if (isPlaying) speakCurrentSegment(engine)
                                }
                            },
                        )
                        availableVoices.take(8).forEach { voice ->
                            DropdownMenuItem(
                                text = { Text(voice.readAloudLabel()) },
                                onClick = {
                                    voiceMenuExpanded = false
                                    onVoiceNameChange(voice.name)
                                    val engine = tts
                                    if (engine != null) {
                                        engine.setVoice(voice)
                                        engine.setSpeechRate(draftSpeechRate)
                                        if (isPlaying) speakCurrentSegment(engine)
                                    }
                                },
                            )
                        }
                        if (availableVoices.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.read_aloud_no_voices)) },
                                onClick = { voiceMenuExpanded = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun TextToSpeech.speakReadAloudSegment(
    text: String,
    index: Int,
    speechRate: Float,
    voice: Voice?,
) {
    voice?.let(::setVoice)
    setSpeechRate(speechRate)
    speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), readAloudUtteranceId(index))
}

private fun TextToSpeech.readAloudVoices(): List<Voice> =
    voices
        ?.filter { voice -> !voice.isNetworkConnectionRequired && voice.locale.language == Locale.getDefault().language }
        ?.sortedWith(compareBy<Voice> { it.locale.displayLanguage }.thenBy { it.name })
        .orEmpty()

private fun Context.buildReadAloudNotification(
    mediaSession: MediaSession,
    title: String,
    source: String,
    artwork: Bitmap?,
    isPlaying: Boolean,
): Notification {
    ensureReadAloudNotificationChannel()
    val launchIntent = Intent(this, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val contentIntent = PendingIntent.getActivity(
        this,
        0,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return Notification.Builder(this, ReadAloudNotificationChannelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(source)
        .setSubText(getString(R.string.read_aloud))
        .setContentIntent(contentIntent)
        .setCategory(Notification.CATEGORY_TRANSPORT)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setOngoing(isPlaying)
        .setDefaults(0)
        .apply {
            artwork?.let(::setLargeIcon)
        }
        .setStyle(
            Notification.MediaStyle()
                .setMediaSession(mediaSession.sessionToken),
        )
        .build()
}

private fun Context.ensureReadAloudNotificationChannel() {
    val manager = readAloudNotificationManager()
    if (manager.getNotificationChannel(ReadAloudNotificationChannelId) != null) return
    manager.createNotificationChannel(
        NotificationChannel(
            ReadAloudNotificationChannelId,
            getString(R.string.read_aloud),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(false)
            setSound(null, null)
        },
    )
}

private fun Context.readAloudNotificationManager(): NotificationManager =
    getSystemService(NotificationManager::class.java)

@SuppressLint("NotificationPermission")
private fun Context.notifyReadAloudMediaNotification(notification: Notification) {
    // Android 13+ exempts media-session notifications from POST_NOTIFICATIONS. Lint cannot infer
    // that this notification is MediaStyle and linked to a live MediaSession.
    readAloudNotificationManager().notify(ReadAloudNotificationId, notification)
}

private fun Bitmap.scaledForMediaMetadata(maxSize: Int = 512): Bitmap {
    val longestSide = width.coerceAtLeast(height)
    if (longestSide <= maxSize) return this
    val scale = maxSize.toFloat() / longestSide.toFloat()
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
        true,
    )
}

private const val ReadAloudNotificationChannelId = "read_aloud"
private const val ReadAloudNotificationId = 9001
private const val ReadAloudBaseWordsPerMinute = 180f
private const val ReadAloudMinimumSegmentMillis = 900L
private val ReadAloudSpeechRates = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

private fun Float.nearestReadAloudSpeechRate(): Float =
    ReadAloudSpeechRates.minBy { kotlin.math.abs(it - this) }

private fun Voice.readAloudLabel(): String =
    locale.getDisplayName(Locale.getDefault()).ifBlank { name }

private fun readAloudUtteranceId(index: Int): String = "read-aloud-$index"

private fun String.readAloudIndexOrNull(): Int? =
    removePrefix("read-aloud-").toIntOrNull()

internal data class ReadAloudSegment(
    val text: String,
    val blockIndex: Int,
) {
    val wordCount: Int = text.readAloudWordCount()
    val estimatedDurationMillis: Long =
        ((wordCount.toFloat() / ReadAloudBaseWordsPerMinute) * 60_000f)
            .toLong()
            .coerceAtLeast(ReadAloudMinimumSegmentMillis)
}

private fun String.readAloudWordCount(): Int =
    trim()
        .split(Regex("\\s+"))
        .count { it.isNotBlank() }
        .coerceAtLeast(1)

internal fun ReaderArticle.readAloudSegments(): List<ReadAloudSegment> =
    blocks.flatMapIndexed { blockIndex, block ->
        block.readAloudParagraphs()
            .flatMap { paragraph -> paragraph.chunkForTextToSpeech() }
            .map { text -> ReadAloudSegment(text = text, blockIndex = blockIndex) }
    }

private fun List<ReadAloudSegment>.estimatedDurationMillis(): Long =
    sumOf { it.estimatedDurationMillis }.coerceAtLeast(ReadAloudMinimumSegmentMillis)

private fun List<ReadAloudSegment>.readAloudPlaybackPositionMillis(
    currentIndex: Int,
    segmentStartElapsedMillis: Long,
    nowElapsedMillis: Long,
    speechRate: Float,
    playbackEnded: Boolean,
): Long {
    if (isEmpty()) return 0L
    val durationMillis = estimatedDurationMillis()
    if (playbackEnded) return durationMillis
    val safeIndex = currentIndex.coerceIn(0, lastIndex)
    val totalWords = sumOf { it.wordCount }.coerceAtLeast(1)
    val baseWords = take(safeIndex).sumOf { it.wordCount }
    val currentSegmentWords = getOrNull(safeIndex)?.wordCount ?: 0
    val elapsedWordsInSegment = (
        (nowElapsedMillis - segmentStartElapsedMillis)
            .coerceAtLeast(0L)
            .toFloat() /
            60_000f *
            ReadAloudBaseWordsPerMinute *
            speechRate.coerceAtLeast(0.1f)
        )
        .coerceAtLeast(0f)
        .coerceAtMost(currentSegmentWords.toFloat())
    val wordProgress = (baseWords.toFloat() + elapsedWordsInSegment).coerceIn(0f, totalWords.toFloat())
    return ((wordProgress / totalWords.toFloat()) * durationMillis.toFloat())
        .toLong()
        .coerceAtLeast(0L)
        .coerceAtMost(durationMillis)
}

internal fun ReaderArticle.firstImageUrlOrNull(): String? =
    blocks.firstNotNullOfOrNull { block ->
        when (block) {
            is ReaderBlock.Image -> block.src
            is ReaderBlock.Figure -> block.images.firstOrNull()?.src
            else -> null
        }
    }

private fun ReaderBlock.readAloudParagraphs(): List<String> =
    when (this) {
        is ReaderBlock.Heading -> listOf(text.plainText())
        is ReaderBlock.Paragraph -> listOf(text.plainText())
        is ReaderBlock.Quote -> blocks.flatMap { it.readAloudParagraphs() }
        is ReaderBlock.CodeBlock -> emptyList()
        is ReaderBlock.BulletedList -> items.map { it.plainText() }
        is ReaderBlock.NumberedList -> items.map { it.plainText() }
        is ReaderBlock.Image -> emptyList()
        is ReaderBlock.Figure -> listOfNotNull(caption?.plainText())
        ReaderBlock.Divider -> emptyList()
    }

private fun List<ReaderInline>.plainText(): String =
    joinToString(separator = "") { it.text }.replace(Regex("\\s+"), " ").trim()

private fun String.chunkForTextToSpeech(): List<String> {
    val maxLength = TextToSpeech.getMaxSpeechInputLength().coerceAtMost(3_500)
    if (length <= maxLength) return listOf(this)
    return chunked(maxLength).map { it.trim() }.filter { it.isNotEmpty() }
}
