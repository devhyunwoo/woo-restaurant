package com.example.wood_restaurant.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wood_restaurant.data.AuthRepository
import com.example.wood_restaurant.ui.auth.LoginScreen
import com.example.wood_restaurant.ui.main.MainScreen
import org.koin.compose.koinInject

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authRepository: AuthRepository = koinInject(),
) {
    // 첫 진입 시 한 번만 판단한다. 로그인 후 세션이 생겨도 NavHost 를 다시 세우지 않기 위해 remember.
    // 서버가 없으면(개발 모드) 로그인 자체가 불가능하니 바로 메인으로.
    val startDestination = remember {
        if (authRepository.isLoggedIn || !authRepository.isAvailable) MainRoute else LoginRoute
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<MainRoute> {
            MainScreen(
                onLoginRequested = { navController.navigate(LoginRoute) },
            )
        }
        composable<LoginRoute> {
            // 첫 진입이면 뒤에 아무것도 없으니 메인으로 교체, 마이 탭에서 왔으면 그냥 뒤로.
            val leave = {
                if (!navController.popBackStack()) {
                    navController.navigate(MainRoute) { popUpTo(LoginRoute) { inclusive = true } }
                }
            }
            LoginScreen(onLoggedIn = leave, onSkip = leave)
        }
    }
}
