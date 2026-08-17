package com.example.wood_restaurant.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.wood_restaurant.domain.LatLng
import com.example.wood_restaurant.domain.Restaurant
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.clustering.Clusterer
import com.naver.maps.map.clustering.ClusteringKey
import com.naver.maps.map.clustering.DefaultLeafMarkerUpdater
import com.naver.maps.map.clustering.LeafMarkerInfo
import com.naver.maps.map.compose.DisposableMapEffect
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.LocationTrackingMode
import com.naver.maps.map.compose.MapProperties
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.compose.rememberFusedLocationSource
import com.naver.maps.map.compose.rememberUpdatedMarkerState
import com.naver.maps.map.overlay.Marker
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
    isDarkMode: Boolean,
    showMyLocation: Boolean,
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

    // 클러스터러의 리프 마커 업데이터는 한 번 만들어지므로, 최신 콜백을 state로 들여다본다.
    val currentOnPlaceClick by rememberUpdatedState(onPlaceClick)
    // Clusterer는 Compose 밖에서 마커를 만들므로 dp가 아니라 px를 직접 줘야 한다.
    val density = LocalDensity.current
    val leafWidthPx = with(density) { 24.dp.roundToPx() }
    val leafHeightPx = with(density) { 32.dp.roundToPx() }
    val clusterer = remember {
        Clusterer.Builder<PlaceKey>()
            .leafMarkerUpdater(object : DefaultLeafMarkerUpdater() {
                override fun updateLeafMarker(info: LeafMarkerInfo, marker: Marker) {
                    super.updateLeafMarker(info, marker)
                    val place = (info.key as PlaceKey).place
                    marker.icon = BlackMarkerIcon
                    marker.iconTintColor = place.category.markerColor.toArgb()
                    marker.width = leafWidthPx
                    marker.height = leafHeightPx
                    marker.captionText = place.name
                    marker.captionMinZoom = MARKER_CAPTION_MIN_ZOOM
                    marker.setOnClickListener {
                        currentOnPlaceClick(place)
                        true
                    }
                }
            })
            .build()
    }

    // 목록이 바뀔 때만 클러스터러 내용을 갈아끼운다.
    LaunchedEffect(clusterer, places) {
        clusterer.clear()
        if (places.isNotEmpty()) {
            clusterer.addAll(places.associate { PlaceKey(it) to it })
        }
    }

    NaverMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            locationTrackingMode = if (showMyLocation) LocationTrackingMode.NoFollow else LocationTrackingMode.None,
            isNightModeEnabled = isDarkMode,
        ),
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
        // 클러스터러는 NaverMap 인스턴스가 필요하다. 지도가 사라지면 붙였던 마커도 같이 걷어낸다.
        DisposableMapEffect(clusterer) { map ->
            clusterer.map = map
            onDispose { clusterer.map = null }
        }

        // 선택된 장소는 클러스터에 묻히지 않도록 위에 따로 크게 그린다.
        val selected = places.firstOrNull { it.id == selectedPlaceId }
        if (selected != null) {
            Marker(
                state = rememberUpdatedMarkerState(position = selected.position.toNaver()),
                icon = BlackMarkerIcon,
                iconTintColor = SelectedMarkerColor,
                width = 32.dp,
                height = 42.dp,
                captionText = selected.name,
                zIndex = 100,
                globalZIndex = SELECTED_GLOBAL_Z,
                onClick = {
                    onPlaceClick(selected)
                    true
                },
            )
        }
    }
}

/** 클러스터러 키. 같은 장소는 같은 키가 되도록 id로만 비교한다. */
private class PlaceKey(val place: Restaurant) : ClusteringKey {
    override fun getPosition(): NaverLatLng = place.position.toNaver()
    override fun equals(other: Any?): Boolean = other is PlaceKey && other.place.id == place.id
    override fun hashCode(): Int = place.id.hashCode()
}

/** iconTintColor는 검정 마커에 입혀야 의도한 색이 나온다. SDK가 기본 제공하는 리소스를 쓴다. */
private val BlackMarkerIcon: OverlayImage by lazy {
    OverlayImage.fromResource(com.naver.maps.map.R.drawable.navermap_default_marker_icon_black)
}

/** 클러스터/리프 마커(기본 전역 z=200000)보다 위에 오도록. */
private const val SELECTED_GLOBAL_Z = 300_000

private fun LatLng.toNaver(): NaverLatLng = NaverLatLng(latitude, longitude)

private fun NaverLatLng.toDomain(): LatLng = LatLng(latitude = latitude, longitude = longitude)
