package com.tbocr.app.presentation.state

import com.tbocr.app.domain.models.OverlayControlMode
import com.tbocr.app.domain.models.RunMode
import com.tbocr.app.domain.state.ResourceInitState
import com.tbocr.app.utils.i18n.UiText

data class HomeUiState(
    val isShowControlOverlay: Boolean = false,
    val isLoading: Boolean = false,
    val serviceStatusText: UiText = UiText.Empty,
    val serviceStatusColor: StatusColorType = StatusColorType.NEUTRAL,
    val serviceStatusLoading: Boolean = false,
    val remoteServiceActive: Boolean = false,
    val resourceInitState: ResourceInitState = ResourceInitState.NotChecked,
    val runMode: RunMode = RunMode.BACKGROUND,
    val overlayControlMode: OverlayControlMode = OverlayControlMode.FLOAT_BALL,
    val isGranting: Boolean = false,
    val showRunModeUnsupportedDialog: Boolean = false,
    val runModeUnsupportedMessage: UiText = UiText.Empty
)
