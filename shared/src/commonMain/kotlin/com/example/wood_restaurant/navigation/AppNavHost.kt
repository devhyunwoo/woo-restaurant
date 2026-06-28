package com.example.wood_restaurant.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.wood_restaurant.ui.DetailScreen
import com.example.wood_restaurant.ui.HomeScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onPostClick = { id -> navController.navigate(DetailRoute(postId = id)) },
            )
        }
        composable<DetailRoute> { entry ->
            val detail = entry.toRoute<DetailRoute>()
            DetailScreen(
                postId = detail.postId,
                onBack = { navController.navigateUp() },
            )
        }
    }
}
