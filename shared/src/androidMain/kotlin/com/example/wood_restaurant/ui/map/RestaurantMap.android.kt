package com.example.wood_restaurant.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wood_restaurant.domain.LatLng
import com.example.wood_restaurant.domain.Restaurant
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.LocationTrackingMode
import com.naver.maps.map.compose.MapProperties
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.compose.rememberFusedLocationSource
import com.naver.maps.map.compose.rememberUpdatedMarkerState
import com.naver.maps.map.overlay.OverlayImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import com.naver.maps.geometry.LatLng as NaverLatLng

@OptIn(ExperimentalNaverMapApi::class)
@Composable
actual fun RestaurantMap(
    cameraTarget: LatLng,
    zoom: Double,
    places: List<Restaurant>,
    selectedPlaceId: String?,
    onPlaceClick: (Restaurant) -> Unit,
    onMapClick: () -> Unit,
    onCameraMoved: (LatLng) -> Unit,
    modifier: Modifier,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(cameraTarget.toNaver(), zoom)
    }

    // cameraTarget이 바뀔 때만 카메라를 옮긴다. 사용자의 팬/줌은 건드리지 않는다.
    LaunchedEffect(cameraTarget) {
        cameraPositionState.animate(
            CameraUpdate.scrollTo(cameraTarget.toNaver()),
            CameraAnimation.Easing,
        )
    }

    // 카메라가 멈춘 순간의 중심만 보고한다.
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }
            .distinctUntilChanged()
            .filter { isMoving -> !isMoving }
            .collect { onCameraMoved(cameraPositionState.position.target.toDomain()) }
    }

    NaverMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(locationTrackingMode = LocationTrackingMode.NoFollow),
        uiSettings = MapUiSettings(
            isLocationButtonEnabled = false,
            isZoomControlEnabled = false,
            isCompassEnabled = false,
            isTiltGesturesEnabled = false,
            isRotateGesturesEnabled = false,
        ),
        locationSource = rememberFusedLocationSource(),
        onMapClick = { _, _ -> onMapClick() },
    ) {
        places.forEach { place ->
            key(place.id) {
                val selected = place.id == selectedPlaceId
                Marker(
                    state = rememberUpdatedMarkerState(position = place.position.toNaver()),
                    // 검정 마커에 색을 입혀야 카테고리 색이 그대로 나온다.
                    icon = BlackMarkerIcon,
                    iconTintColor = if (selected) SelectedMarkerColor else place.category.markerColor,
                    width = if (selected) 32.dp else 24.dp,
                    height = if (selected) 42.dp else 32.dp,
                    captionText = place.name,
                    captionMinZoom = 14.0,
                    zIndex = if (selected) 100 else 0,
                    onClick = {
                        onPlaceClick(place)
                        true
                    },
                )
            }
        }
    }
}

/** iconTintColor는 검정 마커에 입혀야 의도한 색이 나온다. SDK가 기본 제공하는 리소스를 쓴다. */
private val BlackMarkerIcon: OverlayImage by lazy {
    OverlayImage.fromResource(com.naver.maps.map.R.drawable.navermap_default_marker_icon_black)
}

private fun LatLng.toNaver(): NaverLatLng = NaverLatLng(latitude, longitude)

private fun NaverLatLng.toDomain(): LatLng = LatLng(latitude = latitude, longitude = longitude)
