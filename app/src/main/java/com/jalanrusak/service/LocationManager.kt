package com.jalanrusak.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    suspend fun getCurrentLocation(): Location = suspendCancellableCoroutine { continuation ->
        // Check permissions
        if (!hasLocationPermission()) {
            continuation.resumeWithException(
                SecurityException("Location permission not granted")
            )
            return@suspendCancellableCoroutine
        }

        try {
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(location)
                } else {
                    // Fallback to last known location
                    getLastKnownLocation(continuation)
                }
            }.addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }.apply {
                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    private fun getLastKnownLocation(continuation: kotlin.coroutines.CancellableContinuation<Location>) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(location)
                } else {
                    continuation.resumeWithException(
                        Exception("Unable to get location. Please ensure GPS is enabled.")
                    )
                }
            }.addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
