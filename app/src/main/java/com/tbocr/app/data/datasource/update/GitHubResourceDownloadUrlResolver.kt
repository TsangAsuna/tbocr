package com.tbocr.app.data.datasource.update

import com.tbocr.app.constant.MaaApi
import com.tbocr.app.domain.service.update.resolver.ResourceDownloadUrlResolver

class GitHubResourceDownloadUrlResolver : ResourceDownloadUrlResolver {

    override suspend fun resolve(currentVersion: String): Result<String> {
        return Result.success(MaaApi.GITHUB_RESOURCE)
    }
}
