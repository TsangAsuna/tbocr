package com.tbocr.app.koin

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.view.Display
import com.tbocr.app.R
import com.tbocr.app.announcement.AnnouncementManager
import com.tbocr.app.data.achievement.AchievementRepository
import com.tbocr.app.data.achievement.PallasDrunkState
import com.tbocr.app.data.api.CopilotApiService
import com.tbocr.app.data.api.ETagCacheManager
import com.tbocr.app.data.api.GameDataReportService
import com.tbocr.app.data.api.HttpClientHelper
import com.tbocr.app.data.api.MaaApiService
import com.tbocr.app.data.api.MirrorChyanApiClient
import com.tbocr.app.data.api.YituliuApiService
import com.tbocr.app.data.config.MaaPathConfig
import com.tbocr.app.data.datasource.AppDownloader
import com.tbocr.app.data.datasource.AssetExtractor
import com.tbocr.app.data.datasource.ResourceDownloader
import com.tbocr.app.data.datasource.ZipExtractor
import com.tbocr.app.data.datasource.update.MirrorChyanAppVersionChecker
import com.tbocr.app.data.datasource.update.MirrorChyanResourceVersionChecker
import com.tbocr.app.data.log.ApplicationLogWriter
import com.tbocr.app.data.notification.NotificationSettingsManager
import com.tbocr.app.data.notification.live.AospPromotedDetector
import com.tbocr.app.data.notification.live.FocusSequenceStore
import com.tbocr.app.data.notification.live.HyperOsFocusDetector
import com.tbocr.app.data.notification.live.LiveNotificationFactory
import com.tbocr.app.data.notification.live.LiveUpdateStyle
import com.tbocr.app.data.notification.live.TrackerIconStore
import com.tbocr.app.data.notification.live.LivePublisherRouter
import com.tbocr.app.data.notification.live.XmsfNetworkGate
import com.tbocr.app.data.notification.provider.BarkProvider
import com.tbocr.app.data.notification.provider.CustomWebhookProvider
import com.tbocr.app.data.notification.provider.DingTalkProvider
import com.tbocr.app.data.notification.provider.DiscordProvider
import com.tbocr.app.data.notification.provider.DiscordWebhookProvider
import com.tbocr.app.data.notification.provider.GotifyProvider
import com.tbocr.app.data.notification.provider.KookProvider
import com.tbocr.app.data.notification.provider.NotificationProvider
import com.tbocr.app.data.notification.provider.QmsgProvider
import com.tbocr.app.data.notification.provider.QmsgV3Provider
import com.tbocr.app.data.notification.provider.ServerChanProvider
import com.tbocr.app.data.notification.provider.SmtpProvider
import com.tbocr.app.data.notification.provider.TelegramProvider
import com.tbocr.app.data.preferences.AppSettingsManager
import com.tbocr.app.data.preferences.ConfigBackupManager
import com.tbocr.app.data.preferences.TaskChainState
import com.tbocr.app.data.preferences.UnlockGestureStore
import com.tbocr.app.data.repository.CopilotRepository
import com.tbocr.app.data.repository.DepotRepository
import com.tbocr.app.data.repository.OperBoxRepository
import com.tbocr.app.data.resource.ActivityManager
import com.tbocr.app.data.resource.BackgroundImageStore
import com.tbocr.app.data.resource.CopilotResourceProvider
import com.tbocr.app.data.resource.GameMetadataLoader
import com.tbocr.app.data.resource.ItemHelper
import com.tbocr.app.data.resource.ItemIconLoader
import com.tbocr.app.data.resource.OperAvatarLoader
import com.tbocr.app.data.resource.OperRoleIconLoader
import com.tbocr.app.data.resource.ResourceDataManager
import com.tbocr.app.data.resource.StageApCostHelper
import com.tbocr.app.domain.launch.BackendBlock
import com.tbocr.app.domain.launch.CountdownUI
import com.tbocr.app.domain.launch.LaunchMutex
import com.tbocr.app.domain.launch.LaunchPipeline
import com.tbocr.app.domain.launch.LaunchRequest
import com.tbocr.app.domain.launch.PlanSideTaskRunner
import com.tbocr.app.domain.launch.StartTaskChainUseCase
import com.tbocr.app.domain.notification.LiveSessionCoordinator
import com.tbocr.app.domain.notification.LiveUpdatePublisher
import com.tbocr.app.domain.service.AchievementReporter
import com.tbocr.app.domain.service.AppAliveChecker
import com.tbocr.app.domain.service.AppWatchdog
import com.tbocr.app.domain.service.CopilotManager
import com.tbocr.app.domain.service.CopilotOperBoxAssist
import com.tbocr.app.domain.service.CoreDataPusher
import com.tbocr.app.domain.service.ExternalNotificationService
import com.tbocr.app.domain.service.FightDropsRefresher
import com.tbocr.app.domain.service.ForegroundScreenGate
import com.tbocr.app.domain.service.FrameSnapshotter
import com.tbocr.app.domain.service.GameDataReporter
import com.tbocr.app.domain.service.GameFpsReader
import com.tbocr.app.domain.service.GameFpsWatcher
import com.tbocr.app.domain.service.GameMetadataGate
import com.tbocr.app.domain.service.GameMuteCoordinator
import com.tbocr.app.domain.service.LogExportService
import com.tbocr.app.domain.service.MaaCompositionService
import com.tbocr.app.domain.service.MaaEventNotifier
import com.tbocr.app.domain.service.MaaNotificationCenter
import com.tbocr.app.domain.service.MaaResourceLoader
import com.tbocr.app.domain.service.MaaSessionLogger
import com.tbocr.app.domain.service.OperBoxYituliuSync
import com.tbocr.app.domain.service.RemoteAppAliveChecker
import com.tbocr.app.domain.service.RemoteFrameSnapshotter
import com.tbocr.app.domain.service.RemoteGameFpsReader
import com.tbocr.app.domain.service.ResourceInitService
import com.tbocr.app.domain.service.RunTelemetry
import com.tbocr.app.domain.service.ScreenPowerController
import com.tbocr.app.domain.service.ScreenSaverController
import com.tbocr.app.domain.service.TaskEndRegistry
import com.tbocr.app.domain.service.ToolboxExportService
import com.tbocr.app.domain.service.UnifiedStateDispatcher
import com.tbocr.app.domain.service.ProcessExitJournal
import com.tbocr.app.data.log.ProcessExitRecorder
import com.tbocr.app.domain.service.UnlockGestureReader
import com.tbocr.app.domain.service.WakeUnlockEngine
import com.tbocr.app.domain.service.update.UpdateService
import com.tbocr.app.domain.service.update.checker.AppVersionChecker
import com.tbocr.app.domain.service.update.checker.ResourceVersionChecker
import com.tbocr.app.maa.callback.ConnectionInfoHandler
import com.tbocr.app.maa.callback.CopilotRuntimeStateStore
import com.tbocr.app.maa.callback.MaaCallbackDispatcher
import com.tbocr.app.maa.callback.MaaExecutionStateHolder
import com.tbocr.app.maa.callback.OperProgressHandler
import com.tbocr.app.maa.callback.SetParamsErrorSignal
import com.tbocr.app.maa.callback.SubTaskHandler
import com.tbocr.app.maa.callback.TaskChainHandler
import com.tbocr.app.maa.callback.TaskChainStatusTracker
import com.tbocr.app.maa.callback.ToolboxResultCollector
import com.tbocr.app.manager.PermissionManager
import com.tbocr.app.manager.RemoteAccessCoordinator
import com.tbocr.app.manager.RemoteGameAudioAdapter
import com.tbocr.app.manager.RemoteScreenPowerController
import com.tbocr.app.manager.RemoteServiceManager
import com.tbocr.app.manager.ShizukuReadinessProvider
import com.tbocr.app.overlay.OverlayController
import com.tbocr.app.overlay.OverlayViewModelOwner
import com.tbocr.app.overlay.border.BorderOverlayManager
import com.tbocr.app.overlay.screensaver.ScreenSaverOverlayManager
import com.tbocr.app.presentation.navigation.MainTabNavigator
import com.tbocr.app.presentation.search.SettingSearchNavigator
import com.tbocr.app.schedule.LaunchIntentMapper
import com.tbocr.app.schedule.data.ScheduleStrategyRepository
import com.tbocr.app.schedule.service.CountdownUIImpl
import com.tbocr.app.schedule.service.ScheduleAlarmManager
import com.tbocr.app.schedule.service.ScheduleFailureReporter
import com.tbocr.app.schedule.service.ScheduleTriggerHandler
import com.tbocr.app.schedule.service.ScheduleTriggerLogger
import com.tbocr.app.schedule.service.ShizukuDownMonitor
import com.tbocr.app.telemetry.TelemetryController
import com.tbocr.app.utils.CrashHandler
import com.tbocr.app.utils.LauncherApps
import com.tbocr.app.utils.i18n.uiTextOf
import com.tbocr.app.utils.log.LogTreeHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

val appModule = module {


    singleOf(::CrashHandler)
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    singleOf(::HttpClientHelper)
    single<GameDataReporter> {
        GameDataReportService(
            appContext = androidContext(),
            httpClient = get(),
            appSettings = get(),
            taskChainState = get(),
            sessionLogger = get(),
        )
    }
    singleOf(::ETagCacheManager)
    singleOf(::MaaApiService)
    singleOf(::YituliuApiService)
    singleOf(::AnnouncementManager)
    singleOf(::PermissionManager)
    singleOf(::ShizukuReadinessProvider)
    singleOf(::MainTabNavigator)
    singleOf(::SettingSearchNavigator)


    single { AppSettingsManager(androidContext(), get()) }
    single { UnlockGestureStore(get()) } bind UnlockGestureReader::class
    singleOf(::BackgroundImageStore)
    singleOf(::AchievementRepository)
    singleOf(::AchievementReporter)
    single { PallasDrunkState(get()) }
    single { ScheduleStrategyRepository(androidContext()) }
    singleOf(::ScheduleTriggerLogger)
    singleOf(::ScheduleFailureReporter)
    singleOf(::ScheduleAlarmManager)
    singleOf(::ShizukuDownMonitor)
    single { ScheduleTriggerHandler(get(), get(), get(), get(), get()) }
    singleOf(::LaunchMutex)
    singleOf(::StartTaskChainUseCase)
    single(named("launchPipeline")) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    single<CountdownUI> {
        CountdownUIImpl(
            overlayController = get(),
            onUserEvent = { event -> get<LaunchPipeline>().submit(event) },
        )
    }
    single {
        val appContext = get<Context>()
        LaunchPipeline(
            scope = get(named("launchPipeline")),
            mutex = get(),
            appSettingsManager = get(),
            wakeUnlockEngine = get(),
            unlockGestures = get(),
            chainState = get(),
            compositionService = get(),
            triggerLogger = get(),
            scheduleRepository = get(),
            startTaskChain = get(),
            countdownUI = get(),
            screenSaver = get(),
            screenPower = get(),
            taskEndRegistry = get(),
            notificationCenter = get(),
            keyguardLocked = {
                val km = appContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                km.isKeyguardLocked
            },
            deviceLocked = {
                val km = appContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                km.isDeviceLocked
            },
            screenInteractive = {
                val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                pm.isInteractive
            },
            activityLauncher = { request: LaunchRequest ->
                withTimeoutOrNull(10.seconds) {
                    runCatching {
                        RemoteServiceManager.useRemoteService(timeoutMs = 8_000L) {
                            it.startActivity(LaunchIntentMapper.toShowIntent(appContext, request))
                        }
                    }.getOrDefault(false)
                } ?: false
            },
            // 与 MaaCompositionService 启动前检查同口径，免得倒计时走完才被拒
            remoteAccessBlocker = {
                var access = RemoteAccessCoordinator.refresh()
                val backend = access.configuredBackend
                // 冷启动时 Root shell 还在异步申请，快照会误报未授权；和 useRemoteService 一样先申请一次
                if (access.isAvailable(backend) && !access.isGranted(backend)) {
                    RemoteAccessCoordinator.request(backend)
                    access = RemoteAccessCoordinator.refresh()
                }
                when {
                    !access.isAvailable(backend) ->
                        BackendBlock(uiTextOf(R.string.runlog_backend_unavailable, backend.display))

                    !access.isGranted(backend) ->
                        BackendBlock(uiTextOf(R.string.runlog_backend_not_granted, backend.display))

                    // 先连上，否则拉起失败会被报成锁屏或拉起界面失败
                    else -> RemoteServiceManager.awaitConnected()?.let {
                        Timber.w(it, "launch: elevated service connect failed")
                        BackendBlock(
                            reason = uiTextOf(R.string.runlog_backend_connect_failed, backend.display),
                            detail = RemoteServiceManager.shortCause(it),
                        )
                    }
                }
            },
            foregroundPackage = {
                RemoteServiceManager.useRemoteService(timeoutMs = 3_000L) {
                    it.getTopPackage(Display.DEFAULT_DISPLAY)
                }
            },
            appLabel = { LauncherApps.label(appContext, it) ?: it },
        )
    }
    singleOf(::TaskChainState)
    singleOf(::ConfigBackupManager)
    singleOf(::MaaPathConfig)
    singleOf(::CoreDataPusher)
    singleOf(::ResourceDownloader)
    singleOf(::AppDownloader)
    singleOf(::ZipExtractor)
    singleOf(::AssetExtractor)

    // MirrorChyan API Client
    singleOf(::MirrorChyanApiClient)

    // Version Checkers
    single<AppVersionChecker> { MirrorChyanAppVersionChecker(get(), get()) }
    single<ResourceVersionChecker> { MirrorChyanResourceVersionChecker(get()) }

    singleOf(::UpdateService)

    singleOf(::ResourceInitService)
    singleOf(::MaaResourceLoader)
    // scope 走构造默认值
    single { GameMetadataLoader(get(), get(), get(), get(), get(), get()) } bind GameMetadataGate::class
    singleOf(::MaaSessionLogger)

    // 外部通知
    singleOf(::NotificationSettingsManager)
    single { ServerChanProvider(get(), get()) } bind NotificationProvider::class
    single { TelegramProvider(get(), get()) } bind NotificationProvider::class
    single { DiscordProvider(get(), get()) } bind NotificationProvider::class
    single { DingTalkProvider(get(), get()) } bind NotificationProvider::class
    single { KookProvider(get(), get()) } bind NotificationProvider::class
    single { DiscordWebhookProvider(get(), get()) } bind NotificationProvider::class
    single { SmtpProvider(get()) } bind NotificationProvider::class
    single { BarkProvider(get(), get()) } bind NotificationProvider::class
    single { QmsgProvider(get(), get()) } bind NotificationProvider::class
    single { QmsgV3Provider(get(), get()) } bind NotificationProvider::class
    single { GotifyProvider(get(), get()) } bind NotificationProvider::class
    single { CustomWebhookProvider(get(), get()) } bind NotificationProvider::class
    single { ExternalNotificationService(get(), get(), getAll()) }

    // 通知 / 实况
    singleOf(::LiveNotificationFactory)
    singleOf(::TrackerIconStore)
    singleOf(::LiveUpdateStyle)
    singleOf(::AospPromotedDetector)
    singleOf(::HyperOsFocusDetector)
    singleOf(::FocusSequenceStore)
    singleOf(::XmsfNetworkGate)
    singleOf(::LivePublisherRouter)
    single<LiveUpdatePublisher> { get<LivePublisherRouter>() }
    singleOf(::LiveSessionCoordinator)
    singleOf(::MaaEventNotifier)
    singleOf(::MaaNotificationCenter)

    // 仓库 / 干员箱持久化（按配置档分片）
    single { DepotRepository.create(get(), get()) }
    single { OperBoxRepository.create(get(), get()) }

    // 回调处理链
    singleOf(::ConnectionInfoHandler)
    singleOf(::CopilotRuntimeStateStore)
    singleOf(::ToolboxResultCollector)
    singleOf(::OperBoxYituliuSync)
    singleOf(::CopilotOperBoxAssist)
    single { PlanSideTaskRunner(androidContext(), get(), get()) }
    singleOf(::TaskChainStatusTracker)
    singleOf(::FightDropsRefresher)
    singleOf(::TaskChainHandler)
    singleOf(::SubTaskHandler)
    single<AppAliveChecker> { RemoteAppAliveChecker() }
    singleOf(::AppWatchdog)
    single<GameFpsReader> { RemoteGameFpsReader() }
    single { GameFpsWatcher(reader = get(), sessionLogger = get(), context = androidApplication()) }
    singleOf(::TelemetryController) { bind<RunTelemetry>() }
    // 调用时才取 OverlayController：它依赖 MaaCompositionService，构造期取会成环
    single<ForegroundScreenGate> { ForegroundScreenGate { get<OverlayController>().awaitPanelCleared() } }
    singleOf(::MaaCompositionService)
    single<MaaExecutionStateHolder> { get<MaaCompositionService>() }
    single { GameMuteCoordinator(get(), RemoteGameAudioAdapter) }
    single<FrameSnapshotter> { RemoteFrameSnapshotter() }
    singleOf(::MaaCallbackDispatcher)
    singleOf(::SetParamsErrorSignal)
    single { OperProgressHandler(get(), get(), get(), get(), get(), get()) }

    // 定时唤醒 + 解锁
    singleOf(::WakeUnlockEngine)

    singleOf(::UnifiedStateDispatcher)
    singleOf(::ProcessExitRecorder) { bind<ProcessExitJournal>() }
    // scope 走构造默认值，singleOf 会试图解析它
    single {
        val settings = get<AppSettingsManager>()
        TaskEndRegistry(
            compositionService = get(),
            closeGameOnEnd = { settings.closeAppOnTaskEnd.value },
        )
    }
    singleOf(::LogExportService)
    singleOf(::ToolboxExportService)


    singleOf(::BorderOverlayManager)
    singleOf(::ScreenSaverOverlayManager) { bind<ScreenSaverController>() }
    singleOf(::RemoteScreenPowerController) { bind<ScreenPowerController>() }
    singleOf(::OverlayViewModelOwner)
    singleOf(::OverlayController)


    singleOf(::ItemHelper)
    singleOf(::StageApCostHelper)
    singleOf(::ItemIconLoader)
    singleOf(::OperAvatarLoader)
    singleOf(::OperRoleIconLoader)
    singleOf(::ActivityManager)
    singleOf(::ResourceDataManager)
    // Copilot (自动战斗)
    singleOf(::CopilotApiService)
    singleOf(::CopilotRepository)
    singleOf(::CopilotManager)
    singleOf(::CopilotResourceProvider)
    singleOf(::ApplicationLogWriter)
    singleOf(::LogTreeHolder)

    // 前台模式自动任务
}
