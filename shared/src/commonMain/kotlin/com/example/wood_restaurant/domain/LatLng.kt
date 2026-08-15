package com.example.wood_restaurant.domain

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** 플랫폼 지도 SDK에 종속되지 않는 좌표 표현. */
@Serializable
data class LatLng(
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        /** 좌표를 못 구했을 때의 기본 위치(서울시청). */
        val SEOUL_CITY_HALL = LatLng(37.5666102, 126.9783881)
    }
}

private const val EARTH_RADIUS_METERS = 6_371_008.8

/** 보통 걸음 속도. 4km/h ≈ 67m/분. */
private const val WALKING_METERS_PER_MINUTE = 67.0

/** 두 좌표 사이의 대권 거리(미터). Haversine. */
fun LatLng.distanceTo(other: LatLng): Double {
    val dLat = (other.latitude - latitude).toRadians()
    val dLng = (other.longitude - longitude).toRadians()
    val a = sin(dLat / 2).pow(2) +
        cos(latitude.toRadians()) * cos(other.latitude.toRadians()) * sin(dLng / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * atan2(sqrt(a), sqrt(1 - a))
}

/** "230m" / "1.4km" 형태의 표시용 문자열. */
fun formatDistance(meters: Double): String = when {
    meters < 1_000 -> "${meters.roundToInt()}m"
    else -> "${((meters / 100).roundToInt() / 10.0)}km"
}

/** 도보 소요 시간(분). 1분 미만은 1분으로 올린다. */
fun walkingMinutes(meters: Double): Int =
    (meters / WALKING_METERS_PER_MINUTE).roundToInt().coerceAtLeast(1)

private fun Double.toRadians(): Double = this * PI / 180.0
