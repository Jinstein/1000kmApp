package com.jinstein.thousandkm

import java.util.Calendar
import java.util.UUID

data class ChallengeEntry(
    val id: String = UUID.randomUUID().toString(),
    val dateMillis: Long = System.currentTimeMillis(),
    val value: Double
)

data class Challenge(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val unit: String,
    val emoji: String,
    val photoUri: String? = null,
    // Legacy field kept for Gson migration from old data
    val goal: Double? = null,
    // New goal fields
    val finalGoal: Double? = null,
    val dailyGoal: Double? = null,
    val monthlyGoal: Double? = null,
    val yearlyGoal: Double? = null,
    val entries: List<ChallengeEntry> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun hasAnyGoal() = finalGoal != null || dailyGoal != null || monthlyGoal != null || yearlyGoal != null
}

fun Challenge.totalValue() = entries.sumOf { it.value }

fun Challenge.todayTotal(): Double {
    val today = Calendar.getInstance()
    return entries.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }.sumOf { it.value }
}

fun Challenge.thisMonthTotal(): Double {
    val today = Calendar.getInstance()
    return entries.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        cal.get(Calendar.MONTH) == today.get(Calendar.MONTH)
    }.sumOf { it.value }
}

fun Challenge.thisYearTotal(): Double {
    val today = Calendar.getInstance()
    return entries.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    }.sumOf { it.value }
}
