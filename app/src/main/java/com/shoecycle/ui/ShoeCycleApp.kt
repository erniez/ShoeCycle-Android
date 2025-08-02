package com.shoecycle.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.shoecycle.ui.screens.AddDistanceScreen
import com.shoecycle.ui.screens.ActiveShoesScreen
import com.shoecycle.ui.screens.HallOfFameScreen
import com.shoecycle.ui.screens.SettingsScreen
import com.shoecycle.ui.screens.ShoeDetailScreen

sealed class ShoeCycleDestination(val route: String, val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object AddDistance : ShoeCycleDestination("add_distance", R.string.add_distance, Icons.Filled.Add)
    object ActiveShoes : ShoeCycleDestination("active_shoes", R.string.active_shoes, Icons.Filled.Home)
    object HallOfFame : ShoeCycleDestination("hall_of_fame", R.string.hall_of_fame, Icons.Filled.Star)
    object Settings : ShoeCycleDestination("settings", R.string.settings, Icons.Filled.Settings)
    object ShoeDetail : ShoeCycleDestination("shoe_detail/{shoeId}", R.string.shoe_detail, Icons.Filled.Home)
}

val shoeCycleDestinations = listOf(
    ShoeCycleDestination.AddDistance,
    ShoeCycleDestination.ActiveShoes,
    ShoeCycleDestination.HallOfFame,
    ShoeCycleDestination.Settings
)

@Composable
fun ShoeCycleApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    
    Scaffold(
        modifier = modifier,
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
                ActiveShoesScreen(
                    onNavigateToShoeDetail = { shoeId ->
                        navController.navigate("shoe_detail/$shoeId")
                    }
                )
            }
            composable(ShoeCycleDestination.HallOfFame.route) {
                HallOfFameScreen()
            }
            composable(ShoeCycleDestination.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = ShoeCycleDestination.ShoeDetail.route,
                arguments = listOf(navArgument("shoeId") { type = NavType.LongType }),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
            ) { backStackEntry ->
                val shoeId = backStackEntry.arguments?.getLong("shoeId") ?: 0L
                ShoeDetailScreen(
                    shoeId = shoeId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}