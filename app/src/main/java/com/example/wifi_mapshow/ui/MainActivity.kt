package com.example.wifi_mapshow.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.wifi_mapshow.R
import com.example.wifi_mapshow.data.JsonRepository
import com.example.wifi_mapshow.util.RssiColorUtil

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var mapImage: ImageView
    private lateinit var overlay: ImageView
    private lateinit var channelSpinner: Spinner
    private lateinit var networkSpinner: Spinner

    private lateinit var channelAdapter: ArrayAdapter<ChannelBand>
    private lateinit var networkAdapter: ArrayAdapter<NetworkFilterOption>

    private val pickMapLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.setMap(uri)
            mapImage.setImageURI(uri)
            clearOverlay()
        }

    private val pickJsonLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val project = JsonRepository(this).loadProject(uri)
                viewModel.setProject(project)
                updateNetworkOptions()
                Toast.makeText(this, "JSON загружен: ${project.points.size} точек", Toast.LENGTH_SHORT)
                    .show()
                clearOverlay()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка загрузки JSON: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapImage = findViewById(R.id.mapImage)
        overlay = findViewById(R.id.overlay)
        channelSpinner = findViewById(R.id.channelSpinner)
        networkSpinner = findViewById(R.id.networkSpinner)

        setupSpinners()
        setupActions()
    }

    private fun setupSpinners() {
        channelAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            ChannelBand.entries.toTypedArray()
        )
        channelSpinner.adapter = channelAdapter

        networkAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            mutableListOf<NetworkFilterOption>(NetworkFilterOption.AllSsids)
        )
        networkSpinner.adapter = networkAdapter
    }

    private fun setupActions() {
        findViewById<Button>(R.id.openMapButton).setOnClickListener {
            pickMapLauncher.launch(arrayOf("image/*"))
        }

        findViewById<Button>(R.id.openJsonButton).setOnClickListener {
            pickJsonLauncher.launch(arrayOf("application/json", "text/json", "text/plain"))
        }

        findViewById<Button>(R.id.showButton).setOnClickListener {
            viewModel.selectedBand = channelSpinner.selectedItem as? ChannelBand ?: ChannelBand.ALL
            viewModel.selectedNetworkFilter =
                networkSpinner.selectedItem as? NetworkFilterOption ?: NetworkFilterOption.AllSsids
            drawPoints()
        }
    }

    private fun updateNetworkOptions() {
        val options = viewModel.networkFilterOptions()
        networkAdapter.clear()
        networkAdapter.addAll(options)
        networkAdapter.notifyDataSetChanged()
        networkSpinner.setSelection(0)
    }

    private fun clearOverlay() {
        overlay.setImageDrawable(null)
    }

    private fun drawPoints() {
        val points = viewModel.filterNetworks()
        val mapDrawable = mapImage.drawable
        if (mapDrawable == null) {
            Toast.makeText(this, "Сначала загрузите карту", Toast.LENGTH_SHORT).show()
            return
        }

        if (overlay.width == 0 || overlay.height == 0) {
            Toast.makeText(this, "Невозможно отрисовать: layout ещё не измерен", Toast.LENGTH_SHORT).show()
            return
        }

        val bitmap = Bitmap.createBitmap(overlay.width, overlay.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            strokeWidth = 2f
        }

        val mapWidth = mapDrawable.intrinsicWidth.toFloat().coerceAtLeast(1f)
        val mapHeight = mapDrawable.intrinsicHeight.toFloat().coerceAtLeast(1f)

        val scale = minOf(overlay.width / mapWidth, overlay.height / mapHeight)
        val drawWidth = mapWidth * scale
        val drawHeight = mapHeight * scale
        val offsetX = (overlay.width - drawWidth) / 2f
        val offsetY = (overlay.height - drawHeight) / 2f

        points.forEach { filteredPoint ->
            val px = offsetX + (filteredPoint.x * scale)
            val py = offsetY + (filteredPoint.y * scale)

            paint.color = RssiColorUtil.colorForRssi(filteredPoint.network.rssi)
            canvas.drawCircle(px, py, 6f, paint)

            paint.color = Color.BLACK
            val network = filteredPoint.network
            val fullSet = "${network.ssid}/${network.bssid}/ch${network.channel}/${network.rssi}dBm/${network.encryption}"
            canvas.drawText("${network.rssi} dBm ($fullSet)", px + 8f, py - 8f, paint)
        }

        overlay.setImageDrawable(BitmapDrawable(resources, bitmap))
    }
}
