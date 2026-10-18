package com.tbocr.app.data.notification.live

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.tbocr.app.utils.device.Manufacturer

class AospPromotedDetector(context: Context) {
    private val appContext = context.applicationContext

    fun isApiSupported(): Boolean = Build.VERSION.SDK_INT >= 36

    fun isGranted(): Boolean {
        if (!isApiSupported()) return false
        if (Manufacturer.isSamsung()) return true
        return runCatching {
            appContext.getSystemService(NotificationManager::class.java)
                .canPostPromotedNotifications()
        }.getOrDefault(false)
    }
}
