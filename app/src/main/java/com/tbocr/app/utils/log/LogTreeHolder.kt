package com.tbocr.app.utils.log

import com.tbocr.app.BuildConfig
import com.tbocr.app.data.log.ApplicationLogWriter
import com.tbocr.app.data.preferences.AppSettingsManager
import timber.log.Timber

class LogTreeHolder(
    private val writer: ApplicationLogWriter,
    private val appSettings: AppSettingsManager
) {
    fun getTrees(): Array<Timber.Tree> {
        return arrayOf(
            if (BuildConfig.DEBUG) {
                DebugTree()
            } else {
                ReleaseTree()
            },
            FileLogTree(writer, appSettings.debugMode)
        )
    }

    fun setup() {
        getTrees().forEach {
            Timber.plant(it)
        }
    }
}