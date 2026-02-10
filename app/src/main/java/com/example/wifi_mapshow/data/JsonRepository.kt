package com.example.wifi_mapshow.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import java.io.InputStreamReader
import com.google.gson.annotations.SerializedName

class JsonRepository(private val context: Context) {

    fun loadProject(jsonUri: Uri): ProjectRoot {
        val inputStream = context.contentResolver.openInputStream(jsonUri)
            ?: error("Cannot open JSON")

        val reader = InputStreamReader(inputStream)
        return Gson().fromJson(reader, ProjectRoot::class.java)
    }
}
