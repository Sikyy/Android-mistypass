package com.mistyislet.app.data.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "accessible_doors")
data class CachedDoor(
    @PrimaryKey val id: String,
    val name: String,
    val buildingId: String,
    val areaId: String?,
    val status: String,
    val gatewayStatus: String,
    val groupName: String?,
    val canUnlock: Boolean,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface DoorDao {
    @Query("SELECT * FROM accessible_doors")
    fun getAll(): Flow<List<CachedDoor>>

    @Query("SELECT * FROM accessible_doors WHERE canUnlock = 1 LIMIT 3")
    suspend fun getTopDoors(): List<CachedDoor>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(doors: List<CachedDoor>)

    @Query("DELETE FROM accessible_doors")
    suspend fun deleteAll()
}
