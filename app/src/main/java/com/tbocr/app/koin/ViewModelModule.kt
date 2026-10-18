package com.tbocr.app.koin

import com.tbocr.app.presentation.viewmodel.AchievementViewModel
import com.tbocr.app.presentation.viewmodel.AppEventsViewModel
import com.tbocr.app.presentation.viewmodel.BackgroundTaskViewModel
import com.tbocr.app.presentation.viewmodel.CopilotViewModel
import com.tbocr.app.presentation.viewmodel.ErrorLogViewModel
import com.tbocr.app.presentation.viewmodel.ExpandedControlPanelViewModel
import com.tbocr.app.presentation.viewmodel.HomeViewModel
import com.tbocr.app.presentation.viewmodel.LogHistoryViewModel
import com.tbocr.app.presentation.viewmodel.NotificationSettingsViewModel
import com.tbocr.app.presentation.viewmodel.SettingsViewModel
import com.tbocr.app.presentation.viewmodel.TaskOverrideEditorViewModel
import com.tbocr.app.presentation.viewmodel.ToolboxViewModel
import com.tbocr.app.presentation.viewmodel.UpdateViewModel
import com.tbocr.app.schedule.ui.ScheduleEditViewModel
import com.tbocr.app.schedule.ui.ScheduleListViewModel
import com.tbocr.app.schedule.ui.ScheduleTriggerLogViewModel
import com.tbocr.app.schedule.ui.ScheduleAppBlacklistViewModel
import com.tbocr.app.schedule.ui.ScheduleWakeUnlockViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::AchievementViewModel)
    viewModelOf(::AppEventsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::UpdateViewModel)
    viewModelOf(::LogHistoryViewModel)
    viewModelOf(::ErrorLogViewModel)
    viewModelOf(::BackgroundTaskViewModel)
    viewModelOf(::ScheduleListViewModel)
    viewModelOf(::ScheduleEditViewModel)
    viewModelOf(::ScheduleTriggerLogViewModel)
    viewModelOf(::ScheduleWakeUnlockViewModel)
    viewModelOf(::ScheduleAppBlacklistViewModel)
    viewModelOf(::NotificationSettingsViewModel)
    viewModelOf(::TaskOverrideEditorViewModel)
}


val floatingWindowModule = module {
    singleOf(::ExpandedControlPanelViewModel)
    singleOf(::CopilotViewModel)
    singleOf(::ToolboxViewModel)
}
