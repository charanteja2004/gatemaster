package com.gatemaster.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gatemaster.app.core.model.ContentIndex
import com.gatemaster.app.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * Small per-user settings.
 *
 * DataStore rather than SharedPreferences: it is async by construction, so
 * nothing here can block the main thread the way the old app's synchronous
 * preference reads did.
 */
class UserPreferences(private val context: Context) {

    /** The GATE paper the user is preparing for. */
    val branchId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BRANCH] ?: ContentIndex.DEFAULT_BRANCH
    }

    /** False until the user has picked a paper for the first time. */
    val hasChosenBranch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BRANCH] != null
    }

    suspend fun setBranch(branchId: String) {
        context.dataStore.edit { it[KEY_BRANCH] = branchId }
    }

    /**
     * Light, dark, or follow the system.
     *
     * The app owns this rather than deferring to the system, because plenty of
     * people keep their phone in dark mode and still want to read notes on a
     * light page.
     */
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromKey(prefs[KEY_THEME])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.key }
    }

    /** Reader text size, as a WebView zoom percentage. */
    val readerTextZoom: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_TEXT_ZOOM] ?: DEFAULT_TEXT_ZOOM
    }

    suspend fun setReaderTextZoom(percent: Int) {
        context.dataStore.edit { it[KEY_TEXT_ZOOM] = percent }
    }

    private companion object {
        const val DEFAULT_TEXT_ZOOM = 100
        val KEY_BRANCH = stringPreferencesKey("branch_id")
        val KEY_TEXT_ZOOM = intPreferencesKey("reader_text_zoom")
        val KEY_THEME = stringPreferencesKey("theme_mode")
    }
}
