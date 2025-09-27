package com.shoecycle.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.FTURepository
import com.shoecycle.domain.SelectedShoeStrategy
import com.shoecycle.ui.ftu.FTUInteractor
import com.shoecycle.ui.ftu.FTUState
import com.shoecycle.ui.navigation.InitialTabStrategy
import com.shoecycle.ui.screens.add_distance.AddDistanceScreen
import com.shoecycle.ui.screens.active_shoes.ActiveShoesScreen
import com.shoecycle.ui.screens.hall_of_fame.HallOfFameScreen
import com.shoecycle.ui.screens.settings.SettingsScreen
import com.shoecycle.ui.screens.shoe_detail.ShoeDetailScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

sealed class ShoeCycleDestination(val route: String, val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object AddDistance : ShoeCycleDestination("add_distance", R.string.add_distance, Icons.Filled.Add)
    object ActiveShoes : ShoeCycleDestination("active_shoes", R.string.active_shoes, Icons.Filled.Home)
    object HallOfFame : ShoeCycleDestination("hall_of_fame", R.string.hall_of_fame, Icons.Filled.Star)
    object Settings : ShoeCycleDestination("settings", R.string.settings, Icons.Filled.Settings)
}

// Additional navigation routes that are not bottom navigation items
object AdditionalRoutes {
    const val SHOE_DETAIL = "shoe_detail/{shoeId}"
    
    fun createShoeDetailRoute(shoeId: String) = "shoe_detail/$shoeId"
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
    val context = LocalContext.current
    
    // Initialize repositories
    val database = remember { com.shoecycle.data.database.ShoeCycleDatabase.getDatabase(context) }
    val shoeRepository = remember {
        com.shoecycle.data.repository.ShoeRepository(
            database.shoeDao(),
            database.historyDao()
        )
    }
    val ftuRepository = remember { FTURepository(context) }
    val userSettingsRepository = remember { UserSettingsRepository(context) }
    val selectedShoeStrategy = remember {
        SelectedShoeStrategy(shoeRepository, userSettingsRepository)
    }

    // Determine initial tab based on shoe count (like iOS InitialTabStrategy)
    val initialDestination = remember {
        InitialTabStrategy(shoeRepository).initialTab()
    }
    
    // FTU hint system
    val ftuState = remember { mutableStateOf(FTUState()) }
    val ftuInteractor = remember { FTUInteractor(ftuRepository) }

    // Track if we've already checked for hints on app launch
    val hasCheckedForHints = remember { mutableStateOf(false) }

    // Only check for hints once on app launch when we have 2+ active shoes
    // This prevents hints from showing when a user creates their second shoe
    LaunchedEffect(Unit) {
        // Get initial shoe count
        val initialShoes = shoeRepository.getActiveShoes().first()
        if (initialShoes.size >= 2 && !hasCheckedForHints.value) {
            hasCheckedForHints.value = true
            ftuInteractor.handle(ftuState, FTUInteractor.Action.CheckForHints)
        }

        // Update selected shoe strategy on app startup
        selectedShoeStrategy.updateSelectedShoe()
    }
    
    // Show hint when one becomes available
    LaunchedEffect(ftuState.value.currentHint) {
        if (ftuState.value.currentHint != null && !ftuState.value.showHint) {
            ftuInteractor.handle(ftuState, FTUInteractor.Action.ShowNextHint)
        }
    }
    
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
            startDestination = initialDestination,
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
                arguments = listOf(navArgument("shoeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val shoeId = backStackEntry.arguments?.getString("shoeId") ?: ""
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
    
    // FTU Hint Dialog (matching iOS alert presentation)
    if (ftuState.value.showHint) {
        AlertDialog(
            onDismissRequest = {
                ftuInteractor.handle(ftuState, FTUInteractor.Action.DismissHint)
            },
            title = { Text("Hint") },
            text = { Text(ftuState.value.hintMessage) },
            confirmButton = {
                TextButton(onClick = {
                    ftuInteractor.handle(ftuState, FTUInteractor.Action.DismissHint)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    ftuInteractor.handle(ftuState, FTUInteractor.Action.CompleteHint)
                }) {
                    Text("Don't show again")
                }
            }
        )
    }
}