package com.elsfm.mobile.core.network.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.elsfm.mobile.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import javax.inject.Inject

private const val PREFS_NAME = "elsfm_secure_session"

class EncryptedTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) : TokenStore {

    private val prefs: SharedPreferences by lazy { createPrefs() }

    private fun createPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: GeneralSecurityException) {
            // The Keystore-backed key can become unreadable (e.g. corrupted after
            // repeated reinstalls, or invalidated by the OS) - the stored token is
            // unrecoverable either way, so reset the file and force a fresh login
            // rather than crashing the app on every launch.
            context.deleteSharedPreferences(PREFS_NAME)
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }

    override suspend fun save(token: String) = withContext(dispatcherProvider.io) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override suspend fun read(): String? = withContext(dispatcherProvider.io) {
        prefs.getString(KEY_TOKEN, null)
    }

    override suspend fun clear() = withContext(dispatcherProvider.io) {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val KEY_TOKEN = "access_token"
    }
}
