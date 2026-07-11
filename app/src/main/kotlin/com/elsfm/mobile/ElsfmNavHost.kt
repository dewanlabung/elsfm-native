package com.elsfm.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
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
import com.elsfm.mobile.feature.library.AlbumScreen
import com.elsfm.mobile.feature.library.LibraryScreen
import com.elsfm.mobile.feature.library.LikedSongsScreen
import com.elsfm.mobile.feature.library.ListeningHistoryScreen
import com.elsfm.mobile.feature.library.PlaylistScreen
import com.elsfm.mobile.feature.notifications.NotificationsScreen
import com.elsfm.mobile.feature.player.MiniPlayer
import com.elsfm.mobile.feature.player.PlayerScreen
import com.elsfm.mobile.feature.player.PlayerViewModel
import com.elsfm.mobile.feature.profile.ProfileScreen
import com.elsfm.mobile.feature.profile.ThemeViewModel
import com.elsfm.mobile.feature.search.SearchScreen
import com.elsfm.mobile.core.model.Playlist
import java.net.URLDecoder
import java.net.URLEncoder

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
private const val ROUTE_PLAYLIST = "playlist/{playlistId}/{playlistName}"
private const val PLAYLIST_ID_ARG = "playlistId"
private const val PLAYLIST_NAME_ARG = "playlistName"

/**
 * `core:network` has no `GET /playlists/{id}` metadata endpoint (only
 * [com.elsfm.mobile.core.network.api.TrackListApi.getPlaylistTracks] exists), so the
 * playlist's id and name are threaded through the route itself rather than re-fetched -
 * see the KDoc on [com.elsfm.mobile.feature.library.PlaylistDetailState].
 */
private fun NavHostController.navigateToPlaylist(playlist: Playlist) {
    val encodedName = URLEncoder.encode(playlist.name, "UTF-8")
    navigate("playlist/${playlist.id}/$encodedName")
}

private const val ROUTE_ALBUM = "album/{albumId}"
private const val ALBUM_ID_ARG = "albumId"

private fun NavHostController.navigateToAlbum(albumId: Int) {
    navigate("album/$albumId")
}

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(ROUTE_DISCOVERY, "Discovery", Icons.Filled.Home),
    BottomTab(ROUTE_LIBRARY, "Library", Icons.Filled.List),
    BottomTab(ROUTE_DOWNLOADS, "Download", Icons.Filled.Download),
    BottomTab(ROUTE_PROFILE, "Account", Icons.Filled.Person),
)

/**
 * Routes that show the shared top bar (app title, search, theme toggle, notifications) -
 * the bottom nav tabs plus Search, which moved to a header icon instead of its own tab.
 */
private val primaryRoutesForTopBar = bottomTabs.map { it.route }.toSet() + ROUTE_SEARCH

/** Routes with no bottom nav: the pre-auth flow and the full-screen Now Playing player. */
private val routesWithoutBottomBar = setOf(ROUTE_LOGIN, ROUTE_SIGNUP, ROUTE_PASSWORD_RESET, ROUTE_HOME, ROUTE_PLAYER)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElsfmNavHost(
    navController: NavHostController = rememberNavController(),
    startDestinationViewModel: StartDestinationViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val startState by startDestinationViewModel.state.collectAsState()
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()

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
            val showTopBar = currentRoute?.hierarchy?.any { destination ->
                destination.route in primaryRoutesForTopBar
            } == true
            val showBottomBar = currentRoute?.route !in routesWithoutBottomBar

            Scaffold(
                topBar = {
                    if (showTopBar) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "ELSFM",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            },
                            actions = {
                                IconButton(onClick = { navController.navigate(ROUTE_SEARCH) }) {
                                    Icon(Icons.Filled.Search, contentDescription = "Search")
                                }
                                IconButton(onClick = { themeViewModel.toggleTheme() }) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                        contentDescription = if (isDarkMode) "Switch to light mode" else "Switch to dark mode",
                                    )
                                }
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
                                onLoginSuccess = { navController.navigate(ROUTE_DISCOVERY) { popUpTo(0) } },
                                onForgotPasswordClick = { navController.navigate(ROUTE_PASSWORD_RESET) },
                                onSignupClick = { navController.navigate(ROUTE_SIGNUP) },
                            )
                        }
                        composable(ROUTE_SIGNUP) {
                            SignupScreen(
                                onSignupSuccess = { navController.navigate(ROUTE_DISCOVERY) { popUpTo(0) } },
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
                                onGoToAlbum = { albumId -> navController.navigateToAlbum(albumId) },
                                onGoToTrack = { /* TODO: no track-detail screen yet */ },
                                onViewLyrics = { /* TODO: no lyrics screen yet - LyricsApi exists, UI not built */ },
                            )
                        }
                        composable(ROUTE_LIBRARY) {
                            LibraryScreen(
                                onPlaylistTap = { playlist -> navController.navigateToPlaylist(playlist) },
                                onAlbumTap = { album -> navController.navigateToAlbum(album.id) },
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
                            route = ROUTE_ALBUM,
                            arguments = listOf(navArgument(ALBUM_ID_ARG) { type = NavType.IntType }),
                        ) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            AlbumScreen(
                                onTrackTap = { track, queue -> playerViewModel.play(track, queue) },
                                onPlayAll = { tracks -> tracks.firstOrNull()?.let { playerViewModel.play(it, tracks) } },
                            )
                        }
                        composable(
                            route = ROUTE_PLAYLIST,
                            arguments = listOf(
                                navArgument(PLAYLIST_ID_ARG) { type = NavType.IntType },
                                navArgument(PLAYLIST_NAME_ARG) { type = NavType.StringType },
                            ),
                        ) { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getInt(PLAYLIST_ID_ARG)
                                ?: return@composable
                            val encodedName = backStackEntry.arguments?.getString(PLAYLIST_NAME_ARG)
                                ?: return@composable
                            val playlistName = URLDecoder.decode(encodedName, "UTF-8")
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            PlaylistScreen(
                                playlist = Playlist(id = playlistId, name = playlistName, image = null, channelId = null),
                                onTrackTap = { track, queue -> playerViewModel.play(track, queue) },
                                onPlayAll = { tracks -> tracks.firstOrNull()?.let { playerViewModel.play(it, tracks) } },
                            )
                        }
                        composable(
                            route = ROUTE_CHANNEL,
                            arguments = listOf(navArgument(CHANNEL_ID_ARG) { type = NavType.IntType }),
                        ) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            ChannelDetailScreen(
                                onTrackClicked = { track, queue -> playerViewModel.play(track, queue) },
                                onPlaylistClicked = { playlist -> navController.navigateToPlaylist(playlist) },
                                onAlbumClicked = { album -> navController.navigateToAlbum(album.id) },
                                onChannelClicked = { channelId -> navController.navigate("channel/$channelId") },
                            )
                        }
                        composable(ROUTE_SEARCH) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            SearchScreen(
                                onTrackTap = { track, queue -> playerViewModel.play(track, queue) },
                                onAlbumTap = { album -> navController.navigateToAlbum(album.id) },
                                onArtistTap = { artist -> navController.navigate("artist/${artist.id}") },
                                onPlaylistTap = { playlist -> navController.navigateToPlaylist(playlist) },
                            )
                        }
                        composable(
                            route = ROUTE_ARTIST,
                            arguments = listOf(navArgument("artistId") { type = NavType.IntType }),
                        ) { backStackEntry ->
                            val artistId = backStackEntry.arguments?.getInt("artistId")
                                ?: return@composable
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            ArtistDetailScreen(
                                artistId = artistId,
                                onTrackClicked = { track, queue -> playerViewModel.play(track, queue) },
                                onAlbumClicked = { albumId -> navController.navigateToAlbum(albumId) },
                                onArtistClicked = { otherArtistId -> navController.navigate("artist/$otherArtistId") },
                            )
                        }
                        composable(ROUTE_DISCOVERY) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            DiscoveryScreen(
                                onTrackClicked = { track, queue ->
                                    playerViewModel.play(track, queue)
                                },
                                onPlaylistClicked = { playlist -> navController.navigateToPlaylist(playlist) },
                                onAlbumClicked = { album -> navController.navigateToAlbum(album.id) },
                                onChannelClicked = { channelId -> navController.navigate("channel/$channelId") },
                            )
                        }
                        composable(ROUTE_PROFILE) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            ProfileScreen(
                                // Shares the nav-host-scoped instance rather than letting
                                // ProfileScreen create its own via a bare hiltViewModel() -
                                // ThemeViewModel is a plain per-destination Hilt ViewModel,
                                // not an app-wide singleton, so two separate instances would
                                // silently go out of sync and the toggle here would appear
                                // to do nothing to the actual app theme.
                                themeViewModel = themeViewModel,
                                onTrackClicked = { track, queue ->
                                    playerViewModel.play(track, queue)
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
