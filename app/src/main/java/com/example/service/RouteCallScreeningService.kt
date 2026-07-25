package com.example.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.BuildConfig
import com.example.data.CallMessage
import com.example.data.CallSession
import com.example.data.RouteDatabase
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class RouteCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown"
        Log.d("RouteCallScreening", "Incoming call from: $phoneNumber")

        // Intercept and screen the call
        val response = CallResponse.Builder()
            .setSilenceCall(true)
            .setSkipNotification(true)
            .build()
        respondToCall(callDetails, response)

        startAiInteraction(phoneNumber)
    }

    private fun startAiInteraction(phoneNumber: String) {
        serviceScope.launch {
            val db = RouteDatabase.getDatabase(applicationContext)
            val dao = db.callDao()

            val sessionId = UUID.randomUUID().toString()
            dao.insertSession(CallSession(id = sessionId, callerNumber = phoneNumber, status = "active"))

            // AI Greets
            dao.insertMessage(
                CallMessage(
                    sessionId = sessionId,
                    speaker = "AI",
                    text = "Hello, you've reached Route Agent. Who is calling and what is the nature of your call?"
                )
            )

            // Simulate Caller speaking
            delay(3000)
            val callerResponse = "Hi, this is Alice. I'm calling about the recent delivery that was supposed to arrive today."
            dao.insertMessage(
                CallMessage(
                    sessionId = sessionId,
                    speaker = "Caller",
                    text = callerResponse
                )
            )

            // Gemini processes and responds
            val aiResponseText = try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val request = GenerateContentRequest(
                    systemInstruction = Content(parts = listOf(Part("You are an AI assistant screening a phone call. Keep it very brief and professional."))),
                    contents = listOf(
                        Content(role = "user", parts = listOf(Part(callerResponse)))
                    )
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "I understand. I will pass the message along."
            } catch (e: Exception) {
                "I'm sorry, I'm having trouble processing that right now. I will notify the owner."
            }

            delay(2000)
            dao.insertMessage(
                CallMessage(
                    sessionId = sessionId,
                    speaker = "AI",
                    text = aiResponseText
                )
            )

            delay(3000)
            dao.updateSessionStatus(sessionId, "completed")
        }
    }
}
