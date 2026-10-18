package com.tbocr.app.data.datasource.update

import com.tbocr.app.constant.MaaApi
import com.tbocr.app.data.api.MirrorChyanApiClient
import com.tbocr.app.data.api.MirrorChyanBizException
import com.tbocr.app.data.datasource.AppDownloader
import com.tbocr.app.data.model.update.UpdateChannel
import com.tbocr.app.data.model.update.UpdateCheckResult
import com.tbocr.app.data.model.update.UpdateError
import com.tbocr.app.data.model.update.UpdateInfo
import com.tbocr.app.data.preferences.AppSettingsManager
import com.tbocr.app.domain.service.update.checker.AppVersionChecker

class MirrorChyanAppVersionChecker(
    private val apiClient: MirrorChyanApiClient,
    private val appSettings: AppSettingsManager
) : AppVersionChecker {

    override suspend fun check(
        current: String,
        channel: UpdateChannel,
    ): UpdateCheckResult {
        val cdk = appSettings.mirrorChyanCdk.value
        val query = mapOf(
            "current_version" to current,
            "user_agent" to "tbocr",
            "os" to "android",
            "channel" to channel.value,
        ).let {
            if (cdk.length == 24) it + mapOf("cdk" to cdk) else it
        }
        val result = apiClient.getLatest(
            MaaApi.MIRROR_CHYAN_APP_RESOURCE,
            query = query,
            fetchVersion = true
        )

        return result.fold(
            onSuccess = { data ->
                val remoteVersion = data.versionName
                if (remoteVersion.isEmpty() || AppDownloader.compareVersions(
                        current,
                        remoteVersion
                    ) >= 0
                ) {
                    UpdateCheckResult.UpToDate(current)
                } else {
                    UpdateCheckResult.Available(
                        UpdateInfo(
                            version = remoteVersion,
                            releaseNote = data.releaseNote
                        )
                    )
                }
            },
            onFailure = { e ->
                when (e) {
                    is MirrorChyanBizException -> UpdateCheckResult.Error(e.toUpdateError())
                    else -> UpdateCheckResult.Error(
                        UpdateError.NetworkError(e.message)
                    )
                }
            }
        )
    }
}
