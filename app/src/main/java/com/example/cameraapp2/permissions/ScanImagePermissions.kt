package com.example.cameraapp2.permissions

import android.Manifest

val scanImagePermissions = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.INTERNET,
    Manifest.permission.ACCESS_NETWORK_STATE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)

val positionPermissions = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION
)