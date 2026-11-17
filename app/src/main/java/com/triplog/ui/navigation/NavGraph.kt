package com.triplog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.triplog.ui.expenses.FuelExpenseScreen
import com.triplog.ui.expenses.FuelListScreen
import com.triplog.ui.home.HomeScreen
import com.triplog.ui.logtrip.LogTripScreen
import com.triplog.ui.trips.AllTripsScreen

const val ARG_TRIP_ID = "tripId"
const val ARG_EXPENSE_ID = "expenseId"

sealed class Screen(val route: String) {
    /** Tabs shown in the bottom bar. */
    data object Home : Screen("home")
    data object LogTrip : Screen("log_trip")
    data object Expenses : Screen("expenses")
    data object AllTrips : Screen("all_trips")

    /** Screens reached from the tabs. */
    data object NewExpense : Screen("expense_form")
    data object EditTrip : Screen("trip/{$ARG_TRIP_ID}") {
        fun routeFor(id: Long) = "trip/$id"
    }

    data object EditExpense : Screen("expense/{$ARG_EXPENSE_ID}") {
        fun routeFor(id: Long) = "expense/$id"
    }
}

@Composable
fun TripLogNavGraph(navController: NavHostController) {
    fun open(route: String) = navController.navigate(route) { launchSingleTop = true }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToLogTrip = { open(Screen.LogTrip.route) },
                onNavigateToExpenses = { open(Screen.NewExpense.route) },
                onNavigateToAllTrips = { open(Screen.AllTrips.route) },
                onTripClick = { open(Screen.EditTrip.routeFor(it)) }
            )
        }
        composable(Screen.LogTrip.route) {
            LogTripScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.EditTrip.route,
            arguments = listOf(navArgument(ARG_TRIP_ID) { type = NavType.LongType })
        ) {
            LogTripScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Expenses.route) {
            FuelListScreen(
                onAddExpense = { open(Screen.NewExpense.route) },
                onExpenseClick = { open(Screen.EditExpense.routeFor(it)) }
            )
        }
        composable(Screen.NewExpense.route) {
            FuelExpenseScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.EditExpense.route,
            arguments = listOf(navArgument(ARG_EXPENSE_ID) { type = NavType.LongType })
        ) {
            FuelExpenseScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.AllTrips.route) {
            AllTripsScreen(
                onTripClick = { open(Screen.EditTrip.routeFor(it)) }
            )
        }
    }
}
