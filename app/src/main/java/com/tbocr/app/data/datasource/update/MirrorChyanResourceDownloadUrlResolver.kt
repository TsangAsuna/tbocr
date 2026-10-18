package com.tbocr.app.data.datasource.update

import com.tbocr.app.R
import com.tbocr.app.constant.MaaApi
import com.tbocr.app.data.api.CdkRequiredException
import com.tbocr.app.data.api.MirrorChyanApiClient
import com.tbocr.app.data.preferences.AppSettingsManager
import com.tbocr.app.domain.service.update.resolver.ResourceDownloadUrlResolver
import com.tbocr.app.utils.i18n.LocalizedException
import com.tbocr.app.utils.i18n.uiTextOf

class MirrorChyanResourceDownloadUrlResolver(
    private val apiClient: MirrorChyanApiClient,
    private val appSettingsManager: AppSettingsManager
) : ResourceDownloadUrlResolver {

    override suspend fun resolve(currentVersion: String): Result<String> {
        val cdk = appSettingsManager.mirrorChyanCdk.value
        if (cdk.isBlank()) {
            return Result.failure(CdkRequiredException())
        }

        return apiClient.getLatest(
            MaaApi.MIRROR_CHYAN_RESOURCE,
            query = mapOf(
                "current_version" to currentVersion,
                "user_agent" to "tbocr",
                "cdk" to cdk
            )
        ).mapCatching { data ->
            data.url ?: throw LocalizedException(uiTextOf(R.string.update_error_empty_download_url))
        }
    }
}
