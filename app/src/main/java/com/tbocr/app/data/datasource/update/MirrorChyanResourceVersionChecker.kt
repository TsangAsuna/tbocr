package com.tbocr.app.data.datasource.update

import com.tbocr.app.constant.MaaApi
import com.tbocr.app.data.api.MirrorChyanApiClient
import com.tbocr.app.data.api.MirrorChyanBizException
import com.tbocr.app.data.datasource.ResourceDownloader
import com.tbocr.app.data.model.update.UpdateCheckResult
import com.tbocr.app.data.model.update.UpdateError
import com.tbocr.app.data.model.update.UpdateInfo
import com.tbocr.app.domain.service.update.checker.ResourceVersionChecker

class MirrorChyanResourceVersionChecker(
    private val apiClient: MirrorChyanApiClient
) : ResourceVersionChecker {

    override suspend fun check(currentVersion: String): UpdateCheckResult {
        val result = apiClient.getLatest(
            MaaApi.MIRROR_CHYAN_RESOURCE,
            query = mapOf(
                "current_version" to currentVersion,
                "user_agent" to "tbocr"
            )
        )

        return result.fold(
            onSuccess = { data ->
                val remoteVersion = data.versionName
                if (remoteVersion.isEmpty() || ResourceDownloader.compareVersions(
                        currentVersion,
                        remoteVersion
                    ) >= 0
                ) {
                    UpdateCheckResult.UpToDate(currentVersion)
                } else {
                    UpdateCheckResult.Available(
                        UpdateInfo(
                            version = ResourceDownloader.formatVersionForDisplay(remoteVersion),
                            releaseNote = data.releaseNote
                        )
                    )
                }
            },
            onFailure = { e ->
                when (e) {
                    is MirrorChyanBizException -> UpdateCheckResult.Error(e.toUpdateError())
                    else -> UpdateCheckResult.Error(UpdateError.NetworkError(e.message))
                }
            }
        )
    }
}
