package com.wiseyoung.pro.util

data class MapCameraFocus(
    val latitude: Double,
    val longitude: Double,
    val zoomLevel: Int = 15,
    val requestId: Long = System.nanoTime()
)
