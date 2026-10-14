package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlists ORDER BY isDefault DESC, createdAt ASC")
    fun getAllWatchlists(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlists WHERE id = :id")
    suspend fun getWatchlistById(id: Long): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(watchlist: WatchlistEntity): Long

    @Update
    suspend fun updateWatchlist(watchlist: WatchlistEntity)

    @Delete
    suspend fun deleteWatchlist(watchlist: WatchlistEntity)

    @Query("SELECT COUNT(*) FROM watchlists")
    suspend fun getCount(): Int
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM stock_alerts ORDER BY isTriggered ASC, createdAt DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM stock_alerts WHERE isTriggered = 0")
    fun getActiveAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM stock_alerts WHERE isTriggered = 1 ORDER BY triggeredAt DESC")
    fun getTriggeredAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM stock_alerts WHERE isTriggered = 0")
    suspend fun getActiveAlertsList(): List<AlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Query("UPDATE stock_alerts SET isTriggered = 1, triggeredAt = :triggeredAt WHERE id = :id")
    suspend fun markAlertTriggered(id: Long, triggeredAt: Long)

    @Delete
    suspend fun deleteAlert(alert: AlertEntity)

    @Query("DELETE FROM stock_alerts WHERE isTriggered = 1")
    suspend fun clearTriggeredAlerts()
}
