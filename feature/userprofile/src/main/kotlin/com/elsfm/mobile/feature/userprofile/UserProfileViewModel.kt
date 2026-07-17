package com.elsfm.mobile.feature.userprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ProfileApi
import com.elsfm.mobile.core.network.api.UserApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val USER_ID_ARG = "userId"

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val profileApi: ProfileApi,
    private val userApi: UserApi,
    private val savedStateHandle: SavedStateHandle,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val dispatcher = dispatcherProvider.io

    private val _state = MutableStateFlow(UserProfileState())
    val state: StateFlow<UserProfileState> = _state.asStateFlow()

    init {
        savedStateHandle.get<Int>(USER_ID_ARG)?.let { userId -> loadProfile(userId) }
    }

    fun retryLoad() {
        savedStateHandle.get<Int>(USER_ID_ARG)?.let { userId -> loadProfile(userId) }
    }

    private fun loadProfile(userId: Int) {
        viewModelScope.launch(dispatcher) {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = profileApi.getProfile(userId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(profile = result.data, isLoading = false)
                }
                is ApiResult.NetworkError -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.cause.message ?: "Network error",
                    )
                }
                is ApiResult.ValidationError -> {
                    _state.value = _state.value.copy(isLoading = false, error = "Validation error")
                }
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(isLoading = false, error = "Unauthorized")
                }
            }
        }
    }

    /**
     * Switches the active profile tab. Followers/followed-users are fetched lazily the first
     * time each tab is selected, matching `ArtistDetailViewModel.selectTab`'s per-tab loading.
     */
    fun selectTab(tab: UserProfileTab) {
        _state.value = _state.value.copy(selectedTab = tab)
        val userId = _state.value.profile?.id ?: return
        when (tab) {
            UserProfileTab.FOLLOWERS -> {
                if (_state.value.followers.isEmpty() && !_state.value.isFollowersLoading) {
                    loadFollowers(userId)
                }
            }
            UserProfileTab.FOLLOWING -> {
                if (_state.value.followedUsers.isEmpty() && !_state.value.isFollowedUsersLoading) {
                    loadFollowedUsers(userId)
                }
            }
        }
    }

    private fun loadFollowers(userId: Int) {
        viewModelScope.launch(dispatcher) {
            _state.value = _state.value.copy(isFollowersLoading = true)
            when (val result = userApi.getFollowers(userId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(followers = result.data, isFollowersLoading = false)
                }
                else -> {
                    _state.value = _state.value.copy(
                        isFollowersLoading = false,
                        error = "Failed to load followers",
                    )
                }
            }
        }
    }

    private fun loadFollowedUsers(userId: Int) {
        viewModelScope.launch(dispatcher) {
            _state.value = _state.value.copy(isFollowedUsersLoading = true)
            when (val result = userApi.getFollowedUsers(userId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(followedUsers = result.data, isFollowedUsersLoading = false)
                }
                else -> {
                    _state.value = _state.value.copy(
                        isFollowedUsersLoading = false,
                        error = "Failed to load followed users",
                    )
                }
            }
        }
    }

    /** Follows/unfollows the profile owner shown at the top of the screen. */
    fun toggleFollow(userId: Int) {
        val wasFollowing = _state.value.isFollowing
        viewModelScope.launch(dispatcher) {
            _state.value = _state.value.copy(isFollowLoading = true)
            val result = if (wasFollowing) userApi.unfollowUser(userId) else userApi.followUser(userId)
            when (result) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isFollowing = !wasFollowing, isFollowLoading = false)
                }
                else -> {
                    _state.value = _state.value.copy(
                        isFollowLoading = false,
                        error = "Failed to update follow state",
                    )
                }
            }
        }
    }

    /** Follows/unfollows a row user from the Followers/Following tabs. */
    fun toggleFollowUser(userId: Int) {
        val wasFollowed = _state.value.followedUserIds.contains(userId)
        viewModelScope.launch(dispatcher) {
            val result = if (wasFollowed) userApi.unfollowUser(userId) else userApi.followUser(userId)
            when (result) {
                is ApiResult.Success -> {
                    val updatedIds = if (wasFollowed) {
                        _state.value.followedUserIds - userId
                    } else {
                        _state.value.followedUserIds + userId
                    }
                    _state.value = _state.value.copy(followedUserIds = updatedIds)
                }
                else -> {
                    _state.value = _state.value.copy(error = "Failed to update follow state")
                }
            }
        }
    }
}
