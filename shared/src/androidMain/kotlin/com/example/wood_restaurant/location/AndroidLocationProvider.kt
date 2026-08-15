package com.example.wood_restaurant.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.wood_restaurant.domain.LatLng
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidLocationProvider(
    private val context: Context,
) : LocationProvider {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    override suspend fun currentLocation(): LatLng? {
        if (!context.hasLocationPermission()) return null

        val cancellation = CancellationTokenSource()
        return try {
            // getCurrentLocation은 새 측위를 시도하고, 실패하면 마지막 위치로 폴백한다.
            val location = client
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                .awaitOrNull()
                ?: client.lastLocation.awaitOrNull()

            location?.let { LatLng(latitude = it.latitude, longitude = it.longitude) }
        } catch (e: SecurityException) {
            null
        } finally {
            cancellation.cancel()
        }
    }
}

internal fun Context.hasLocationPermission(): Boolean =
    LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

internal val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** Task를 코루틴으로. 실패는 예외 대신 null로 돌려 호출부를 단순하게 유지한다. */
private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (continuation.isActive) {
            continuation.resume(if (task.isSuccessful) task.result else null)
        }
    }
}
