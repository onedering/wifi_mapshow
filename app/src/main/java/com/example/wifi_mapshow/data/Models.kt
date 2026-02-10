package com.example.wifi_mapshow.data

import com.google.gson.annotations.SerializedName
import com.google.gson.Gson

data class ProjectRoot(
    @SerializedName("map_image")
    val mapImage: String,

    @SerializedName("points_data")
    val points: List<ScanPoint>
)

data class ScanPoint(
    val x: Int,
    val y: Int,
    val networks: List<WifiNetwork>
)

data class WifiNetwork(
    @SerializedName("SSID")
    val ssid: String,

    @SerializedName("BSSID")
    val bssid: String,

    @SerializedName("Channel")
    val channel: Int,

    @SerializedName("RSSI (dBm)")
    val rssiRaw: String,

    @SerializedName("Encryption")
    val encryption: String
) {
    val rssi: Int
        get() = rssiRaw.replace(" dBm", "").toInt()
}
