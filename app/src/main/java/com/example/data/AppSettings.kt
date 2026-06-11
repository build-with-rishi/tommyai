package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1, // Always 1 for single row settings
    val systemPrompt: String = "You are a notification classifier. Classify this notification into URGENT, IMPORTANT, LOW, or SILENT.\n" +
            "Rules:\n" +
            "1. Urgent notifications are from family members or are high-value financial alerts (e.g., fraud alerts, money transfers).\n" +
            "2. Important notifications are direct personal messages, work calendar reminders, or system alerts (e.g. low battery).\n" +
            "3. Low notifications are promos, news, newsletters, newsletters, sports, or updates that don't need immediate attention.\n" +
            "4. Silent notifications are logging, background syncs, or music players.",
    val backendType: String = "LOCAL_SIMULATION", // LOCAL_SIMULATION, OLLAMA_LOCAL, CUSTOM_ENDPOINT, GEMINI_API
    val customBaseUrl: String = "http://10.0.2.2:11434", // Ollama local port via emulator loopback or custom IP
    val customApiPath: String = "/api/chat", // e.g. /v1/chat/completions or /api/chat
    val customApiKey: String = "",
    val requestFormat: String = "OPENAI", // OPENAI, ANTHROPIC, OLLAMA
    val passThroughThreshold: String = "IMPORTANT", // URGENT, IMPORTANT, LOW, SILENT
    val whitelistedApps: String = "", // Comma-separated packages that always ring
    val blacklistedApps: String = "", // Comma-separated packages that always get filtered out entirely
    val hourlyDigestEnabled: Boolean = true,
    val hourlyDigestInterval: String = "1hr", // 30min, 1hr, 2hr
    val lastDigestTime: Long = 0L,
    val taskScanIntervalHours: Int = 2 // Customizable interval for assistant scan
)
