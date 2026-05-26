package com.example.surtiapp.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_session")

class SessionManager(private val context: Context) {
    companion object {
        private val USER_ID = longPreferencesKey("user_id")
        private val NEGOCIO_ID = longPreferencesKey("negocio_id")
    }

    suspend fun saveSession(userId: Long, negocioId: Long) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
            preferences[NEGOCIO_ID] = negocioId
        }
    }

    val userId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID]
    }

    val negocioId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[NEGOCIO_ID]
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
