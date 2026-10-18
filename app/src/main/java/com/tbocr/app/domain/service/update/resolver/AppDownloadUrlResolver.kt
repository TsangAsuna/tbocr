package com.tbocr.app.domain.service.update.resolver

import com.tbocr.app.data.model.update.UpdateChannel

interface AppDownloadUrlResolver {
    suspend fun resolve(version: String, channel: UpdateChannel): Result<String>
}
