package com.example.data

import kotlinx.coroutines.flow.Flow

class CallRepository(private val callDao: CallDao) {
    val allSessions: Flow<List<CallSession>> = callDao.getAllSessions()
    
    fun getMessagesForSession(sessionId: String): Flow<List<CallMessage>> {
        return callDao.getMessagesForSession(sessionId)
    }
    
    fun getActiveSession(): Flow<CallSession?> {
        return callDao.getActiveSession()
    }

    suspend fun insertSession(session: CallSession) {
        callDao.insertSession(session)
    }

    suspend fun insertMessage(message: CallMessage) {
        callDao.insertMessage(message)
    }
    
    suspend fun updateSessionStatus(sessionId: String, status: String) {
        callDao.updateSessionStatus(sessionId, status)
    }
}
