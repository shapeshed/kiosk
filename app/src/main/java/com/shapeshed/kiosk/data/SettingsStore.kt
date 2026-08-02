package com.shapeshed.kiosk.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reader appearance choice; SYSTEM follows the device light/dark setting. */
enum class ReaderTheme { SYSTEM, LIGHT, DARK, SEPIA }

/** Reader body typeface choice. Monospace remains reserved for code blocks. */
enum class ReaderFont { NEWSREADER, LITERATA, ATKINSON, SYSTEM_SANS }
enum class ReaderFontSize(val scale: Float) { SMALL(0.9f), MEDIUM(1f), LARGE(1.15f) }
enum class ReaderAlignment { LEFT, JUSTIFY }
enum class ReaderLineSpacing(val multiplier: Float) { COMPACT(1.5f), RELAXED(1.75f) }
enum class ReaderWidth(val maxDp: Int) { NARROW(640), WIDE(760) }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Persisted user settings, backed by Preferences DataStore. */
class SettingsStore(private val context: Context) {

    private val readerThemeKey = stringPreferencesKey("reader_theme")
    private val readerFontKey = stringPreferencesKey("reader_font")
    private val readerFontSizeKey = stringPreferencesKey("reader_font_size")
    private val readerAlignmentKey = stringPreferencesKey("reader_alignment")
    private val readerLineSpacingKey = stringPreferencesKey("reader_line_spacing")
    private val readerWidthKey = stringPreferencesKey("reader_width")
    private val readAloudSpeechRateKey = floatPreferencesKey("read_aloud_speech_rate")
    private val readAloudVoiceNameKey = stringPreferencesKey("read_aloud_voice_name")
    private val speedReaderWordsPerMinuteKey = intPreferencesKey("speed_reader_words_per_minute")
    private val speedReaderThemeKey = stringPreferencesKey("speed_reader_theme")
    private val speedReaderFontKey = stringPreferencesKey("speed_reader_font")
    private val viewedKey = stringSetPreferencesKey("viewed_story_ids")
    private val selectedFeedKey = stringPreferencesKey("selected_feed")

    /** The feed the user last viewed; the home screen opens here. Defaults to [Feed.TOP]. */
    val selectedFeed: Flow<Feed> = context.dataStore.data.map { prefs ->
        prefs[selectedFeedKey]?.let { runCatching { Feed.valueOf(it) }.getOrNull() } ?: Feed.TOP
    }

    suspend fun setSelectedFeed(feed: Feed) {
        context.dataStore.edit { it[selectedFeedKey] = feed.name }
    }

    val readerTheme: Flow<ReaderTheme> = context.dataStore.data.map { prefs ->
        prefs[readerThemeKey]
            ?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() }
            ?: ReaderTheme.SYSTEM
    }

    suspend fun setReaderTheme(theme: ReaderTheme) {
        context.dataStore.edit { it[readerThemeKey] = theme.name }
    }

    val readerFont: Flow<ReaderFont> = context.dataStore.data.map { prefs ->
        prefs[readerFontKey]
            ?.let { runCatching { ReaderFont.valueOf(it) }.getOrNull() }
            ?: ReaderFont.NEWSREADER
    }

    suspend fun setReaderFont(font: ReaderFont) {
        context.dataStore.edit { it[readerFontKey] = font.name }
    }

    val readerFontSize: Flow<ReaderFontSize> = context.dataStore.data.map { prefs ->
        prefs[readerFontSizeKey]?.let { runCatching { ReaderFontSize.valueOf(it) }.getOrNull() } ?: ReaderFontSize.MEDIUM
    }

    suspend fun setReaderFontSize(size: ReaderFontSize) { context.dataStore.edit { it[readerFontSizeKey] = size.name } }

    val readerAlignment: Flow<ReaderAlignment> = context.dataStore.data.map { prefs ->
        prefs[readerAlignmentKey]?.let { runCatching { ReaderAlignment.valueOf(it) }.getOrNull() } ?: ReaderAlignment.LEFT
    }

    suspend fun setReaderAlignment(alignment: ReaderAlignment) { context.dataStore.edit { it[readerAlignmentKey] = alignment.name } }

    val readerLineSpacing: Flow<ReaderLineSpacing> = context.dataStore.data.map { prefs ->
        prefs[readerLineSpacingKey]?.let { runCatching { ReaderLineSpacing.valueOf(it) }.getOrNull() } ?: ReaderLineSpacing.RELAXED
    }

    suspend fun setReaderLineSpacing(spacing: ReaderLineSpacing) { context.dataStore.edit { it[readerLineSpacingKey] = spacing.name } }

    val readerWidth: Flow<ReaderWidth> = context.dataStore.data.map { prefs ->
        prefs[readerWidthKey]?.let { runCatching { ReaderWidth.valueOf(it) }.getOrNull() } ?: ReaderWidth.WIDE
    }

    suspend fun setReaderWidth(width: ReaderWidth) { context.dataStore.edit { it[readerWidthKey] = width.name } }

    val readAloudSpeechRate: Flow<Float> = context.dataStore.data.map { prefs ->
        (prefs[readAloudSpeechRateKey] ?: DEFAULT_READ_ALOUD_SPEECH_RATE).coerceIn(
            MIN_READ_ALOUD_SPEECH_RATE,
            MAX_READ_ALOUD_SPEECH_RATE,
        )
    }

    suspend fun setReadAloudSpeechRate(rate: Float) {
        context.dataStore.edit { prefs ->
            prefs[readAloudSpeechRateKey] = rate.coerceIn(
                MIN_READ_ALOUD_SPEECH_RATE,
                MAX_READ_ALOUD_SPEECH_RATE,
            )
        }
    }

    val readAloudVoiceName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[readAloudVoiceNameKey]?.takeIf { it.isNotBlank() }
    }

    suspend fun setReadAloudVoiceName(voiceName: String?) {
        context.dataStore.edit { prefs ->
            if (voiceName.isNullOrBlank()) {
                prefs.remove(readAloudVoiceNameKey)
            } else {
                prefs[readAloudVoiceNameKey] = voiceName
            }
        }
    }

    val speedReaderWordsPerMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        (prefs[speedReaderWordsPerMinuteKey] ?: DEFAULT_SPEED_READER_WPM).coerceIn(
            MIN_SPEED_READER_WPM,
            MAX_SPEED_READER_WPM,
        )
    }

    suspend fun setSpeedReaderWordsPerMinute(wordsPerMinute: Int) {
        context.dataStore.edit { prefs ->
            prefs[speedReaderWordsPerMinuteKey] = wordsPerMinute.coerceIn(
                MIN_SPEED_READER_WPM,
                MAX_SPEED_READER_WPM,
            )
        }
    }

    val speedReaderTheme: Flow<ReaderTheme> = context.dataStore.data.map { prefs ->
        prefs[speedReaderThemeKey]
            ?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() }
            ?: ReaderTheme.DARK
    }

    suspend fun setSpeedReaderTheme(theme: ReaderTheme) {
        context.dataStore.edit { prefs ->
            prefs[speedReaderThemeKey] = theme.name
        }
    }

    val speedReaderFont: Flow<ReaderFont> = context.dataStore.data.map { prefs ->
        prefs[speedReaderFontKey]
            ?.let { runCatching { ReaderFont.valueOf(it) }.getOrNull() }
            ?: ReaderFont.ATKINSON
    }

    suspend fun setSpeedReaderFont(font: ReaderFont) {
        context.dataStore.edit { prefs ->
            prefs[speedReaderFontKey] = font.name
        }
    }

    /** Ids of stories the user has opened, for read/unread styling. */
    val viewedStoryIds: Flow<Set<Long>> = context.dataStore.data.map { prefs ->
        prefs[viewedKey].orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
    }

    suspend fun markViewed(id: Long) {
        context.dataStore.edit { prefs ->
            val updated = prefs[viewedKey].orEmpty() + id.toString()
            // Bound growth; dropping the odd old id just re-bolds a long-read story.
            prefs[viewedKey] = if (updated.size > MAX_VIEWED) updated.drop(updated.size - MAX_VIEWED).toSet() else updated
        }
    }

    private companion object {
        const val MAX_VIEWED = 2000
        const val DEFAULT_READ_ALOUD_SPEECH_RATE = 1f
        const val MIN_READ_ALOUD_SPEECH_RATE = 0.75f
        const val MAX_READ_ALOUD_SPEECH_RATE = 2f
        const val DEFAULT_SPEED_READER_WPM = 350
        const val MIN_SPEED_READER_WPM = 150
        const val MAX_SPEED_READER_WPM = 800
    }
}
