package com.example.wifi_mapshow.ui

import com.example.wifi_mapshow.data.ProjectRoot
import com.example.wifi_mapshow.data.ScanPoint
import com.example.wifi_mapshow.data.WifiNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelTest {

    private val project = ProjectRoot(
        mapImage = "map.png",
        points = listOf(
            ScanPoint(
                x = 100,
                y = 200,
                networks = listOf(
                    WifiNetwork("Office", "AA:AA", 1, "-70", "WPA2"),
                    WifiNetwork("Office", "BB:BB", 1, "-45", "WPA2"),
                    WifiNetwork("Guest", "CC:CC", 6, "-60", "WPA2")
                )
            ),
            ScanPoint(
                x = 300,
                y = 400,
                networks = listOf(
                    WifiNetwork("Office", "AA:AA", 1, "-55", "WPA2"),
                    WifiNetwork("Office", "BB:BB", 1, "-65", "WPA2")
                )
            )
        )
    )

    @Test
    fun `networkFilterOptions includes ssid option with all bssid`() {
        val viewModel = MainViewModel()
        viewModel.setProject(project)

        val options = viewModel.networkFilterOptions()

        assertTrue(options.any { it is NetworkFilterOption.AllSsids })
        assertTrue(options.any { it == NetworkFilterOption.SsidAllBssids("Office") })
        assertTrue(options.any { it == NetworkFilterOption.SsidBssid("Office", "AA:AA") })
        assertTrue(options.any { it == NetworkFilterOption.SsidBssid("Office", "BB:BB") })
    }

    @Test
    fun `ssid all bssid mode picks strongest signal per point`() {
        val viewModel = MainViewModel()
        viewModel.setProject(project)
        viewModel.selectedNetworkFilter = NetworkFilterOption.SsidAllBssids("Office")

        val points = viewModel.filterNetworks()

        assertEquals(2, points.size)
        assertEquals(-45, points[0].network.rssi)
        assertEquals("BB:BB", points[0].network.bssid)
        assertEquals(-55, points[1].network.rssi)
        assertEquals("AA:AA", points[1].network.bssid)
    }
}
