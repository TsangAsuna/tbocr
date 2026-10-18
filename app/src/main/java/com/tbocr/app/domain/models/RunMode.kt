package com.tbocr.app.domain.models

import com.tbocr.app.constant.DisplayMode

enum class RunMode(
    val displayMode: Int
) {
    FOREGROUND(DisplayMode.PRIMARY),

    BACKGROUND(DisplayMode.BACKGROUND)
}
