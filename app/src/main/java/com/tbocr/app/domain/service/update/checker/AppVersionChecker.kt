package com.tbocr.app.domain.service.update.checker

import com.tbocr.app.data.model.update.UpdateChannel
import com.tbocr.app.data.model.update.UpdateCheckResult

interface AppVersionChecker {
    suspend fun check(current: String, channel: UpdateChannel): UpdateCheckResult
}
