package com.elsfm.mobile.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
    onLogout: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    val accountState by accountViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        accountViewModel.loadSessions()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                ErrorScreen(message = state.error ?: "Failed to load profile")
            }

            state.userProfile != null -> {
                val profile = state.userProfile!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    item {
                        ProfileHeader(
                            profile = profile,
                            onEditProfileClicked = { viewModel.setEditMode(true) }
                        )
                        HorizontalDivider()
                    }
                    item {
                        AccountSection(
                            profile = profile,
                            isEditMode = state.isEditMode,
                            onEditProfileClicked = { viewModel.setEditMode(true) },
                            onSaveProfile = { name, bio -> viewModel.updateProfile(name, bio) },
                            onCancelEdit = { viewModel.setEditMode(false) },
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { themeViewModel.setDarkMode(it) },
                            onLogout = onLogout,
                        )
                    }
                    item {
                        AccountDetailsPanel(
                            profile = profile,
                            isSavingName = accountState.isSavingName,
                            isUploadingAvatar = accountState.isUploadingAvatar,
                            accountError = accountState.accountError,
                            onAvatarSelected = { bytes, filename, mimeType ->
                                accountViewModel.uploadAvatar(profile.id, bytes, filename, mimeType)
                            },
                            onRemoveAvatar = { accountViewModel.removeAvatar(profile.id) },
                            onSaveName = { name -> accountViewModel.updateName(profile.id, name) },
                        )
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }
                    item {
                        SessionsPanel(
                            sessions = accountState.sessions,
                            isLoading = accountState.isLoadingSessions,
                            error = accountState.sessionsError,
                        )
                    }
                }
            }

            else -> {
                ErrorScreen(message = "No profile data available")
            }
        }
    }
}

@Composable
fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Unable to Load Profile",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
