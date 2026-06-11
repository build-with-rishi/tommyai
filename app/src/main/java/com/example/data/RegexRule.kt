package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "regex_rules")
data class RegexRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val matchTarget: String, // TITLE, BODY, APP_NAME, ANY
    val action: String, // ALWAYS_ALERT, ALWAYS_MUTE
    val isEnabled: Boolean = true
)
