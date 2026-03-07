package com.trueskies.android.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.trueskies.android.ui.screens.*
import com.trueskies.android.ui.theme.*

/**
 * Main navigation tabs — mirrors iOS MainTab enum.
 * 3 primary tabs: My Flights, Friends, Flight Log.
 */
enum class MainTab(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    MY_FLIGHTS("My Flights", Icons.Default.FlightTakeoff, "my_flights"),
    FRIENDS("Friends", Icons.Default.People, "friends"),
    FLIGHT_LOG("Flight Log", Icons.Default.Language, "flight_log")
}

/**
 * Root navigation host.
 * Bottom NavigationBar always visible on tab screens, matching iOS tab bar placement.
 * Flight detail pushes as a full-screen destination over the active tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrueSkiesNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val tabRoutes = MainTab.entries.map { it.route }.toSet()
    val isTabScreen = currentRoute in tabRoutes || currentRoute == null

    Scaffold(
        bottomBar = {
            if (isTabScreen) {
                NavigationBar(
                    containerColor = TrueSkiesColors.SurfaceSecondary,
                    tonalElevation = 0.dp
                ) {
                    MainTab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route ||
                            (currentRoute == null && tab == MainTab.MY_FLIGHTS)
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(MainTab.MY_FLIGHTS.route) {
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
        },
        containerColor = TrueSkiesColors.SurfacePrimary
    ) { innerPadding ->
        // Only apply bottom padding — content extends behind status bar (edge-to-edge)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = MainTab.MY_FLIGHTS.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(MainTab.MY_FLIGHTS.route) {
                    HomeScreen(
                        onFlightClick = { flightId ->
                            navController.navigate("flight/$flightId")
                        }
                    )
                }
                composable(MainTab.FRIENDS.route) {
                    FriendsScreen(
                        onFlightClick = { flightId ->
                            navController.navigate("flight/$flightId")
                        }
                    )
                }
                composable(MainTab.FLIGHT_LOG.route) {
                    FlightLogScreen(
                        onSettingsClick = { navController.navigate("settings") }
                    )
                }
                composable("settings") {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
                composable("about") {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = "flight/{flightId}",
                    arguments = listOf(navArgument("flightId") { type = NavType.StringType })
                ) {
                    FlightDetailScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
