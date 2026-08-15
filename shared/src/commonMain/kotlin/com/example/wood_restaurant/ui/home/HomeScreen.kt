package com.example.wood_restaurant.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.wood_restaurant.domain.PlaceLinks
import com.example.wood_restaurant.domain.Restaurant
import com.example.wood_restaurant.location.rememberLocationPermissionState
import com.example.wood_restaurant.platform.ExternalActions
import com.example.wood_restaurant.platform.rememberExternalActions
import com.example.wood_restaurant.ui.home.components.FilterBar
import com.example.wood_restaurant.ui.home.components.PlaceDetailCard
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
    val externalActions = rememberExternalActions()

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
                recentKeywords = state.recentKeywords,
                onKeywordChanged = viewModel::onKeywordChanged,
                onSubmit = viewModel::onKeywordSubmitted,
                onRecentKeywordClick = viewModel::onRecentKeywordClick,
                onRecentKeywordsCleared = viewModel::onRecentKeywordsCleared,
            )

            FilterBar(
                filter = state.filter,
                categoryCounts = state.categoryCounts,
                favoriteCount = state.favorites.size,
                onCategoryToggled = viewModel::onCategoryToggled,
                onFavoritesOnlyToggled = viewModel::onFavoritesOnlyToggled,
                onSortSelected = viewModel::onSortSelected,
                onRadiusSelected = viewModel::onRadiusSelected,
                onMinRatingSelected = viewModel::onMinRatingSelected,
                onReset = viewModel::onFiltersReset,
            )

            MapSection(
                state = state,
                viewModel = viewModel,
                externalActions = externalActions,
                onRequestPermission = permissionState::request,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )

            ResultSection(
                state = state,
                onPlaceClick = viewModel::onPlaceSelected,
                onFavoriteClick = viewModel::onFavoriteToggled,
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
    recentKeywords: List<String>,
    onKeywordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onRecentKeywordClick: (String) -> Unit,
    onRecentKeywordsCleared: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .onFocusChanged { focused = it.isFocused },
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

        // 검색창에 포커스가 있고 아직 아무것도 안 쳤을 때만 최근 검색어를 보여준다.
        AnimatedVisibility(visible = focused && keyword.isEmpty() && recentKeywords.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "최근",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                recentKeywords.forEach { recent ->
                    AssistChip(
                        onClick = { onRecentKeywordClick(recent) },
                        label = { Text(recent) },
                    )
                }
                TextButton(onClick = onRecentKeywordsCleared) { Text("지우기") }
            }
        }
    }
}

@Composable
private fun MapSection(
    state: HomeState,
    viewModel: HomeViewModel,
    externalActions: ExternalActions,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedPlace

    Box(modifier = modifier) {
        RestaurantMap(
            cameraTarget = state.cameraTarget,
            zoom = state.zoom,
            places = state.places,
            selectedPlaceId = state.selectedPlaceId,
            onPlaceClick = viewModel::onPlaceSelected,
            onMapClick = viewModel::onSelectionCleared,
            onCameraMoved = viewModel::onCameraMoved,
            isDarkMode = isSystemInDarkTheme(),
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (!state.hasLocationPermission) {
                PermissionBanner(
                    onAllow = onRequestPermission,
                    onOpenSettings = externalActions::openAppSettings,
                )
            }
            if (state.canResearchHere) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::onResearchHereClick,
                    modifier = Modifier.padding(top = 12.dp),
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    text = { Text("이 지역 재검색") },
                )
            }
        }

        // 상세 카드가 떠 있을 땐 아래쪽 버튼을 숨겨 겹치지 않게 한다.
        if (selected == null) {
            ExtendedFloatingActionButton(
                onClick = viewModel::onRandomPickClick,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                text = { Text("🎲 오늘 뭐 먹지?") },
                icon = {},
            )
            SmallFloatingActionButton(
                onClick = viewModel::onMyLocationClick,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = "내 위치")
            }
        }

        // exit 애니메이션 동안 selected는 이미 null이므로, 마지막으로 보여준 장소를 붙잡아 둔다.
        val lastShown = remember { arrayOfNulls<Restaurant>(1) }
        if (selected != null) lastShown[0] = selected

        AnimatedVisibility(
            visible = selected != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            val place = lastShown[0] ?: return@AnimatedVisibility
            PlaceDetailCard(
                place = place,
                isFavorite = place.id in state.favoriteIds,
                onClose = viewModel::onSelectionCleared,
                onFavoriteClick = { viewModel.onFavoriteToggled(place) },
                onCallClick = { externalActions.dial(place.telephone) },
                onDirectionsClick = {
                    externalActions.openUrl(
                        url = PlaceLinks.naverMapDirections(place, externalActions.appIdentifier),
                        fallbackUrl = PlaceLinks.webSearchUrl(place),
                    )
                },
                onOpenLinkClick = { externalActions.openUrl(PlaceLinks.placePageUrl(place)) },
                onShareClick = { externalActions.share(PlaceLinks.shareText(place)) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun PermissionBanner(onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "위치 권한이 없어 기본 위치 기준입니다",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            TextButton(onClick = onAllow) { Text("허용") }
            TextButton(onClick = onOpenSettings) { Text("설정") }
        }
    }
}

@Composable
private fun ResultSection(
    state: HomeState,
    onPlaceClick: (Restaurant) -> Unit,
    onFavoriteClick: (Restaurant) -> Unit,
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
            state.errorMessage != null && !state.filter.favoritesOnly ->
                ErrorContent(state.errorMessage, onRetry)

            state.isLoading && places.isEmpty() && !state.filter.favoritesOnly -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            places.isEmpty() -> {
                Text(
                    text = emptyMessage(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }

            else -> {
                Column {
                    ResultHeader(
                        count = places.size,
                        label = if (state.filter.favoritesOnly) "찜 · ${state.filter.sort.label}" else state.filter.sort.label,
                    )
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(places, key = { it.id }) { place ->
                            RestaurantRow(
                                place = place,
                                selected = place.id == state.selectedPlaceId,
                                isFavorite = place.id in state.favoriteIds,
                                onClick = { onPlaceClick(place) },
                                onFavoriteClick = { onFavoriteClick(place) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun emptyMessage(state: HomeState): String = when {
    state.filter.favoritesOnly ->
        "조건에 맞는 찜이 없습니다.\n카테고리나 별점 필터를 풀어보세요."

    // 지역명 없이 검색하면 지역검색 API가 전국 결과를 주므로 반경 필터에 전부 걸린다.
    // 원인을 알 수 있게 그대로 알려준다.
    state.regionName == null ->
        "조건에 맞는 장소가 없습니다.\n" +
            "지역명을 못 구해 전국 대상으로 검색했습니다. " +
            "검색창에 지역명(예: 강남역)을 넣거나 리버스 지오코딩 키를 설정하세요."

    else -> "조건에 맞는 장소가 없습니다.\n반경을 넓히거나 필터를 줄여보세요."
}

@Composable
private fun ResultHeader(count: Int, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${count}곳", style = MaterialTheme.typography.titleSmall)
        Text(
            text = label,
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
