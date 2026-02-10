package com.example.wifi_mapshow.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import java.io.InputStreamReader

class JsonRepository(private val context: Context) {

    fun loadProject(jsonUri: Uri): ProjectRoot {
        val inputStream = context.contentResolver.openInputStream(jsonUri)
            ?: error("Cannot open JSON")

        inputStream.use { stream ->
            InputStreamReader(stream).use { reader ->
                return Gson().fromJson(reader, ProjectRoot::class.java)
            }
        }
    }
}
