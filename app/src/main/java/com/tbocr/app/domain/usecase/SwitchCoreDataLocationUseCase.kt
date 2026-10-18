package com.tbocr.app.domain.usecase

import android.content.Context
import com.tbocr.app.data.preferences.AppSettingsManager
import com.tbocr.app.domain.models.CoreDataLocation
import com.tbocr.app.manager.RemoteServiceManager
import com.tbocr.app.utils.Misc

/** 落设置 → 断提权进程 → 重启：MaaPathConfig 与 MaaCore 状态都是进程内固定的 */
class SwitchCoreDataLocationUseCase(
    private val context: Context,
    private val appSettings: AppSettingsManager,
) {
    suspend operator fun invoke(target: CoreDataLocation) {
        appSettings.setCoreDataLocation(target)
        RemoteServiceManager.unbind()
        Misc.restartApp(context)
    }
}
