package com.example.wifi_mapshow.ui



import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.wifi_mapshow.data.*

class MainViewModel : ViewModel() {

    private val _points = MutableStateFlow<List<ScanPoint>>(emptyList())
    val points: StateFlow<List<ScanPoint>> = _points

    private val _mapUri = MutableStateFlow<Uri?>(null)
    val mapUri: StateFlow<Uri?> = _mapUri

    var selectedChannelRange: (Int) -> Boolean = { true }
    var selectedSsid: String? = null
    var selectedBssid: String? = null

    fun setProject(project: ProjectRoot) {
        _points.value = project.points
    }

    fun setMap(uri: Uri) {
        _mapUri.value = uri
    }

    fun filterNetworks(): List<FilteredPoint> {
        return _points.value.mapNotNull { point ->
            val filtered = point.networks
                .filter { selectedChannelRange(it.channel) }
                .filter {
                    when {
                        selectedBssid != null ->
                            it.ssid == selectedSsid && it.bssid == selectedBssid
                        selectedSsid != null ->
                            it.ssid == selectedSsid
                        else -> true
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
