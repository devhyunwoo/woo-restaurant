package com.example.wood_restaurant.ui.home

import androidx.lifecycle.ViewModel
import com.example.wood_restaurant.data.PlaceRepository
import com.example.wood_restaurant.domain.LatLng
import com.example.wood_restaurant.domain.MinRating
import com.example.wood_restaurant.domain.PlaceCategory
import com.example.wood_restaurant.domain.PlaceFilter
import com.example.wood_restaurant.domain.Restaurant
import com.example.wood_restaurant.domain.SearchRadius
import com.example.wood_restaurant.domain.SortOption
import com.example.wood_restaurant.domain.distanceTo
import com.example.wood_restaurant.location.LocationProvider
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container

private typealias HomeSyntax = Syntax<HomeState, HomeSideEffect>

/** 지도를 이만큼 옮기면 "이 지역 재검색"을 띄운다. */
private const val RESEARCH_THRESHOLD_METERS = 300.0

class HomeViewModel(
    private val repository: PlaceRepository,
    private val locationProvider: LocationProvider,
) : ViewModel(), ContainerHost<HomeState, HomeSideEffect> {

    override val container = container<HomeState, HomeSideEffect>(HomeState())

    /** 화면 최초 진입. 권한이 있으면 내 위치에서, 없으면 기본 위치에서 검색한다. */
    fun onScreenStarted(hasPermission: Boolean) = intent {
        if (state.allPlaces.isNotEmpty() || state.isLoading) return@intent
        reduce { state.copy(hasLocationPermission = hasPermission) }
        if (!hasPermission) {
            postSideEffect(HomeSideEffect.RequestLocationPermission)
        }
        moveToCurrentLocationAndSearch(fallbackToDefault = true)
    }

    fun onPermissionResult(granted: Boolean) = intent {
        val changed = state.hasLocationPermission != granted
        reduce { state.copy(hasLocationPermission = granted) }
        if (granted && changed) {
            moveToCurrentLocationAndSearch(fallbackToDefault = false)
        }
    }

    fun onMyLocationClick() = intent {
        if (!state.hasLocationPermission) {
            postSideEffect(HomeSideEffect.RequestLocationPermission)
            return@intent
        }
        moveToCurrentLocationAndSearch(fallbackToDefault = false)
    }

    /** 지도를 끌어 옮겼을 때. 임계치를 넘으면 재검색 버튼을 노출한다. */
    fun onCameraMoved(center: LatLng) = intent {
        val moved = state.searchCenter.distanceTo(center) >= RESEARCH_THRESHOLD_METERS
        reduce { state.copy(pendingCenter = if (moved) center else null) }
    }

    fun onResearchHereClick() = intent {
        val center = state.pendingCenter ?: return@intent
        reduce { state.copy(pendingCenter = null) }
        search(center)
    }

    fun onRetryClick() = intent {
        search(state.searchCenter)
    }

    // ---- 필터 ----

    fun onCategoryToggled(category: PlaceCategory) = intent {
        val current = state.filter.categories
        val next = if (category in current) current - category else current + category
        // 전부 끄면 아무것도 안 보이므로 마지막 하나는 끌 수 없게 한다.
        if (next.isEmpty()) {
            postSideEffect(HomeSideEffect.ShowMessage("카테고리는 하나 이상 선택해야 합니다"))
            return@intent
        }
        updateFilter { it.copy(categories = next) }
        // 새로 켠 카테고리는 아직 받아온 적이 없을 수 있으므로 다시 검색한다.
        if (category !in current) search(state.searchCenter)
    }

    fun onSortSelected(sort: SortOption) = intent {
        updateFilter { it.copy(sort = sort) }
    }

    fun onRadiusSelected(radius: SearchRadius) = intent {
        updateFilter { it.copy(radius = radius) }
    }

    fun onMinRatingSelected(minRating: MinRating) = intent {
        updateFilter { it.copy(minRating = minRating) }
    }

    fun onKeywordChanged(keyword: String) = intent {
        updateFilter { it.copy(keyword = keyword) }
    }

    fun onKeywordSubmitted() = intent {
        search(state.searchCenter)
    }

    fun onFiltersReset() = intent {
        reduce { state.copy(filter = PlaceFilter(), selectedPlaceId = null) }
    }

    fun onFilterSheetOpenChange(open: Boolean) = intent {
        reduce { state.copy(isFilterSheetOpen = open) }
    }

    // ---- 선택 ----

    fun onPlaceSelected(place: Restaurant) = intent {
        reduce {
            state.copy(
                selectedPlaceId = place.id,
                cameraTarget = place.position,
            )
        }
    }

    fun onSelectionCleared() = intent {
        reduce { state.copy(selectedPlaceId = null) }
    }

    // ---- 내부 ----

    private suspend fun HomeSyntax.updateFilter(
        transform: (PlaceFilter) -> PlaceFilter,
    ) {
        reduce { state.copy(filter = transform(state.filter), selectedPlaceId = null) }
    }

    private suspend fun HomeSyntax.moveToCurrentLocationAndSearch(
        fallbackToDefault: Boolean,
    ) {
        val current = locationProvider.currentLocation()
        when {
            current != null -> {
                reduce { state.copy(cameraTarget = current) }
                search(current)
            }

            fallbackToDefault -> {
                search(state.searchCenter)
            }

            else -> {
                postSideEffect(HomeSideEffect.ShowMessage("현재 위치를 가져오지 못했습니다"))
            }
        }
    }

    private suspend fun HomeSyntax.search(center: LatLng) {
        reduce { state.copy(isLoading = true, errorMessage = null, searchCenter = center) }

        val result = runCatching {
            repository.searchNearby(
                center = center,
                categories = state.filter.categories,
                extraKeyword = state.filter.keyword,
            )
        }

        // Result.fold는 suspend 람다를 받지 못하므로(reduce가 suspend) 분기로 처리한다.
        val nearby = result.getOrNull()
        if (nearby == null) {
            val message = result.exceptionOrNull()?.message ?: "장소를 불러오지 못했습니다"
            reduce { state.copy(isLoading = false, errorMessage = message) }
            return
        }

        reduce {
            state.copy(
                isLoading = false,
                allPlaces = nearby.places,
                regionName = nearby.regionName,
                pendingCenter = null,
            )
        }
        if (nearby.places.isEmpty()) {
            postSideEffect(HomeSideEffect.ShowMessage("주변에서 찾은 장소가 없습니다"))
        }
    }
}
