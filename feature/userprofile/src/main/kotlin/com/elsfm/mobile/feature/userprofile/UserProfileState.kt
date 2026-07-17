package com.elsfm.mobile.feature.userprofile

import com.elsfm.mobile.core.model.FollowUser
import com.elsfm.mobile.core.model.UserProfile

/** Tabs on the user profile screen, matching [com.elsfm.mobile.feature.artist.ArtistTab]'s pattern for the Followers tab. */
enum class UserProfileTab {
    FOLLOWERS,
    FOLLOWING,
}

/**
 * Immutable, hoisted UI state for [UserProfileScreen].
 *
 * There is no "am I following this user" field on the `GET user-profile/{id}` response
 * ([com.elsfm.mobile.core.network.api.ProfileApi.getProfile] -> [UserProfile]), and
 * `POST users/{id}/follow`/`unfollow` return no state payload either, so [isFollowing]
 * starts `false` on every screen open and only reflects toggles made this session - same
 * limitation already present in `ArtistDetailViewModel.followedUserIds`.
 */
data class UserProfileState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollowLoading: Boolean = false,
    val selectedTab: UserProfileTab = UserProfileTab.FOLLOWERS,
    val followers: List<FollowUser> = emptyList(),
    val isFollowersLoading: Boolean = false,
    val followedUsers: List<FollowUser> = emptyList(),
    val isFollowedUsersLoading: Boolean = false,
    /**
     * User ids the current viewer follows, among the rows shown in [followers]/[followedUsers].
     * Tracked separately from [isFollowing] (which is only the profile owner's follow state)
     * since each row in those lists needs its own independent follow/unfollow affordance,
     * matching `ArtistDetailState.followedUserIds`.
     */
    val followedUserIds: Set<Int> = emptySet(),
    val error: String? = null,
)
