package com.example.wood_restaurant.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.wood_restaurant.location.rememberLocationPermissionState
import com.example.wood_restaurant.ui.home.components.FilterBar
import com.example.wood_restaurant.ui.home.components.RestaurantRow
import com.example.wood_restaurant.ui.map.RestaurantMap
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionState = rememberLocationPermissionState()

    // 권한 상태는 시스템이 들고 있으므로 화면에서 관찰해 ViewModel로 흘려준다.
    LaunchedEffect(permissionState.isGranted) {
        viewModel.onPermissionResult(permissionState.isGranted)
    }
    LaunchedEffect(Unit) {
        viewModel.onScreenStarted(permissionState.isGranted)
    }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is HomeSideEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            HomeSideEffect.RequestLocationPermission -> permissionState.request()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SearchField(
                keyword = state.filter.keyword,
                regionName = state.regionName,
                onKeywordChanged = viewModel::onKeywordChanged,
                onSubmit = viewModel::onKeywordSubmitted,
            )

            FilterBar(
                filter = state.filter,
                onCategoryToggled = viewModel::onCategoryToggled,
                onSortSelected = viewModel::onSortSelected,
                onRadiusSelected = viewModel::onRadiusSelected,
                onMinRatingSelected = viewModel::onMinRatingSelected,
                onReset = viewModel::onFiltersReset,
            )

            MapSection(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )

            ResultSection(
                state = state,
                onPlaceClick = viewModel::onPlaceSelected,
                onRetry = viewModel::onRetryClick,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

@Composable
private fun SearchField(
    keyword: String,
    regionName: String?,
    onKeywordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    OutlinedTextField(
        value = keyword,
        onValueChange = onKeywordChanged,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        singleLine = true,
        placeholder = {
            Text(regionName?.let { "$it 에서 검색" } ?: "가게 이름·메뉴로 검색")
        },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (keyword.isNotEmpty()) {
                IconButton(onClick = { onKeywordChanged("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "검색어 지우기")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
    )
}

@Composable
private fun MapSection(
    state: HomeState,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        RestaurantMap(
            cameraTarget = state.cameraTarget,
            zoom = state.zoom,
            places = state.places,
            selectedPlaceId = state.selectedPlaceId,
            onPlaceClick = viewModel::onPlaceSelected,
            onMapClick = viewModel::onSelectionCleared,
            onCameraMoved = viewModel::onCameraMoved,
            modifier = Modifier.fillMaxSize(),
        )

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }

        if (state.canResearchHere) {
            ExtendedFloatingActionButton(
                onClick = viewModel::onResearchHereClick,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                text = { Text("이 지역 재검색") },
            )
        }

        SmallFloatingActionButton(
            onClick = viewModel::onMyLocationClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = "내 위치")
        }
    }
}

@Composable
private fun ResultSection(
    state: HomeState,
    onPlaceClick: (com.example.wood_restaurant.domain.Restaurant) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val places = state.places
    val listState = rememberLazyListState()

    // 지도에서 마커를 고르면 목록도 그 자리로 스크롤한다.
    LaunchedEffect(state.selectedPlaceId, places) {
        val index = places.indexOfFirst { it.id == state.selectedPlaceId }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Box(modifier = modifier) {
        when {
            state.errorMessage != null -> ErrorContent(state.errorMessage, onRetry)

            state.isLoading && places.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            places.isEmpty() -> {
                Text(
                    text = "조건에 맞는 장소가 없습니다.\n반경을 넓히거나 필터를 줄여보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }

            else -> {
                Column {
                    ResultHeader(count = places.size, sortLabel = state.filter.sort.label)
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(places, key = { it.id }) { place ->
                            RestaurantRow(
                                place = place,
                                selected = place.id == state.selectedPlaceId,
                                onClick = { onPlaceClick(place) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultHeader(count: Int, sortLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${count}곳", style = MaterialTheme.typography.titleSmall)
        Text(
            text = sortLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Button(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("다시 시도", modifier = Modifier.padding(start = 8.dp))
        }
    }
}
