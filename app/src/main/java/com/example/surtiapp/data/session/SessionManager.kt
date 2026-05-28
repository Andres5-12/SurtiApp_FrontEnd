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
        private val BASE_CAJA = longPreferencesKey("base_caja") // Guardamos como Long (centavos o entero) o Double
    }

    suspend fun saveSession(userId: Long, negocioId: Long) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
            preferences[NEGOCIO_ID] = negocioId
        }
    }

    suspend fun saveBaseCaja(monto: Double) {
        context.dataStore.edit { preferences ->
            // DataStore preferences no soporta Double directamente de forma nativa en todas las versiones
            // así que lo guardamos como bits de Long para no perder precisión
            preferences[BASE_CAJA] = java.lang.Double.doubleToRawLongBits(monto)
        }
    }

    val baseCaja: Flow<Double> = context.dataStore.data.map { preferences ->
        val bits = preferences[BASE_CAJA] ?: 0L
        java.lang.Double.longBitsToDouble(bits)
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
