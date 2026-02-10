package com.example.wifi_mapshow.util


import android.graphics.Color

object RssiColorUtil {

    fun colorForRssi(rssi: Int): Int =
        when {
            rssi >= -50 -> Color.GREEN
            rssi >= -60 -> Color.YELLOW
            rssi >= -70 -> Color.rgb(255, 165, 0)
            else -> Color.RED
        }
}