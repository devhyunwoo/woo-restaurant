package com.example.wood_restaurant.ui.home

import com.example.wood_restaurant.data.AppPreferences
import com.example.wood_restaurant.data.FavoritesRepository
import com.example.wood_restaurant.data.NearbyResult
import com.example.wood_restaurant.data.PlaceRepository
import com.example.wood_restaurant.domain.LatLng
import com.example.wood_restaurant.domain.PlaceCategory
import com.example.wood_restaurant.domain.PlaceFilter
import com.example.wood_restaurant.domain.Restaurant
import com.example.wood_restaurant.domain.SortOption
import com.example.wood_restaurant.location.LocationProvider
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeViewModelTest {

    // ---- 페이크 ----

    private class FakePlaceRepository(
        var result: NearbyResult = NearbyResult(emptyList(), regionName = "서울특별시 중구", queryCount = 0),
        var error: Throwable? = null,
    ) : PlaceRepository {
        val calls = mutableListOf<LatLng>()

        override suspend fun searchNearby(
            center: LatLng,
            categories: Set<PlaceCategory>,
            extraKeyword: String,
        ): NearbyResult {
            calls += center
            error?.let { throw it }
            return result
        }
    }

    private class FakeLocationProvider(var location: LatLng? = null) : LocationProvider {
        override suspend fun currentLocation(): LatLng? = location
    }

    private fun place(
        id: String,
        category: PlaceCategory = PlaceCategory.RESTAURANT,
        distance: Double = 100.0,
        rating: Double? = 4.0,
    ) = Restaurant(
        id = id,
        name = id,
        category = category,
        categoryDetail = "음식점>한식",
        roadAddress = "서울 어딘가",
        address = "",
        telephone = "",
        link = "",
        position = LatLng(37.5 + distance / 1_000_000, 127.0),
        rating = rating,
        reviewCount = 10,
        distanceMeters = distance,
    )

    private val gangnam = LatLng(37.4979, 127.0276)
    private val hongdae = LatLng(37.5563, 126.9236)

    private fun buildViewModel(
        repository: FakePlaceRepository = FakePlaceRepository(),
        location: FakeLocationProvider = FakeLocationProvider(),
        settings: MapSettings = MapSettings(),
    ) = HomeViewModel(
        repository = repository,
        favoritesRepository = FavoritesRepository(settings),
        locationProvider = location,
        preferences = AppPreferences(settings),
    )

    // ---- 진입 · 검색 ----

    @Test
    fun `권한이 있으면 현재 위치에서 검색하고 결과를 상태에 담는다`() = runTest {
        val a = place("a")
        val repository = FakePlaceRepository(
            result = NearbyResult(listOf(a), regionName = "서울특별시 강남구", queryCount = 4),
        )
        val viewModel = buildViewModel(repository, FakeLocationProvider(gangnam))

        viewModel.test(this) {
            expectInitialState()
            containerHost.onScreenStarted(hasPermission = true)

            expectState { copy(hasLocationPermission = true) }
            expectState { copy(cameraTarget = gangnam) }
            expectState { copy(isLoading = true, searchCenter = gangnam) }
            expectState {
                copy(isLoading = false, allPlaces = listOf(a), regionName = "서울특별시 강남구")
            }
        }
        assertEquals(listOf(gangnam), repository.calls)
    }

    @Test
    fun `권한이 없으면 권한을 요청하고 기본 위치에서 검색한다`() = runTest {
        val repository = FakePlaceRepository()
        val viewModel = buildViewModel(repository, FakeLocationProvider(location = null))

        viewModel.test(this) {
            expectInitialState()
            containerHost.onScreenStarted(hasPermission = false)

            expectSideEffect(HomeSideEffect.RequestLocationPermission)
            expectState { copy(isLoading = true, searchCenter = LatLng.SEOUL_CITY_HALL) }
            expectState { copy(isLoading = false, allPlaces = emptyList(), regionName = "서울특별시 중구") }
            expectSideEffect(HomeSideEffect.ShowMessage("주변에서 찾은 장소가 없습니다"))
        }
        assertEquals(listOf(LatLng.SEOUL_CITY_HALL), repository.calls)
    }

    @Test
    fun `검색이 실패하면 에러 메시지를 상태에 남긴다`() = runTest {
        val repository = FakePlaceRepository(error = IllegalStateException("키 없음"))
        val viewModel = buildViewModel(repository)

        viewModel.test(this) {
            expectInitialState()
            containerHost.onRetryClick()

            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false, errorMessage = "키 없음") }
        }
    }

    @Test
    fun `마지막 검색 위치를 기억해 다음 실행의 시작점으로 쓴다`() = runTest {
        val settings = MapSettings()
        val first = buildViewModel(
            repository = FakePlaceRepository(),
            location = FakeLocationProvider(hongdae),
            settings = settings,
        )
        first.test(this) {
            expectInitialState()
            containerHost.onScreenStarted(hasPermission = true)
            expectState { copy(hasLocationPermission = true) }
            expectState { copy(cameraTarget = hongdae) }
            expectState { copy(isLoading = true, searchCenter = hongdae) }
            expectState { copy(isLoading = false, allPlaces = emptyList(), regionName = "서울특별시 중구") }
            expectSideEffect(HomeSideEffect.ShowMessage("주변에서 찾은 장소가 없습니다"))
        }

        val second = buildViewModel(settings = settings)
        assertEquals(hongdae, second.container.stateFlow.value.searchCenter)
        assertEquals(hongdae, second.container.stateFlow.value.cameraTarget)
    }

    // ---- 지도 이동 · 재검색 ----

    @Test
    fun `지도를 조금 옮기면 재검색 버튼이 뜨지 않고 많이 옮기면 뜬다`() = runTest {
        val viewModel = buildViewModel()
        val tiny = LatLng(LatLng.SEOUL_CITY_HALL.latitude + 0.0005, LatLng.SEOUL_CITY_HALL.longitude)

        viewModel.test(this) {
            expectInitialState()

            containerHost.onCameraMoved(gangnam) // 수 km → 버튼 노출
            expectState { copy(pendingCenter = gangnam) }

            containerHost.onCameraMoved(tiny) // 약 55m → 임계치 미만이라 버튼을 내린다
            expectState { copy(pendingCenter = null) }
        }
    }

    @Test
    fun `자동 재검색이 켜져 있으면 지도를 옮기자마자 검색한다`() = runTest {
        val repository = FakePlaceRepository()
        val settings = MapSettings().also { AppPreferences(it).isAutoResearchEnabled = true }
        val viewModel = buildViewModel(repository, settings = settings)

        viewModel.test(this) {
            expectInitialState()
            assertTrue(containerHost.container.stateFlow.value.isAutoResearchEnabled)

            containerHost.onCameraMoved(gangnam)
            expectState { copy(isLoading = true, searchCenter = gangnam) }
            expectState { copy(isLoading = false, allPlaces = emptyList(), regionName = "서울특별시 중구") }
            expectSideEffect(HomeSideEffect.ShowMessage("주변에서 찾은 장소가 없습니다"))
        }
        assertEquals(listOf(gangnam), repository.calls)
    }

    @Test
    fun `자동 재검색을 켜는 순간 미뤄둔 위치가 있으면 바로 따라잡는다`() = runTest {
        val repository = FakePlaceRepository()
        val settings = MapSettings()
        val viewModel = buildViewModel(repository, settings = settings)

        viewModel.test(this) {
            expectInitialState()
            containerHost.onCameraMoved(gangnam)
            expectState { copy(pendingCenter = gangnam) }

            containerHost.onAutoResearchToggled()
            expectState { copy(isAutoResearchEnabled = true) }
            expectState { copy(isLoading = true, searchCenter = gangnam) }
            expectState { copy(isLoading = false, pendingCenter = null, regionName = "서울특별시 중구") }
            expectSideEffect(HomeSideEffect.ShowMessage("주변에서 찾은 장소가 없습니다"))
        }
        assertTrue(AppPreferences(settings).isAutoResearchEnabled, "설정이 저장돼야 한다")
    }

    // ---- 필터 ----

    @Test
    fun `마지막 카테고리는 끌 수 없다`() = runTest {
        val viewModel = buildViewModel()

        viewModel.test(this) {
            expectInitialState()
            containerHost.onCategoryToggled(PlaceCategory.CAFE)
            expectState { copy(filter = filter.copy(categories = setOf(PlaceCategory.RESTAURANT, PlaceCategory.BAKERY))) }
            containerHost.onCategoryToggled(PlaceCategory.BAKERY)
            expectState { copy(filter = filter.copy(categories = setOf(PlaceCategory.RESTAURANT))) }

            containerHost.onCategoryToggled(PlaceCategory.RESTAURANT)
            expectSideEffect(HomeSideEffect.ShowMessage("카테고리는 하나 이상 선택해야 합니다"))
        }
    }

    @Test
    fun `정렬을 바꾸면 선택은 풀리고 네트워크는 타지 않는다`() = runTest {
        val repository = FakePlaceRepository()
        val viewModel = buildViewModel(repository)

        viewModel.test(this) {
            expectInitialState()
            containerHost.onSortSelected(SortOption.RATING)
            expectState { copy(filter = filter.copy(sort = SortOption.RATING), selectedPlaceId = null) }
        }
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `찜이 하나도 없으면 찜만 보기를 켤 수 없다`() = runTest {
        val viewModel = buildViewModel()

        viewModel.test(this) {
            expectInitialState()
            containerHost.onFavoritesOnlyToggled()
            expectSideEffect(HomeSideEffect.ShowMessage("아직 찜한 곳이 없어요. 하트를 눌러 담아보세요"))
        }
    }

    // ---- 찜 · 추천 ----

    @Test
    fun `찜 토글은 저장소에 반영되고 메시지를 띄운다`() = runTest {
        val settings = MapSettings()
        val favorites = FavoritesRepository(settings)
        val viewModel = HomeViewModel(
            repository = FakePlaceRepository(),
            favoritesRepository = favorites,
            locationProvider = FakeLocationProvider(),
            preferences = AppPreferences(settings),
        )
        val a = place("a")

        viewModel.test(this) {
            expectInitialState()
            containerHost.onFavoriteToggled(a)
            expectSideEffect(HomeSideEffect.ShowMessage("a 찜 완료 ❤️"))
            containerHost.onFavoriteToggled(a)
            expectSideEffect(HomeSideEffect.ShowMessage("a 찜 해제"))
        }
        assertTrue(favorites.favorites.value.isEmpty())
    }

    @Test
    fun `찜 탭에서 지도로 보내면 찜만 보기로 바꾸고 그 장소를 선택한다`() = runTest {
        val settings = MapSettings()
        val favorites = FavoritesRepository(settings)
        val a = place("a", category = PlaceCategory.CAFE)
        favorites.toggle(a)
        val viewModel = HomeViewModel(
            repository = FakePlaceRepository(),
            favoritesRepository = favorites,
            locationProvider = FakeLocationProvider(),
            preferences = AppPreferences(settings),
        )

        viewModel.test(this) {
            expectInitialState()
            containerHost.onSortSelected(SortOption.NAME)
            expectState { copy(filter = filter.copy(sort = SortOption.NAME)) }

            containerHost.onFavoriteOpenedFromList(a)
            expectState {
                copy(
                    filter = PlaceFilter(favoritesOnly = true, sort = SortOption.NAME),
                    selectedPlaceId = "a",
                    cameraTarget = a.position,
                )
            }
            assertEquals("a", containerHost.container.stateFlow.value.selectedPlace?.id)
        }
    }

    @Test
    fun `랜덤 추천은 보이는 목록에서 하나를 고르고 카메라를 옮긴다`() = runTest {
        val a = place("a")
        val repository = FakePlaceRepository(
            result = NearbyResult(listOf(a), regionName = "서울특별시 중구", queryCount = 1),
        )
        val viewModel = buildViewModel(repository)

        viewModel.test(this) {
            expectInitialState()
            containerHost.onRetryClick()
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false, allPlaces = listOf(a), regionName = "서울특별시 중구") }

            containerHost.onRandomPickClick()
            expectState { copy(selectedPlaceId = "a", cameraTarget = a.position) }
            expectSideEffect(HomeSideEffect.ShowMessage("🎲 오늘은 a 어때요?"))
        }
    }

    @Test
    fun `보이는 장소가 없으면 랜덤 추천은 안내만 한다`() = runTest {
        val viewModel = buildViewModel()

        viewModel.test(this) {
            expectInitialState()
            containerHost.onRandomPickClick()
            expectSideEffect(HomeSideEffect.ShowMessage("고를 만한 곳이 없어요. 필터를 풀어보세요"))
        }
    }
}
