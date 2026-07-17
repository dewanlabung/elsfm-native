package com.elsfm.mobile.feature.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PasswordSaver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun save(email: String, password: String) {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.createCredential(context, CreatePasswordRequest(id = email, password = password))
        } catch (e: CreateCredentialException) {
            Log.w("PasswordSaver", "Credential Manager declined to save the password", e)
        }
    }
}
