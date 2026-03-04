package com.jinstein.thousandkm

import java.util.UUID

data class WalkEntry(
    val id: String = UUID.randomUUID().toString(),
    val dateMillis: Long = System.currentTimeMillis(),
    val distance: Double
)
