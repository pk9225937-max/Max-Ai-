package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY timestamp ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY timestamp DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("DELETE FROM reminders")
    suspend fun clearAllReminders()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_items ORDER BY timestamp DESC")
    fun getAllMemory(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory_items ORDER BY timestamp DESC")
    suspend fun getAllMemoryList(): List<MemoryEntity>

    @Query("SELECT * FROM memory_items WHERE topic LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    suspend fun searchMemory(query: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(item: MemoryEntity): Long

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM memory_items WHERE topic = :topic")
    suspend fun deleteMemoryByTopic(topic: String)

    @Query("DELETE FROM memory_items")
    suspend fun clearAllMemory()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM assistant_audit_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("DELETE FROM assistant_audit_logs")
    suspend fun clearAllLogs()
}
