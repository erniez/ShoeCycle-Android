package com.shoecycle.ui.screens

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class ActiveShoesRoute(val route: String) {
    object List : ActiveShoesRoute("active_shoes_list")
    object Detail : ActiveShoesRoute("shoe_detail/{shoeId}") {
        fun createRoute(shoeId: Long) = "shoe_detail/$shoeId"
    }
}

@Composable
fun ActiveShoesNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = ActiveShoesRoute.List.route
    ) {
        composable(ActiveShoesRoute.List.route) {
            ActiveShoesListScreen(
                onNavigateToShoeDetail = { shoeId ->
                    navController.navigate(ActiveShoesRoute.Detail.createRoute(shoeId))
                }
            )
        }
        
        composable(
            route = ActiveShoesRoute.Detail.route,
            arguments = listOf(navArgument("shoeId") { type = NavType.LongType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
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
    }
}