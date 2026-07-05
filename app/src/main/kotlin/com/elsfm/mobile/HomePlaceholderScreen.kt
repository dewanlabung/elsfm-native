package com.elsfm.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.model.User

@Composable
fun HomePlaceholderScreen(
    user: User,
    onLogoutClicked: () -> Unit,
    onTrackClicked: (Track, List<Track>) -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val tracks by homeViewModel.tracks.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Logged in as ${user.email}")
        Button(onClick = onLogoutClicked) {
            Text("Log out")
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tracks) { track ->
                Text(
                    text = track.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackClicked(track, tracks) },
                )
            }
        }
    }
}
