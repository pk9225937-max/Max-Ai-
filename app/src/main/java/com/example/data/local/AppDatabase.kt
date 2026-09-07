package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ReminderEntity::class,
        MemoryEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun memoryDao(): MemoryDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "max_assistant_db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
