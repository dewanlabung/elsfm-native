package com.elsfm.mobile.feature.profile

import com.elsfm.mobile.core.model.UserProfile
import com.elsfm.mobile.core.model.UserSessionInfo

/**
 * State for [AccountViewModel]: backs the real "Update name and profile image" and
 * (read-only) "Sessions" account settings panels. See [AccountViewModel] for which
 * PWA account-settings features have real backend support and which are skipped.
 */
data class AccountState(
    val account: UserProfile? = null,
    val isSavingName: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val accountError: String? = null,
    val sessions: List<UserSessionInfo> = emptyList(),
    val isLoadingSessions: Boolean = false,
    val sessionsError: String? = null,
)
