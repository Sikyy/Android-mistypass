package com.mistyislet.app.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mistyislet.app.data.dao.AccessLogDao
import com.mistyislet.app.data.dao.CachedAccessLog
import com.mistyislet.app.data.dao.CachedCredential
import com.mistyislet.app.data.dao.CachedDoor
import com.mistyislet.app.data.dao.CredentialDao
import com.mistyislet.app.data.dao.DoorDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [CachedDoor::class, CachedCredential::class, CachedAccessLog::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun doorDao(): DoorDao
    abstract fun credentialDao(): CredentialDao
    abstract fun accessLogDao(): AccessLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mistyislet_db",
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mistyislet_db",
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideDoorDao(db: AppDatabase): DoorDao = db.doorDao()

    @Provides
    fun provideCredentialDao(db: AppDatabase): CredentialDao = db.credentialDao()

    @Provides
    fun provideAccessLogDao(db: AppDatabase): AccessLogDao = db.accessLogDao()
}
