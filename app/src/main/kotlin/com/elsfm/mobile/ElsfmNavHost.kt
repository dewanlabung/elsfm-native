package com.elsfm.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.elsfm.mobile.core.network.auth.SessionEvent
import com.elsfm.mobile.feature.auth.LoginScreen

private const val ROUTE_LOGIN = "login"
private const val ROUTE_HOME = "home"

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
            NavHost(navController = navController, startDestination = current.route) {
                composable(ROUTE_LOGIN) {
                    LoginScreen(onLoggedIn = {
                        navController.navigate(ROUTE_HOME) { popUpTo(0) }
                    })
                }
                composable(ROUTE_HOME) {
                    val user = current.restoredUser
                    if (user != null) {
                        HomePlaceholderScreen(
                            user = user,
                            onLogoutClicked = {
                                startDestinationViewModel.logout()
                                navController.navigate(ROUTE_LOGIN) { popUpTo(0) }
                            },
                        )
                    }
                }
            }
        }
    }
}
