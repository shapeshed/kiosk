package com.shapeshed.kiosk.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.LocalActivity
import android.view.WindowManager
import com.shapeshed.kiosk.R
import com.shapeshed.kiosk.data.ReaderFont
import com.shapeshed.kiosk.data.ReaderTheme
import kotlinx.coroutines.delay

private const val SpeedReaderInitialPauseMillis = 300L
private const val SpeedReaderEndPauseMillis = 800L
private const val SpeedReaderExitFadeMillis = 240
private const val SpeedReaderMinWordsPerMinute = 150
private const val SpeedReaderMaxWordsPerMinute = 800
private const val SpeedReaderWordsPerMinuteStep = 25

@Composable
internal fun SpeedReaderOverlay(
    words: List<String>,
    palette: ReaderPalette,
    readerFont: ReaderFont,
    wordsPerMinute: Int,
    currentTheme: ReaderTheme,
    onWordsPerMinuteChange: (Int) -> Unit,
    onSelectTheme: (ReaderTheme) -> Unit,
    onSelectFont: (ReaderFont) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = remember(palette) { Color(android.graphics.Color.parseColor(palette.background)) }
    val foreground = remember(palette) { Color(android.graphics.Color.parseColor(palette.foreground)) }
    val muted = remember(palette) { Color(android.graphics.Color.parseColor(palette.muted)) }
    val readerFontFamily = readerFont.fontFamily
    var wordIndex by rememberSaveable(words) { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var exiting by remember(words) { mutableStateOf(false) }
    val latestWordsPerMinute by rememberUpdatedState(
        wordsPerMinute.coerceIn(SpeedReaderMinWordsPerMinute, SpeedReaderMaxWordsPerMinute),
    )
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val overlayAlpha by animateFloatAsState(
        targetValue = if (exiting) 0f else 1f,
        animationSpec = tween(durationMillis = SpeedReaderExitFadeMillis),
        label = "speedReaderExitAlpha",
    )
    val progress = if (words.isEmpty()) {
        0f
    } else {
        ((wordIndex + 1).coerceAtMost(words.size).toFloat() / words.size.toFloat()).coerceIn(0f, 1f)
    }
    val window = LocalActivity.current?.window

    DisposableEffect(window) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(words) {
        wordIndex = 0
        exiting = false
        if (words.isEmpty()) return@LaunchedEffect
        delay(SpeedReaderInitialPauseMillis)
        while (wordIndex < words.lastIndex) {
            delay(60_000L / latestWordsPerMinute.coerceAtLeast(1))
            wordIndex += 1
        }
        delay(SpeedReaderEndPauseMillis)
        exiting = true
        delay(SpeedReaderExitFadeMillis.toLong())
        latestOnDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha }
            .background(background),
    ) {
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.speed_reader_settings),
                    tint = muted,
                )
            }
        }
        val currentWord = words.getOrNull(wordIndex).orEmpty()
        Text(
            text = currentWord,
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = readerFontFamily,
                fontSize = 46.sp,
                lineHeight = 54.sp,
                fontWeight = FontWeight.Normal,
            ),
            color = foreground,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 28.dp),
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50.dp)),
            color = foreground.copy(alpha = 0.58f),
            trackColor = muted.copy(alpha = 0.24f),
        )
    }

    if (showSettings) {
        SpeedReaderSettingsSheet(
            wordsPerMinute = wordsPerMinute,
            currentTheme = currentTheme,
            currentFont = readerFont,
            onWordsPerMinuteChange = onWordsPerMinuteChange,
            onSelectTheme = onSelectTheme,
            onSelectFont = onSelectFont,
            onDismiss = { showSettings = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedReaderSettingsSheet(
    wordsPerMinute: Int,
    currentTheme: ReaderTheme,
    currentFont: ReaderFont,
    onWordsPerMinuteChange: (Int) -> Unit,
    onSelectTheme: (ReaderTheme) -> Unit,
    onSelectFont: (ReaderFont) -> Unit,
    onDismiss: () -> Unit,
) {
    var sliderValue by remember(wordsPerMinute) { mutableFloatStateOf(wordsPerMinute.toFloat()) }
    val snappedWordsPerMinute = sliderValue
        .roundToNearest(SpeedReaderWordsPerMinuteStep)
        .toInt()
        .coerceIn(SpeedReaderMinWordsPerMinute, SpeedReaderMaxWordsPerMinute)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Text(
                text = stringResource(R.string.speed_reader_settings),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.speed_reader_speed_value, snappedWordsPerMinute),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    sliderValue = value
                    onWordsPerMinuteChange(value.roundToNearest(SpeedReaderWordsPerMinuteStep).toInt())
                },
                valueRange = SpeedReaderMinWordsPerMinute.toFloat()..SpeedReaderMaxWordsPerMinute.toFloat(),
                steps = ((SpeedReaderMaxWordsPerMinute - SpeedReaderMinWordsPerMinute) / SpeedReaderWordsPerMinuteStep) - 1,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.speed_reader_slow),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.speed_reader_fast),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.speed_reader_colors),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(ReaderTheme.SYSTEM, ReaderTheme.LIGHT, ReaderTheme.SEPIA, ReaderTheme.DARK)
                    .forEach { theme ->
                        ThemeSwatch(
                            fill = swatchBrush(theme),
                            label = readerThemeLabel(theme),
                            selected = currentTheme == theme,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectTheme(theme) },
                        )
                    }
            }
            Text(
                text = stringResource(R.string.reader_font),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            SpeedReaderFontPicker(current = currentFont, onSelect = onSelectFont)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SpeedReaderFontPicker(
    current: ReaderFont,
    onSelect: (ReaderFont) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = current.speedReaderLabel,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = current.fontFamily),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ReaderFont.entries.forEach { font ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = font.speedReaderLabel,
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = font.fontFamily),
                        )
                    },
                    onClick = {
                        onSelect(font)
                        expanded = false
                    },
                )
            }
        }
    }
}

private val ReaderFont.speedReaderLabel: String
    get() = when (this) {
        ReaderFont.NEWSREADER -> "Newsreader"
        ReaderFont.SOURCE_SERIF_4 -> "Source Serif 4"
        ReaderFont.LITERATA -> "Literata"
        ReaderFont.ATKINSON_NEXT -> "Atkinson Hyperlegible Next"
        ReaderFont.INTER -> "Inter"
        ReaderFont.SYSTEM_SANS -> "System Sans"
    }

private fun Float.roundToNearest(step: Int): Float =
    (kotlin.math.round(this / step.toFloat()) * step).coerceIn(
        SpeedReaderMinWordsPerMinute.toFloat(),
        SpeedReaderMaxWordsPerMinute.toFloat(),
    )

internal fun String.speedReadWords(): List<String> =
    this
        .replace(Regex("\\s+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
