package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.AppSettings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ClassificationResult(
    val priority: String, // URGENT, IMPORTANT, LOW, SILENT
    val reason: String
)

object AiClassifier {
    private const val TAG = "AiClassifier"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun classify(
        appName: String,
        title: String,
        body: String,
        settings: AppSettings
    ): ClassificationResult {
        return try {
            when (settings.backendType) {
                "GEMINI_API" -> {
                    classifyViaGemini(appName, title, body, settings)
                }
                "OLLAMA_LOCAL", "CUSTOM_ENDPOINT" -> {
                    classifyViaCustomEndpoint(appName, title, body, settings)
                }
                else -> {
                    classifyViaHeuristics(appName, title, body, settings)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AI Classification failed, falling back to heuristics", e)
            classifyViaHeuristics(appName, title, body, settings, errorMsg = e.localizedMessage)
        }
    }

    private fun classifyViaHeuristics(
        appName: String,
        title: String,
        body: String,
        settings: AppSettings,
        errorMsg: String? = null
    ): ClassificationResult {
        // Simple, highly effective semantic heuristic rule-matching
        val fullText = "$appName $title $body".lowercase()
        val promptText = settings.systemPrompt.lowercase()

        // Extract custom keywords from system prompt if any are mentioned
        // Let's analyze the rules
        var priority = "LOW"
        var reason = "Classified as LOW by local on-device heuristics."

        // Check whitelists / blacklists first
        val cleanPackage = appName.lowercase()
        val isWhitelisted = settings.whitelistedApps.lowercase().split(",").any { it.trim().isNotEmpty() && cleanPackage.contains(it.trim()) }
        val isBlacklisted = settings.blacklistedApps.lowercase().split(",").any { it.trim().isNotEmpty() && cleanPackage.contains(it.trim()) }

        if (isWhitelisted) {
            return ClassificationResult("URGENT", "Matched user's Whitelisted Apps exception rules.")
        }
        if (isBlacklisted) {
            return ClassificationResult("SILENT", "Matched user's Blacklisted Apps exception rules.")
        }

        // Standard categories
        val urgentKeywords = listOf(
            "mom", "dad", "family", "wife", "husband", "son", "daughter", "sister", "brother",
            "bank", "otp", "code", "fraud", "transfer", "verification", "secure", "security",
            "emergency", "urgent", "critical", "blocking"
        )

        val importantKeywords = listOf(
            "slack", "teams", "whatsapp", "signal", "telegram", "messenger", "calendar",
            "meeting", "schedule", "work", "boss", "manager", "invite", "task", "project", 
            "todo", "personal", "message", "chat", "email", "gmail", "outlook"
        )

        val lowKeywords = listOf(
            "discount", "promo", "deal", "coupon", "sale", "offer", "newsletter", "recommend",
            "suggest", "news", "sports", "score", "game", "cricket", "football", "league",
            "playlist", "song", "viral", "trending", "subscribers", "youtube", "tiktok", "instagram"
        )

        val silentKeywords = listOf(
            "syncing", "backup", "running", "downloading", "uploading", "battery", "keyboard",
            "charging", "connected", "service", "background", "music playing", "track"
        )

        // Count matches
        val urgentCount = urgentKeywords.count { fullText.contains(it) }
        val importantCount = importantKeywords.count { fullText.contains(it) }
        val lowCount = lowKeywords.count { fullText.contains(it) }
        val silentCount = silentKeywords.count { fullText.contains(it) }

        if (urgentCount > 0) {
            priority = "URGENT"
            val matchedWord = urgentKeywords.find { fullText.contains(it) }
            reason = "On-device AI heuristic: Detected urgent family/financial signal ($matchedWord)"
        } else if (importantCount > 0) {
            priority = "IMPORTANT"
            val matchedWord = importantKeywords.find { fullText.contains(it) }
            reason = "On-device AI heuristic: Identified direct message/work keyword ($matchedWord)"
        } else if (silentCount > 0) {
            priority = "SILENT"
            val matchedWord = silentKeywords.find { fullText.contains(it) }
            reason = "On-device AI heuristic: Filtered as quiet system/background operation ($matchedWord)"
        } else if (lowCount > 0) {
            priority = "LOW"
            val matchedWord = lowKeywords.find { fullText.contains(it) }
            reason = "On-device AI heuristic: Classified as low-priority update ($matchedWord)"
        } else {
            // Default check based on general text context
            priority = "LOW"
            reason = "On-device AI heuristic: Categorized as low priority (general notification)."
        }

        if (errorMsg != null) {
            reason += " [Remote API Error Fallback: $errorMsg]"
        }

        return ClassificationResult(priority, reason)
    }

    private fun classifyViaGemini(
        appName: String,
        title: String,
        body: String,
        settings: AppSettings
    ): ClassificationResult {
        // Use user-configured dynamic key if available; otherwise fall back to environment variable
        val apiKey = if (settings.customApiKey.isNotEmpty()) {
            settings.customApiKey
        } else {
            BuildConfig.GEMINI_API_KEY
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return classifyViaHeuristics(appName, title, body, settings, "Gemini API Key is not configured. Please add your key in Settings.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val systemPrompt = settings.systemPrompt + "\n" +
                "Respond ONLY with a valid JSON in this exact schema, do not include markdown blocks:\n" +
                "{\"priority\": \"URGENT|IMPORTANT|LOW|SILENT\", \"reason\": \"string reasoning\"}"

        val userPrompt = "App: $appName\nTitle: $title\nBody: $body"

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", userPrompt))
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemPrompt))
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonRequest.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            val responseBody = response.body?.string() ?: throw Exception("Empty response from Gemini")
            return parseLlmResponse(responseBody, isGemini = true)
        }
    }

    private fun classifyViaCustomEndpoint(
        appName: String,
        title: String,
        body: String,
        settings: AppSettings
    ): ClassificationResult {
        val baseUrl = settings.customBaseUrl.removeSuffix("/")
        val apiPath = if (settings.customApiPath.startsWith("/")) settings.customApiPath else "/${settings.customApiPath}"
        val url = "$baseUrl$apiPath"

        val systemPrompt = settings.systemPrompt + "\n" +
                "Your output must contain a JSON block with {\"priority\": \"URGENT|IMPORTANT|LOW|SILENT\", \"reason\": \"some explanation\"} representation."

        val userPrompt = "App: $appName\nTitle: $title\nBody: $body"

        val requestBodyString = if (settings.requestFormat == "ANTHROPIC") {
            // Anthropic Payload Schema
            JSONObject().apply {
                put("model", "claude-3-5-sonnet-20241022")
                put("max_tokens", 512)
                put("system", systemPrompt)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                })
            }.toString()
        } else {
            // OpenAI Payload Schema / Ollama / Default
            JSONObject().apply {
                put("model", "llama3")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                })
                put("temperature", 0.1)
                put("stream", false)
            }.toString()
        }

        val builder = Request.Builder()
            .url(url)
            .post(requestBodyString.toRequestBody(JSON_MEDIA_TYPE))

        if (settings.customApiKey.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer ${settings.customApiKey}")
            builder.addHeader("x-api-key", settings.customApiKey) // Anthropic compatibility
        }

        val request = builder.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            val responseBody = response.body?.string() ?: throw Exception("Empty response from custom API endpoint")
            return parseLlmResponse(responseBody, isGemini = false)
        }
    }

    private fun parseLlmResponse(raw: String, isGemini: Boolean): ClassificationResult {
        Log.d(TAG, "Parsing raw response: $raw")
        
        var generatedText = ""
        try {
            val json = JSONObject(raw)
            if (isGemini) {
                // Gemini structural nesting: candidates[0].content.parts[0].text
                val candidates = json.optJSONArray("candidates")
                val parts = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                generatedText = parts?.optJSONObject(0)?.optString("text") ?: ""
            } else {
                // OpenAI structural nesting: choices[0].message.content or Ollama "response"
                if (json.has("choices")) {
                    val choices = json.optJSONArray("choices")
                    generatedText = choices?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content") ?: ""
                } else if (json.has("response")) {
                    // Raw Ollama standard /api/generate format
                    generatedText = json.optString("response")
                } else if (json.has("content")) {
                    // Anthropic structural nesting or simple keys
                    val contentArr = json.optJSONArray("content")
                    if (contentArr != null && contentArr.length() > 0) {
                        generatedText = contentArr.optJSONObject(0)?.optString("text") ?: ""
                    } else {
                        generatedText = json.optString("content")
                    }
                } else {
                    generatedText = raw
                }
            }
        } catch (e: Exception) {
            generatedText = raw
        }

        // Now we have the raw generated text, let's extract the JSON block or the text details
        val cleanText = generatedText.trim()
        Log.d(TAG, "Extracted text for final analysis: $cleanText")

        // 1. Try to find complete JSON inside the text
        try {
            val jsonObject = if (cleanText.startsWith("{")) {
                JSONObject(cleanText)
            } else {
                val startIdx = cleanText.indexOf("{")
                val endIdx = cleanText.lastIndexOf("}")
                if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                    JSONObject(cleanText.substring(startIdx, endIdx + 1))
                } else {
                    null
                }
            }

            if (jsonObject != null) {
                val priorityVal = jsonObject.optString("priority", "LOW").uppercase().trim()
                val reasonVal = jsonObject.optString("reason", "Parsed correctly from JSON payload.")
                if (isValidPriority(priorityVal)) {
                    return ClassificationResult(priorityVal, reasonVal)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Regex JSON parsing failed, attempting fallback lexical analysis", e)
        }

        // 2. Lexical Fallback parser (Check if any category matches explicitly)
        // Find the priority first
        val upperText = cleanText.uppercase()
        val priority = when {
            upperText.contains("URGENT") -> "URGENT"
            upperText.contains("IMPORTANT") -> "IMPORTANT"
            upperText.contains("SILENT") -> "SILENT"
            upperText.contains("LOW") -> "LOW"
            else -> "LOW"
        }

        return ClassificationResult(priority, "Processed via textual analysis fallback. Original text: $cleanText")
    }

    private fun isValidPriority(p: String): Boolean {
        return p == "URGENT" || p == "IMPORTANT" || p == "LOW" || p == "SILENT"
    }

    suspend fun extractTasksFromNotifications(
        notificationsJson: String,
        settings: AppSettings
    ): String {
        val apiKey = if (settings.customApiKey.isNotEmpty()) {
            settings.customApiKey
        } else {
            BuildConfig.GEMINI_API_KEY
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "No Gemini API key available, using heuristic fallback.")
            return "[]" // Let ViewModel call heuristic fallback if needed, or return empty
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val systemPrompt = "You are an intelligent personalized AI task extractor assistant. Your job is to analyze the user's notifications and extract actionable tasks.\n" +
                "Scan the provided notifications. If anyone asked the user to call them, text back, set an alarm, set a reminder, do some chore, or perform on-demand tasks, extract it.\n" +
                "Respond ONLY with a valid JSON array at root containing objects with the following keys. Do NOT include markdown blocks, do NOT include triple backticks or ```json:\n" +
                "[\n" +
                "  {\n" +
                "    \"title\": \"Short task title (e.g., Call Mom, Set 7:30 AM Alarm, Review slides)\",\n" +
                "    \"description\": \"Brief explanation of what message prompted this\",\n" +
                "    \"actionType\": \"CALL | ALARM | REMINDER | TODO\",\n" +
                "    \"actionData\": \"The metadata: phone number if CALL (digits only), HH:MM if ALARM (format 24h e.g. 08:30 or 15:00), or empty string if general\",\n" +
                "    \"appNameSource\": \"App label\",\n" +
                "    \"contactName\": \"Person name if available\"\n" +
                "  }\n" +
                "]\n" +
                "If no actionable items are present, output an empty array: []"

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "Notifications content:\n$notificationsJson"))
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemPrompt))
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonRequest.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }
                val bodyStr = response.body?.string() ?: "[]"
                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                val parts = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                val generatedText = parts?.optJSONObject(0)?.optString("text") ?: "[]"
                generatedText
            }
        } catch (e: Exception) {
            Log.e(TAG, "Task extraction via Gemini API failed", e)
            "[]"
        }
    }

    fun extractTasksHeuristically(notifications: List<com.example.data.CapturedNotification>): String {
        val array = JSONArray()
        for (item in notifications) {
            val title = item.title
            val body = item.body
            val fullText = "$title $body".lowercase()
            
            if (fullText.contains("call ") || fullText.contains("dial ") || fullText.contains("calling") || fullText.contains("phone me")) {
                val task = JSONObject().apply {
                    put("title", "Call ${if (item.title.isNotEmpty()) item.title else "Contact"}")
                    put("description", "From notification: \"${item.body}\"")
                    put("actionType", "CALL")
                    val phoneMatcher = Pattern.compile("\\+?\\d{10,15}").matcher(body)
                    val phone = if (phoneMatcher.find()) phoneMatcher.group() else ""
                    put("actionData", phone)
                    put("appNameSource", item.appName)
                    put("contactName", item.title)
                }
                array.put(task)
            } else if (fullText.contains("alarm ") || fullText.contains("wake up") || fullText.contains("alarm at") || fullText.contains("timer")) {
                val task = JSONObject().apply {
                    put("title", "Set Alarm")
                    put("description", "From notification: \"${item.body}\"")
                    put("actionType", "ALARM")
                    val timeMatcher = Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(body)
                    val timeString = if (timeMatcher.find()) {
                        val hr = timeMatcher.group(1).toIntOrNull() ?: 7
                        val min = timeMatcher.group(2).toIntOrNull() ?: 0
                        String.format("%02d:%02d", hr, min)
                    } else "07:30"
                    put("actionData", timeString)
                    put("appNameSource", item.appName)
                    put("contactName", item.title)
                }
                array.put(task)
            } else if (fullText.contains("remind") || fullText.contains("reminder") || fullText.contains("remember to") || fullText.contains("don't forget")) {
                val task = JSONObject().apply {
                    put("title", "Remind Me: ${if (item.title.isNotEmpty()) item.title else "Task"}")
                    put("description", "From notification: \"${item.body}\"")
                    put("actionType", "REMINDER")
                    put("actionData", "")
                    put("appNameSource", item.appName)
                    put("contactName", item.title)
                }
                array.put(task)
            } else if (fullText.contains("buy ") || fullText.contains("todo ") || fullText.contains("task") || fullText.contains("need to")) {
                val task = JSONObject().apply {
                    put("title", "Task: ${if (item.title.isNotEmpty()) item.title else "Action Item"}")
                    put("description", "From notification: \"${item.body}\"")
                    put("actionType", "TODO")
                    put("actionData", "")
                    put("appNameSource", item.appName)
                    put("contactName", item.title)
                }
                array.put(task)
            }
        }
        return array.toString()
    }
}
