package com.example.wood_restaurant.location

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberLocationPermissionState(): LocationPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasLocationPermission()) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        // 정확한 위치 또는 대략적인 위치 중 하나만 허용돼도 지도를 쓸 수 있다.
        granted = result.values.any { it }
    }

    return remember(launcher) {
        object : LocationPermissionState {
            override val isGranted: Boolean get() = granted
            override fun request() = launcher.launch(LOCATION_PERMISSIONS)
        }
    }
}
