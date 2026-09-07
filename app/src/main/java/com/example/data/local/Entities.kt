package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timestamp: Long,
    val repeatRule: String = "NONE", // NONE, DAILY, WEEKLY
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_items")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topic: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "assistant_audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String,
    val details: String,
    val riskLevel: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)
