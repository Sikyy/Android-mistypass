package com.mistyislet.app.data.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "credentials")
data class CachedCredential(
    @PrimaryKey val id: String,
    val credentialKind: String,
    val provider: String?,
    val status: String,
    val saveLink: String?,
    val cardNumber: String?,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface CredentialDao {
    @Query("SELECT * FROM credentials")
    fun getAll(): Flow<List<CachedCredential>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(credentials: List<CachedCredential>)

    @Query("DELETE FROM credentials")
    suspend fun deleteAll()
}
