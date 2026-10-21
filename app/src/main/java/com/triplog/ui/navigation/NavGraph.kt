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
    object Home : Screen("home")
    object LogTrip : Screen("log_trip")
    object Expenses : Screen("expenses")
    object AllTrips : Screen("all_trips")
}

@Composable
fun TripLogNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToLogTrip = { navController.navigate(Screen.LogTrip.route) },
                onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                onNavigateToAllTrips = { navController.navigate(Screen.AllTrips.route) }
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
