package com.tbocr.app.data.notification.provider

import com.tbocr.app.utils.i18n.UiText

sealed interface NotificationSendResult {
    data object Success : NotificationSendResult
    data class Failed(val message: UiText) : NotificationSendResult
    data class Transient(val message: UiText) : NotificationSendResult
}
