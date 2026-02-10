package com.example.wifi_mapshow.ui


import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.example.wifi_mapshow.R
import com.example.wifi_mapshow.util.RssiColorUtil

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var overlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlay = findViewById(R.id.overlay)

        findViewById<View>(R.id.showButton).setOnClickListener {
            drawPoints()
        }
    }

    private fun drawPoints() {
        val points = viewModel.filterNetworks()

        val bitmap = Bitmap.createBitmap(
            overlay.width,
            overlay.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
        }

        points.forEach {
            paint.color = RssiColorUtil.colorForRssi(it.network.rssi)
            canvas.drawCircle(it.x.toFloat(), it.y.toFloat(), 6f, paint)

            paint.color = Color.BLACK
            canvas.drawText(
                "${it.network.rssi} dBm (${it.network.ssid} / ch${it.network.channel})",
                it.x + 8f,
                it.y - 8f,
                paint
            )
        }

        overlay.background = BitmapDrawable(resources, bitmap)
    }
}