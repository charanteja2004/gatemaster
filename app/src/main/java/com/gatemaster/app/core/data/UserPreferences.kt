package com.gatemaster.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gatemaster.app.core.model.ContentIndex
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

    private companion object {
        val KEY_BRANCH = stringPreferencesKey("branch_id")
    }
}
