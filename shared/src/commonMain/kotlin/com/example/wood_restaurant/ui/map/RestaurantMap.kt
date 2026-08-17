package com.example.wood_restaurant.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.wood_restaurant.domain.LatLng
import com.example.wood_restaurant.domain.PlaceCategory
import com.example.wood_restaurant.domain.Restaurant

/**
 * 네이버 지도 + 장소 마커.
 *
 * Android는 naver-map-compose, iOS는 NMapsMap(NMFNaverMapView)을 UIKitView로 감싼다.
 * 마커는 SDK 클러스터러에 맡겨서 줌아웃하면 뭉치고, 선택된 장소만 위에 크게 따로 그린다.
 *
 * [cameraTarget]은 "카메라를 여기로 옮겨라"는 명령에 가깝다. 값이 바뀔 때만 카메라가 움직이며,
 * 사용자가 지도를 끌어서 옮긴 결과는 [onCameraMoved]로만 보고한다.
 * (양방향으로 물리면 되먹임 루프가 생긴다.)
 *
 * [showMyLocation]은 위치 권한이 있을 때만 true로 준다. 파란 현재 위치 점을 켠다.
 */
@Composable
expect fun RestaurantMap(
    cameraTarget: LatLng,
    zoom: Double,
    places: List<Restaurant>,
    selectedPlaceId: String?,
    onPlaceClick: (Restaurant) -> Unit,
    onMapClick: () -> Unit,
    onCameraMoved: (LatLng) -> Unit,
    isDarkMode: Boolean,
    showMyLocation: Boolean,
    modifier: Modifier,
)

/** 클러스터가 풀려 개별 마커 캡션(상호명)이 보이기 시작하는 줌. */
const val MARKER_CAPTION_MIN_ZOOM = 14.0

/** 카테고리별 마커 색. 두 플랫폼이 같은 색을 쓰도록 공통 코드에 둔다. */
val PlaceCategory.markerColor: Color
    get() = when (this) {
        PlaceCategory.RESTAURANT -> Color(0xFFE8590C)
        PlaceCategory.CAFE -> Color(0xFF6F4E37)
        PlaceCategory.BAKERY -> Color(0xFFF08C00)
    }

/** 선택된 마커 색. */
val SelectedMarkerColor = Color(0xFF1971C2)
