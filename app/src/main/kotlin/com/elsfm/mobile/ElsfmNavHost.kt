package com.elsfm.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.elsfm.mobile.core.network.auth.SessionEvent
import com.elsfm.mobile.feature.artist.ArtistDetailScreen
import com.elsfm.mobile.feature.auth.LoginScreen
import com.elsfm.mobile.feature.auth.PasswordResetScreen
import com.elsfm.mobile.feature.auth.SignupScreen
import com.elsfm.mobile.feature.discovery.ChannelDetailScreen
import com.elsfm.mobile.feature.discovery.DiscoveryScreen
import com.elsfm.mobile.feature.downloads.DownloadsScreen
import com.elsfm.mobile.feature.library.LibraryScreen
import com.elsfm.mobile.feature.library.LikedSongsScreen
import com.elsfm.mobile.feature.library.ListeningHistoryScreen
import com.elsfm.mobile.feature.notifications.NotificationsScreen
import com.elsfm.mobile.feature.player.MiniPlayer
import com.elsfm.mobile.feature.player.PlayerScreen
import com.elsfm.mobile.feature.player.PlayerViewModel
import com.elsfm.mobile.feature.profile.ProfileScreen
import com.elsfm.mobile.feature.search.SearchScreen

private const val ROUTE_LOGIN = "login"
private const val ROUTE_HOME = "home"
private const val ROUTE_PLAYER = "player"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_ARTIST = "artist/{artistId}"
private const val ROUTE_DISCOVERY = "discovery"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_DOWNLOADS = "downloads"
private const val ROUTE_SIGNUP = "signup"
private const val ROUTE_PASSWORD_RESET = "password_reset"
private const val ROUTE_CHANNEL = "channel/{channelId}"
private const val ROUTE_NOTIFICATIONS = "notifications"
private const val ROUTE_LIKED_SONGS = "liked_songs"
private const val ROUTE_LISTENING_HISTORY = "listening_history"
private const val CHANNEL_ID_ARG = "channelId"

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(ROUTE_DISCOVERY, "Discovery", Icons.Filled.Home),
    BottomTab(ROUTE_LIBRARY, "Library", Icons.Filled.List),
    BottomTab(ROUTE_SEARCH, "Search", Icons.Filled.Search),
    BottomTab(ROUTE_PROFILE, "Profile", Icons.Filled.Person),
    BottomTab(ROUTE_DOWNLOADS, "Downloads", Icons.Filled.Star),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElsfmNavHost(
    navController: NavHostController = rememberNavController(),
    startDestinationViewModel: StartDestinationViewModel = hiltViewModel(),
) {
    val startState by startDestinationViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        startDestinationViewModel.sessionEvents.collect { event ->
            if (event is SessionEvent.Expired) {
                navController.navigate(ROUTE_LOGIN) {
                    popUpTo(0)
                }
            }
        }
    }

    when (val current = startState) {
        StartDestinationState.Loading -> Unit
        is StartDestinationState.Resolved -> {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination
            val showBottomBar = currentRoute?.hierarchy?.any { destination ->
                bottomTabs.any { it.route == destination.route }
            } == true

            Scaffold(
                topBar = {
                    if (showBottomBar) {
                        TopAppBar(
                            title = {},
                            actions = {
                                IconButton(onClick = { navController.navigate(ROUTE_NOTIFICATIONS) }) {
                                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                                }
                            },
                        )
                    }
                },
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            bottomTabs.forEach { tab ->
                                val selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    NavHost(
                        navController = navController,
                        startDestination = if (current.route == ROUTE_LOGIN) ROUTE_LOGIN else ROUTE_DISCOVERY,
                        modifier = Modifier.weight(1f),
                    ) {
                        composable(ROUTE_LOGIN) {
                            LoginScreen(
                                onForgotPasswordClick = { navController.navigate(ROUTE_PASSWORD_RESET) },
                                onSignupClick = { navController.navigate(ROUTE_SIGNUP) },
                            )
                        }
                        composable(ROUTE_SIGNUP) {
                            SignupScreen(
                                onSignupSuccess = { navController.navigate(ROUTE_HOME) { popUpTo(0) } },
                                onSigninClick = { navController.popBackStack() },
                            )
                        }
                        composable(ROUTE_PASSWORD_RESET) {
                            PasswordResetScreen(
                                onBackClick = { navController.popBackStack() },
                            )
                        }
                        composable(ROUTE_HOME) {
                            val user = current.restoredUser
                            if (user != null) {
                                val playerViewModel: PlayerViewModel = hiltViewModel()
                                HomePlaceholderScreen(
                                    user = user,
                                    onLogoutClicked = {
                                        startDestinationViewModel.logout()
                                        navController.navigate(ROUTE_LOGIN) { popUpTo(0) }
                                    },
                                    onTrackClicked = { track, queue ->
                                        playerViewModel.play(track, queue)
                                    },
                                )
                            }
                        }
                        composable(ROUTE_PLAYER) {
                            PlayerScreen(
                                onCollapse = { navController.popBackStack() },
                                onGoToArtist = { artistId -> navController.navigate("artist/$artistId") },
                                onGoToAlbum = { /* TODO: navigate to album detail */ },
                                onGoToTrack = { /* TODO: no track-detail screen yet */ },
                                onViewLyrics = { /* TODO: no lyrics screen yet - LyricsApi exists, UI not built */ },
                            )
                        }
                        composable(ROUTE_LIBRARY) {
                            LibraryScreen(
                                onPlaylistTap = { /* TODO: navigate to playlist detail */ },
                                onAlbumTap = { /* TODO: navigate to album detail */ },
                                onChannelTap = { channel -> navController.navigate("channel/${channel.id}") },
                                onSongsClicked = { navController.navigate(ROUTE_LIKED_SONGS) },
                                onPlayHistoryClicked = { navController.navigate(ROUTE_LISTENING_HISTORY) },
                            )
                        }
                        composable(ROUTE_LIKED_SONGS) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            LikedSongsScreen(
                                onTrackTap = { track, queue -> playerViewModel.play(track, queue) },
                            )
                        }
                        composable(ROUTE_LISTENING_HISTORY) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            ListeningHistoryScreen(
                                onTrackTap = { track, queue -> playerViewModel.play(track, queue) },
                            )
                        }
                        composable(ROUTE_NOTIFICATIONS) {
                            NotificationsScreen(onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = ROUTE_CHANNEL,
                            arguments = listOf(navArgument(CHANNEL_ID_ARG) { type = NavType.IntType }),
                        ) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            ChannelDetailScreen(
                                onTrackClicked = { track, queue -> playerViewModel.play(track, queue) },
                                onPlaylistClicked = { /* TODO: navigate to playlist detail */ },
                                onAlbumClicked = { /* TODO: navigate to album detail */ },
                                onChannelClicked = { channelId -> navController.navigate("channel/$channelId") },
                            )
                        }
                        composable(ROUTE_SEARCH) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            SearchScreen(
                                onTrackTap = { track -> playerViewModel.play(track, queue = emptyList()) },
                                onAlbumTap = { /* TODO: navigate to album detail */ },
                                onArtistTap = { artist -> navController.navigate("artist/${artist.id}") },
                                onPlaylistTap = { /* TODO: navigate to playlist detail */ },
                            )
                        }
                        composable(ROUTE_ARTIST) { backStackEntry ->
                            val artistId = backStackEntry.arguments
                                ?.getString("artistId")
                                ?.toIntOrNull()
                                ?: return@composable
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            ArtistDetailScreen(
                                artistId = artistId,
                                onTrackClicked = { track, queue -> playerViewModel.play(track, queue) },
                                onAlbumClicked = { /* TODO: navigate to album detail */ },
                                onArtistClicked = { otherArtistId -> navController.navigate("artist/$otherArtistId") },
                            )
                        }
                        composable(ROUTE_DISCOVERY) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            DiscoveryScreen(
                                onTrackClicked = { track, queue ->
                                    playerViewModel.play(track, queue)
                                },
                                onChannelClicked = { channelId -> navController.navigate("channel/$channelId") },
                            )
                        }
                        composable(ROUTE_PROFILE) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            ProfileScreen(
                                onTrackClicked = { track ->
                                    playerViewModel.play(track, queue = emptyList())
                                },
                                onLogout = {
                                    startDestinationViewModel.logout()
                                    navController.navigate(ROUTE_LOGIN) { popUpTo(0) }
                                },
                            )
                        }
                        composable(ROUTE_DOWNLOADS) {
                            DownloadsScreen()
                        }
                    }
                    MiniPlayer(onExpandClicked = { navController.navigate(ROUTE_PLAYER) })
                }
            }
        }
    }
}
