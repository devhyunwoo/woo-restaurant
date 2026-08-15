package com.example.wood_restaurant.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.darwin.NSObject

@Composable
actual fun rememberLocationPermissionState(): LocationPermissionState {
    val manager = remember { CLLocationManager() }
    var status by remember { mutableStateOf(manager.authorizationStatus) }

    // delegate가 weak 참조라 remember로 강하게 붙잡아 둔다.
    val delegate = remember {
        object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                status = manager.authorizationStatus
            }
        }
    }

    DisposableEffect(manager, delegate) {
        manager.delegate = delegate
        status = manager.authorizationStatus
        onDispose { manager.delegate = null }
    }

    val granted = status == kCLAuthorizationStatusAuthorizedWhenInUse ||
        status == kCLAuthorizationStatusAuthorizedAlways

    return remember(granted) {
        object : LocationPermissionState {
            override val isGranted: Boolean = granted
            override fun request() {
                manager.requestWhenInUseAuthorization()
            }
        }
    }
}
