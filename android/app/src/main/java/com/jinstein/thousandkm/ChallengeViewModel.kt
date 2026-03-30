package com.jinstein.thousandkm

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.TimeZone

class ChallengeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("challenge_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    val challenges: StateFlow<List<Challenge>> = _challenges.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    init {
        loadChallenges()
    }

    fun addChallenge(name: String, goal: Double, unit: String, emoji: String) {
        val challenge = Challenge(name = name, goal = goal, unit = unit, emoji = emoji)
        _challenges.value = _challenges.value + challenge
        saveChallenges()
    }

    fun deleteChallenge(id: String) {
        _challenges.value = _challenges.value.filter { it.id != id }
        saveChallenges()
    }

    fun addEntry(challengeId: String) {
        val normalized = _inputText.value.replace(",", ".")
        val value = normalized.toDoubleOrNull() ?: return
        if (value <= 0) return
        val entry = ChallengeEntry(value = value, dateMillis = _selectedDateMillis.value)
        _challenges.value = _challenges.value.map { c ->
            if (c.id == challengeId) c.copy(entries = listOf(entry) + c.entries)
            else c
        }
        _inputText.value = ""
        _selectedDateMillis.value = System.currentTimeMillis()
        saveChallenges()
    }

    fun deleteEntry(challengeId: String, entryId: String) {
        _challenges.value = _challenges.value.map { c ->
            if (c.id == challengeId) c.copy(entries = c.entries.filter { it.id != entryId })
            else c
        }
        saveChallenges()
    }

    fun resetChallenge(challengeId: String) {
        _challenges.value = _challenges.value.map { c ->
            if (c.id == challengeId) c.copy(entries = emptyList())
            else c
        }
        saveChallenges()
    }

    fun updateInput(text: String) {
        _inputText.value = text
    }

    fun updateSelectedDate(utcDateMillis: Long) {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcDateMillis
        }
        val localCal = Calendar.getInstance().apply {
            set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        _selectedDateMillis.value = localCal.timeInMillis
    }

    private fun loadChallenges() {
        val json = prefs.getString("challenges", null) ?: return
        val type = object : TypeToken<List<Challenge>>() {}.type
        _challenges.value = gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveChallenges() {
        val json = gson.toJson(_challenges.value)
        prefs.edit().putString("challenges", json).apply()
    }
}
