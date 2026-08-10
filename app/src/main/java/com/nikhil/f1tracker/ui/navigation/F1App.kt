package com.nikhil.f1tracker.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nikhil.f1tracker.ui.compare.CompareRoute
import com.nikhil.f1tracker.ui.driver.DriverDetailRoute
import com.nikhil.f1tracker.ui.favorites.FavoritesRoute
import com.nikhil.f1tracker.ui.grandprix.GrandPrixDetailRoute
import com.nikhil.f1tracker.ui.home.HomeRoute
import com.nikhil.f1tracker.ui.standings.StandingsRoute
import com.nikhil.f1tracker.ui.team.TeamDetailRoute

private object F1Destinations {
    const val HOME = "home"
    const val STANDINGS = "standings"
    const val COMPARE = "compare"
    const val FAVORITES = "favorites"
    const val DRIVER_DETAIL = "driverDetail/{driverId}"
    const val TEAM_DETAIL = "teamDetail/{constructorId}"
    const val GRAND_PRIX_DETAIL = "grandPrixDetail/{circuitId}?driverId={driverId}"

    fun driverDetail(driverId: String) = "driverDetail/$driverId"
    fun teamDetail(constructorId: String) = "teamDetail/$constructorId"
    fun grandPrixDetail(circuitId: String, driverId: String? = null) =
        if (driverId == null) "grandPrixDetail/$circuitId" else "grandPrixDetail/$circuitId?driverId=$driverId"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(F1Destinations.HOME, "Home") { Icon(Icons.Filled.Home, contentDescription = null) },
    TopLevelDestination(F1Destinations.STANDINGS, "Standings") {
        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
    },
    TopLevelDestination(F1Destinations.COMPARE, "Compare") {
        Text("VS", style = MaterialTheme.typography.titleMedium)
    },
    TopLevelDestination(F1Destinations.FAVORITES, "Favorites") { Icon(Icons.Filled.Star, contentDescription = null) },
)

@Composable
fun F1App(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = topLevelDestinations.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = destination.icon,
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = F1Destinations.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(F1Destinations.HOME) {
                HomeRoute(
                    onDriverClick = { driverId -> navController.navigate(F1Destinations.driverDetail(driverId)) },
                    onTeamClick = { constructorId -> navController.navigate(F1Destinations.teamDetail(constructorId)) },
                    onGrandPrixClick = { circuitId -> navController.navigate(F1Destinations.grandPrixDetail(circuitId)) },
                )
            }
            composable(F1Destinations.STANDINGS) {
                StandingsRoute(
                    onDriverClick = { driverId -> navController.navigate(F1Destinations.driverDetail(driverId)) },
                    onTeamClick = { constructorId -> navController.navigate(F1Destinations.teamDetail(constructorId)) },
                )
            }
            composable(F1Destinations.COMPARE) {
                CompareRoute()
            }
            composable(F1Destinations.FAVORITES) {
                FavoritesRoute()
            }
            composable(
                route = F1Destinations.DRIVER_DETAIL,
                arguments = listOf(navArgument("driverId") { type = NavType.StringType }),
            ) { entry ->
                val driverId = entry.arguments?.getString("driverId").orEmpty()
                DriverDetailRoute(
                    onBackClick = { navController.navigateUp() },
                    onResultClick = { circuitId ->
                        navController.navigate(F1Destinations.grandPrixDetail(circuitId, driverId))
                    },
                )
            }
            composable(
                route = F1Destinations.TEAM_DETAIL,
                arguments = listOf(navArgument("constructorId") { type = NavType.StringType }),
            ) {
                TeamDetailRoute(onBackClick = { navController.navigateUp() })
            }
            composable(
                route = F1Destinations.GRAND_PRIX_DETAIL,
                arguments = listOf(
                    navArgument("circuitId") { type = NavType.StringType },
                    navArgument("driverId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                GrandPrixDetailRoute(onBackClick = { navController.navigateUp() })
            }
        }
    }
}
