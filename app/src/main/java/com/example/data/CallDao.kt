package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
    @Query("SELECT * FROM call_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<CallSession>>

    @Query("SELECT * FROM call_sessions WHERE id = :sessionId")
    fun getSession(sessionId: String): Flow<CallSession?>

    @Query("SELECT * FROM call_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<CallMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CallSession)

    @Query("UPDATE call_sessions SET status = :status WHERE id = :sessionId")
    suspend fun updateSessionStatus(sessionId: String, status: String)

    @Insert
    suspend fun insertMessage(message: CallMessage)
    
    @Query("SELECT * FROM call_sessions WHERE status = 'active' LIMIT 1")
    fun getActiveSession(): Flow<CallSession?>
}
