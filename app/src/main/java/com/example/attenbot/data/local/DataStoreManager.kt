package com.example.attenbot.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.attenbot.util.UrlUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "attenbot_preferences")

class DataStoreManager(private val context: Context) {

    companion object {
        private val KEY_ESP32_BASE_URL = stringPreferencesKey("esp32_base_url")
    }

    val baseUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_ESP32_BASE_URL] ?: UrlUtils.DEFAULT_BASE_URL
    }

    suspend fun saveBaseUrl(url: String) {
        val normalized = UrlUtils.normalizeBaseUrl(url)
        context.dataStore.edit { preferences ->
            preferences[KEY_ESP32_BASE_URL] = normalized
        }
    }
}
