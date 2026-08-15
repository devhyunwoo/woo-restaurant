package com.example.wood_restaurant.ui.home

import com.example.wood_restaurant.domain.LatLng
import com.example.wood_restaurant.domain.PlaceCategory
import com.example.wood_restaurant.domain.PlaceFilter
import com.example.wood_restaurant.domain.Restaurant
import com.example.wood_restaurant.domain.applyFilter
import com.example.wood_restaurant.domain.distanceTo

/**
 * 홈(지도) 화면 상태.
 *
 * [allPlaces]는 API에서 받아온 원본, [places]는 필터/정렬을 적용한 결과다.
 * 필터를 바꿀 때 네트워크를 다시 타지 않도록 원본을 들고 있는다.
 */
data class HomeState(
    val isLoading: Boolean = false,
    val allPlaces: List<Restaurant> = emptyList(),
    /** 찜 목록. 저장소에서 흘러들어오며 검색과 무관하게 유지된다. */
    val favorites: List<Restaurant> = emptyList(),
    val filter: PlaceFilter = PlaceFilter(),
    val recentKeywords: List<String> = emptyList(),
    /** 검색 기준점(= 마지막으로 검색한 위치). */
    val searchCenter: LatLng = LatLng.SEOUL_CITY_HALL,
    /** 지도 카메라를 옮기라는 명령값. 바뀔 때만 지도가 움직인다. */
    val cameraTarget: LatLng = LatLng.SEOUL_CITY_HALL,
    val zoom: Double = 15.0,
    /** 사용자가 지도를 끌어 옮긴 현재 중심. 재검색 버튼 노출 판단에 쓴다. */
    val pendingCenter: LatLng? = null,
    val selectedPlaceId: String? = null,
    val regionName: String? = null,
    val hasLocationPermission: Boolean = false,
    val errorMessage: String? = null,
) {
    val favoriteIds: Set<String> by lazy { favorites.mapTo(HashSet()) { it.id } }

    /**
     * 필터가 적용된 최종 목록. 상태 인스턴스당 한 번만 계산한다.
     *
     * "찜만 보기"면 검색 결과 대신 찜 목록이 소스가 된다. 저장된 거리값은 찜할 당시 기준이라
     * 현재 검색 중심으로 다시 잰다. 이 모드에서는 반경을 무시한다.
     */
    val places: List<Restaurant> by lazy {
        source().applyFilter(filter, applyRadius = !filter.favoritesOnly)
    }

    /**
     * 카테고리 칩에 붙일 개수. "그 카테고리를 켜면 몇 개가 보이는가"를 뜻하므로
     * 카테고리 조건만 빼고 나머지 필터는 그대로 적용해서 센다.
     */
    val categoryCounts: Map<PlaceCategory, Int> by lazy {
        source()
            .applyFilter(
                filter.copy(categories = PlaceCategory.entries.toSet()),
                applyRadius = !filter.favoritesOnly,
            )
            .groupingBy { it.category }
            .eachCount()
    }

    val selectedPlace: Restaurant? get() = places.firstOrNull { it.id == selectedPlaceId }

    /** 지도를 충분히 옮겼으면 "이 지역 재검색" 버튼을 띄운다. */
    val canResearchHere: Boolean get() = pendingCenter != null

    private fun source(): List<Restaurant> =
        if (filter.favoritesOnly) {
            favorites.map { it.copy(distanceMeters = searchCenter.distanceTo(it.position)) }
        } else {
            allPlaces
        }
}

sealed interface HomeSideEffect {
    data class ShowMessage(val message: String) : HomeSideEffect
    data object RequestLocationPermission : HomeSideEffect
}
