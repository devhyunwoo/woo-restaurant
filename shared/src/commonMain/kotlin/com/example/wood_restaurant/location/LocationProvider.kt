package com.example.wood_restaurant.location

import com.example.wood_restaurant.domain.LatLng

/**
 * 현재 위치 조회. 플랫폼별 구현은 Koin platformModule에서 주입한다.
 * (Android: FusedLocationProviderClient, iOS: CLLocationManager)
 */
interface LocationProvider {
    /** 권한이 없거나 위치를 못 얻으면 null. */
    suspend fun currentLocation(): LatLng?
}
