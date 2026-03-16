package com.jinstein.thousandkm

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WalkViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("walk_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val goalDistance = 1000.0

    private val _entries = MutableStateFlow<List<WalkEntry>>(emptyList())
    val entries: StateFlow<List<WalkEntry>> = _entries.asStateFlow()

    private val _inputKmText = MutableStateFlow("")
    val inputKmText: StateFlow<String> = _inputKmText.asStateFlow()

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    val totalDistance: Double
        get() = _entries.value.sumOf { it.distance }

    val remainingDistance: Double
        get() = maxOf(0.0, goalDistance - totalDistance)

    val progress: Float
        get() = (totalDistance / goalDistance).coerceIn(0.0, 1.0).toFloat()

    val goalReached: Boolean
        get() = totalDistance >= goalDistance

    init {
        loadEntries()
    }

    fun updateInput(text: String) {
        _inputKmText.value = text
    }

    fun updateSelectedDate(millis: Long) {
        _selectedDateMillis.value = millis
    }

    fun addEntry() {
        val normalized = _inputKmText.value.replace(",", ".")
        val distance = normalized.toDoubleOrNull() ?: return
        if (distance <= 0 || distance > 500) return

        val newEntry = WalkEntry(dateMillis = _selectedDateMillis.value, distance = distance)
        _entries.value = (_entries.value + newEntry).sortedByDescending { it.dateMillis }
        _inputKmText.value = ""
        _selectedDateMillis.value = System.currentTimeMillis()
        saveEntries()
    }

    fun deleteEntry(id: String) {
        _entries.value = _entries.value.filter { it.id != id }
        saveEntries()
    }

    fun resetChallenge() {
        archiveCurrentEntries()
        _entries.value = emptyList()
        saveEntries()
    }

    private fun loadEntries() {
        val json = prefs.getString("walkEntries_current", null) ?: return
        val type = object : TypeToken<List<WalkEntry>>() {}.type
        _entries.value = gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveEntries() {
        val json = gson.toJson(_entries.value)
        prefs.edit().putString("walkEntries_current", json).apply()
    }

    private fun archiveCurrentEntries() {
        if (_entries.value.isEmpty()) return
        val timestamp = System.currentTimeMillis()
        val json = gson.toJson(_entries.value)
        prefs.edit().putString("walkEntries_archive_$timestamp", json).apply()
    }
}
