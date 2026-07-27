package com.shapeshed.kiosk.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reader appearance choice; SYSTEM follows the device light/dark setting. */
enum class ReaderTheme { SYSTEM, LIGHT, DARK, SEPIA }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Persisted user settings, backed by Preferences DataStore. */
class SettingsStore(private val context: Context) {

    private val readerThemeKey = stringPreferencesKey("reader_theme")
    private val readerModeKey = booleanPreferencesKey("reader_mode")
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

    /** Whether articles open in reader view (vs the raw web page). Defaults to reader. */
    val readerModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[readerModeKey] ?: true }

    suspend fun setReaderModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[readerModeKey] = enabled }
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
    }
}
