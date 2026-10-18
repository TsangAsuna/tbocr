package com.tbocr.app.domain.service.update.checker

import com.tbocr.app.data.model.update.UpdateCheckResult

interface ResourceVersionChecker {
    suspend fun check(currentVersion: String): UpdateCheckResult
}
