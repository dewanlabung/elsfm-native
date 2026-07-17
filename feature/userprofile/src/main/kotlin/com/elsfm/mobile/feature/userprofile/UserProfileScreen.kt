package com.elsfm.mobile.feature.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elsfm.mobile.core.model.FollowUser
import com.elsfm.mobile.core.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: Int,
    onBack: () -> Unit = {},
    onUserClicked: (Int) -> Unit = {},
    viewModel: UserProfileViewModel = hiltViewModel(key = userId.toString()),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { Text(state.profile?.name ?: "Profile") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.profile == null -> {
                UserProfileErrorState(
                    message = state.error ?: "Unknown error",
                    onRetry = { viewModel.retryLoad() },
                )
            }
            else -> state.profile?.let { profile ->
                Column(modifier = Modifier.fillMaxSize()) {
                    UserProfileHeader(
                        profile = profile,
                        isFollowing = state.isFollowing,
                        isFollowLoading = state.isFollowLoading,
                        onToggleFollow = { viewModel.toggleFollow(profile.id) },
                    )

                    UserProfileTabRow(
                        selectedTab = state.selectedTab,
                        onTabSelected = { viewModel.selectTab(it) },
                    )

                    when (state.selectedTab) {
                        UserProfileTab.FOLLOWERS -> UserListTab(
                            users = state.followers,
                            isLoading = state.isFollowersLoading,
                            emptyMessage = "No followers yet",
                            followedUserIds = state.followedUserIds,
                            onToggleFollowUser = { viewModel.toggleFollowUser(it) },
                            onUserClicked = onUserClicked,
                        )
                        UserProfileTab.FOLLOWING -> UserListTab(
                            users = state.followedUsers,
                            isLoading = state.isFollowedUsersLoading,
                            emptyMessage = "Not following anyone yet",
                            followedUserIds = state.followedUserIds,
                            onToggleFollowUser = { viewModel.toggleFollowUser(it) },
                            onUserClicked = onUserClicked,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfileErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Error Loading Profile",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfileHeader(
    profile: UserProfile,
    isFollowing: Boolean,
    isFollowLoading: Boolean,
    onToggleFollow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = profile.profileImage,
            contentDescription = profile.name,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = profile.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        profile.bio?.takeIf { it.isNotBlank() }?.let { bio ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = profile.followersCount.toString(), style = MaterialTheme.typography.titleMedium)
                Text(text = "Followers", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = profile.followedCount.toString(), style = MaterialTheme.typography.titleMedium)
                Text(text = "Following", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onToggleFollow,
            enabled = !isFollowLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isFollowing) "Following" else "Follow")
        }
    }
}

@Composable
private fun UserProfileTabRow(
    selectedTab: UserProfileTab,
    onTabSelected: (UserProfileTab) -> Unit,
) {
    val tabs = listOf(
        UserProfileTab.FOLLOWERS to "Followers",
        UserProfileTab.FOLLOWING to "Following",
    )
    TabRow(selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)) {
        tabs.forEach { (tab, label) ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                text = { Text(label) },
            )
        }
    }
}

@Composable
private fun UserListTab(
    users: List<FollowUser>,
    isLoading: Boolean,
    emptyMessage: String,
    followedUserIds: Set<Int>,
    onToggleFollowUser: (Int) -> Unit,
    onUserClicked: (Int) -> Unit,
) {
    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        users.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emptyMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(users, key = { it.id }) { user ->
                UserRow(
                    user = user,
                    isFollowed = followedUserIds.contains(user.id),
                    onToggleFollow = { onToggleFollowUser(user.id) },
                    onClick = { onUserClicked(user.id) },
                )
            }
        }
    }
}

@Composable
private fun UserRow(
    user: FollowUser,
    isFollowed: Boolean,
    onToggleFollow: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = user.image,
            contentDescription = user.name,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = user.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = onToggleFollow) {
            Text(if (isFollowed) "Following" else "Follow")
        }
    }
}
