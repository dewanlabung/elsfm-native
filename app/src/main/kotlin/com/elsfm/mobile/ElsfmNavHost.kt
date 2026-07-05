package com.elsfm.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.elsfm.mobile.core.network.auth.SessionEvent
import com.elsfm.mobile.feature.auth.LoginScreen
import com.elsfm.mobile.feature.player.MiniPlayer
import com.elsfm.mobile.feature.player.PlayerScreen
import com.elsfm.mobile.feature.player.PlayerViewModel

private const val ROUTE_LOGIN = "login"
private const val ROUTE_HOME = "home"
private const val ROUTE_PLAYER = "player"

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
            Column(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = current.route,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(ROUTE_LOGIN) {
                        LoginScreen(onLoggedIn = {
                            navController.navigate(ROUTE_HOME) { popUpTo(0) }
                        })
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
                        PlayerScreen()
                    }
                }
                MiniPlayer(onExpandClicked = { navController.navigate(ROUTE_PLAYER) })
            }
        }
    }
}
