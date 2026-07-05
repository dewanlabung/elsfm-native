package com.elsfm.mobile.feature.profile

import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.model.UserProfile

data class ProfileState(
    val userProfile: UserProfile? = null,
    val recentlyPlayed: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
)
