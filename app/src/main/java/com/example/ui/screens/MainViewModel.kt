package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallMessage
import com.example.data.CallRepository
import com.example.data.CallSession
import com.example.data.RouteDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = RouteDatabase.getDatabase(application).callDao()
    private val repository = CallRepository(dao)

    val activeSession: StateFlow<CallSession?> = repository.getActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<CallMessage>> = activeSession.flatMapLatest { session ->
        if (session != null) {
            repository.getMessagesForSession(session.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<CallSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun humanTakeover() {
        val currentSession = activeSession.value
        if (currentSession != null) {
            viewModelScope.launch {
                repository.updateSessionStatus(currentSession.id, "human_takeover")
                // Normally this would also signal telecom to un-silence or accept the call,
                // but in this simulation we just update the state.
            }
        }
    }
    
    fun simulateIncomingCall() {
        viewModelScope.launch {
            val sessionId = java.util.UUID.randomUUID().toString()
            repository.insertSession(CallSession(id = sessionId, callerNumber = "+1 555-0199", status = "active"))
            
            repository.insertMessage(
                CallMessage(sessionId = sessionId, speaker = "AI", text = "Hello, you've reached Route Agent. Who is calling and what is the nature of your call?")
            )

            kotlinx.coroutines.delay(3000)
            val currentSession = repository.getActiveSession().firstOrNull()
            if (currentSession?.status != "active") return@launch

            val callerResponse = "Hi, this is Alice. I'm calling about the recent delivery that was supposed to arrive today."
            repository.insertMessage(CallMessage(sessionId = sessionId, speaker = "Caller", text = callerResponse))

            val aiResponseText = try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                val request = com.example.network.GenerateContentRequest(
                    systemInstruction = com.example.network.Content(parts = listOf(com.example.network.Part("You are an AI assistant screening a phone call. Keep it very brief and professional."))),
                    contents = listOf(com.example.network.Content(role = "user", parts = listOf(com.example.network.Part(callerResponse))))
                )
                val response = com.example.network.RetrofitClient.service.generateContent(apiKey, request)
                response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "I understand. I will pass the message along."
            } catch (e: Exception) {
                "I'm sorry, I'm having trouble processing that right now. I will notify the owner."
            }

            kotlinx.coroutines.delay(2000)
            val currentSession2 = repository.getActiveSession().firstOrNull()
            if (currentSession2?.status != "active") return@launch

            repository.insertMessage(CallMessage(sessionId = sessionId, speaker = "AI", text = aiResponseText))

            kotlinx.coroutines.delay(3000)
            val finalSession = repository.getActiveSession().firstOrNull()
            if (finalSession?.status == "active") {
                repository.updateSessionStatus(sessionId, "completed")
            }
        }
    }
}
