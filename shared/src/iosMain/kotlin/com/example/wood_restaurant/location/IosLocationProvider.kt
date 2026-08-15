package com.example.wood_restaurant.location

import com.example.wood_restaurant.domain.LatLng
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

private const val LOCATION_TIMEOUT_MS = 8_000L

class IosLocationProvider : LocationProvider {

    // CLLocationManager.delegate는 weak라서 강한 참조를 이쪽에서 들고 있어야 콜백이 온다.
    private var manager: CLLocationManager? = null
    private var delegate: CLLocationManagerDelegateProtocol? = null

    override suspend fun currentLocation(): LatLng? = withContext(Dispatchers.Main) {
        // 권한 미결정/거부 상태에서는 델리게이트 콜백이 아예 안 올 수 있어 타임아웃을 둔다.
        withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val locationManager = CLLocationManager()
                var resumed = false

                fun finish(value: LatLng?) {
                    if (!resumed && continuation.isActive) {
                        resumed = true
                        continuation.resume(value)
                    }
                }

                val locationDelegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManager(
                        manager: CLLocationManager,
                        didUpdateLocations: List<*>,
                    ) {
                        finish((didUpdateLocations.lastOrNull() as? CLLocation)?.toLatLng())
                    }

                    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                        finish(null)
                    }
                }

                manager = locationManager
                delegate = locationDelegate

                locationManager.delegate = locationDelegate
                locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
                locationManager.requestLocation()
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toLatLng(): LatLng = coordinate.useContents {
    LatLng(latitude = latitude, longitude = longitude)
}
