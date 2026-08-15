package com.example.wood_restaurant.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/** 위치 권한 상태. Compose에서 관찰 가능하도록 [Stable]. */
@Stable
interface LocationPermissionState {
    val isGranted: Boolean

    /** 시스템 권한 다이얼로그를 띄운다. 이미 허용됐거나 영구 거부면 아무 일도 일어나지 않을 수 있다. */
    fun request()
}

@Composable
expect fun rememberLocationPermissionState(): LocationPermissionState
