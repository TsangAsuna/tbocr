package com.tbocr.app.data.notification.provider

import com.tbocr.app.data.notification.NotificationSettings
import com.tbocr.app.data.notification.NotificationSettingsManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

internal fun buildResponse(code: Int = 200, body: String = ""): Response =
    Response.Builder()
        .request(Request.Builder().url("http://localhost").build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("test")
        .body(body.toResponseBody(null))
        .build()

internal fun settingsManagerOf(settings: NotificationSettings = NotificationSettings()) =
    mockk<NotificationSettingsManager> { every { this@mockk.settings } returns flowOf(settings) }
