package com.parousya.android.sample.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.parousya.android.sdk.ParousyaSAASSDK
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Tuong (Alan) on 2019-08-26.
 * Copyright (c) 2019 Buuuk. All rights reserved.
 */

abstract class BaseActivity : AppCompatActivity() {
    protected val pref: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    protected val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    protected val dfDisplay = SimpleDateFormat("dd MMM yyyy 'at' hh:mm aaa", Locale.getDefault())

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    abstract fun containerView(): View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getLocationUpdates()
        df.timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun getLocationUpdates() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create().apply {
            interval = 50000
            fastestInterval = 50000
            smallestDisplacement = 170f // 170 m = 0.1 mile
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.locations.isNotEmpty()) {
                    val location = locationResult.lastLocation
                    println("lastLocation: $location")
                    ParousyaSAASSDK.getInstance().latLocation(location)
                } else {
                    println("lastLocation not available")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        ParousyaSAASSDK.getInstance().permissionsWizard(this,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            onSuccess = {
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }, onError = { error ->
                Snackbar.make(containerView(), error.localizedMessage, Snackbar.LENGTH_SHORT)
                    .show()
            })
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
}