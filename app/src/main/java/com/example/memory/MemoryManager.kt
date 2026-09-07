package com.example.memory

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.MemoryEntity
import com.example.security.SecurityManager
import kotlinx.coroutines.flow.Flow

class MemoryManager(context: Context) {

    private val memoryDao by lazy {
        AppDatabase.getDatabase(context).memoryDao()
    }

    suspend fun saveMemory(topic: String, content: String): Pair<Boolean, String> {
        if (SecurityManager.containsSensitiveData(topic) || SecurityManager.containsSensitiveData(content)) {
            return Pair(false, "Security policy violation: Passwords, OTPs, or financial tokens cannot be saved in memory.")
        }

        val entity = MemoryEntity(
            topic = topic.trim(),
            content = content.trim(),
            timestamp = System.currentTimeMillis()
        )
        val id = memoryDao.insertMemory(entity)
        return Pair(true, "Yaad rakh liya! (Topic: $topic)")
    }

    suspend fun forgetMemory(query: String): Pair<Boolean, String> {
        val matches = memoryDao.searchMemory(query)
        if (matches.isEmpty()) {
            return Pair(false, "Aisi koi baat memory me nahi mili.")
        }
        for (item in matches) {
            memoryDao.deleteMemoryById(item.id)
        }
        return Pair(true, "${matches.size} items memory se delete kar diye.")
    }

    suspend fun searchMemory(query: String): List<MemoryEntity> {
        return memoryDao.searchMemory(query)
    }

    suspend fun getAllMemoryList(): List<MemoryEntity> {
        return memoryDao.getAllMemoryList()
    }

    fun getAllMemory(): Flow<List<MemoryEntity>> {
        return memoryDao.getAllMemory()
    }

    suspend fun deleteMemoryById(id: Long) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun clearAllMemory() {
        memoryDao.clearAllMemory()
    }
}
