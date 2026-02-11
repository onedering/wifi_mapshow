package com.example.wifi_mapshow.util

import android.graphics.Color
import kotlin.math.roundToInt

object RssiColorUtil {

    private const val MIN_RSSI = -85
    private const val MAX_RSSI = -30

    // Приближенная палитра из шкалы на примере Ekahau: от красно-оранжевого к зеленому.
    private val palette = listOf(
        Color.parseColor("#E85C47"), // -85
        Color.parseColor("#F06D3F"), // -80
        Color.parseColor("#F68A3C"), // -75
        Color.parseColor("#F8A644"), // -70
        Color.parseColor("#F8BF4B"), // -65
        Color.parseColor("#F6D651"), // -60
        Color.parseColor("#E8E54F"), // -55
        Color.parseColor("#CDEA4D"), // -50
        Color.parseColor("#A9E84C"), // -45
        Color.parseColor("#89E94E"), // -40
        Color.parseColor("#6EEA51"), // -35
        Color.parseColor("#54EA57")  // -30
    )

    fun colorForRssi(rssi: Int): Int {
        val clamped = rssi.coerceIn(MIN_RSSI, MAX_RSSI)
        val ratio = (clamped - MIN_RSSI).toFloat() / (MAX_RSSI - MIN_RSSI)
        val position = ratio * (palette.size - 1)

        val leftIndex = position.toInt().coerceIn(0, palette.lastIndex)
        val rightIndex = (leftIndex + 1).coerceAtMost(palette.lastIndex)
        val fraction = position - leftIndex

        if (leftIndex == rightIndex) return palette[leftIndex]
        return blend(palette[leftIndex], palette[rightIndex], fraction)
    }

    private fun blend(startColor: Int, endColor: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val r = (Color.red(startColor) + (Color.red(endColor) - Color.red(startColor)) * f).roundToInt()
        val g = (Color.green(startColor) + (Color.green(endColor) - Color.green(startColor)) * f).roundToInt()
        val b = (Color.blue(startColor) + (Color.blue(endColor) - Color.blue(startColor)) * f).roundToInt()
        return Color.rgb(r, g, b)
    }
}
