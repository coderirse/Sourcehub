package com.example.sourcehub.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sourcehub_settings")

class PreferencesManager(private val context: Context) {

    // Recent searches
    suspend fun addRecentSearch(query: String) {
        val searches = getRecentSearches().toMutableList()
        searches.remove(query)
        searches.add(0, query)
        if (searches.size > MAX_RECENT_SEARCHES) {
            searches.removeAt(searches.lastIndex)
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_RECENT_SEARCHES] = searches.joinToString(SEPARATOR)
        }
    }

    suspend fun getRecentSearches(): List<String> {
        var result = emptyList<String>()
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_RECENT_SEARCHES] ?: ""
            result = raw.split(SEPARATOR).filter { it.isNotEmpty() }
        }
        return result
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { it.remove(KEY_RECENT_SEARCHES) }
    }

    val recentSearchesFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_RECENT_SEARCHES] ?: ""
        raw.split(SEPARATOR).filter { it.isNotEmpty() }
    }

    // Settings
    val wifiOnlyDownload: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIFI_ONLY] ?: true
    }

    suspend fun setWifiOnlyDownload(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WIFI_ONLY] = enabled }
    }

    val biometricLock: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_LOCK] ?: false
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC_LOCK] = enabled }
    }

    // Remote API mode
    val useRemoteApi: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_REMOTE_API] ?: false
    }

    suspend fun setUseRemoteApi(enabled: Boolean) {
        context.dataStore.edit { it[KEY_REMOTE_API] = enabled }
    }

    companion object {
        private val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        private val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only_download")
        private val KEY_BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        private val KEY_REMOTE_API = booleanPreferencesKey("use_remote_api")
        private const val MAX_RECENT_SEARCHES = 10
        private const val SEPARATOR = "|||"
    }
}
