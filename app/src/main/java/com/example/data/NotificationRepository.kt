package com.example.data

import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationDao) {

    val allNotifications: Flow<List<CapturedNotification>> = dao.getAllNotifications()
    val settingsFlow: Flow<AppSettings?> = dao.getSettingsFlow()

    suspend fun getSettings(): AppSettings {
        return dao.getSettings() ?: AppSettings()
    }

    suspend fun saveSettings(settings: AppSettings) {
        dao.saveSettings(settings)
    }

    suspend fun insertNotification(notification: CapturedNotification): Long {
        return dao.insertNotification(notification)
    }

    suspend fun updateNotification(notification: CapturedNotification) {
        dao.updateNotification(notification)
    }

    suspend fun deleteNotificationById(id: Long) {
        dao.deleteNotificationById(id)
    }

    suspend fun clearAllNotifications() {
        dao.clearAllNotifications()
    }

    suspend fun markAsRead(id: Long) {
        dao.markAsRead(id)
    }

    suspend fun setArchived(id: Long, isArchived: Boolean) {
        dao.setArchived(id, isArchived)
    }

    suspend fun setPinned(id: Long, isPinned: Boolean) {
        dao.setPinned(id, isPinned)
    }

    // --- Assistant Tasks ---
    val assistantTasks: Flow<List<AssistantTask>> = dao.getAssistantTasksFlow()

    suspend fun getAssistantTasksList(): List<AssistantTask> {
        return dao.getAssistantTasksList()
    }

    suspend fun insertAssistantTask(task: AssistantTask): Long {
        return dao.insertAssistantTask(task)
    }

    suspend fun updateAssistantTask(task: AssistantTask) {
        dao.updateAssistantTask(task)
    }

    suspend fun deleteAssistantTaskById(id: Long) {
        dao.deleteAssistantTaskById(id)
    }

    suspend fun clearAllAssistantTasks() {
        dao.clearAllAssistantTasks()
    }

    suspend fun setAssistantTaskCompleted(id: Long, completed: Boolean) {
        dao.setAssistantTaskCompleted(id, completed)
    }

    suspend fun setAssistantTaskConfirmed(id: Long, confirmed: Boolean) {
        dao.setAssistantTaskConfirmed(id, confirmed)
    }

    // --- Regex Rules ---
    val regexRules: Flow<List<RegexRule>> = dao.getRegexRulesFlow()

    suspend fun getRegexRulesList(): List<RegexRule> {
        return dao.getRegexRulesList()
    }

    suspend fun insertRegexRule(rule: RegexRule): Long {
        return dao.insertRegexRule(rule)
    }

    suspend fun updateRegexRule(rule: RegexRule) {
        dao.updateRegexRule(rule)
    }

    suspend fun deleteRegexRule(id: Long) {
        dao.deleteRegexRule(id)
    }

    suspend fun clearAllRegexRules() {
        dao.clearAllRegexRules()
    }
}
