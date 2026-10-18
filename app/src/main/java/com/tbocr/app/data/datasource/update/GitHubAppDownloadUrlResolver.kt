package com.tbocr.app.data.datasource.update

import com.tbocr.app.R
import com.tbocr.app.constant.MaaApi
import com.tbocr.app.data.api.HttpClientHelper
import com.tbocr.app.data.api.model.GitHubRelease
import com.tbocr.app.data.model.update.UpdateChannel
import com.tbocr.app.domain.service.update.resolver.AppDownloadUrlResolver
import com.tbocr.app.utils.JsonUtils
import com.tbocr.app.utils.i18n.LocalizedException
import com.tbocr.app.utils.i18n.uiTextOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GitHubAppDownloadUrlResolver(
    private val httpClient: HttpClientHelper
) : AppDownloadUrlResolver {

    private val json = JsonUtils.common

    override suspend fun resolve(version: String, channel: UpdateChannel): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tag = if (version.startsWith("v", ignoreCase = true)) version else "v$version"
                httpClient.get(MaaApi.appGitHubReleaseByTag(tag)).use { response ->
                    if (!response.isSuccessful) {
                        throw LocalizedException(
                            uiTextOf(R.string.update_error_github_api_failed, response.code)
                        )
                    }

                    val release = json.decodeFromString<GitHubRelease>(response.body.string())

                    val apkAsset = release.assets.firstOrNull { it.name.endsWith("universal.apk") }
                        ?: throw LocalizedException(uiTextOf(R.string.update_error_github_no_apk))

                    apkAsset.browserDownloadUrl
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                Timber.e(e, "GitHub 获取 Release 失败: $version")
            }
        }
}
