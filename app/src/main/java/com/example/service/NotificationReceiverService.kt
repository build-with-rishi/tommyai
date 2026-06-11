package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ai.AiClassifier
import com.example.ai.ClassificationResult
import com.example.data.AppDatabase
import com.example.data.AppSettings
import com.example.data.CapturedNotification
import com.example.data.RegexRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationReceiverService : NotificationListenerService() {
    private val TAG = "NotificationReceiverSvc"
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onCreate() {
        super.onCreate()
        startDigestCheckerLoop()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun startDigestCheckerLoop() {
        scope.launch {
            while (true) {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val settings = db.notificationDao().getSettings() ?: AppSettings()
                    
                    if (settings.hourlyDigestEnabled) {
                        val intervalMs = when (settings.hourlyDigestInterval) {
                            "30min" -> 30 * 60 * 1000L
                            "1hr" -> 60 * 60 * 1000L
                            "2hr" -> 120 * 60 * 1000L
                            else -> 60 * 60 * 1000L
                        }

                        val now = System.currentTimeMillis()
                        if (settings.lastDigestTime == 0L) {
                            db.notificationDao().saveSettings(settings.copy(lastDigestTime = now))
                        } else if (now - settings.lastDigestTime >= intervalMs) {
                            // Time to fire digest!
                            generateHourlyDigest(db, settings, now)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in digest checker loop", e)
                }
                // Check every 15 seconds
                kotlinx.coroutines.delay(15000)
            }
        }
    }

    private suspend fun generateHourlyDigest(db: AppDatabase, settings: AppSettings, timestamp: Long) {
        val missed = db.notificationDao().getUndigestedMutedNotifications(settings.lastDigestTime)
        
        // Always advance the timestamp window
        db.notificationDao().saveSettings(settings.copy(lastDigestTime = timestamp))

        if (missed.isEmpty()) {
            Log.d(TAG, "No snoozed alerts to compile details into Hourly Digest.")
            return
        }

        val totalCount = missed.sumOf { it.messageCount }
        val appsBreakdown = missed.groupBy { it.appName }
            .map { (appName, list) -> "$appName (${list.sumOf { it.messageCount }})" }
            .joinToString(" · ")

        val notificationLog = missed.take(15).joinToString("\n") {
            "• ${it.appName} (${it.senderName}): ${it.body}"
        }

        val prompt = "Missed Snoozed Alerts:\n$notificationLog\n\nCompile a beautifully structured summary briefing of these notifications in 2 sentences max. Focus on summarizing common themes or messages. No markdown."
        
        // Prepare temporary AppSettings for summary compilation
        val promptSettings = settings.copy(
            systemPrompt = "You are Tommy hourly briefing compiler. Synthesize multiple messages cleanly."
        )

        val summaryResult = try {
            AiClassifier.classify("Tommy Digest Core", "Digest Builder", prompt, promptSettings)
        } catch (e: Exception) {
            ClassificationResult("SILENT", "You have pending notifications on your dashboard from $appsBreakdown.")
        }

        // Insert Digest entity into Database so it lists on the UI Dashboard
        val digestEntity = CapturedNotification(
            packageName = "com.aistudio.tommy.digest",
            appName = "Tommy Hourly Digest",
            title = "You missed $totalCount notifications",
            body = summaryResult.reason,
            timestamp = timestamp,
            priority = "SILENT",
            reason = "Generated status briefing of background events.",
            isRead = false,
            isArchived = false,
            isDigest = true,
            digestItemsCount = totalCount,
            digestAppsBreakdown = appsBreakdown
        )
        db.notificationDao().insertNotification(digestEntity)
        SharedWidgetUtil.updateAllWidgets(applicationContext)

        // Fire physical notification on device
        fireSystemDigestNotification(totalCount, appsBreakdown, summaryResult.reason)
    }

    private fun fireSystemDigestNotification(count: Int, breakdown: String, aiSummary: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "tommy_channel_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tommy Priority Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent or Important signals prioritized by Tommy AI."
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🧠 Tommy Hourly Digest")
            .setContentText("You missed $count notifications")
            .setSubText("Hourly Snoozed Summary")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You missed $count notifications\n$breakdown\n\nAI Summary: $aiSummary")
            )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(8888, notification) // Constant ID for Hourly Digest alert
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName
        // CRITICAL INFINITE LOOP PREVENTION: Never inspect/filter notifications from Tommy itself
        if (packageName == this.packageName) {
            return
        }

        // Avoid capturing broad group summary containers (which duplicate single cards)
        val isGroupSummary = (sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary) {
            Log.d(TAG, "Skipping system group summary notification container for $packageName")
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        // Skip blank system notifications if they have details missing
        if (title.isEmpty() && text.isEmpty()) {
            return
        }

        // Get actual human-readable application label from Package Manager
        val pm = applicationContext.packageManager
        val appLabel = try {
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName
        }

        scope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val settings = db.notificationDao().getSettings() ?: AppSettings()

                // Check whitelist / blacklist settings
                val isWhitelisted = matchesRule(packageName, settings.whitelistedApps)
                val isBlacklisted = matchesRule(packageName, settings.blacklistedApps)

                var finalPriority = ""
                var finalReason = ""

                if (isWhitelisted) {
                    finalPriority = "URGENT"
                    finalReason = "Bypassed filtering: App listed in your manual Whitelist rules."
                } else if (isBlacklisted) {
                    finalPriority = "SILENT"
                    finalReason = "Auto-muted filtering: App listed in your manual Blacklist rules."
                } else {
                    // Let the AI Engine determine status
                    val classification = AiClassifier.classify(appLabel, title, text, settings)
                    finalPriority = classification.priority
                    finalReason = classification.reason
                }

                // Evaluate Regex Rules
                val activeRegexRules = db.notificationDao().getRegexRulesList().filter { it.isEnabled }
                for (rule in activeRegexRules) {
                    var isMatch = false
                    val regex = try {
                        rule.pattern.toRegex(RegexOption.IGNORE_CASE)
                    } catch (e: Exception) {
                        null
                    }
                    if (regex != null) {
                        isMatch = when (rule.matchTarget.uppercase()) {
                            "TITLE" -> regex.containsMatchIn(title)
                            "BODY" -> regex.containsMatchIn(text)
                            "APP_NAME" -> regex.containsMatchIn(appLabel)
                            "ANY" -> regex.containsMatchIn(title) || regex.containsMatchIn(text) || regex.containsMatchIn(appLabel)
                            else -> false
                        }
                    }
                    if (isMatch) {
                        finalPriority = if (rule.action.uppercase() == "ALWAYS_ALERT") "URGENT" else "SILENT"
                        finalReason = "Regex override [Target: ${rule.matchTarget}, Pattern: '${rule.pattern}']: Forced to ${if (rule.action.uppercase() == "ALWAYS_ALERT") "Alert" else "Mute"}."
                        break // First matching rule wins
                    }
                }

                val numericPriority = mapPriorityToValue(finalPriority)
                val numericThreshold = mapPriorityToValue(settings.passThroughThreshold)

                // 1. Check for duplicates within a 15-second window
                val duplicate = db.notificationDao().findDuplicate(packageName, title, sbn.postTime)
                if (duplicate != null) {
                    val updated = duplicate.copy(
                        messageCount = duplicate.messageCount + 1,
                        timestamp = sbn.postTime // Update to latest timestamp inside window
                    )
                    db.notificationDao().updateNotification(updated)
                    SharedWidgetUtil.updateAllWidgets(applicationContext)
                    if (numericPriority < numericThreshold) {
                        cancelNotification(sbn.key) // Suppress duplicate background noises silently
                        Log.d(TAG, "Deduplicated and cancelled noise from $appLabel ($title)")
                    } else {
                        Log.d(TAG, "Deduplicated and passed naturally from $appLabel ($title)")
                    }
                    return@launch
                }

                // 2. Persist the intercepted/passed notification to database
                val captured = CapturedNotification(
                    packageName = packageName,
                    appName = appLabel,
                    title = title,
                    body = text,
                    timestamp = sbn.postTime,
                    priority = finalPriority,
                    reason = finalReason,
                    isRead = false,
                    isArchived = false,
                    senderName = title,
                    messageCount = 1
                )
                db.notificationDao().insertNotification(captured)
                SharedWidgetUtil.updateAllWidgets(applicationContext)

                // 3. Control sound and cancel action depending on priority threshold
                if (numericPriority >= numericThreshold) {
                    // This is a PRIORITY notification!
                    // Buzz naturally (DO NOT cancel/silence), and DO NOT post custom Tommy notification (no duplicates!)
                    Log.d(TAG, "Pass-through priority notification: $appLabel ($finalPriority). Buzzing naturally, saved to DB.")
                } else {
                    // This is a MUTED background notification!
                    // Silence it immediately!
                    cancelNotification(sbn.key)
                    Log.d(TAG, "Muted & Cancelled noise from: $appLabel ($finalPriority)")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error trying to parse and handle notification", e)
            }
        }
    }

    private fun matchesRule(pkg: String, rulesCsv: String): Boolean {
        if (rulesCsv.trim().isEmpty()) return false
        val lowercasePkg = pkg.lowercase()
        return rulesCsv.split(",").any {
            val trimmed = it.trim().lowercase()
            trimmed.isNotEmpty() && lowercasePkg.contains(trimmed)
        }
    }

    private fun mapPriorityToValue(priority: String): Int {
        return when (priority.uppercase()) {
            "SILENT" -> 0
            "LOW" -> 1
            "IMPORTANT" -> 2
            "URGENT" -> 3
            else -> 1
        }
    }

    private fun reTriggerPromotedAlert(
        originalAppName: String,
        title: String,
        body: String,
        priority: String,
        reason: String
    ) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "tommy_channel_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tommy Priority Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent or Important signals prioritized by Tommy AI."
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }

        val designIndicator = when (priority) {
            "URGENT" -> "🚨 [URGENT]"
            "IMPORTANT" -> "✨ [IMPORTANT]"
            else -> "🔔 [PROMOTED]"
        }

        val cleanTitle = title.ifEmpty { "New Notification" }
        val bodyLabel = if (body.isNotEmpty() && body != title) "\n\n💬 MESSAGE:\n$body" else ""
        
        val bigTextLayout = "📩 FROM SENDER:\n$cleanTitle$bodyLabel\n\n" +
                            "🧠 TOMMY'S AI ANALYSIS:\n" +
                            "• App: $originalAppName ($priority)\n" +
                            "• Intel: $reason"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("$designIndicator $originalAppName")
            .setContentText(cleanTitle)
            .setSubText("Tommy AI Filter")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextLayout))
            .setSmallIcon(applicationContext.applicationInfo.icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }
}
