package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_notifications")
data class CapturedNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val priority: String, // URGENT, IMPORTANT, LOW, SILENT
    val reason: String = "",
    val isRead: Boolean = false,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false, // Adding support for Pin
    val senderName: String = "",
    val messageCount: Int = 1,
    val isDigest: Boolean = false,
    val digestItemsCount: Int = 0,
    val digestAppsBreakdown: String = ""
)
