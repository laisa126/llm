package com.laiserdev.localllm.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiserdev.localllm.data.repository.ChatSession
import com.laiserdev.localllm.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistorySheet(
    sessions: List<ChatSession>,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BgBorder) }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Chat History",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text("${sessions.size} sessions", color = TextMuted, fontSize = 12.sp)
        }

        if (sessions.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(40.dp), tint = TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Text("No past sessions yet", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
                items(sessions, key = { it.fileName }) { session ->
                    SessionRow(
                        session = session,
                        onLoad = { onLoad(session.fileName); onDismiss() },
                        onDelete = { onDelete(session.fileName) }
                    )
                    HorizontalDivider(color = BgBorder, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: ChatSession,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val dateStr = remember(session.lastTimestamp) {
        fmt.format(Date(session.lastTimestamp))
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onLoad() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(BgElevated, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ChatBubble, null, Modifier.size(16.dp), tint = AccentGreen)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                session.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "$dateStr · ${session.messageCount} messages",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
        IconButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.DeleteOutline, null, Modifier.size(16.dp), tint = TextMuted)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = BgSurface,
            title = { Text("Delete session?", color = TextPrimary) },
            text = { Text("This cannot be undone.", color = TextSecond, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecond)
                }
            }
        )
    }
}
