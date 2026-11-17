package com.triplog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.triplog.ui.expenses.FuelExpenseScreen
import com.triplog.ui.home.HomeScreen
import com.triplog.ui.logtrip.LogTripScreen
import com.triplog.ui.trips.AllTripsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object LogTrip : Screen("log_trip")
    data object Expenses : Screen("expenses")
    data object AllTrips : Screen("all_trips")
}

@Composable
fun TripLogNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToLogTrip = { navController.navigate(Screen.LogTrip.route) { launchSingleTop = true } },
                onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) { launchSingleTop = true } },
                onNavigateToAllTrips = { navController.navigate(Screen.AllTrips.route) { launchSingleTop = true } }
            )
        }
        composable(Screen.LogTrip.route) {
            LogTripScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Expenses.route) {
            FuelExpenseScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.AllTrips.route) {
            AllTripsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
