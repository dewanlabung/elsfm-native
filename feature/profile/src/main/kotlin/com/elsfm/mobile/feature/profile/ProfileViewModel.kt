package com.elsfm.mobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.dao.ProfileCacheDao
import com.elsfm.mobile.core.database.entity.ProfileCache
import com.elsfm.mobile.core.model.UserProfile
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ProfileApi
import com.elsfm.mobile.core.network.elsfmJson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileApi: ProfileApi,
    private val userDao: UserDao,
    private val profileCacheDao: ProfileCacheDao,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch(dispatcherProvider.io) {
            loadCachedProfileIfAvailable()
            fetchProfile()
        }
    }

    private suspend fun loadCachedProfileIfAvailable() {
        val cached = profileCacheDao.get() ?: return
        val profile = runCatching {
            elsfmJson().decodeFromString(UserProfile.serializer(), cached.payloadJson)
        }.getOrNull() ?: return

        _state.update { it.copy(userProfile = profile, isLoading = false) }
    }

    private suspend fun fetchProfile() {
        val hadCachedProfile = _state.value.userProfile != null
        if (!hadCachedProfile) {
            _state.update { it.copy(isLoading = true, error = null) }
        }

        val userId = userDao.get()?.id
        if (userId == null) {
            _state.update { it.copy(isLoading = false, error = "Not signed in") }
            return
        }

        when (val result = profileApi.getProfile(userId)) {
            is ApiResult.Success -> {
                _state.update { it.copy(userProfile = result.data, isLoading = false, error = null) }
                cacheProfile(result.data)
                loadRecentlyPlayed()
            }
            is ApiResult.NetworkError -> {
                _state.update {
                    it.copy(
                        isLoading = false,
                        // Keep stale cached content visible; only surface error on fresh install.
                        error = if (hadCachedProfile) null else (result.cause.message ?: "Network error"),
                    )
                }
            }
            is ApiResult.ValidationError -> {
                _state.update { it.copy(isLoading = false, error = "Validation error") }
            }
            is ApiResult.Unauthorized -> {
                _state.update { it.copy(isLoading = false, error = "Unauthorized") }
            }
        }
    }

    private suspend fun cacheProfile(profile: UserProfile) {
        val json = elsfmJson().encodeToString(UserProfile.serializer(), profile)
        profileCacheDao.save(ProfileCache(payloadJson = json))
    }

    private fun loadRecentlyPlayed() {
        viewModelScope.launch(dispatcherProvider.io) {
            // Recently-played failure must not fail the whole profile page.
            when (val result = profileApi.getRecentlyPlayed()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(recentlyPlayed = result.data) }
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    // Silently ignore: recently played is a non-critical section.
                }
            }
        }
    }

    fun updateProfile(name: String, bio: String?) {
        viewModelScope.launch(dispatcherProvider.io) {
            val userId = userDao.get()?.id
            if (userId == null) {
                _state.update { it.copy(error = "Not signed in") }
                return@launch
            }
            when (val result = profileApi.updateProfile(userId, name, bio)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(userProfile = result.data, isEditMode = false) }
                    cacheProfile(result.data)
                }
                is ApiResult.NetworkError -> {
                    _state.update { it.copy(error = result.cause.message ?: "Network error") }
                }
                is ApiResult.ValidationError -> {
                    _state.update { it.copy(error = "Validation error") }
                }
                is ApiResult.Unauthorized -> {
                    _state.update { it.copy(error = "Unauthorized") }
                }
            }
        }
    }

    fun setEditMode(isEditMode: Boolean) {
        _state.update { it.copy(isEditMode = isEditMode) }
    }
}
