package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AlertCondition

@Entity(tableName = "watchlists")
data class WatchlistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val symbolsCsv: String, // Comma separated symbols e.g. "RELIANCE,TCS,INFY"
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val companyName: String,
    val targetPrice: Double,
    val condition: String, // AlertCondition name
    val note: String = "",
    val isTriggered: Boolean = false,
    val triggeredAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
