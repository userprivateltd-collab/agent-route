package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CallMessage
import com.example.data.CallSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()

    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val coroutineScope = rememberCoroutineScope()
    
    // Automatically navigate to Detail pane if there is an active session
    androidx.compose.runtime.LaunchedEffect(activeSession) {
        if (activeSession != null) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Route Agent") },
                actions = {
                    Button(onClick = { viewModel.simulateIncomingCall() }) {
                        Text("Simulate Call")
                    }
                }
            )
        }
    ) { innerPadding ->
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            listPane = {
                SessionListPane(
                    sessions = allSessions,
                    onSessionClick = { 
                        coroutineScope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail) }
                    }
                )
            },
            detailPane = {
                if (activeSession != null) {
                    ActiveCallPane(
                        session = activeSession!!,
                        messages = activeMessages,
                        onTakeover = { viewModel.humanTakeover() }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active call", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }
}

@Composable
fun SessionListPane(
    sessions: List<CallSession>,
    onSessionClick: (CallSession) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Call History", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        }
        items(sessions) { session ->
            Card(
                onClick = { onSessionClick(session) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(session.callerNumber, fontWeight = FontWeight.Bold)
                        Text(formatTime(session.startTime), style = MaterialTheme.typography.bodySmall)
                    }
                    val statusColor = when (session.status) {
                        "active" -> MaterialTheme.colorScheme.primary
                        "human_takeover" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    }
                    Text(session.status.uppercase(), color = statusColor, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ActiveCallPane(
    session: CallSession,
    messages: List<CallMessage>,
    onTakeover: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (session.status == "active") {
            PulsingIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("AI Screening Active", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(session.callerNumber, style = MaterialTheme.typography.bodyLarge)
            
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Human Takeover", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
            Text(session.callerNumber, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = 600.dp)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    Column {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
                        ) {
                            MessageBubble(message = message)
                        }
                    }
                }
            }
        }

        if (session.status == "active") {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onTakeover,
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("takeover_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Talk with Person", fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
        }
    }
}

@Composable
fun PulsingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        )
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = "AI Agent",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun MessageBubble(message: CallMessage) {
    val isAI = message.speaker == "AI"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAI) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isAI) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.speaker,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAI) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isAI) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))
}
