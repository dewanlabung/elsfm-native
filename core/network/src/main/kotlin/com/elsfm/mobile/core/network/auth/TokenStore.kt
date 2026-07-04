package com.elsfm.mobile.core.network.auth

interface TokenStore {
    suspend fun save(token: String)
    suspend fun read(): String?
    suspend fun clear()
}
