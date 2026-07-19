package com.elsfm.mobile

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.elsfm.mobile.core.designsystem.ConnectivityBanner
import com.elsfm.mobile.core.designsystem.TrackContextMenu
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.auth.SessionEvent
import com.elsfm.mobile.feature.artist.ArtistDetailScreen
import com.elsfm.mobile.feature.auth.EmailVerificationScreen
import com.elsfm.mobile.feature.auth.LoginScreen
import com.elsfm.mobile.feature.auth.PasswordResetScreen
import com.elsfm.mobile.feature.auth.SignupScreen
import com.elsfm.mobile.feature.comments.CommentsScreen
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
import com.elsfm.mobile.feature.player.lyrics.LyricsScreen
import com.elsfm.mobile.feature.profile.ProfileScreen
import com.elsfm.mobile.feature.profile.SettingsScreen
import com.elsfm.mobile.feature.profile.ThemeViewModel
import com.elsfm.mobile.feature.search.SearchScreen
import com.elsfm.mobile.feature.subscriptions.SubscriptionScreen
import com.elsfm.mobile.feature.userprofile.UserProfileScreen
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
private const val ROUTE_EMAIL_VERIFY = "email_verify/{email}"
private const val EMAIL_VERIFY_ARG = "email"
private const val ROUTE_CHANNEL = "channel/{channelId}"
private const val ROUTE_NOTIFICATIONS = "notifications"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_LIKED_SONGS = "liked_songs"
private const val ROUTE_LISTENING_HISTORY = "listening_history"
private const val ROUTE_SUBSCRIPTIONS = "subscriptions"
private const val CHANNEL_ID_ARG = "channelId"
private const val ROUTE_PLAYLIST = "playlist/{playlistId}/{playlistName}?channelId={channelId}"
private const val PLAYLIST_ID_ARG = "playlistId"
private const val PLAYLIST_NAME_ARG = "playlistName"
private const val PLAYLIST_CHANNEL_ID_ARG = "channelId"
private const val NO_CHANNEL_ID = -1

/**
 * `core:network` has no `GET /playlists/{id}` metadata endpoint (only
 * [com.elsfm.mobile.core.network.api.TrackListApi.getPlaylistTracks] exists), so the
 * playlist's id, name, and channelId are threaded through the route itself rather than
 * re-fetched - see the KDoc on [com.elsfm.mobile.feature.library.PlaylistDetailState].
 * channelId must survive the trip: it's how the destination tells curated channel
 * playlists (not owned by the signed-in user) apart from personal ones, to decide
 * whether to show the rename/delete menu.
 */
private fun NavHostController.navigateToPlaylist(playlist: Playlist) {
    val encodedName = URLEncoder.encode(playlist.name, "UTF-8")
    navigate("playlist/${playlist.id}/$encodedName?channelId=${playlist.channelId ?: NO_CHANNEL_ID}")
}

private const val ROUTE_ALBUM = "album/{albumId}"
private const val ALBUM_ID_ARG = "albumId"

private fun NavHostController.navigateToAlbum(albumId: Int) {
    navigate("album/$albumId")
}

private const val ROUTE_USER_PROFILE = "user/{userId}"
private const val USER_ID_ARG = "userId"

private fun NavHostController.navigateToUserProfile(userId: Int) {
    navigate("user/$userId")
}

private const val ROUTE_TRACK_COMMENTS = "track/{trackId}/comments"
private const val COMMENTS_TRACK_ID_ARG = "trackId"

private fun NavHostController.navigateToTrackComments(trackId: Int) {
    navigate("track/$trackId/comments")
}

private const val ROUTE_LYRICS = "lyrics/{trackId}"
private const val TRACK_ID_ARG = "trackId"

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
private val routesWithoutBottomBar = setOf(
    ROUTE_LOGIN,
    ROUTE_SIGNUP,
    ROUTE_PASSWORD_RESET,
    ROUTE_EMAIL_VERIFY,
    ROUTE_HOME,
    ROUTE_PLAYER,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElsfmNavHost(
    navController: NavHostController = rememberNavController(),
    startDestinationViewModel: StartDestinationViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    connectivityViewModel: ConnectivityViewModel = hiltViewModel(),
    deepLinkTrackId: Int? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val startState by startDestinationViewModel.state.collectAsState()
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    val isOnline by connectivityViewModel.isOnline.collectAsState()

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
            // No track-detail screen exists yet, so a track deep link starts playback and
            // opens the full-screen player instead of navigating to a dedicated page. Only
            // acts once the user is signed in - a deep link tapped from the login screen is
            // silently dropped rather than queued.
            val deepLinkPlayerViewModel: PlayerViewModel = hiltViewModel()
            val deepLinkTrackViewModel: DeepLinkTrackViewModel = hiltViewModel()
            LaunchedEffect(deepLinkTrackId, current.restoredUser) {
                val trackId = deepLinkTrackId ?: return@LaunchedEffect
                if (current.restoredUser == null) return@LaunchedEffect
                val track = deepLinkTrackViewModel.getTrack(trackId)
                if (track != null) {
                    deepLinkPlayerViewModel.play(track, listOf(track))
                    navController.navigate(ROUTE_PLAYER)
                }
                onDeepLinkConsumed()
            }

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
                                IconButton(onClick = { navController.navigate(ROUTE_SETTINGS) }) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
                                        // No saveState/restoreState: this is a single flat NavHost,
                                        // not per-tab nested graphs, so restoreState would restore
                                        // whatever screen (e.g. a pushed Playlist) was on top of
                                        // *any* tab when last saved, not necessarily this tab's own
                                        // root. Always land on the tab's root screen instead.
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                inclusive = false
                                            }
                                            launchSingleTop = true
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
                    ConnectivityBanner(isOnline = isOnline)
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
                                onNeedsEmailVerification = { email ->
                                    val encoded = URLEncoder.encode(email, "UTF-8")
                                    navController.navigate("email_verify/$encoded") {
                                        // Keep signup in back stack so user can go back
                                    }
                                },
                                onSigninClick = { navController.popBackStack() },
                            )
                        }
                        composable(
                            route = ROUTE_EMAIL_VERIFY,
                            arguments = listOf(navArgument(EMAIL_VERIFY_ARG) { type = NavType.StringType }),
                        ) { backStackEntry ->
                            val encodedEmail = backStackEntry.arguments?.getString(EMAIL_VERIFY_ARG) ?: ""
                            val email = URLDecoder.decode(encodedEmail, "UTF-8")
                            EmailVerificationScreen(
                                email = email,
                                onVerified = {
                                    navController.navigate(ROUTE_DISCOVERY) { popUpTo(0) }
                                },
                                onBackClick = { navController.popBackStack() },
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
                                onViewLyrics = { trackId -> navController.navigate("lyrics/$trackId") },
                                onViewComments = { trackId -> navController.navigateToTrackComments(trackId) },
                            )
                        }
                        composable(
                            route = ROUTE_LYRICS,
                            arguments = listOf(navArgument(TRACK_ID_ARG) { type = NavType.IntType }),
                        ) { backStackEntry ->
                            val trackId = backStackEntry.arguments?.getInt(TRACK_ID_ARG) ?: return@composable
                            LyricsScreen(
                                trackId = trackId,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(ROUTE_LIBRARY) {
                            LibraryScreen(
                                onPlaylistTap = { playlist -> navController.navigateToPlaylist(playlist) },
                                onAlbumTap = { album -> navController.navigateToAlbum(album.id) },
                                onArtistTap = { artist -> navController.navigate("artist/${artist.id}") },
                                onChannelTap = { channel -> navController.navigate("channel/${channel.id}") },
                                onSongsClicked = { navController.navigate(ROUTE_LIKED_SONGS) },
                                onPlayHistoryClicked = { navController.navigate(ROUTE_LISTENING_HISTORY) },
                            )
                        }
                        composable(ROUTE_LIKED_SONGS) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            LikedSongsScreen(
                                onTrackTap = { track, queue -> playerViewModel.play(track, queue) },
                                onGoToArtist = { artistId -> navController.navigate("artist/$artistId") },
                                onGoToAlbum = { albumId -> navController.navigateToAlbum(albumId) },
                                onGoToTrack = { /* no track-detail screen yet */ },
                                onViewLyrics = { trackId -> navController.navigate("lyrics/$trackId") },
                                onViewComments = { trackId -> navController.navigateToTrackComments(trackId) },
                            )
                        }
                        composable(ROUTE_LISTENING_HISTORY) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            ListeningHistoryScreen(
                                onTrackTap = { track, queue -> playerViewModel.play(track, queue) },
                                onGoToArtist = { artistId -> navController.navigate("artist/$artistId") },
                                onGoToAlbum = { albumId -> navController.navigateToAlbum(albumId) },
                                onGoToTrack = { /* no track-detail screen yet */ },
                                onViewLyrics = { trackId -> navController.navigate("lyrics/$trackId") },
                                onViewComments = { trackId -> navController.navigateToTrackComments(trackId) },
                            )
                        }
                        composable(ROUTE_NOTIFICATIONS) {
                            NotificationsScreen(onBack = { navController.popBackStack() })
                        }
                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = ROUTE_ALBUM,
                            arguments = listOf(navArgument(ALBUM_ID_ARG) { type = NavType.IntType }),
                        ) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            AlbumScreen(
                                onTrackTap = { track, queue -> playerViewModel.play(track, queue) },
                                onPlayAll = { tracks -> tracks.firstOrNull()?.let { playerViewModel.play(it, tracks) } },
                                onViewComments = { trackId -> navController.navigateToTrackComments(trackId) },
                                onGoToArtist = { artistId -> navController.navigate("artist/$artistId") },
                                onGoToAlbum = { albumId -> navController.navigateToAlbum(albumId) },
                                onGoToTrack = { /* no track-detail screen yet */ },
                                onViewLyrics = { trackId -> navController.navigate("lyrics/$trackId") },
                            )
                        }
                        composable(
                            route = ROUTE_PLAYLIST,
                            arguments = listOf(
                                navArgument(PLAYLIST_ID_ARG) { type = NavType.IntType },
                                navArgument(PLAYLIST_NAME_ARG) { type = NavType.StringType },
                                navArgument(PLAYLIST_CHANNEL_ID_ARG) {
                                    type = NavType.IntType
                                    defaultValue = NO_CHANNEL_ID
                                },
                            ),
                        ) { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getInt(PLAYLIST_ID_ARG)
                                ?: return@composable
                            val encodedName = backStackEntry.arguments?.getString(PLAYLIST_NAME_ARG)
                                ?: return@composable
                            val playlistName = URLDecoder.decode(encodedName, "UTF-8")
                            val channelId = backStackEntry.arguments
                                ?.getInt(PLAYLIST_CHANNEL_ID_ARG)
                                ?.takeIf { it != NO_CHANNEL_ID }
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            PlaylistScreen(
                                playlist = Playlist(id = playlistId, name = playlistName, image = null, channelId = channelId),
                                onTrackTap = { track, queue -> playerViewModel.play(track, queue) },
                                onPlayAll = { tracks -> tracks.firstOrNull()?.let { playerViewModel.play(it, tracks) } },
                                onBack = { navController.popBackStack() },
                                onGoToArtist = { artistId -> navController.navigate("artist/$artistId") },
                                onGoToAlbum = { albumId -> navController.navigateToAlbum(albumId) },
                                onGoToTrack = { /* TODO: no track-detail screen yet */ },
                                onViewLyrics = { trackId -> navController.navigate("lyrics/$trackId") },
                                onViewComments = { trackId -> navController.navigateToTrackComments(trackId) },
                            )
                        }
                        composable(
                            route = ROUTE_CHANNEL,
                            arguments = listOf(navArgument(CHANNEL_ID_ARG) { type = NavType.IntType }),
                        ) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            var channelMoreTrack by remember { mutableStateOf<Track?>(null) }
                            Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                                ChannelDetailScreen(
                                    onTrackClicked = { track, queue -> playerViewModel.play(track, queue) },
                                    onPlaylistClicked = { playlist -> navController.navigateToPlaylist(playlist) },
                                    onAlbumClicked = { album -> navController.navigateToAlbum(album.id) },
                                    onChannelClicked = { channelId -> navController.navigate("channel/$channelId") },
                                    onTrackMoreClicked = { track -> channelMoreTrack = track },
                                )
                                channelMoreTrack?.let { track ->
                                    TrackContextMenu(
                                        trackId = track.id,
                                        artistId = track.artists.firstOrNull()?.id,
                                        albumId = track.album?.id,
                                        isVisible = true,
                                        onDismiss = { channelMoreTrack = null },
                                        onAddToQueue = { playerViewModel.addToQueue(track) },
                                        onAddToLibrary = {},
                                        onAddToPlaylist = {},
                                        onShare = {},
                                        onRepost = {},
                                        onGoToArtist = { artistId -> navController.navigate("artist/$artistId") },
                                        onGoToAlbum = { albumId -> navController.navigateToAlbum(albumId) },
                                        onViewComments = { trackId -> navController.navigateToTrackComments(trackId) },
                                    )
                                }
                            }
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
                                onArtistClicked = { otherArtistId ->
                                    // Replaces the current artist rather than pushing on top of it -
                                    // without this, browsing a chain of "similar artists" stacked an
                                    // unbounded number of ArtistDetailScreen instances (each keeping
                                    // its own ViewModel/Compose state alive), which is what made
                                    // returning to Home feel sluggish and need repeated back presses.
                                    navController.navigate("artist/$otherArtistId") {
                                        popUpTo(ROUTE_ARTIST) { inclusive = true }
                                    }
                                },
                                onUserClicked = { userId -> navController.navigateToUserProfile(userId) },
                            )
                        }
                        composable(
                            route = ROUTE_USER_PROFILE,
                            arguments = listOf(navArgument(USER_ID_ARG) { type = NavType.IntType }),
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getInt(USER_ID_ARG) ?: return@composable
                            UserProfileScreen(
                                userId = userId,
                                onBack = { navController.popBackStack() },
                                onUserClicked = { otherUserId -> navController.navigateToUserProfile(otherUserId) },
                            )
                        }
                        composable(
                            route = ROUTE_TRACK_COMMENTS,
                            arguments = listOf(navArgument(COMMENTS_TRACK_ID_ARG) { type = NavType.IntType }),
                        ) { backStackEntry ->
                            val trackId = backStackEntry.arguments?.getInt(COMMENTS_TRACK_ID_ARG) ?: return@composable
                            CommentsScreen(
                                trackId = trackId,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(ROUTE_DISCOVERY) {
                            val playerViewModel: PlayerViewModel = hiltViewModel()
                            var discoveryMoreTrack by remember { mutableStateOf<Track?>(null) }
                            Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                                DiscoveryScreen(
                                    onTrackClicked = { track, queue ->
                                        playerViewModel.play(track, queue)
                                    },
                                    onPlaylistClicked = { playlist -> navController.navigateToPlaylist(playlist) },
                                    onAlbumClicked = { album -> navController.navigateToAlbum(album.id) },
                                    onChannelClicked = { channelId -> navController.navigate("channel/$channelId") },
                                    onTrackMoreClicked = { track -> discoveryMoreTrack = track },
                                )
                                discoveryMoreTrack?.let { track ->
                                    TrackContextMenu(
                                        trackId = track.id,
                                        artistId = track.artists.firstOrNull()?.id,
                                        albumId = track.album?.id,
                                        isVisible = true,
                                        onDismiss = { discoveryMoreTrack = null },
                                        onAddToQueue = { playerViewModel.addToQueue(track) },
                                        onAddToLibrary = {},
                                        onAddToPlaylist = {},
                                        onShare = {},
                                        onRepost = {},
                                        onGoToArtist = { artistId -> navController.navigate("artist/$artistId") },
                                        onGoToAlbum = { albumId -> navController.navigateToAlbum(albumId) },
                                        onViewComments = { trackId -> navController.navigateToTrackComments(trackId) },
                                    )
                                }
                            }
                        }
                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                // Shares the nav-host-scoped instance rather than letting
                                // ProfileScreen create its own via a bare hiltViewModel() -
                                // ThemeViewModel is a plain per-destination Hilt ViewModel,
                                // not an app-wide singleton, so two separate instances would
                                // silently go out of sync and the toggle here would appear
                                // to do nothing to the actual app theme.
                                themeViewModel = themeViewModel,
                                onLogout = {
                                    startDestinationViewModel.logout()
                                    navController.navigate(ROUTE_LOGIN) { popUpTo(0) }
                                },
                                onManageSubscriptionClicked = { navController.navigate(ROUTE_SUBSCRIPTIONS) },
                                onChangePasswordClicked = { navController.navigate(ROUTE_PASSWORD_RESET) },
                            )
                        }
                        composable(ROUTE_SUBSCRIPTIONS) {
                            SubscriptionScreen(onBack = { navController.popBackStack() })
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
