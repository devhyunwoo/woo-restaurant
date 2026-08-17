package com.example.wood_restaurant.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import cocoapods.NMapsMap.NMCBuilder
import cocoapods.NMapsMap.NMCClusterer
import cocoapods.NMapsMap.NMCClusteringKeyProtocol
import cocoapods.NMapsMap.NMCDefaultLeafMarkerUpdater
import cocoapods.NMapsMap.NMCLeafMarkerInfo
import cocoapods.NMapsMap.NMFCameraUpdate
import cocoapods.NMapsMap.NMFMapView
import cocoapods.NMapsMap.NMFMapViewCameraDelegateProtocol
import cocoapods.NMapsMap.NMFMapViewTouchDelegateProtocol
import cocoapods.NMapsMap.NMFMarker
import cocoapods.NMapsMap.NMFMyPositionDisabled
import cocoapods.NMapsMap.NMFMyPositionNormal
import cocoapods.NMapsMap.NMFNaverMapView
import cocoapods.NMapsMap.NMGLatLng
import cocoapods.NMapsMap.NMF_MARKER_IMAGE_BLACK
import com.example.wood_restaurant.domain.LatLng
import com.example.wood_restaurant.domain.Restaurant
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGPoint
import platform.Foundation.NSZone
import platform.UIKit.UIColor
import platform.darwin.NSObject

/**
 * NMapsMap(NMFNaverMapView)을 Compose에 얹는다.
 *
 * 마커는 Compose가 아니라 [NaverMapController]가 SDK 클러스터러(NMCClusterer)에 맡긴다.
 * 오버레이 하나하나를 리컴포지션에 맡기면 매번 지도를 다시 그리게 되어 손해다.
 *
 * cinterop이 만든 바인딩에서 Objective-C 프로퍼티는 전부 `x()` / `setX()` 함수 쌍으로 나온다.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
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
    val controller = remember { NaverMapController() }

    // 델리게이트/터치 핸들러가 항상 최신 콜백을 보도록 매 컴포지션마다 갱신한다.
    controller.onPlaceClick = onPlaceClick
    controller.onMapClick = onMapClick
    controller.onCameraMoved = onCameraMoved

    UIKitView(
        factory = { controller.createView(cameraTarget, zoom, isDarkMode) },
        modifier = modifier,
        update = { controller.update(cameraTarget, places, selectedPlaceId, isDarkMode, showMyLocation) },
        onRelease = { controller.release() },
        properties = UIKitInteropProperties(
            // 지도 제스처가 Compose에 가로채이지 않도록 터치를 네이티브 뷰에 넘긴다.
            interactionMode = UIKitInteropInteractionMode.NonCooperative,
        ),
    )
}

@OptIn(ExperimentalForeignApi::class)
private class NaverMapController {

    var onPlaceClick: (Restaurant) -> Unit = {}
    var onMapClick: () -> Unit = {}
    var onCameraMoved: (LatLng) -> Unit = {}

    private var naverMapView: NMFNaverMapView? = null
    private var clusterer: NMCClusterer? = null
    private var selectedMarker: NMFMarker? = null
    private var lastCameraTarget: LatLng? = null
    private var lastPlaces: List<Restaurant> = emptyList()

    // 델리게이트/업데이터는 weak 참조로 보관되므로 여기서 강하게 붙잡아야 콜백이 온다.
    private var cameraDelegate: NMFMapViewCameraDelegateProtocol? = null
    private var touchDelegate: NMFMapViewTouchDelegateProtocol? = null
    private var leafUpdater: PlaceLeafMarkerUpdater? = null

    fun createView(cameraTarget: LatLng, zoom: Double, isDarkMode: Boolean): NMFNaverMapView {
        val view = NMFNaverMapView()
        view.setShowCompass(false)
        view.setShowScaleBar(false)
        view.setShowZoomControls(false)
        view.setShowLocationButton(false)

        val mapView = view.mapView()
        mapView.setRotateGestureEnabled(false)
        mapView.setTiltGestureEnabled(false)
        mapView.setNightModeEnabled(isDarkMode)
        mapView.moveCamera(
            NMFCameraUpdate.cameraUpdateWithScrollTo(cameraTarget.toNMG(), zoomTo = zoom)
        )

        val camera = object : NSObject(), NMFMapViewCameraDelegateProtocol {
            override fun mapViewCameraIdle(mapView: NMFMapView) {
                val target = mapView.cameraPosition().target()
                onCameraMoved(LatLng(latitude = target.lat(), longitude = target.lng()))
            }
        }
        val touch = object : NSObject(), NMFMapViewTouchDelegateProtocol {
            override fun mapView(mapView: NMFMapView, didTapMap: NMGLatLng, point: CValue<CGPoint>) {
                onMapClick()
            }
        }
        cameraDelegate = camera
        touchDelegate = touch
        mapView.addCameraDelegate(delegate = camera)
        mapView.setTouchDelegate(touch)

        val updater = PlaceLeafMarkerUpdater { place -> onPlaceClick(place) }
        leafUpdater = updater
        clusterer = NMCBuilder().apply {
            setLeafMarkerUpdater(updater)
        }.build().also { it.setMapView(mapView) }

        naverMapView = view
        lastCameraTarget = cameraTarget
        return view
    }

    fun update(
        cameraTarget: LatLng,
        places: List<Restaurant>,
        selectedPlaceId: String?,
        isDarkMode: Boolean,
        showMyLocation: Boolean,
    ) {
        val view = naverMapView ?: return
        val mapView = view.mapView()

        if (mapView.isNightModeEnabled() != isDarkMode) mapView.setNightModeEnabled(isDarkMode)

        // 현재 위치 파란 점. 권한이 없을 때 켜면 SDK가 자체적으로 권한 팝업을 띄우므로 권한 있을 때만.
        val wantedMode = if (showMyLocation) NMFMyPositionNormal else NMFMyPositionDisabled
        if (mapView.positionMode() != wantedMode) mapView.setPositionMode(wantedMode)

        // cameraTarget이 실제로 바뀌었을 때만 움직인다(사용자 팬을 되돌리지 않기 위해).
        // 줌은 최초 생성 때만 지정한다. 여기서 다시 주면 사용자가 맞춰둔 배율이 되돌아간다.
        if (cameraTarget != lastCameraTarget) {
            lastCameraTarget = cameraTarget
            mapView.moveCamera(NMFCameraUpdate.cameraUpdateWithScrollTo(cameraTarget.toNMG()))
        }

        // 목록이 바뀌었을 때만 클러스터러 내용을 갈아끼운다. (update는 자주 불린다)
        if (places != lastPlaces) {
            lastPlaces = places
            clusterer?.let { c ->
                c.clear()
                if (places.isNotEmpty()) {
                    // 태그는 키를 그대로 넣는다. 실제 장소는 키가 들고 있다.
                    val keyTagMap: Map<Any?, Any?> = places.associate { place ->
                        val key = PlaceKey(place)
                        key to key
                    }
                    c.addAll(keyTagMap)
                }
            }
        }

        updateSelectedMarker(mapView, places.firstOrNull { it.id == selectedPlaceId })
    }

    /** 선택된 장소는 클러스터에 묻히지 않도록 위에 따로 크게 그린다. */
    private fun updateSelectedMarker(mapView: NMFMapView, selected: Restaurant?) {
        if (selected == null) {
            selectedMarker?.setMapView(null)
            selectedMarker = null
            return
        }
        val marker = selectedMarker ?: NMFMarker().also { created ->
            created.setIconImage(NMF_MARKER_IMAGE_BLACK)
            created.setIconTintColor(SelectedMarkerColor.toUIColor())
            created.setWidth(32.0)
            created.setHeight(42.0)
            created.setZIndex(100L)
            created.setGlobalZIndex(SELECTED_GLOBAL_Z)
            selectedMarker = created
        }
        marker.setPosition(selected.position.toNMG())
        marker.setCaptionText(selected.name)
        marker.setTouchHandler { _ ->
            onPlaceClick(selected)
            true
        }
        if (marker.mapView() == null) marker.setMapView(mapView)
    }

    fun release() {
        clusterer?.let {
            it.clear()
            it.setMapView(null)
        }
        clusterer = null
        selectedMarker?.setMapView(null)
        selectedMarker = null
        naverMapView?.mapView()?.let { mapView ->
            cameraDelegate?.let { mapView.removeCameraDelegate(delegate = it) }
            mapView.setTouchDelegate(null)
        }
        cameraDelegate = null
        touchDelegate = null
        leafUpdater = null
        naverMapView = null
    }
}

/**
 * 클러스터러 키. NMCClusteringKey는 NSCopying을 요구하고 딕셔너리 키로 쓰이므로
 * 복사·동등성·해시를 id 기준으로 맞춘다.
 */
@OptIn(ExperimentalForeignApi::class)
private class PlaceKey(val place: Restaurant) : NSObject(), NMCClusteringKeyProtocol {
    private val latLng: NMGLatLng = place.position.toNMG()

    override fun position(): NMGLatLng = latLng

    override fun copyWithZone(zone: CPointer<NSZone>?): Any = this

    override fun isEqual(`object`: Any?): Boolean =
        `object` is PlaceKey && `object`.place.id == place.id

    override fun hash(): ULong = place.id.hashCode().toULong()
}

/** 리프(개별) 마커 꾸미기. 기본 업데이터가 위치를 잡아주고, 우리는 색·캡션·탭만 얹는다. */
@OptIn(ExperimentalForeignApi::class)
private class PlaceLeafMarkerUpdater(
    private val onPlaceClick: (Restaurant) -> Unit,
) : NMCDefaultLeafMarkerUpdater() {

    override fun updateLeafMarker(info: NMCLeafMarkerInfo, _1: NMFMarker) {
        super.updateLeafMarker(info, _1)
        val place = (info.key() as? PlaceKey)?.place ?: return
        _1.setIconImage(NMF_MARKER_IMAGE_BLACK)
        _1.setIconTintColor(place.category.markerColor.toUIColor())
        _1.setWidth(24.0)
        _1.setHeight(32.0)
        _1.setCaptionText(place.name)
        _1.setCaptionMinZoom(MARKER_CAPTION_MIN_ZOOM)
        _1.setTouchHandler { _ ->
            onPlaceClick(place)
            true
        }
    }
}

/** 클러스터/리프 마커(기본 전역 z=200000)보다 위에 오도록. */
private const val SELECTED_GLOBAL_Z = 300_000L

@OptIn(ExperimentalForeignApi::class)
private fun LatLng.toNMG(): NMGLatLng = NMGLatLng.latLngWithLat(lat = latitude, lng = longitude)

private fun Color.toUIColor(): UIColor = UIColor.colorWithRed(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble(),
)
