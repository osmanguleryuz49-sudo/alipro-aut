package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, LIQUID_GLASS
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dogrula_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val SECURE_SCREEN_ENABLED = booleanPreferencesKey("secure_screen_enabled")
        val HIDE_CODES_ENABLED = booleanPreferencesKey("hide_codes_enabled")
        val AUTO_CLEAR_CLIPBOARD_ENABLED = booleanPreferencesKey("auto_clear_clipboard_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false
    }

    val isSecureScreenEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SECURE_SCREEN_ENABLED] ?: false
    }

    val isHideCodesEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HIDE_CODES_ENABLED] ?: false
    }

    val isAutoClearClipboardEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_CLEAR_CLIPBOARD_ENABLED] ?: true
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(raw)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val isDynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DYNAMIC_COLOR_ENABLED] ?: true
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setSecureScreenEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SECURE_SCREEN_ENABLED] = enabled
        }
    }

    suspend fun setHideCodesEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_CODES_ENABLED] = enabled
        }
    }

    suspend fun setAutoClearClipboardEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CLEAR_CLIPBOARD_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR_ENABLED] = enabled
        }
    }
}
