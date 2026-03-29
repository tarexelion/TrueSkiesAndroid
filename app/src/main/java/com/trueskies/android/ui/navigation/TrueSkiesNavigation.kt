package com.trueskies.android.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.trueskies.android.ui.screens.*
import com.trueskies.android.ui.theme.*

/**
 * Main navigation tabs — mirrors iOS MainTab enum.
 * 3 primary tabs: My Flights, Friends' Flights, Flight Log.
 */
enum class MainTab(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    MY_FLIGHTS("My Flights", Icons.Default.FlightTakeoff, "my_flights"),
    FRIENDS("Friends' Flights", Icons.Default.People, "friends"),
    FLIGHT_LOG("Flight Log", Icons.Default.Language, "flight_log")
}

/**
 * Root navigation host.
 * Floating pill-shaped bottom nav bar matching iOS LiquidGlass style.
 */
@Composable
fun TrueSkiesNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val tabRoutes = MainTab.entries.map { it.route }.toSet()
    val isTabScreen = currentRoute in tabRoutes || currentRoute == null

    Box(modifier = Modifier.fillMaxSize()) {
        // Content area — fills entire screen
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
                    onSettingsClick = { navController.navigate("settings") },
                    onFlightClick = { flightId ->
                        navController.navigate("flight/$flightId")
                    }
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

        // Floating pill nav bar
        if (isTabScreen) {
            FloatingNavBar(
                currentRoute = currentRoute,
                onTabSelected = { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(MainTab.MY_FLIGHTS.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = TrueSkiesSpacing.sm)
            )
        }
    }
}

@Composable
private fun FloatingNavBar(
    currentRoute: String?,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(TrueSkiesCornerRadius.pill))
            .background(Color(0xFF1C1C1E).copy(alpha = 0.92f))
            .padding(horizontal = TrueSkiesSpacing.xs, vertical = TrueSkiesSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MainTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route ||
                (currentRoute == null && tab == MainTab.MY_FLIGHTS)

            val bgColor by animateColorAsState(
                if (selected) TrueSkiesColors.AccentBlue else Color.Transparent,
                label = "tabBg"
            )
            val contentColor by animateColorAsState(
                if (selected) Color.White else TrueSkiesColors.TabInactive,
                label = "tabContent"
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(TrueSkiesCornerRadius.pill))
                    .background(bgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(tab) }
                    .padding(horizontal = TrueSkiesSpacing.md, vertical = TrueSkiesSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(TrueSkiesSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                if (selected) {
                    Text(
                        text = tab.title,
                        style = TrueSkiesTypography.labelMedium,
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
