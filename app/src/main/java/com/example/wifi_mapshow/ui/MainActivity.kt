package com.example.wifi_mapshow.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.wifi_mapshow.R
import com.example.wifi_mapshow.data.JsonRepository
import com.example.wifi_mapshow.util.RssiColorUtil
import kotlin.math.max

class MainActivity : ComponentActivity() {

    private companion object {
        const val TEMPLATE_MAX_X = 1000f
        const val TEMPLATE_MAX_Y = 1700f
        const val DOT_SIZE_PX = 30f
        const val DOT_RADIUS_PX = DOT_SIZE_PX / 2f
    }

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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        applySystemInsets()

        mapImage = findViewById(R.id.mapImage)
        overlay = findViewById(R.id.overlay)
        channelSpinner = findViewById(R.id.channelSpinner)
        networkSpinner = findViewById(R.id.networkSpinner)

        setupSpinners()
        setupActions()
    }

    private fun applySystemInsets() {
        val root = findViewById<android.view.View>(R.id.root)
        val controlPanel = findViewById<android.view.View>(R.id.controlPanel)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val displayCutoutAndBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            root.setPadding(
                displayCutoutAndBars.left,
                displayCutoutAndBars.top,
                displayCutoutAndBars.right,
                0
            )

            val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            controlPanel.setPadding(
                controlPanel.paddingLeft,
                controlPanel.paddingTop,
                controlPanel.paddingRight,
                bottomInsets.bottom + resources.getDimensionPixelSize(R.dimen.control_panel_bottom_padding)
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
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

        val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            strokeWidth = 2f
        }
        val labelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 0, 0, 0)
            style = Paint.Style.FILL
        }

        val mapWidth = mapDrawable.intrinsicWidth.toFloat().coerceAtLeast(1f)
        val mapHeight = mapDrawable.intrinsicHeight.toFloat().coerceAtLeast(1f)

        // Рассчитываем область фактической отрисовки карты в fitCenter
        val mapScale = minOf(overlay.width / mapWidth, overlay.height / mapHeight)
        val drawWidth = mapWidth * mapScale
        val drawHeight = mapHeight * mapScale
        val offsetX = (overlay.width - drawWidth) / 2f
        val offsetY = (overlay.height - drawHeight) / 2f

        // Нормализуем координаты по размерности шаблона источника (примерно 1000 x 1700)
        val xScale = drawWidth / TEMPLATE_MAX_X
        val yScale = drawHeight / TEMPLATE_MAX_Y

        points.forEach { filteredPoint ->
            val px = offsetX + (filteredPoint.x * xScale)
            val py = offsetY + (filteredPoint.y * yScale)

            pointPaint.color = RssiColorUtil.colorForRssi(filteredPoint.network.rssi)
            canvas.drawCircle(px, py, DOT_RADIUS_PX, pointPaint)

            val network = filteredPoint.network
            val label = "${network.rssi} dBm (${network.ssid}/${network.bssid}/ch${network.channel}/${network.rssi}dBm/${network.encryption})"

            drawLabel(
                canvas = canvas,
                textPaint = textPaint,
                backgroundPaint = labelBackgroundPaint,
                anchorX = px,
                anchorY = py,
                label = label
            )
        }

        overlay.setImageDrawable(BitmapDrawable(resources, bitmap))
    }

    private fun drawLabel(
        canvas: Canvas,
        textPaint: Paint,
        backgroundPaint: Paint,
        anchorX: Float,
        anchorY: Float,
        label: String
    ) {
        val textPaddingX = 10f
        val textPaddingY = 6f
        val sideGap = DOT_RADIUS_PX + 6f
        val lineHeight = textPaint.fontSpacing

        val maxTextWidth = max(overlay.width * 0.5f, 180f)
        val lines = wrapText(label, textPaint, maxTextWidth)
        val maxLineWidth = lines.maxOfOrNull { textPaint.measureText(it) } ?: 0f

        val alignLeftSide = anchorX <= overlay.width * (2f / 3f)
        val desiredTextX = if (alignLeftSide) {
            anchorX - sideGap - maxLineWidth
        } else {
            anchorX + sideGap
        }

        val textX = desiredTextX.coerceIn(
            textPaddingX,
            overlay.width - maxLineWidth - textPaddingX
        )

        val baselineTop = (anchorY - DOT_RADIUS_PX).coerceIn(
            lineHeight,
            overlay.height - (lineHeight * (lines.size - 1)) - textPaddingY
        )

        val bgRect = RectF(
            textX - textPaddingX,
            baselineTop - lineHeight - textPaddingY,
            textX + maxLineWidth + textPaddingX,
            baselineTop + ((lines.size - 1) * lineHeight) + textPaddingY
        )
        canvas.drawRoundRect(bgRect, 8f, 8f, backgroundPaint)

        lines.forEachIndexed { index, line ->
            val baselineY = baselineTop + (index * lineHeight)
            canvas.drawText(line, textX, baselineY, textPaint)
        }
    }

    private fun wrapText(text: String, textPaint: Paint, maxWidth: Float): List<String> {
        if (textPaint.measureText(text) <= maxWidth) return listOf(text)

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()

        words.forEach { word ->
            val candidate = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (textPaint.measureText(candidate) <= maxWidth) {
                currentLine.clear()
                currentLine.append(candidate)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine.clear()
                }

                if (textPaint.measureText(word) <= maxWidth) {
                    currentLine.append(word)
                } else {
                    lines.addAll(breakLongWord(word, textPaint, maxWidth))
                }
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return lines
    }

    private fun breakLongWord(word: String, textPaint: Paint, maxWidth: Float): List<String> {
        val chunks = mutableListOf<String>()
        val current = StringBuilder()

        word.forEach { char ->
            val candidate = current.toString() + char
            if (textPaint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current.append(char)
            } else {
                chunks.add(current.toString())
                current.clear()
                current.append(char)
            }
        }

        if (current.isNotEmpty()) {
            chunks.add(current.toString())
        }

        return chunks
    }
}
