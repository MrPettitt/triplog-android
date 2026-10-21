package com.triplog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.triplog.ui.navigation.Screen
import com.triplog.ui.navigation.TripLogNavGraph
import com.triplog.ui.theme.*
import com.triplog.worker.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // Schedule periodic background sync
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            SyncWorker.buildPeriodicRequest()
        )

        setContent {
            TripLogTheme {
                TripLogApp()
            }
        }
    }
}

@Composable
fun TripLogApp() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home, "Home", Icons.Default.Home),
        BottomNavItem(Screen.LogTrip, "Log trip", Icons.Default.AddLocation),
        BottomNavItem(Screen.Expenses, "Fuel", Icons.Default.LocalGasStation),
        BottomNavItem(Screen.AllTrips, "Trips", Icons.AutoMirrored.Filled.List),
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.screen.route }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface900),
        containerColor = Surface900,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Surface800,
                    contentColor = TextSecondary,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Brand400,
                                selectedTextColor = Brand400,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Brand400.copy(alpha = 0.16f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            TripLogNavGraph(navController = navController)
        }
    }
}
