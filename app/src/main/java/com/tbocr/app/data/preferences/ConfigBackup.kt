package com.tbocr.app.data.preferences

import com.tbocr.app.data.model.TaskProfile
import com.tbocr.app.data.notification.NotificationSettings
import com.tbocr.app.domain.models.AppSettings
import com.tbocr.app.schedule.model.ScheduleStrategy
import kotlinx.serialization.Serializable

@Serializable
data class ConfigBackup(
    val version: Int = 1,
    val exportedAt: String = "",
    val appSettings: AppSettings,
    val notificationSettings: NotificationSettings,
    val taskProfiles: List<TaskProfile>,
    val activeProfileId: String,
    val scheduleStrategies: List<ScheduleStrategy>
)
