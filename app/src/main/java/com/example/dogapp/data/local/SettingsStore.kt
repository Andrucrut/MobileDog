package com.example.dogapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "dogapp_settings")

class SettingsStore(private val context: Context) {
    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    private val compactUiKey = booleanPreferencesKey("compact_ui")
    private val hideNotificationsPreviewKey = booleanPreferencesKey("hide_notifications_preview")

    val darkThemeFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[darkThemeKey] ?: false }
    val compactUiFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[compactUiKey] ?: false }
    val hideNotificationsPreviewFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[hideNotificationsPreviewKey] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { it[darkThemeKey] = enabled }
    }

    suspend fun setCompactUi(enabled: Boolean) {
        context.settingsDataStore.edit { it[compactUiKey] = enabled }
    }

    suspend fun setHideNotificationsPreview(enabled: Boolean) {
        context.settingsDataStore.edit { it[hideNotificationsPreviewKey] = enabled }
    }
}
