package com.trueskies.android.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.trueskies.android.ui.screens.*
import com.trueskies.android.ui.theme.TrueSkiesColors
import com.trueskies.android.ui.theme.TrueSkiesCornerRadius

/**
 * Navigation routes.
 */
sealed class DetailScreen(val route: String) {
    data object FlightDetail : DetailScreen("flight/{flightId}") {
        fun createRoute(flightId: String) = "flight/$flightId"
    }
}

/**
 * Root navigation host — map-centric layout with bottom sheet.
 * Replaces the old 4-tab NavigationBar approach.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrueSkiesNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show map + sheet only on the "home" route
    val isHome = currentRoute == "home" || currentRoute == null

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("home") {
            HomeScreen(
                onFlightClick = { flightId ->
                    navController.navigate(DetailScreen.FlightDetail.createRoute(flightId))
                }
            )
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
