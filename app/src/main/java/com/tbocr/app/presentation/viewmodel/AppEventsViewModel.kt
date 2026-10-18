package com.tbocr.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.tbocr.app.R
import com.tbocr.app.data.achievement.AchievementField
import com.tbocr.app.data.achievement.AchievementRepository
import com.tbocr.app.data.achievement.achievementStringResId
import com.tbocr.app.data.preferences.AppSettingsManager
import com.tbocr.app.presentation.state.UiEffect
import com.tbocr.app.utils.i18n.uiTextOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull


class AppEventsViewModel(
    achievementRepository: AchievementRepository,
    private val appSettings: AppSettingsManager,
) : ViewModel() {

    val effects: Flow<UiEffect> = achievementRepository.unlockEvents
        .filter { appSettings.showAchievementSnackbar.value }
        .mapNotNull {
            val resId = achievementStringResId(it, AchievementField.TITLE)
            if (resId != 0) {
                UiEffect.toast(R.string.achievement_unlocked_message, uiTextOf(resId))
            } else {
                null
            }
        }

}
