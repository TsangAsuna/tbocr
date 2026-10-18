package com.tbocr.app.presentation.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tbocr.app.announcement.AnnouncementConfig
import com.tbocr.app.announcement.AnnouncementContent
import com.tbocr.app.announcement.AnnouncementManager
import com.tbocr.app.constant.Routes
import com.tbocr.app.data.preferences.AppSettingsManager
import com.tbocr.app.data.resource.BackgroundImageStore
import com.tbocr.app.domain.models.RunMode
import com.tbocr.app.domain.service.AchievementReporter
import com.tbocr.app.domain.service.ExternalNotificationService
import com.tbocr.app.domain.service.ResourceInitService
import com.tbocr.app.domain.state.ResourceInitState
import com.tbocr.app.overlay.OverlayController
import com.tbocr.app.presentation.LocalToaster
import com.tbocr.app.presentation.components.AnnouncementDialog
import com.tbocr.app.presentation.components.MaaWindowInsets
import com.tbocr.app.presentation.components.ResourceLoadingOverlay
import com.tbocr.app.presentation.components.clearFocusOnBlankTap
import com.tbocr.app.presentation.components.LocalPagerPreloadState
import com.tbocr.app.presentation.components.PagerPreloadState
import com.tbocr.app.presentation.onboarding.LocalOnboardingState
import com.tbocr.app.presentation.onboarding.OnboardingOverlay
import com.tbocr.app.presentation.onboarding.OnboardingState
import com.tbocr.app.presentation.pip.LocalIsInPip
import com.tbocr.app.presentation.state.UiEffect
import com.tbocr.app.presentation.view.notification.NotificationSettingsView
import com.tbocr.app.presentation.view.settings.AchievementDebugView
import com.tbocr.app.presentation.view.settings.AchievementView
import com.tbocr.app.presentation.view.settings.ErrorLogView
import com.tbocr.app.presentation.view.settings.LogHistoryView
import com.tbocr.app.presentation.view.settings.LiveUpdateSettingsView
import com.tbocr.app.presentation.view.settings.TaskOverrideEditorView
import com.tbocr.app.presentation.view.settings.WallpaperSettingsView
import com.tbocr.app.presentation.viewmodel.AppEventsViewModel
import com.tbocr.app.presentation.viewmodel.BackgroundTaskViewModel
import com.tbocr.app.schedule.model.CountdownState
import com.tbocr.app.schedule.ui.CountdownDialog
import com.tbocr.app.schedule.ui.ScheduleEditView
import com.tbocr.app.schedule.ui.ScheduleTriggerLogView
import com.tbocr.app.schedule.ui.ScheduleAppBlacklistView
import com.tbocr.app.schedule.ui.ScheduleWakeUnlockView
import com.tbocr.app.theme.AppBackgroundHost
import com.tbocr.app.theme.LocalReduceMotion
import com.tbocr.app.theme.MaaMotion
import com.tbocr.app.theme.MaxBackgroundBlur
import com.tbocr.app.utils.i18n.resolve
import com.dokar.sonner.ToastType
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** 主 Tab 路由集合（与 [BottomNavTab.all] 单一真源），用于判断是否处于主界面。 */
private val MAIN_TAB_ROUTES: Set<String> = BottomNavTab.all.mapTo(HashSet()) { it.route }

@Composable
fun AppNavigation(
    backgroundTaskViewModel: BackgroundTaskViewModel,
    appSettings: AppSettingsManager = koinInject(),
    notificationService: ExternalNotificationService = koinInject(),
    overlayController: OverlayController = koinInject(),
    announcementManager: AnnouncementManager = koinInject(),
    achievementReporter: AchievementReporter = koinInject(),
    resourceInitService: ResourceInitService = koinInject(),
    mainTabNavigator: MainTabNavigator = koinInject(),
    appEventsViewModel: AppEventsViewModel = koinViewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentNavRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val toaster = rememberToasterState()
    val isFullscreen by remember(backgroundTaskViewModel) {
        backgroundTaskViewModel.state
            .map { it.isFullscreenMonitor }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    var forceShowAnnouncement by remember { mutableStateOf(false) }
    var announcementDismissedOnce by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val runMode by appSettings.runMode.collectAsStateWithLifecycle()
    val announcementReadHash by appSettings.announcementReadHash.collectAsStateWithLifecycle()
    val language by appSettings.language.collectAsStateWithLifecycle()
    val announcementContent by announcementManager.content.collectAsStateWithLifecycle()

    // tbocr：淘宝壳不展示 MAA 公告（不拉远端内容）
    // LaunchedEffect(language) { announcementManager.refresh(language) }

    // 首启引导：资源就绪后自动开始
    val needsOnboarding by appSettings.needsOnboarding.collectAsStateWithLifecycle()
    val onboardingState = remember { OnboardingState(pending = needsOnboarding) }
    LaunchedEffect(needsOnboarding) { onboardingState.pending = needsOnboarding }
    val resourceInitState by resourceInitService.state.collectAsStateWithLifecycle()
    val resourceReady = resourceInitState is ResourceInitState.Ready
    LaunchedEffect(resourceReady, needsOnboarding) {
        if (resourceReady && needsOnboarding) onboardingState.start()
    }
    // 跳过或看完都算看过；重看再写一次是幂等的
    LaunchedEffect(onboardingState) {
        snapshotFlow { onboardingState.active }
            .drop(1)
            .filter { !it }
            .collect { appSettings.markOnboardingSeen() }
    }
    val scheduledCountdownState by backgroundTaskViewModel.countdownState.collectAsStateWithLifecycle()

    // 判断是否处于主 Tab 页面
    val isOnMainTab = currentNavRoute == null || currentNavRoute in MAIN_TAB_ROUTES

    // 自定义背景：上提到导航根层，让主界面与所有子页面共用同一背景与玻璃配色
    val backgroundStore: BackgroundImageStore = koinInject()
    val backgroundImage by backgroundStore.imageBitmap.collectAsStateWithLifecycle()
    val backgroundImageAlpha by appSettings.customBackgroundImageAlpha.collectAsStateWithLifecycle()
    val backgroundScrim by appSettings.customBackgroundScrim.collectAsStateWithLifecycle()
    val backgroundBlur by appSettings.customBackgroundBlur.collectAsStateWithLifecycle()
    val backgroundMonet by appSettings.customBackgroundMonet.collectAsStateWithLifecycle()

    // 主界面分页偏移，供背景层做切 Tab 视差
    val backgroundParallax = remember { mutableFloatStateOf(0f) }

    // 后台倒计时 Overlay 由 CountdownUIImpl 写入；前台无倒计时不触碰
    LaunchedEffect(backgroundTaskViewModel) {
        overlayController.onCountdownClick = {
            backgroundTaskViewModel.onScheduledStartNow()
        }
    }
    LaunchedEffect(notificationService) {
        notificationService.feedbackMessages.collect { message ->
            Toast.makeText(context, message.resolve(context), Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(backgroundTaskViewModel) {
        backgroundTaskViewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.Toast -> toaster.show(
                    message = effect.message.resolve(context),
                    type = ToastType.Info,
                )
            }
        }
    }
    LaunchedEffect(appEventsViewModel) {
        appEventsViewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.Toast -> toaster.show(
                    message = effect.message.resolve(context),
                    type = ToastType.Success,
                )
            }
        }
    }

    // LocalToaster 未提供即抛错，须同时覆盖 pager 与 NavHost 子页
    val reduceMotion = LocalReduceMotion.current
    CompositionLocalProvider(
        LocalToaster provides toaster,
        LocalOnboardingState provides onboardingState,
        LocalPagerPreloadState provides remember { PagerPreloadState() },
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .clearFocusOnBlankTap()) {
            AppBackgroundHost(
                image = backgroundImage,
                imageAlpha = backgroundImageAlpha / 100f,
                scrimAlpha = backgroundScrim / 100f,
                blurRadius = MaxBackgroundBlur * (backgroundBlur / 100f),
                monetFromWallpaper = backgroundMonet,
                parallax = backgroundParallax,
            ) {
                MainScreen(
                    navController = navController,
                    backgroundTaskViewModel = backgroundTaskViewModel,
                    onViewAnnouncement = { forceShowAnnouncement = true },
                    onViewOnboarding = { onboardingState.start() },
                    visible = isOnMainTab,
                    fullscreen = isFullscreen,
                    parallax = backgroundParallax,
                )

                // NavHost 只承载子页面；主 Tab 切换完全由 MainScreen 的 HorizontalPager 处理
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    enterTransition = { MaaMotion.pageEnter(forward = true, reduceMotion) },
                    exitTransition = { MaaMotion.pageExit(forward = true, reduceMotion) },
                    popEnterTransition = { MaaMotion.pageEnter(forward = false, reduceMotion) },
                    popExitTransition = { MaaMotion.pageExit(forward = false, reduceMotion) },
                ) {
                    // 主 Tab 路由仅作占位，真实内容由 MainScreen 的 HorizontalPager 渲染
                    BottomNavTab.all.forEach { tab -> composable(tab.route) {} }

                    composable(Routes.NOTIFICATION) {
                        NotificationSettingsView(navController = navController)
                    }
                    composable(Routes.LIVE_UPDATE) {
                        LiveUpdateSettingsView(navController = navController)
                    }
                    composable(Routes.WALLPAPER) {
                        WallpaperSettingsView(navController = navController)
                    }
                    composable(Routes.ACHIEVEMENT) {
                        AchievementView(navController = navController)
                    }
                    composable(Routes.ACHIEVEMENT_DEBUG) {
                        AchievementDebugView(navController = navController)
                    }
                    composable(Routes.LOG_HISTORY) {
                        LogHistoryView(navController = navController)
                    }
                    composable(Routes.ERROR_LOG) {
                        ErrorLogView(navController = navController)
                    }
                    composable(Routes.SCHEDULE_EDIT) { backStackEntry ->
                        val strategyId = backStackEntry.arguments?.getString("strategyId")
                            .let { if (it == "new") null else it }
                        ScheduleEditView(navController = navController, strategyId = strategyId)
                    }
                    composable(Routes.SCHEDULE_TRIGGER_LOG) {
                        ScheduleTriggerLogView(navController = navController)
                    }
                    composable(Routes.SCHEDULE_WAKE_UNLOCK) {
                        ScheduleWakeUnlockView(navController = navController)
                    }
                    composable(Routes.SCHEDULE_APP_BLACKLIST) {
                        ScheduleAppBlacklistView(navController = navController)
                    }
                    composable(Routes.TASK_OVERRIDE_EDITOR) {
                        TaskOverrideEditorView(navController = navController)
                    }
                }
            }
            // 画中画只留预览画面
            if (!LocalIsInPip.current) {
                ResourceLoadingOverlay()
                // 聚光灯引导：压在主界面之上、轻提示之下，toast 仍可读
                OnboardingOverlay(
                    state = onboardingState,
                    onRequestTab = { mainTabNavigator.navigateTo(it, animate = false) },
                )
                // 顶部轻提示（sonner）：替代旧的 Material3 Snackbar，按类型上色（成功=绿、错误=红）
                Toaster(
                    state = toaster,
                    alignment = Alignment.TopCenter,
                    richColors = true,
                    showCloseButton = true,
                    darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f,
                    containerPadding = PaddingValues(top = 8.dp),
                    modifier = Modifier.windowInsetsPadding(MaaWindowInsets.topBar),
                )
                // 全局定时任务倒计时弹窗（前台所有控制模式均不弹出对话框，静默处理）
                val countdown = scheduledCountdownState
                val hideCountdownDialog = runMode == RunMode.FOREGROUND
                if (countdown is CountdownState.Counting && !hideCountdownDialog) {
                    CountdownDialog(
                        state = countdown,
                        onCancel = { backgroundTaskViewModel.onScheduledCountdownCancel() },
                        onStartNow = { backgroundTaskViewModel.onScheduledStartNow() },
                    )
                }
                // 长期公告弹窗：远端内容变化（哈希与已读标记不符）后首次启动自动弹出，或从设置中手动打开
                val current = announcementContent
                val needsToShow = current != null && current.hash != announcementReadHash
                val showAnnouncement = forceShowAnnouncement ||
                        (needsToShow && !announcementDismissedOnce && !onboardingState.blocksStartupDialogs)
                val shownAnnouncement = remember(showAnnouncement, language, current) {
                    if (!showAnnouncement) {
                        null
                    } else {
                        // 手动打开时拉取可能尚未完成，回退内置 assets
                        current ?: AnnouncementConfig.loadContent(context, language)
                            .takeIf { it.isNotBlank() }
                            ?.let { AnnouncementContent.of(it) }
                    }
                }
                if (shownAnnouncement != null) {
                    AnnouncementDialog(
                        imageAssetPath = remember(language) {
                            AnnouncementConfig.imageAssetPath(language)
                        },
                        markdown = shownAnnouncement.markdown,
                        onDismiss = { dontShowAgain ->
                            forceShowAnnouncement = false
                            if (dontShowAgain) {
                                coroutineScope.launch {
                                    appSettings.setAnnouncementReadHash(shownAnnouncement.hash)
                                }
                            } else {
                                announcementDismissedOnce = true
                            }
                        },
                        onStubbornUnlock = { achievementReporter.reportAnnouncementStubbornClick() },
                    )
                }
            }
        }
    }
}
