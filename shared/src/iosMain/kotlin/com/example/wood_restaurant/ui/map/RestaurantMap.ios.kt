package com.example.wood_restaurant.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import cocoapods.NMapsGeometry.NMGLatLng
import cocoapods.NMapsMap.NMFCameraUpdate
import cocoapods.NMapsMap.NMFMapView
import cocoapods.NMapsMap.NMFMapViewCameraDelegateProtocol
import cocoapods.NMapsMap.NMFMapViewTouchDelegateProtocol
import cocoapods.NMapsMap.NMFMarker
import cocoapods.NMapsMap.NMFNaverMapView
import cocoapods.NMapsMap.NMF_MARKER_IMAGE_BLACK
import com.example.wood_restaurant.domain.LatLng
import com.example.wood_restaurant.domain.Restaurant
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGPoint
import platform.UIKit.UIColor
import platform.darwin.NSObject

/**
 * NMapsMap(NMFNaverMapView)을 Compose에 얹는다.
 *
 * 마커는 Compose가 아니라 [NaverMapController]가 직접 관리한다.
 * 오버레이 하나하나를 리컴포지션에 맡기면 지도가 매번 다시 그려져 성능이 나빠진다.
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
    modifier: Modifier,
) {
    val controller = remember { NaverMapController() }

    // 델리게이트/터치 핸들러가 항상 최신 콜백을 보도록 매 컴포지션마다 갱신한다.
    controller.onPlaceClick = onPlaceClick
    controller.onMapClick = onMapClick
    controller.onCameraMoved = onCameraMoved

    UIKitView(
        factory = { controller.createView(cameraTarget, zoom) },
        modifier = modifier,
        update = { controller.update(cameraTarget, zoom, places, selectedPlaceId) },
        onRelease = { controller.release() },
        properties = UIKitInteropProperties(
            // 지도 제스처가 Compose에 먹히지 않도록 터치를 네이티브 뷰에 넘긴다.
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
    private val markers = mutableMapOf<String, NMFMarker>()
    private var lastCameraTarget: LatLng? = null
    private var lastSelectedId: String? = null

    // 델리게이트는 weak 참조로 보관되므로 여기서 강하게 붙잡는다.
    private var cameraDelegate: NMFMapViewCameraDelegateProtocol? = null
    private var touchDelegate: NMFMapViewTouchDelegateProtocol? = null

    fun createView(cameraTarget: LatLng, zoom: Double): NMFNaverMapView {
        val view = NMFNaverMapView()
        view.showCompass = false
        view.showScaleBar = false
        view.showZoomControls = false
        view.showLocationButton = false

        val mapView = view.mapView
        mapView.rotateGestureEnabled = false
        mapView.tiltGestureEnabled = false
        mapView.moveCamera(
            NMFCameraUpdate.cameraUpdateWithScrollTo(cameraTarget.toNMG(), zoomTo = zoom)
        )

        val camera = object : NSObject(), NMFMapViewCameraDelegateProtocol {
            override fun mapViewCameraIdle(mapView: NMFMapView) {
                val target = mapView.cameraPosition.target
                onCameraMoved(LatLng(latitude = target.lat, longitude = target.lng))
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
        mapView.touchDelegate = touch

        naverMapView = view
        lastCameraTarget = cameraTarget
        return view
    }

    fun update(
        cameraTarget: LatLng,
        zoom: Double,
        places: List<Restaurant>,
        selectedPlaceId: String?,
    ) {
        val view = naverMapView ?: return
        val mapView = view.mapView

        // cameraTarget이 실제로 바뀌었을 때만 카메라를 움직인다(사용자 팬을 되돌리지 않기 위해).
        if (cameraTarget != lastCameraTarget) {
            lastCameraTarget = cameraTarget
            mapView.moveCamera(
                NMFCameraUpdate.cameraUpdateWithScrollTo(cameraTarget.toNMG(), zoomTo = zoom)
            )
        }

        val wanted = places.associateBy { it.id }

        // 사라진 마커 제거
        markers.keys.toList()
            .filter { it !in wanted }
            .forEach { id -> markers.remove(id)?.setMapView(null) }

        // 추가 · 갱신
        wanted.forEach { (id, place) ->
            val selected = id == selectedPlaceId
            val marker = markers.getOrPut(id) {
                NMFMarker().also { created ->
                    created.iconImage = NMF_MARKER_IMAGE_BLACK
                    created.captionText = place.name
                    created.captionMinZoom = 14.0
                    created.touchHandler = { _ ->
                        onPlaceClick(place)
                        true
                    }
                    created.setMapView(mapView)
                }
            }
            marker.position = place.position.toNMG()
            marker.iconTintColor = (if (selected) SelectedMarkerColor else place.category.markerColor).toUIColor()
            marker.width = if (selected) 32.0 else 24.0
            marker.height = if (selected) 42.0 else 32.0
            marker.zIndex = if (selected) 100 else 0
        }

        lastSelectedId = selectedPlaceId
    }

    fun release() {
        markers.values.forEach { it.setMapView(null) }
        markers.clear()
        naverMapView?.mapView?.let { mapView ->
            cameraDelegate?.let { mapView.removeCameraDelegate(delegate = it) }
            mapView.touchDelegate = null
        }
        cameraDelegate = null
        touchDelegate = null
        naverMapView = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun LatLng.toNMG(): NMGLatLng = NMGLatLng.latLngWithLat(latitude, lng = longitude)

private fun Color.toUIColor(): UIColor = UIColor.colorWithRed(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble(),
)
