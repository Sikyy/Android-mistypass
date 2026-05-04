package com.mistyislet.app.data.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "access_logs")
data class CachedAccessLog(
    @PrimaryKey val id: String,
    val doorName: String?,
    val eventType: String,
    val result: String,
    val credentialType: String?,
    val actor: String?,
    val timestamp: String,
)

@Dao
interface AccessLogDao {
    @Query("SELECT * FROM access_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<CachedAccessLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<CachedAccessLog>)

    @Query("DELETE FROM access_logs")
    suspend fun deleteAll()
}
