package com.trueskies.android.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.trueskies.android.ui.screens.*
import com.trueskies.android.ui.theme.TrueSkiesColors

/**
 * Navigation routes and bottom navigation bar.
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Map : Screen("map", "Map", Icons.Default.Map)
    data object Flights : Screen("flights", "Flights", Icons.Default.AirplanemodeActive)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object More : Screen("more", "More", Icons.Default.MoreHoriz)
}

sealed class DetailScreen(val route: String) {
    data object FlightDetail : DetailScreen("flight/{flightId}") {
        fun createRoute(flightId: String) = "flight/$flightId"
    }
}

val bottomNavItems = listOf(Screen.Map, Screen.Flights, Screen.Search, Screen.More)

@Composable
fun TrueSkiesNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Show bottom bar only on top-level destinations
    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        containerColor = TrueSkiesColors.SurfacePrimary,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = TrueSkiesColors.SurfaceSecondary,
                    contentColor = TrueSkiesColors.TextPrimary
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TrueSkiesColors.AccentBlue,
                                selectedTextColor = TrueSkiesColors.AccentBlue,
                                unselectedIconColor = TrueSkiesColors.TabInactive,
                                unselectedTextColor = TrueSkiesColors.TabInactive,
                                indicatorColor = TrueSkiesColors.AccentBlue.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Map.route) {
                MapScreen(
                    onFlightClick = { flightId ->
                        navController.navigate(DetailScreen.FlightDetail.createRoute(flightId))
                    }
                )
            }
            composable(Screen.Flights.route) {
                FlightsScreen(
                    onFlightClick = { flightId ->
                        navController.navigate(DetailScreen.FlightDetail.createRoute(flightId))
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onFlightClick = { flightId ->
                        navController.navigate(DetailScreen.FlightDetail.createRoute(flightId))
                    }
                )
            }
            composable(Screen.More.route) {
                MoreScreen()
            }
            composable(
                route = DetailScreen.FlightDetail.route,
                arguments = listOf(navArgument("flightId") { type = NavType.StringType })
            ) {
                FlightDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
