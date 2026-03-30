package com.jinstein.thousandkm

import java.util.UUID

data class ChallengeEntry(
    val id: String = UUID.randomUUID().toString(),
    val dateMillis: Long = System.currentTimeMillis(),
    val value: Double
)

data class Challenge(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val goal: Double,
    val unit: String,
    val emoji: String,
    val entries: List<ChallengeEntry> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis()
)
