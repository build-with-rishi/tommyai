package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.ai.AiClassifier
import com.example.data.AppDatabase
import com.example.data.CapturedNotification
import com.example.data.AssistantTask
import com.example.data.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SharedWidgetUtil {
    companion object {
        private const val TAG = "SharedWidgetUtil"

        fun getPrefs(context: Context): SharedPreferences {
            return context.applicationContext.getSharedPreferences("tommy_widget_prefs", Context.MODE_PRIVATE)
        }

        fun updateAllWidgets(context: Context) {
            Log.d(TAG, "Requesting Widget Update Broadcast for all active providers")
            val manager = AppWidgetManager.getInstance(context)
            
            // 1. Update AiDigestWidget
            val aiDigestIds = manager.getAppWidgetIds(ComponentName(context, AiDigestWidget::class.java))
            if (aiDigestIds.isNotEmpty()) {
                val intent = Intent(context, AiDigestWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, aiDigestIds)
                }
                context.sendBroadcast(intent)
            }

            // 2. Update ActionableRegistryWidget
            val actionableIds = manager.getAppWidgetIds(ComponentName(context, ActionableRegistryWidget::class.java))
            if (actionableIds.isNotEmpty()) {
                val intent = Intent(context, ActionableRegistryWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, actionableIds)
                }
                context.sendBroadcast(intent)
            }

            // 3. Update StreamStatsWidget
            val statsIds = manager.getAppWidgetIds(ComponentName(context, StreamStatsWidget::class.java))
            if (statsIds.isNotEmpty()) {
                val intent = Intent(context, StreamStatsWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, statsIds)
                }
                context.sendBroadcast(intent)
            }

            // 4. Update UnifiedWidget
            val unifiedIds = manager.getAppWidgetIds(ComponentName(context, UnifiedWidget::class.java))
            if (unifiedIds.isNotEmpty()) {
                val intent = Intent(context, UnifiedWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, unifiedIds)
                }
                context.sendBroadcast(intent)
            }
        }

        fun triggerBackgroundAudit(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Scanning streams for action items...", Toast.LENGTH_SHORT).show()
                    }

                    val db = AppDatabase.getDatabase(context)
                    val dao = db.notificationDao()
                    val activeSettings = dao.getSettings() ?: AppSettings()
                    val allNotify = dao.getAllNotificationsList().filter { !it.isArchived }

                    val intervalMs = activeSettings.taskScanIntervalHours * 60 * 60 * 1000L
                    val cutoffTime = System.currentTimeMillis() - intervalMs
                    var targets = allNotify.filter { it.timestamp >= cutoffTime }
                    if (targets.size < 5) {
                        targets = allNotify.take(20)
                    }

                    if (targets.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "No captured notifications to audit. Please inject mock notifications first!", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    val notificationsJson = org.json.JSONArray().apply {
                        for (n in targets) {
                            put(org.json.JSONObject().apply {
                                put("appName", n.appName)
                                put("title", n.title)
                                put("body", n.body)
                                put("timestamp", n.timestamp)
                            })
                        }
                    }.toString()

                    var parsedResultString = "[]"
                    try {
                        val apiResultString = AiClassifier.extractTasksFromNotifications(notificationsJson, activeSettings)
                        parsedResultString = if (apiResultString == "[]" || apiResultString.trim().isEmpty() || apiResultString.trim() == "{}") {
                            AiClassifier.extractTasksHeuristically(targets)
                        } else {
                            apiResultString
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Gemini background extract failed, falling back to heuristics", e)
                        parsedResultString = AiClassifier.extractTasksHeuristically(targets)
                    }

                    val jsonArray = try {
                        org.json.JSONArray(parsedResultString)
                    } catch (e: Exception) {
                        org.json.JSONArray(AiClassifier.extractTasksHeuristically(targets))
                    }

                    val currentTasks = dao.getAssistantTasksList()
                    var addedCount = 0

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val title = obj.optString("title")
                        val desc = obj.optString("description")
                        val actionType = obj.optString("actionType", "TODO")
                        val actionData = obj.optString("actionData", "")
                        val appNameSource = obj.optString("appNameSource", "")
                        val contactName = obj.optString("contactName", "")

                        val exists = currentTasks.any { 
                            it.title.equals(title, ignoreCase = true) && 
                            it.actionType.equals(actionType, ignoreCase = true)
                        }

                        if (!exists && title.trim().isNotEmpty()) {
                            val task = AssistantTask(
                                title = title,
                                description = desc,
                                actionType = actionType,
                                actionData = actionData,
                                timestamp = System.currentTimeMillis(),
                                isCompleted = false,
                                isConfirmed = false,
                                appNameSource = appNameSource,
                                contactName = contactName
                            )
                            dao.insertAssistantTask(task)
                            
                            // Trigger system notification
                            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            val channelId = "Tommy Priority Notifications"
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                val channel = android.app.NotificationChannel(
                                    channelId,
                                    "Tommy Priority Notifications",
                                    NotificationManager.IMPORTANCE_HIGH
                                )
                                manager.createNotificationChannel(channel)
                            }
                            val actionLabel = when (task.actionType.uppercase()) {
                                "CALL" -> "📞 ACTION LISTED: Call Contact"
                                "ALARM" -> "⏰ ACTION LISTED: Calendar/Alarm"
                                else -> "💡 ACTION LISTED: Tasks Registry"
                            }
                            val actionDesc = "${task.title} (Source: ${task.appNameSource})"
                            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                                .setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle(actionLabel)
                                .setContentText(actionDesc)
                                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                                .setAutoCancel(true)
                            manager.notify((System.currentTimeMillis() % 100000).toInt() + 100, builder.build())

                            addedCount++
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (addedCount > 0) {
                            Toast.makeText(context, "Audit complete! Extracted $addedCount new actionable tasks to registry.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Audit complete! No new actionable tasks found.", Toast.LENGTH_LONG).show()
                        }
                    }

                    // Refresh all widgets with the new content
                    updateAllWidgets(context)

                } catch (e: Exception) {
                    Log.e(TAG, "Background task scanning failed", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Scanner audit error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

class AiDigestWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.example.service.ACTION_TRIGGER_AUDIT") {
            SharedWidgetUtil.triggerBackgroundAudit(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val prefs = SharedWidgetUtil.getPrefs(context)
                val latestDigest = prefs.getString("latest_ai_digest", null)
                val notifications = db.notificationDao().getAllNotificationsList().filter { !it.isArchived }
                
                val displayText = if (latestDigest != null && latestDigest.trim().isNotEmpty()) {
                    latestDigest
                } else if (notifications.isNotEmpty()) {
                    val totalCount = notifications.size
                    "Currently intercepting $totalCount notification streams. Tap container to show dashboard."
                } else {
                    "Awaiting incoming notification packets. Tap background box to launch interactive workspace dashboard."
                }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_ai_digest)
                    views.setTextViewText(R.id.widget_digest_text, displayText)
                    
                    // Clicking the widget launches the MainActivity
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context, 
                        100, 
                        intent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    // Bind the refresh button trigger to broadcast audit action
                    val refreshIntent = Intent(context, AiDigestWidget::class.java).apply {
                        action = "com.example.service.ACTION_TRIGGER_AUDIT"
                    }
                    val pendingIntentRefresh = PendingIntent.getBroadcast(
                        context, 
                        150, 
                        refreshIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_refresh, pendingIntentRefresh)
                    
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e("AiDigestWidget", "Widget update failed gracefully", e)
            }
        }
    }
}

class ActionableRegistryWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.example.service.ACTION_TRIGGER_AUDIT") {
            SharedWidgetUtil.triggerBackgroundAudit(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val tasks = db.notificationDao().getAssistantTasksList().filter { !it.isCompleted }
                
                val displayText = if (tasks.isEmpty()) {
                    "No pending actions. Logs clear."
                } else {
                    tasks.mapIndexed { i, t ->
                        "${i + 1}. ${t.title} • ${t.appNameSource}"
                    }.joinToString("\n")
                }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_actionable_registry)
                    views.setTextViewText(R.id.widget_registry_text, displayText)
                    
                    // Tapping leads to workspace
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context, 
                        200, 
                        intent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    // Bind the refresh button trigger to broadcast audit action
                    val refreshIntent = Intent(context, ActionableRegistryWidget::class.java).apply {
                        action = "com.example.service.ACTION_TRIGGER_AUDIT"
                    }
                    val pendingIntentRefresh = PendingIntent.getBroadcast(
                        context, 
                        250, 
                        refreshIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_refresh, pendingIntentRefresh)
                    
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e("ActionableWidget", "Widget update failed gracefully", e)
            }
        }
    }
}

class StreamStatsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val settings = db.notificationDao().getSettings() ?: AppSettings()
                val notifications = db.notificationDao().getAllNotificationsList()

                val thresholdVal = when (settings.passThroughThreshold.uppercase()) {
                    "SILENT" -> 0
                    "LOW" -> 1
                    "IMPORTANT" -> 2
                    "URGENT" -> 3
                    else -> 2
                }

                val escapedCount = notifications.count {
                    val pVal = when (it.priority.uppercase()) {
                        "SILENT" -> 0
                        "LOW" -> 1
                        "IMPORTANT" -> 2
                        "URGENT" -> 3
                        else -> 1
                    }
                    pVal >= thresholdVal
                }

                val mutedCount = notifications.size - escapedCount

                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val todayStart = cal.timeInMillis
                val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L

                val deltaCount = notifications.count { it.timestamp >= todayStart }
                val yesterdayCount = notifications.count { it.timestamp in yesterdayStart until todayStart }
                val delta = deltaCount - yesterdayCount
                val deltaStr = if (delta > 0) "+$delta ▲" else if (delta < 0) "$delta ▼" else "0 •"
                val deltaColor = if (delta > 0) "#EA4335" else if (delta < 0) "#34A853" else "#4285F4"

                val accuracyValue = if (notifications.isEmpty()) 100 else (mutedCount * 105 / notifications.size).coerceAtMost(100)
                val accuracyRateStr = "$accuracyValue%"

                val hourCounts = IntArray(24)
                notifications.forEach {
                    val itemCal = java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    val hr = itemCal.get(java.util.Calendar.HOUR_OF_DAY)
                    if (hr in 0..23) hourCounts[hr]++
                }
                val peakHour = hourCounts.indices.maxByOrNull { hourCounts[it] } ?: 9
                val pmStr = if (peakHour >= 12) "PM" else "AM"
                val displayPeakHour = when {
                    peakHour == 0 -> 12
                    peakHour > 12 -> peakHour - 12
                    else -> peakHour
                }
                val peakHourStr = "PEAK: $displayPeakHour $pmStr"

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_stream_stats)
                    views.setTextViewText(R.id.widget_stats_escaped, "$escapedCount Escaped")
                    views.setTextViewText(R.id.widget_stats_muted, "$mutedCount Muted")
                    
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context, 
                        300, 
                        intent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e("StreamStatsWidget", "Widget update failed gracefully", e)
            }
        }
    }
}

class UnifiedWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.example.service.ACTION_TRIGGER_AUDIT") {
            SharedWidgetUtil.triggerBackgroundAudit(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                
                // 1. Fetch Telemetry Data
                val settings = db.notificationDao().getSettings() ?: AppSettings()
                val notifications = db.notificationDao().getAllNotificationsList()

                val thresholdVal = when (settings.passThroughThreshold.uppercase()) {
                    "SILENT" -> 0
                    "LOW" -> 1
                    "IMPORTANT" -> 2
                    "URGENT" -> 3
                    else -> 2
                }

                val escapedCount = notifications.count {
                    val pVal = when (it.priority.uppercase()) {
                        "SILENT" -> 0
                        "LOW" -> 1
                        "IMPORTANT" -> 2
                        "URGENT" -> 3
                        else -> 1
                    }
                    pVal >= thresholdVal
                }
                val mutedCount = notifications.size - escapedCount

                // 2. Fetch Tasks/Intents List
                val tasks = db.notificationDao().getAssistantTasksList().filter { !it.isCompleted }
                val taskText = if (tasks.isEmpty()) {
                    "No pending actions. Logs clear."
                } else {
                    tasks.mapIndexed { i, t ->
                        "${i + 1}. ${t.title} • ${t.appNameSource}"
                    }.joinToString("\n")
                }

                // 3. Update RemoteViews
                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_unified)
                    views.setTextViewText(R.id.widget_stats_escaped, "$escapedCount Escaped")
                    views.setTextViewText(R.id.widget_stats_muted, "$mutedCount Muted")
                    views.setTextViewText(R.id.widget_registry_text, taskText)

                    // Bind whole layout elements/boxes to app launch
                    val appIntent = Intent(context, MainActivity::class.java)
                    val pendingAppIntent = PendingIntent.getActivity(
                        context, 
                        400, 
                        appIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_telemetry_box, pendingAppIntent)
                    views.setOnClickPendingIntent(R.id.widget_intent_box, pendingAppIntent)

                    // Bind the refresh button trigger to broadcast audit action
                    val refreshIntent = Intent(context, UnifiedWidget::class.java).apply {
                        action = "com.example.service.ACTION_TRIGGER_AUDIT"
                    }
                    val pendingIntentRefresh = PendingIntent.getBroadcast(
                        context, 
                        450, 
                        refreshIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_refresh, pendingIntentRefresh)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e("UnifiedWidget", "Widget update failed gracefully", e)
            }
        }
    }
}



