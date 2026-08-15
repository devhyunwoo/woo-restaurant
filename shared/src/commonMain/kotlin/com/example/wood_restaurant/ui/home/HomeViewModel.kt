package com.example.wood_restaurant.ui.home

import androidx.lifecycle.ViewModel
import com.example.wood_restaurant.data.AppPreferences
import com.example.wood_restaurant.data.FavoritesRepository
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
import kotlin.random.Random

private typealias HomeSyntax = Syntax<HomeState, HomeSideEffect>

/** 지도를 이만큼 옮기면 "이 지역 재검색"을 띄운다. */
private const val RESEARCH_THRESHOLD_METERS = 300.0

class HomeViewModel(
    private val repository: PlaceRepository,
    private val favoritesRepository: FavoritesRepository,
    private val locationProvider: LocationProvider,
    private val preferences: AppPreferences,
) : ViewModel(), ContainerHost<HomeState, HomeSideEffect> {

    override val container = container<HomeState, HomeSideEffect>(initialState())

    init {
        // 찜 목록은 저장소가 진실의 원천. 바뀔 때마다 상태에 반영한다.
        intent {
            favoritesRepository.favorites.collect { favorites ->
                reduce { state.copy(favorites = favorites) }
            }
        }
    }

    /** 마지막으로 검색한 위치가 있으면 거기서 시작한다. 권한이 없어도 서울시청 대신 익숙한 곳이 뜬다. */
    private fun initialState(): HomeState {
        val start = preferences.lastSearchCenter ?: LatLng.SEOUL_CITY_HALL
        return HomeState(
            searchCenter = start,
            cameraTarget = start,
            recentKeywords = preferences.recentKeywords,
            favorites = favoritesRepository.favorites.value,
        )
    }

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
        // 새로 켠 카테고리는 아직 받아온 적이 없을 수 있으므로 다시 검색한다. (찜만 보기 중엔 불필요)
        if (category !in current && !state.filter.favoritesOnly) search(state.searchCenter)
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

    fun onFavoritesOnlyToggled() = intent {
        val turningOn = !state.filter.favoritesOnly
        if (turningOn && state.favorites.isEmpty()) {
            postSideEffect(HomeSideEffect.ShowMessage("아직 찜한 곳이 없어요. 하트를 눌러 담아보세요"))
            return@intent
        }
        updateFilter { it.copy(favoritesOnly = turningOn) }
    }

    fun onKeywordChanged(keyword: String) = intent {
        updateFilter { it.copy(keyword = keyword) }
    }

    fun onKeywordSubmitted() = intent {
        submitKeyword()
    }

    fun onRecentKeywordClick(keyword: String) = intent {
        updateFilter { it.copy(keyword = keyword) }
        submitKeyword()
    }

    fun onRecentKeywordsCleared() = intent {
        preferences.clearRecentKeywords()
        reduce { state.copy(recentKeywords = emptyList()) }
    }

    fun onFiltersReset() = intent {
        reduce { state.copy(filter = PlaceFilter(), selectedPlaceId = null) }
    }

    // ---- 선택 · 찜 · 추천 ----

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

    fun onFavoriteToggled(place: Restaurant) = intent {
        val added = favoritesRepository.toggle(place)
        postSideEffect(
            HomeSideEffect.ShowMessage(
                if (added) "${place.name} 찜 완료 ❤️" else "${place.name} 찜 해제"
            )
        )
    }

    /** 지금 보이는 목록에서 하나를 무작위로 골라 준다. */
    fun onRandomPickClick() = intent {
        val candidates = state.places
        if (candidates.isEmpty()) {
            postSideEffect(HomeSideEffect.ShowMessage("고를 만한 곳이 없어요. 필터를 풀어보세요"))
            return@intent
        }
        // 이미 선택된 곳은 빼서 연타하면 계속 새로운 곳이 나오게 한다.
        val pool = candidates.filter { it.id != state.selectedPlaceId }.ifEmpty { candidates }
        val pick = pool[Random.nextInt(pool.size)]
        reduce { state.copy(selectedPlaceId = pick.id, cameraTarget = pick.position) }
        postSideEffect(HomeSideEffect.ShowMessage("🎲 오늘은 ${pick.name} 어때요?"))
    }

    // ---- 내부 ----

    private suspend fun HomeSyntax.submitKeyword() {
        val keyword = state.filter.keyword.trim()
        if (keyword.isNotEmpty()) {
            preferences.pushRecentKeyword(keyword)
            reduce { state.copy(recentKeywords = preferences.recentKeywords) }
        }
        // 찜만 보기 중엔 로컬 필터만으로 충분하다. 네트워크는 검색 결과 모드에서만.
        if (!state.filter.favoritesOnly) search(state.searchCenter)
    }

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

        preferences.lastSearchCenter = center

        reduce {
            state.copy(
                isLoading = false,
                allPlaces = nearby.places,
                regionName = nearby.regionName,
                pendingCenter = null,
            )
        }
        if (nearby.places.isEmpty() && !state.filter.favoritesOnly) {
            postSideEffect(HomeSideEffect.ShowMessage("주변에서 찾은 장소가 없습니다"))
        }
    }
}
