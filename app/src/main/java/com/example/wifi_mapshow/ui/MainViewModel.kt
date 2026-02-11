package com.example.wifi_mapshow.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.wifi_mapshow.data.ProjectRoot
import com.example.wifi_mapshow.data.ScanPoint
import com.example.wifi_mapshow.data.WifiNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ChannelBand(
    val title: String,
    private val matcher: (Int) -> Boolean
) {
    BAND_24("2.4 ГГц (1-13)", { it in 1..13 }),
    BAND_5("5 ГГц (34-180)", { it in 34..180 }),
    BAND_6_LOW("6 ГГц (14-33)", { it in 14..33 }),
    BAND_6_HIGH("6 ГГц (>180)", { it > 180 }),
    ALL("Все каналы", { true });

    fun matches(channel: Int): Boolean = matcher(channel)

    override fun toString(): String = title
}

sealed interface NetworkFilterOption {
    val label: String

    data object AllSsids : NetworkFilterOption {
        override val label: String = "Все SSID (все BSSID)"

        override fun toString(): String = label
    }

    data class SsidBssid(
        val ssid: String,
        val bssid: String
    ) : NetworkFilterOption {
        override val label: String = "$ssid ($bssid)"

        override fun toString(): String = label
    }

    data class SsidAllBssids(
        val ssid: String
    ) : NetworkFilterOption {
        override val label: String = "$ssid (все BSSID)"

        override fun toString(): String = label
    }
}

class MainViewModel : ViewModel() {

    private val _points = MutableStateFlow<List<ScanPoint>>(emptyList())
    val points: StateFlow<List<ScanPoint>> = _points

    private val _mapUri = MutableStateFlow<Uri?>(null)
    val mapUri: StateFlow<Uri?> = _mapUri

    var selectedBand: ChannelBand = ChannelBand.ALL
    var selectedNetworkFilter: NetworkFilterOption = NetworkFilterOption.AllSsids

    fun setProject(project: ProjectRoot) {
        _points.value = project.points
    }

    fun setMap(uri: Uri) {
        _mapUri.value = uri
    }

    fun networkFilterOptions(): List<NetworkFilterOption> {
        val uniqueSsids = _points.value
            .flatMap { it.networks }
            .map { it.ssid }
            .distinct()
            .sorted()
            .map { NetworkFilterOption.SsidAllBssids(it) }

        val uniqueNetworks = _points.value
            .flatMap { it.networks }
            .distinctBy { "${it.ssid}|${it.bssid}" }
            .sortedWith(compareBy<WifiNetwork> { it.ssid }.thenBy { it.bssid })
            .map { NetworkFilterOption.SsidBssid(it.ssid, it.bssid) }

        return listOf(NetworkFilterOption.AllSsids) + uniqueSsids + uniqueNetworks
    }

    fun filterNetworks(): List<FilteredPoint> {
        return _points.value.mapNotNull { point ->
            val filtered = point.networks
                .filter { selectedBand.matches(it.channel) }
                .filter {
                    when (val option = selectedNetworkFilter) {
                        is NetworkFilterOption.AllSsids -> true
                        is NetworkFilterOption.SsidAllBssids -> {
                            it.ssid == option.ssid
                        }
                        is NetworkFilterOption.SsidBssid -> {
                            it.ssid == option.ssid && it.bssid == option.bssid
                        }
                    }
                }

            val best = filtered.maxByOrNull { it.rssi } ?: return@mapNotNull null
            FilteredPoint(point.x, point.y, best)
        }
    }
}

data class FilteredPoint(
    val x: Int,
    val y: Int,
    val network: WifiNetwork
)
