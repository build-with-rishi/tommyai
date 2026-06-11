package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiClassifier
import com.example.data.AppDatabase
import com.example.data.AppSettings
import com.example.data.CapturedNotification
import com.example.data.NotificationRepository
import com.example.data.AssistantTask
import com.example.data.RegexRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "NotificationVM"
    private val repository: NotificationRepository

    val allNotifications: StateFlow<List<CapturedNotification>>
    val settings: StateFlow<AppSettings>
    val assistantTasks: StateFlow<List<AssistantTask>>
    val regexRules: StateFlow<List<RegexRule>>

    private val _isGeneratingDigest = MutableStateFlow(false)
    val isGeneratingDigest: StateFlow<Boolean> = _isGeneratingDigest.asStateFlow()

    private val _aiDigestResult = MutableStateFlow<String?>(null)
    val aiDigestResult: StateFlow<String?> = _aiDigestResult.asStateFlow()

    private val _isScanningTasks = MutableStateFlow(false)
    val isScanningTasks: StateFlow<Boolean> = _isScanningTasks.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).notificationDao()
        repository = NotificationRepository(dao)

        allNotifications = repository.allNotifications.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        settings = repository.settingsFlow
            .map { it ?: AppSettings() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AppSettings()
            )

        assistantTasks = repository.assistantTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        regexRules = repository.regexRules.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        loadInstalledApps()

        // Auto-synchronize database triggers to Home Screen widgets in real-time
        viewModelScope.launch {
            allNotifications.collect {
                com.example.service.SharedWidgetUtil.updateAllWidgets(application)
            }
        }
        viewModelScope.launch {
            assistantTasks.collect {
                com.example.service.SharedWidgetUtil.updateAllWidgets(application)
            }
        }
        viewModelScope.launch {
            regexRules.collect {
                com.example.service.SharedWidgetUtil.updateAllWidgets(application)
            }
        }
    }

    fun saveSettings(newSettings: AppSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(newSettings)
        }
    }

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val pm = context.packageManager
                val apps = pm.getInstalledPackages(0)
                val list = apps.mapNotNull { pkg ->
                    val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                    val label = appInfo.loadLabel(pm).toString()
                    val pkgName = appInfo.packageName
                    if (pkgName.isEmpty()) return@mapNotNull null
                    val icon = try {
                        appInfo.loadIcon(pm)
                    } catch (e: Exception) {
                        null
                    }
                    InstalledAppInfo(packageName = pkgName, label = label, icon = icon)
                }.sortedBy { it.label.lowercase() }
                _installedApps.value = list
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load installed apps", e)
            }
        }
    }

    fun insertRegexRule(rule: RegexRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertRegexRule(rule)
        }
    }

    fun updateRegexRule(rule: RegexRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRegexRule(rule)
        }
    }

    fun deleteRegexRule(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRegexRule(id)
        }
    }

    fun clearAllRegexRules() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllRegexRules()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNotificationById(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllNotifications()
        }
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAsRead(id)
        }
    }

    fun setArchived(id: Long, archived: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setArchived(id, archived)
        }
    }

    fun setPinned(id: Long, pinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setPinned(id, pinned)
        }
    }

    fun promoteNotification(item: CapturedNotification) {
        viewModelScope.launch(Dispatchers.IO) {
            // Re-trigger visual and audio native device notification
            val manager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "tommy_promoted_manual"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Tommy Manual Promo Alert",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "User manually promoted priority alerts."
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }

            val designIndicator = when (item.priority) {
                "URGENT" -> "🔴 [URGENT PROMOTED]"
                "IMPORTANT" -> "🟡 [IMPORTANT PROMOTED]"
                else -> "🟢 [PROMOTED]"
            }

            val notification = NotificationCompat.Builder(getApplication(), channelId)
                .setContentTitle("$designIndicator ${item.appName}")
                .setContentText(item.title)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("${item.title}\n${item.body}\n\nOriginally Intercepted Priority: ${item.priority}\nReason: ${item.reason}")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .build()

            manager.notify(System.currentTimeMillis().toInt(), notification)
            
            // Mark as read when promoted
            repository.markAsRead(item.id)
        }
    }

    fun generateSummaryDigest() {
        val currentList = allNotifications.value.filter { !it.isArchived }
        if (currentList.isEmpty()) {
            _aiDigestResult.value = "No dynamic notifications captured yet to summarize. Inbound items will accumulate here!"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingDigest.value = true
            _aiDigestResult.value = "AI is scanning your dashboard and compiling your digest..."

            try {
                val activeSettings = settings.value
                val textToSummarize = currentList.take(25).joinToString("\n") {
                    "• [${it.priority}] ${it.appName} - ${it.title}: ${it.body}"
                }

                val prompt = "Analyze the following set of recent notification headers. Write an elegant, structured summary digest for the user. Group them by category (e.g. Urgent/Family, Work, Subscriptions/Spam). Point out if something immediately needs attention. Do not write full list replica, generalize smartly and keep it compact.\n\nCaptured Notifications:\n$textToSummarize"

                // Create a temporary AppSettings to make sure we get a descriptive markdown text instead of structural JSON
                val promptSettings = activeSettings.copy(
                    systemPrompt = "You are a personalized digest compiler. Compile notifications into beautiful sections, giving a summary overview of what came in. Highlight keys, and help the user digest everything in 30 seconds."
                )

                val summaryResult = AiClassifier.classify(
                    appName = "Tommy Summary Engine",
                    title = "Digest Generation",
                    body = prompt,
                    settings = promptSettings
                )

                val fullText = summaryResult.reason
                val words = fullText.split(" ")
                var currentString = ""
                for (index in words.indices) {
                    currentString += (if (index == 0) "" else " ") + words[index]
                    _aiDigestResult.value = currentString
                    kotlinx.coroutines.delay(70) // 70ms per word creates an extremely natural streaming pace
                }

                // Cache final synthesized digest for Home Screen Widgets
                val prefs = getApplication<Application>().getSharedPreferences("tommy_widget_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("latest_ai_digest", currentString).apply()
                com.example.service.SharedWidgetUtil.updateAllWidgets(getApplication())

            } catch (e: Exception) {
                Log.e(TAG, "Digest generation failed", e)
                _aiDigestResult.value = "Failed to generate AI Digest due to connection issues. Check your Internet connection and settings endpoint configuration: ${e.localizedMessage}"
            } finally {
                _isGeneratingDigest.value = false
            }
        }
    }

    fun simulateNotificationInjection(context: Context, appName: String, title: String, text: String) {
        // Generates a mock notification on the listener so the user can test the app instantly
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val activeSettings = settings.value

            val classification = AiClassifier.classify(appName, title, text, activeSettings)

            val captured = CapturedNotification(
                packageName = "com.sample.${appName.lowercase().replace(" ", "")}",
                appName = appName,
                title = title,
                body = text,
                timestamp = System.currentTimeMillis(),
                priority = classification.priority,
                reason = classification.reason,
                isRead = false,
                isArchived = false,
                senderName = title,
                messageCount = 1
            )
            db.notificationDao().insertNotification(captured)

            // Trigger actual system notification if it exceeds threshold
            val numericPriority = when (classification.priority) {
                "SILENT" -> 0
                "LOW" -> 1
                "IMPORTANT" -> 2
                "URGENT" -> 3
                else -> 1
            }
            val numericThreshold = when (activeSettings.passThroughThreshold) {
                "SILENT" -> 0
                "LOW" -> 1
                "IMPORTANT" -> 2
                "URGENT" -> 3
                else -> 2
            }

            if (numericPriority >= numericThreshold) {
                // Post system alert
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

                val designIndicator = when (classification.priority) {
                    "URGENT" -> "🔴 [URGENT]"
                    "IMPORTANT" -> "🟡 [IMPORTANT]"
                    else -> "🟢 [PROMOTED]"
                }

                val notification = NotificationCompat.Builder(context, channelId)
                    .setContentTitle("$designIndicator $appName")
                    .setContentText(title)
                    .setSubText("Simulation Injector")
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("$title\n$text\n\nAI classification: ${classification.priority}\nReason: ${classification.reason}")
                    )
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                manager.notify(System.currentTimeMillis().toInt(), notification)
            }
        }
    }

    fun scanForAssistantTasks() {
        if (_isScanningTasks.value) return
        _isScanningTasks.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Initial feedback
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Auditing notification streams...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

                val activeSettings = settings.value
                val allNotify = allNotifications.value.filter { !it.isArchived }
                
                val intervalMs = activeSettings.taskScanIntervalHours * 60 * 60 * 1000L
                val cutoffTime = System.currentTimeMillis() - intervalMs
                var targets = allNotify.filter { it.timestamp >= cutoffTime }
                
                if (targets.size < 5) {
                    targets = allNotify.take(20)
                }

                if (targets.isEmpty()) {
                    _isScanningTasks.value = false
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            getApplication(),
                            "No captured notifications to audit. Please inject mock notifications first!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
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

                val apiKey = if (activeSettings.customApiKey.isNotEmpty()) {
                    activeSettings.customApiKey
                } else {
                    com.example.BuildConfig.GEMINI_API_KEY
                }

                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            getApplication(),
                            "No Gemini API key - running local smart heuristics...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                var parsedResultString = "[]"
                try {
                    val apiResultString = AiClassifier.extractTasksFromNotifications(notificationsJson, activeSettings)
                    parsedResultString = if (apiResultString == "[]" || apiResultString.trim().isEmpty() || apiResultString.trim() == "{}") {
                        AiClassifier.extractTasksHeuristically(targets)
                    } else {
                        apiResultString
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gemini extract failed, falling back to heuristics", e)
                    parsedResultString = AiClassifier.extractTasksHeuristically(targets)
                }

                val jsonArray = try {
                    org.json.JSONArray(parsedResultString)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed parsing tasks JSON, falling back to heuristics", e)
                    org.json.JSONArray(AiClassifier.extractTasksHeuristically(targets))
                }

                val currentTasks = repository.getAssistantTasksList()
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
                        val task = com.example.data.AssistantTask(
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
                        repository.insertAssistantTask(task)
                        triggerAssistantNotificationAlert(task)
                        addedCount++
                    }
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (addedCount > 0) {
                        android.widget.Toast.makeText(
                            getApplication(),
                            "Audit complete! Extracted $addedCount new actionable tasks to registry.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } else {
                        android.widget.Toast.makeText(
                            getApplication(),
                            "Audit complete! No new actionable tasks found.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Task scanning failed", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Scanner audit error: ${e.localizedMessage}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                _isScanningTasks.value = false
            }
        }
    }

    private fun triggerAssistantNotificationAlert(task: com.example.data.AssistantTask) {
        val context = getApplication<Application>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channelId = "assistant_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tommy AI Tasks",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val actionDesc = when (task.actionType.uppercase()) {
            "CALL" -> "Call ${task.contactName} [${task.actionData}]"
            "ALARM" -> "Set Alarm at ${task.actionData}"
            "REMINDER" -> "Remind you: ${task.title}"
            else -> "Task: ${task.title}"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle("💡 Tommy AI Assistant Found a Task")
            .setContentText(actionDesc)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Tommy analyzed recent notifications and found an item:\n\n$actionDesc\n\nContext:\n${task.description}"))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        manager.notify((task.id + 50000).toInt(), builder.build())
    }

    fun completeTask(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setAssistantTaskCompleted(id, true)
        }
    }

    fun confirmTaskAction(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setAssistantTaskConfirmed(id, true)
        }
    }

    fun deleteAssistantTask(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAssistantTaskById(id)
        }
    }

    fun submitDirectAssistantCommand(command: String) {
        if (command.trim().isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lower = command.lowercase()
                var title = "Direct Task"
                var description = "Created via direct assistant command dialog"
                var actionType = "TODO"
                var actionData = ""
                var contactName = ""

                if (lower.contains("call")) {
                    actionType = "CALL"
                    val pNum = command.filter { it.isDigit() }
                    actionData = if (pNum.isNotEmpty()) pNum else "5550199"
                    val contactMatch = Regex("call\\s+([a-zA-Z\\s]+)", RegexOption.IGNORE_CASE).find(command)
                    val rawName = contactMatch?.groupValues?.getOrNull(1)?.trim() ?: ""
                    val namePart = rawName.split(Regex("\\s+")).firstOrNull() ?: "Contact"
                    contactName = namePart
                    title = "Call $contactName"
                    description = "Request to call $contactName at $actionData spawned from user direct command."
                } else if (lower.contains("alarm")) {
                    actionType = "ALARM"
                    val timeMatch = Regex("(\\d{1,2}):(\\d{2})").find(command)
                    if (timeMatch != null) {
                        val hr = timeMatch.groupValues[1].padStart(2, '0')
                        val min = timeMatch.groupValues[2].padStart(2, '0')
                        actionData = "$hr:$min"
                    } else {
                        actionData = "08:00"
                    }
                    title = "Set Alarm for $actionData"
                    description = "Alarm set trigger manually scheduled."
                } else if (lower.contains("remind") || lower.contains("reminder")) {
                    actionType = "REMINDER"
                    val cleaned = command.replace(Regex("(?i)remind(er)?\\s+(me\\s+to)?"), "").trim()
                    title = if (cleaned.isNotEmpty()) cleaned.replaceFirstChar { it.uppercase() } else "Reminder"
                    description = "Directly scheduled reminder task: $title"
                } else {
                    title = command.replaceFirstChar { it.uppercase() }
                    description = "Added via direct user entry command."
                }

                val task = com.example.data.AssistantTask(
                    title = title,
                    description = description,
                    actionType = actionType,
                    actionData = actionData,
                    timestamp = System.currentTimeMillis(),
                    isCompleted = false,
                    isConfirmed = false,
                    appNameSource = "Tommy AI",
                    contactName = contactName
                )
                repository.insertAssistantTask(task)
                triggerAssistantNotificationAlert(task)
            } catch (e: Exception) {
                Log.e(TAG, "Direct command parsing failed", e)
            }
        }
    }

    fun clearAllAssistantTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllAssistantTasks()
        }
    }
}

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable? = null
)
