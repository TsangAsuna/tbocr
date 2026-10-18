package com.tbocr.app.schedule.service

import com.tbocr.app.data.preferences.AppSettingsManager
import com.tbocr.app.domain.service.MaaNotificationCenter
import com.tbocr.app.schedule.data.ScheduleStrategyRepository
import com.tbocr.app.schedule.model.ExecutionResult
import com.tbocr.app.utils.i18n.UiText
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber

/** 未进启动流水线的失败上报：触发日志、策略结果与通知各自容错 */
class ScheduleFailureReporter(
    private val triggerLogger: ScheduleTriggerLogger,
    private val repository: ScheduleStrategyRepository,
    private val appSettings: AppSettingsManager,
    private val notificationCenter: MaaNotificationCenter,
) {
    companion object {
        private const val RECORD_TIMEOUT_MS = 5_000L
    }

    suspend fun report(
        strategyId: String,
        strategyName: String,
        scheduledTimeMs: Long,
        result: ExecutionResult,
        message: UiText,
        /** 已排了重试就别报，重试成功时这条就成了误报 */
        notify: Boolean = true,
    ) = withContext(NonCancellable) {
        try {
            triggerLogger.writeClosed(
                strategyId = strategyId,
                strategyName = strategyName,
                scheduledTimeMs = scheduledTimeMs,
                result = result,
                message = message,
                runMode = appSettings.runMode.value.name,
            )
        } catch (e: Exception) {
            Timber.w(e, "Trigger log write failed: %s", strategyId)
        }
        try {
            withTimeout(RECORD_TIMEOUT_MS) {
                repository.recordExecutionResult(
                    strategyId = strategyId,
                    result = result,
                    message = triggerLogger.resolveMessage(message),
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Execution result record failed: %s", strategyId)
        }
        if (!notify) return@withContext
        try {
            notificationCenter.notifyLaunchNotStarted(strategyName, result, message)
        } catch (e: Exception) {
            Timber.w(e, "Launch failure notification failed: %s", strategyId)
        }
    }
}
