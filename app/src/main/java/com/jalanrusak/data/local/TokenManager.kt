package com.jalanrusak.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class TokenManager(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }.first()
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.map { preferences ->
            preferences[REFRESH_TOKEN_KEY]
        }.first()
    }

    suspend fun saveUser(userId: String, email: String, name: String?) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USER_EMAIL_KEY] = email
            preferences[USER_NAME_KEY] = name ?: ""
        }
    }

    suspend fun getUserId(): String? {
        return context.dataStore.map { preferences ->
            preferences[USER_ID_KEY]
        }.first()
    }

    suspend fun getUserEmail(): String? {
        return context.dataStore.map { preferences ->
            preferences[USER_EMAIL_KEY]
        }.first()
    }

    suspend fun getUserName(): String? {
        return context.dataStore.map { preferences ->
            preferences[USER_NAME_KEY]
        }.first().takeIf { it.isNotEmpty() }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }

    // Convenience method for non-coroutine contexts
    fun getAccessTokenSync(): String? = runBlocking { getAccessToken() }

    fun isLoggedInSync(): Boolean = runBlocking { isLoggedIn() }
}
