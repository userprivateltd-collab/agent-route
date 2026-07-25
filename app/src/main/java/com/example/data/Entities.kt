package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(tableName = "call_sessions")
data class CallSession(
    @PrimaryKey val id: String,
    val callerNumber: String,
    val startTime: Long = System.currentTimeMillis(),
    val status: String // "active", "completed", "human_takeover"
)

@Entity(
    tableName = "call_messages",
    foreignKeys = [
        ForeignKey(
            entity = CallSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["sessionId"])]
)
data class CallMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: String,
    val speaker: String, // "AI" or "Caller"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
