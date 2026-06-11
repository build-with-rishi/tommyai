package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    // --- Captured Notifications ---
    @Query("SELECT * FROM captured_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<CapturedNotification>>

    @Query("SELECT * FROM captured_notifications ORDER BY timestamp DESC")
    suspend fun getAllNotificationsList(): List<CapturedNotification>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: CapturedNotification): Long

    @Update
    suspend fun updateNotification(notification: CapturedNotification)

    @Query("DELETE FROM captured_notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Long)

    @Query("DELETE FROM captured_notifications")
    suspend fun clearAllNotifications()

    @Query("UPDATE captured_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE captured_notifications SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE captured_notifications SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("SELECT * FROM captured_notifications WHERE packageName = :packageName AND (title = :title OR senderName = :title) AND abs(timestamp - :timestamp) < 15000 LIMIT 1")
    suspend fun findDuplicate(packageName: String, title: String, timestamp: Long): CapturedNotification?

    @Query("SELECT * FROM captured_notifications WHERE (priority = 'LOW' OR priority = 'SILENT') AND isDigest = 0 AND timestamp > :lastTime AND isArchived = 0")
    suspend fun getUndigestedMutedNotifications(lastTime: Long): List<CapturedNotification>

    // --- App Settings (Single Row with id = 1) ---
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)

    // --- Assistant Tasks ---
    @Query("SELECT * FROM assistant_tasks ORDER BY timestamp DESC")
    fun getAssistantTasksFlow(): Flow<List<AssistantTask>>

    @Query("SELECT * FROM assistant_tasks ORDER BY timestamp DESC")
    suspend fun getAssistantTasksList(): List<AssistantTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssistantTask(task: AssistantTask): Long

    @Update
    suspend fun updateAssistantTask(task: AssistantTask)

    @Query("DELETE FROM assistant_tasks WHERE id = :id")
    suspend fun deleteAssistantTaskById(id: Long)

    @Query("DELETE FROM assistant_tasks")
    suspend fun clearAllAssistantTasks()

    @Query("UPDATE assistant_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setAssistantTaskCompleted(id: Long, completed: Boolean)

    @Query("UPDATE assistant_tasks SET isConfirmed = :confirmed WHERE id = :id")
    suspend fun setAssistantTaskConfirmed(id: Long, confirmed: Boolean)

    // --- Regex Rules ---
    @Query("SELECT * FROM regex_rules ORDER BY id DESC")
    fun getRegexRulesFlow(): Flow<List<RegexRule>>

    @Query("SELECT * FROM regex_rules ORDER BY id DESC")
    suspend fun getRegexRulesList(): List<RegexRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegexRule(rule: RegexRule): Long

    @Update
    suspend fun updateRegexRule(rule: RegexRule)

    @Query("DELETE FROM regex_rules WHERE id = :id")
    suspend fun deleteRegexRule(id: Long)

    @Query("DELETE FROM regex_rules")
    suspend fun clearAllRegexRules()
}
