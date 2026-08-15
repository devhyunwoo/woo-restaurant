package com.example.wood_restaurant.ui.home

import com.example.wood_restaurant.domain.LatLng
import com.example.wood_restaurant.domain.PlaceFilter
import com.example.wood_restaurant.domain.Restaurant
import com.example.wood_restaurant.domain.applyFilter

/**
 * 홈(지도) 화면 상태.
 *
 * [allPlaces]는 API에서 받아온 원본, [places]는 필터/정렬을 적용한 결과다.
 * 필터를 바꿀 때 네트워크를 다시 타지 않도록 원본을 들고 있는다.
 */
data class HomeState(
    val isLoading: Boolean = false,
    val allPlaces: List<Restaurant> = emptyList(),
    val filter: PlaceFilter = PlaceFilter(),
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
    val isFilterSheetOpen: Boolean = false,
) {
    /** 상태 인스턴스당 한 번만 계산한다. 한 컴포지션에서 여러 번 읽히기 때문. */
    val places: List<Restaurant> by lazy { allPlaces.applyFilter(filter) }

    val selectedPlace: Restaurant? get() = places.firstOrNull { it.id == selectedPlaceId }

    /** 지도를 충분히 옮겼으면 "이 지역 재검색" 버튼을 띄운다. */
    val canResearchHere: Boolean get() = pendingCenter != null
}

sealed interface HomeSideEffect {
    data class ShowMessage(val message: String) : HomeSideEffect
    data object RequestLocationPermission : HomeSideEffect
}
