package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assistant_tasks")
data class AssistantTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val actionType: String, // CALL, ALARM, REMINDER, TODO
    val actionData: String, // e.g. Phone number, or Hour:Minute (07:30)
    val timestamp: Long,
    val isCompleted: Boolean = false,
    val isConfirmed: Boolean = false, // If user clicked "Confirm Set Alarm"
    val appNameSource: String = "",
    val contactName: String = ""
)
