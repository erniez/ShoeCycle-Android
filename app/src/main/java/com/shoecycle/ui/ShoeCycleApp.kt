package com.shoecycle.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shoecycle.R
import com.shoecycle.ui.screens.add_distance.AddDistanceScreen
import com.shoecycle.ui.screens.active_shoes.ActiveShoesScreen
import com.shoecycle.ui.screens.hall_of_fame.HallOfFameScreen
import com.shoecycle.ui.screens.settings.SettingsScreen
import com.shoecycle.ui.screens.shoe_detail.ShoeDetailScreen

sealed class ShoeCycleDestination(val route: String, val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object AddDistance : ShoeCycleDestination("add_distance", R.string.add_distance, Icons.Filled.Add)
    object ActiveShoes : ShoeCycleDestination("active_shoes", R.string.active_shoes, Icons.Filled.Home)
    object HallOfFame : ShoeCycleDestination("hall_of_fame", R.string.hall_of_fame, Icons.Filled.Star)
    object Settings : ShoeCycleDestination("settings", R.string.settings, Icons.Filled.Settings)
}

// Additional navigation routes that are not bottom navigation items
object AdditionalRoutes {
    const val SHOE_DETAIL = "shoe_detail/{shoeId}"
    
    fun createShoeDetailRoute(shoeId: Long) = "shoe_detail/$shoeId"
}

val shoeCycleDestinations = listOf(
    ShoeCycleDestination.AddDistance,
    ShoeCycleDestination.ActiveShoes,
    ShoeCycleDestination.HallOfFame,
    ShoeCycleDestination.Settings
)

@Composable
fun ShoeCycleApp() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            NavigationBar {
                shoeCycleDestinations.forEach { destination ->
                    NavigationBarItem(
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.titleRes)) },
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ShoeCycleDestination.AddDistance.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ShoeCycleDestination.AddDistance.route) {
                AddDistanceScreen()
            }
            composable(ShoeCycleDestination.ActiveShoes.route) {
                ActiveShoesScreen()
            }
            composable(ShoeCycleDestination.HallOfFame.route) {
                HallOfFameScreen(
                    onNavigateToShoeDetail = { shoeId ->
                        navController.navigate(AdditionalRoutes.createShoeDetailRoute(shoeId))
                    }
                )
            }
            composable(
                route = AdditionalRoutes.SHOE_DETAIL,
                arguments = listOf(navArgument("shoeId") { type = NavType.LongType })
            ) { backStackEntry ->
                val shoeId = backStackEntry.arguments?.getLong("shoeId") ?: 0L
                ShoeDetailScreen(
                    shoeId = shoeId,
                    isCreateMode = false,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(ShoeCycleDestination.Settings.route) {
                SettingsScreen()
            }
        }
    }
}