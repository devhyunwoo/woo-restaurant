package com.example.wood_restaurant.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.wood_restaurant.ui.favorites.FavoritesScreen
import com.example.wood_restaurant.ui.home.HomeScreen
import com.example.wood_restaurant.ui.home.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = koinViewModel(),
    // 홈 ViewModel을 여기서 잡아 두면 탭을 오가도 같은 인스턴스가 유지되고,
    // 찜 탭에서 "지도에서 보기"로 홈에 명령을 보낼 수 있다.
    homeViewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is MainSideEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (state.selectedTab) {
                MainTab.HOME -> HomeScreen(viewModel = homeViewModel)
                MainTab.SEARCH -> PlaceholderTabContent(MainTab.SEARCH.label)
                MainTab.FAVORITES -> FavoritesScreen(
                    onShowOnMap = { place ->
                        homeViewModel.onFavoriteOpenedFromList(place)
                        viewModel.switchTo(MainTab.HOME)
                    },
                )
                MainTab.PROFILE -> PlaceholderTabContent(MainTab.PROFILE.label)
            }
        }
    }
}

private val MainTab.icon: ImageVector
    get() = when (this) {
        MainTab.HOME -> Icons.Filled.Home
        MainTab.SEARCH -> Icons.Filled.Search
        MainTab.FAVORITES -> Icons.Filled.Favorite
        MainTab.PROFILE -> Icons.Filled.Person
    }

@Composable
private fun PlaceholderTabContent(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$label 화면 준비 중")
    }
}
