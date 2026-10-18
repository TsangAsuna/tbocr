package com.tbocr.app.data.datasource.update

import com.tbocr.app.BuildConfig
import com.tbocr.app.R
import com.tbocr.app.constant.MaaApi
import com.tbocr.app.data.api.CdkRequiredException
import com.tbocr.app.data.api.MirrorChyanApiClient
import com.tbocr.app.data.model.update.UpdateChannel
import com.tbocr.app.data.preferences.AppSettingsManager
import com.tbocr.app.domain.service.update.resolver.AppDownloadUrlResolver
import com.tbocr.app.utils.i18n.LocalizedException
import com.tbocr.app.utils.i18n.uiTextOf

class MirrorChyanAppDownloadUrlResolver(
    private val apiClient: MirrorChyanApiClient,
    private val appSettingsManager: AppSettingsManager
) : AppDownloadUrlResolver {

    override suspend fun resolve(version: String, channel: UpdateChannel): Result<String> {
        val cdk = appSettingsManager.mirrorChyanCdk.value
        if (cdk.isBlank()) {
            return Result.failure(CdkRequiredException())
        }

        return apiClient.getLatest(
            MaaApi.MIRROR_CHYAN_APP_RESOURCE,
            query = mapOf(
                "current_version" to BuildConfig.VERSION_NAME,
                "user_agent" to "tbocr",
                "os" to "android",
                "channel" to channel.value,
                "cdk" to cdk
            )
        ).mapCatching { data ->
            data.url ?: throw LocalizedException(uiTextOf(R.string.update_error_empty_download_url))
        }
    }
}
