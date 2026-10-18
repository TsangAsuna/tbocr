package com.tbocr.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withStarted
import com.tbocr.app.data.achievement.AchievementEvents
import com.tbocr.app.data.achievement.AchievementRepository
import com.tbocr.app.data.achievement.PallasDrunkState
import com.tbocr.app.data.preferences.AppSettingsManager
import com.tbocr.app.domain.launch.LaunchSession
import com.tbocr.app.domain.service.MaaCompositionService
import com.tbocr.app.domain.state.MaaExecutionState
import com.tbocr.app.overlay.screensaver.ScreenSaverOverlayManager
import com.tbocr.app.presentation.PallasDrunkHost
import com.tbocr.app.presentation.ProvideInputFocusManager
import com.tbocr.app.presentation.navigation.AppNavigation
import com.tbocr.app.presentation.pip.LocalIsInPip
import com.tbocr.app.presentation.pip.PipController
import com.tbocr.app.presentation.pip.PipHost
import com.tbocr.app.presentation.pip.PipRequest
import com.tbocr.app.presentation.viewmodel.BackgroundTaskViewModel
import com.tbocr.app.schedule.LaunchIntentMapper
import com.tbocr.app.theme.TbOcrTheme
import com.tbocr.app.utils.EyeProtectionDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : AppCompatActivity(), PipHost {

    private companion object {
        const val STATE_LAUNCH_DISPATCHED = "launch_dispatched"
    }

    private var launchDispatched = false

    @Volatile
    private var isUiReady: Boolean = false

    /** 系统栏是否由屏保收起 */
    private var systemBarsHiddenBySaver: Boolean = false

    @Volatile
    override var pipRequest: PipRequest? = null

    private val _isInPictureInPicture = MutableStateFlow(false)
    override val isInPictureInPicture: StateFlow<Boolean> = _isInPictureInPicture.asStateFlow()

    private val appSettingsManager: AppSettingsManager by inject()
    private val achievementRepository: AchievementRepository by inject()
    private val compositionService: MaaCompositionService by inject()
    private val screenSaverManager: ScreenSaverOverlayManager by inject()
    private val pallasDrunkState: PallasDrunkState by inject()
    private val backgroundTaskViewModel: BackgroundTaskViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !isUiReady }
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) {
            val eyeProtection = EyeProtectionDetector.detect(this@MainActivity)
            Timber.i("isEyeProtectionEnabled: %s (source=%s)", eyeProtection.isEnabled, eyeProtection.source)
        }
        launchDispatched = savedInstanceState?.getBoolean(STATE_LAUNCH_DISPATCHED) ?: false
        if (!launchDispatched) dispatchScheduledLaunchIntent(intent)
        enableEdgeToEdge()
        lifecycleScope.launch {
            (application as MaaApplication).awaitReady()
            initializeUi()
        }
    }

    private fun initializeUi() {
        // 定义表构建与匹配不占首帧
        lifecycleScope.launch(Dispatchers.Default) {
            achievementRepository.report {
                event = AchievementEvents.APP_LAUNCH
            }
        }
        doObserveKeepScreenOn()
        doObserveScreenSaverBars()
        window.decorView.viewTreeObserver.addOnPreDrawListener(object :
            ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                isUiReady = true
                window.decorView.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
        setContent {
            val themeMode by appSettingsManager.themeMode.collectAsStateWithLifecycle()
            val useSystemMonetColor by appSettingsManager.useSystemMonetColor.collectAsStateWithLifecycle()
            val fontSizeScale by appSettingsManager.fontSizeScale.collectAsStateWithLifecycle()

            val isInPip by _isInPictureInPicture.collectAsStateWithLifecycle()

            TbOcrTheme(themeMode = themeMode, useSystemMonetColor = useSystemMonetColor) {
                val baseDensity = LocalDensity.current
                val configuration = LocalConfiguration.current
                val effectiveScale = AppSettingsManager.resolveFontSizeScale(
                    stored = fontSizeScale,
                    smallestWidthDp = configuration.smallestScreenWidthDp,
                    fontScale = baseDensity.fontScale,
                )
                CompositionLocalProvider(
                    LocalIsInPip provides isInPip,
                    LocalDensity provides Density(
                        density = baseDensity.density * effectiveScale / 100f,
                        // 主界面保持系统 fontScale（与历史行为一致）
                        fontScale = baseDensity.fontScale,
                    )
                ) {
                    PallasDrunkHost(pallasDrunkState) {
                        ProvideInputFocusManager {
                            AppNavigation(backgroundTaskViewModel = backgroundTaskViewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchDispatched = false
        dispatchScheduledLaunchIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // 异步恢复语言和主题可能重建 Activity，已消费的请求不能再执行
        outState.putBoolean(STATE_LAUNCH_DISPATCHED, launchDispatched)
        super.onSaveInstanceState(outState)
    }

    /** API 28~30 没有 auto-enter，只能在这里手动进画中画 */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val request = pipRequest ?: return
        PipController.enterNow(this, request)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        _isInPictureInPicture.value = isInPictureInPictureMode
    }

    private fun dispatchScheduledLaunchIntent(intent: Intent?) {
        // SHOW：仅拉起 UI，禁止二次 execute（Service 已拥有 Schedule 管线）
        if (LaunchIntentMapper.isShowScheduleIntent(intent)) {
            return
        }
        lifecycleScope.launch {
            (application as MaaApplication).awaitReady()
            lifecycle.withStarted {
                LaunchIntentMapper.fromExternalIntent(this@MainActivity, intent)?.let { request ->
                    backgroundTaskViewModel.onExternalLaunch(request)
                    if (intent === this@MainActivity.intent) launchDispatched = true
                }
            }
        }
    }

    private fun doObserveKeepScreenOn() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    compositionService.state,
                    screenSaverManager.showing,
                    appSettingsManager.useHardwareScreenOff,
                    backgroundTaskViewModel.launchSession.map { it is LaunchSession.InFlight },
                ) { taskState, saverShowing, hwFeatureEnabled, launching ->
                    val taskActive = taskState == MaaExecutionState.STARTING
                            || taskState == MaaExecutionState.RUNNING
                            || taskState == MaaExecutionState.STOPPING
                    // 启用硬件熄屏功能后，前台时始终保持屏幕常亮、与任务状态解耦：
                    // 确保系统不会自动休眠/锁屏而中断正在运行的任务，硬件熄屏也无需再维护状态。
                    // 倒计时也常亮，免得待机触发后又锁屏
                    hwFeatureEnabled || taskActive || saverShowing || launching
                }.distinctUntilChanged().collect { keepOn ->
                    if (keepOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }
    }

    /** 屏保时收系统栏；标记挂 Activity 上以便 STOPPED 后仍能恢复 */
    private fun doObserveScreenSaverBars() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                screenSaverManager.showing.collect { showing ->
                    if (showing) {
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        systemBarsHiddenBySaver = true
                    } else if (systemBarsHiddenBySaver) {
                        controller.show(WindowInsetsCompat.Type.systemBars())
                        systemBarsHiddenBySaver = false
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
