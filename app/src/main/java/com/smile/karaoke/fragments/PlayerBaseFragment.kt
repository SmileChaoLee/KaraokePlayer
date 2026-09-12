package com.smile.karaoke.fragments

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.provider.Settings
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import androidx.core.view.get
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.smile.karaoke.R
import com.smile.karaoke.SmileAppBase
import com.smile.karaoke.constants.CommonConstants
import com.smile.karaoke.constants.MyPlayerConstants
import com.smile.karaoke.interfaces.PlaySongs
import com.smile.karaoke.models.MySingleton
import com.smile.karaoke.models.SongInfo
import com.smile.karaoke.presenters.PlayerBasePresenter
import com.smile.karaoke.presenters.PlayerBasePresenter.BasePresentView
import com.smile.karaoke.utilities.DatabaseUtil
import com.smile.karaoke.utilities.LogUtil
import com.smile.karaoke.utilities.CommonUtil
import com.smile.nativetemplates_models.GoogleAdMobNativeTemplate
import com.smile.smilelibraries.models.ExitAppTimer
import com.smile.smilelibraries.privacy_policy.PrivacyPolicyUtil
import com.smile.smilelibraries.show_banner_ads.SetBannerAdView
import com.smile.smilelibraries.utilities.ScreenUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import androidx.core.view.isVisible

@UnstableApi
abstract class PlayerBaseFragment : Fragment(),
    BasePresentView {

    companion object {
        private const val TAG: String = "PlayerBaseFragment"
        private const val PLAY_VIEW_TIMEOUT = 10000 //  10 seconds
        private const val MAX_BRIGHTNESS = 100
    }

    interface PlayBaseFragmentFunc {
        fun baseHidePlayerView()
        fun baseShowPlayerView()
    }

    lateinit var mPresenter: PlayerBasePresenter
    var screenSizeX = 0
    var screenSizeY = 0
    private var playSongs: PlaySongs? = null
    private var playBaseFragmentFunc: PlayBaseFragmentFunc? = null
    var fragmentView: View? = null
    var textFontSize = 0f
    private var fontScale = 0f
    var toastTextSize = 0f
    var playerViewLinearLayout: LinearLayout? = null
    var toolbarAudioLayout: LinearLayout? = null
    var adsMsgLayout: LinearLayout? = null
    private var supportToolbar // use customized ToolBar
            : Toolbar? = null
    private var actionMenuView: ActionMenuView? = null
    var audioControllerView: LinearLayout? = null
    private var brightAdjustSeekbar: AppCompatSeekBar? = null
    private var volumeImageButton: ImageButton? = null
    private var previousMediaImageButton: ImageButton? = null
    private var playMediaImageButton: ImageButton? = null
    private var replayMediaImageButton: ImageButton? = null
    private var stopMediaImageButton: ImageButton? = null
    private var nextMediaImageButton: ImageButton? = null
    private var heartImageButton: ImageButton? = null

    private var playingTimeTextView: TextView? = null
    private var playerDurationSeekbar: AppCompatSeekBar? = null
    private var durationTimeTextView: TextView? = null

    private var orientationImageButton: ImageButton? = null
    private var repeatImageButton: ImageButton? = null
    var switchToMusicImageButton: ImageButton? = null
    var switchToVocalImageButton: ImageButton? = null
    private var hideVideoImageButton: ImageButton? = null
    private var actionMenuImageButton: ImageButton? = null
    var audioChannelImageButton: ImageButton? = null
    var audioTrackImageButton: ImageButton? = null

    private var mediaRouteButton: MediaRouteButton? = null
    var castContext: CastContext? = null
    var deviceType: String = ScreenUtil.DEVICE_TYPE_PHONE

    private var bannerAdsLayout: LinearLayout? = null
    private var bannerLinearLayout: LinearLayout? = null
    private var myBannerAdView: SetBannerAdView? = null
    private var nativeTemplate: GoogleAdMobNativeTemplate? = null

    private var messageAreaLinearLayout: LinearLayout? = null
    private var bufferingStringTextView: TextView? = null
    private var animationText: Animation? = null
    private var nativeAdsFrameLayout: FrameLayout? = null
    private var nativeAdViewVisibility = 0
    private var nativeAdTemplateView: TemplateView? = null
    var mainMenu: Menu? = null
    var channelMenuItem: MenuItem? = null

    // submenu of file
    var softDecoderFirstMenuItem: MenuItem? = null
    private var autoPlayMenuItem: MenuItem? = null
    private var audioMenuItem: MenuItem? = null
    // submenu of audio
    private var audioTrackMenuItem: MenuItem? = null
    // submenu of channel
    private var leftChannelMenuItem: MenuItem? = null
    private var rightChannelMenuItem: MenuItem? = null
    private var stereoChannelMenuItem: MenuItem? = null
    private var oldMotionEventX = 0.0f
    private var oldMotionEventY = 0.0f
    private var currentAudioPosition = 0L
    private var orgOrientation = Configuration.ORIENTATION_PORTRAIT
    private var lastFocusView: View? = null
    private val focusHashSet: HashSet<View?> = HashSet()
    /**
     * this function will double the timeout PlayerView_Timeout = 5000 //  5 seconds
     */
    private val focusChangeListener = ViewTreeObserver.OnGlobalFocusChangeListener { oldFocus, newFocus ->
        if (newFocus != null) {
            // A child view just gained focus
            val unknowId = "Unknown ID"
            val viewIdName = try {
                resources.getResourceEntryName(newFocus.id)
            } catch (e: Exception) {
                unknowId
            }
            LogUtil.d(TAG,"focusChangeListener.View gained focus.$viewIdName (Type: ${newFocus.javaClass.simpleName})")
            if (focusHashSet.contains(newFocus)) {
                LogUtil.d(TAG,"focusChangeListener.focusHashSet contains newFocus")
                lastFocusView = newFocus
                showSupportToolbarAudioControlSetTimer()
            }
        }
    }

    private val controllerTimerHandler = Handler(Looper.getMainLooper())
    private val controllerTimerRunnable = Runnable {
        LogUtil.d(TAG, "controllerTimerRunnable")
        controllerTimerHandler.removeCallbacksAndMessages(null)
        mPresenter.playingParam.let {
            LogUtil.d(TAG, "controllerTimerRunnable.playingParam")
            if (supportToolbar?.visibility == View.VISIBLE) {
                // hide supportToolbar
                hideSupportToolbarAndAudioController()
            }
            if (it.currentPlaybackState != PlaybackStateCompat.STATE_PLAYING) {
                // there is no media playing or playing is finished
                showNativeAndHideBannerAd()
                setTimerToHideSupportAudioControl()   // reset the timer
            }
        }
    }

    abstract fun getPlayerPresenter(): PlayerBasePresenter?
    abstract fun setupMenuItems()
    abstract fun getPlayServiceIntent(): Intent?
    abstract fun onPlayServiceConnected(service: IBinder)
    abstract fun audioChannelButtonListener()
    abstract fun getFavDatabaseName(): String
    abstract fun obtainCastContext(): CastContext?
    abstract fun isCasting(): Boolean

    var mPlayServiceIntent: Intent? = null
    private fun startAndBindPlayService() {
        val logStr = "startAndBindPlayService"
        activity?.let {
            /*
            if (isServiceDestroyed) {
                LogUtil.d(TAG, "$logStr.startService()")
                it.startService(mPlayServiceIntent)
                isServiceDestroyed = false
            } else {
                LogUtil.d(TAG, "$logStr.PlayService already started")
            }
            */
            if (!isServiceBound) {
                val result: Boolean = it.bindService(mPlayServiceIntent!!,
                    connection, Context.BIND_AUTO_CREATE)
                LogUtil.d(TAG, "$logStr.isBound = $result")
            } else {
                LogUtil.d(TAG, "$logStr.PlayService already bound")
            }
        }
    }

    fun unbindAndStopPlayService() {
        val logStr = "unbindAndStopPlayService"
        activity?.let {
            if (isServiceBound) {
                LogUtil.d(TAG, "$logStr.unbindService()")
                it.unbindService(connection)
                // it.stopService(mPlayServiceIntent)
                isServiceBound = false
                isServiceDestroyed = true
            } else {
                LogUtil.d(TAG, "$logStr.PlayService is not bound")
            }
        }
    }

    private fun onPlayServiceDisconnected() {
        LogUtil.i(TAG, "onPlayServiceDisconnected")
        /*
        activity?.stopService(mPlayServiceIntent)
        isServiceDestroyed = true
        */
    }

    var isServiceBound: Boolean = false
    var isServiceDestroyed: Boolean = true
    val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            LogUtil.i(TAG, "onServiceConnected")
            onPlayServiceConnected(service)
            isServiceBound = true
            isServiceDestroyed = false
            LogUtil.d(TAG, "onServiceConnected.isAutoPlay = " +
                    "${mPresenter.playingParam.isAutoPlay}")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            LogUtil.i(TAG, "onServiceDisconnected")
            isServiceBound = false
            onPlayServiceDisconnected()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        LogUtil.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        MySingleton.clearSingleton()
        activity?.let {
            textFontSize = ScreenUtil.getPxTextFontSizeNeeded(activity)
            fontScale = ScreenUtil.getPxFontScale(activity)
            toastTextSize = textFontSize * 0.7f
            castContext = obtainCastContext()
            deviceType = ScreenUtil.getDeviceType(it)
            if (deviceType != ScreenUtil.DEVICE_TYPE_PHONE) {
                LogUtil.d(TAG, "onCreate.deviceType is not phone")
                val res = it.resources
                orgOrientation = res.configuration.orientation
                if (deviceType == ScreenUtil.DEVICE_TYPE_ANDROID_TV) {
                    // disable cast for ExoPlayer for Android TV
                    LogUtil.d(TAG, "onCreate.disable cast for Android TV")
                    castContext = null  // disable cast
                }
            }

            if (it is PlaySongs) playSongs = it
            if (it is PlayBaseFragmentFunc) playBaseFragmentFunc = it
            LogUtil.d(TAG, "onCreate.playBaseFragmentFunc = $playBaseFragmentFunc")
        }
        /*
        // keep the screen on all the time, added on 2021-02-18
        activity?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        */
        setHasOptionsMenu(true) // must have because it has menu
        getPlayerPresenter()?.let {
            mPresenter = it
        } ?: run {
            LogUtil.d(TAG, "onCreate.presenter is null so exit activity.")
            playSongs?.returnToPrevious(false)
            return
        }

        var isAutoPlay = false
        arguments?.let {
            LogUtil.d(TAG, "onCreate.arguments is not null")
            isAutoPlay = it.getBoolean(MyPlayerConstants.IS_AUTOPLAY_STATE,
                false)
        }

        // must be after super.onCreate(savedInstanceState)
        activity?.let {
            mPlayServiceIntent = getPlayServiceIntent()
            mPresenter.initializeVariables(savedInstanceState, isAutoPlay)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        LogUtil.i(TAG, "onCreateView")
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_player_base_view,
            container, false)

        // commented out because moving to onViewCreated()
        // Make the root view focusable
        // Allows it to receive focus when touched
        // view.isFocusableInTouchMode = true
        // view.isFocusable = true
        // view.requestFocus()
        return view
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        LogUtil.i(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        view.viewTreeObserver.addOnGlobalFocusChangeListener(focusChangeListener)

        fragmentView = view
        val screen = ScreenUtil.getScreenSize(activity)
        screenSizeX = screen.x
        screenSizeY = screen.y

        // Video player view
        view.let {
            playerViewLinearLayout = it.findViewById(R.id.playerViewLinearLayout)
            supportToolbar = it.findViewById(R.id.player_view_toolbar)
            supportToolbar?.visibility = View.VISIBLE
            toolbarAudioLayout = it.findViewById(R.id.toolbarAudioLayout)
            adsMsgLayout = it.findViewById(R.id.adsMsgLayout)

            audioControllerView = it.findViewById(R.id.audioControllerView)
            brightAdjustSeekbar = it.findViewById(R.id.brightAdjustSeekbar)
            focusHashSet.add(brightAdjustSeekbar)
            volumeImageButton = it.findViewById(R.id.volumeImageButton)
            focusHashSet.add(volumeImageButton)
            previousMediaImageButton = it.findViewById(R.id.previousMediaImageButton)
            focusHashSet.add(previousMediaImageButton)
            playMediaImageButton = it.findViewById(R.id.playMediaImageButton)
            focusHashSet.add(playMediaImageButton)
            replayMediaImageButton = it.findViewById(R.id.replayMediaImageButton)
            focusHashSet.add(replayMediaImageButton)
            stopMediaImageButton = it.findViewById(R.id.stopMediaImageButton)
            focusHashSet.add(stopMediaImageButton)
            nextMediaImageButton = it.findViewById(R.id.nextMediaImageButton)
            focusHashSet.add(nextMediaImageButton)
            heartImageButton = it.findViewById(R.id.heartImageButton)
            focusHashSet.add(heartImageButton)
            actionMenuImageButton = it.findViewById(R.id.actionMenuImageButton)
            focusHashSet.add(actionMenuImageButton)

            orientationImageButton = it.findViewById(R.id.orientationImageButton)
            focusHashSet.add(orientationImageButton)
            repeatImageButton = it.findViewById(R.id.repeatImageButton)
            focusHashSet.add(repeatImageButton)
            switchToMusicImageButton = it.findViewById(R.id.switchToMusicImageButton)
            focusHashSet.add(switchToMusicImageButton)
            switchToVocalImageButton = it.findViewById(R.id.switchToVocalImageButton)
            focusHashSet.add(switchToVocalImageButton)
            hideVideoImageButton = it.findViewById(R.id.hideVideoImageButton)
            focusHashSet.add(hideVideoImageButton)
            audioChannelImageButton = it.findViewById(R.id.audioChannelImageButton)
            focusHashSet.add(audioChannelImageButton)
            audioTrackImageButton = it.findViewById(R.id.audioTrackImageButton)
            focusHashSet.add(audioTrackImageButton)

            bannerLinearLayout = it.findViewById(R.id.bannerLinearLayout)

            // message area
            messageAreaLinearLayout = it.findViewById(R.id.message_area_LinearLayout)
            messageAreaLinearLayout?.visibility = View.GONE
            bufferingStringTextView = it.findViewById(R.id.bufferingStringTextView)
            ScreenUtil.resizeTextSize(bufferingStringTextView, textFontSize)

            val durationTextSize = textFontSize * 0.6f
            playingTimeTextView = it.findViewById(R.id.playingTimeTextView)
            playingTimeTextView?.text = "000:00"
            ScreenUtil.resizeTextSize(playingTimeTextView, durationTextSize)
            playerDurationSeekbar = it.findViewById(R.id.player_duration_seekbar)
            focusHashSet.add(playerDurationSeekbar)
            durationTimeTextView = it.findViewById(R.id.durationTimeTextView)
            durationTimeTextView?.text = "000:00"
            ScreenUtil.resizeTextSize(durationTimeTextView, durationTextSize)
            nativeAdsFrameLayout = it.findViewById(R.id.nativeAdsFrameLayout)
            nativeAdTemplateView = it.findViewById(R.id.nativeAdTemplateView)

            it.isFocusable = true
            it.isFocusableInTouchMode = true
            it.post { it.requestFocus() }
            it.setOnKeyListener {
                    _, keyCode, _ ->
                LogUtil.d(TAG, "fragmentView.setOnKeyListener.keyCode = $keyCode")
                if (playerViewLinearLayout?.visibility == View.VISIBLE) {
                    supportToolbar?.performClick()
                }
                return@setOnKeyListener false
            }
        }
        actionMenuView = supportToolbar?.findViewById(R.id.actionMenuViewLayout) // main menu

        animationText = AlphaAnimation(0.0f, 1.0f)
        animationText?.duration = 500
        animationText?.startOffset = 0
        animationText?.repeatMode = Animation.REVERSE
        animationText?.repeatCount = Animation.INFINITE

        bannerLinearLayout?.also {layoutIt ->
            layoutIt.visibility = View.VISIBLE // Show Banner Ad
            showBannerAd()
        }
        if (nativeAdsFrameLayout != null) nativeAdViewVisibility = view.visibility

        activity?.let { actIt ->
            (actIt as AppCompatActivity).apply {
                setSupportActionBar(supportToolbar)
                supportActionBar?.setDisplayShowTitleEnabled(false)
            }
            nativeTemplate = (actIt.application as SmileAppBase)
                .getNativeTemplate(actIt,
                    nativeAdsFrameLayout,
                    nativeAdTemplateView)
        }

        // must before setImageButtonStatus() and showNativeAndBannerAd
        mPresenter.playingParam.let {
            if (it.currentPlaybackState == PlaybackStateCompat.STATE_PLAYING) {
                playButtonOffPauseButtonOn()
            } else {
                playButtonOnPauseButtonOff()
            }
            if (it.isPlayerViewVisible) showPlayerView() else hidePlayerView()
        }

        setImageButtonStatus() // must before setButtonsPositionAndSize()
        activity?.let { act ->
            val res = act.resources
            setButtonsPositionAndSize(res.configuration)
        }
        setOnClickEvents()
        showNativeAndHideBannerAd()

        LogUtil.d(TAG, "onViewCreated is finished.")
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        LogUtil.i(TAG, "onCreateOptionsMenu")
        // Inflate the menu; this adds items to the action bar if it is present.
        // mainMenu = menu;
        // menu.clear() does not work for the issue of onCreateOptionsMenu being called multiple times
        mainMenu = actionMenuView?.menu
        mainMenu?.clear()    // to avoid the issue of onCreateOptionsMenu being called multiple times
        inflater.inflate(R.menu.menu_main, mainMenu)
        // submenu of file
        mainMenu?.let {
            softDecoderFirstMenuItem = it.findItem(R.id.softDecoderFirst)
            autoPlayMenuItem = it.findItem(R.id.autoPlay)
            audioMenuItem = it.findItem(R.id.audio)
            // submenu of audio
            audioTrackMenuItem = it.findItem(R.id.audioTrack)
            // submenu of channel
            channelMenuItem = it.findItem(R.id.channel)
            leftChannelMenuItem = it.findItem(R.id.leftChannel)
            rightChannelMenuItem = it.findItem(R.id.rightChannel)
            stereoChannelMenuItem = it.findItem(R.id.stereoChannel)
        }
        setMainMenu()
        // final Context wrapper = new ContextThemeWrapper(this, R.style.menu_text_style);
        // or
        supportToolbar?.popupTheme?.let {
            val wrapper: Context = ContextThemeWrapper(activity, it)
            ScreenUtil.resizeMenuTextIconSize(wrapper, mainMenu, fontScale)
        }

        return super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val playingParam = mPresenter.playingParam
        val currentChannelPlayed = playingParam.currentChannelPlayed
        if (item.hasSubMenu()) {
            item.subMenu?.clearHeader()
        }
        val id = item.itemId
        if (id == R.id.softDecoderFirst) {
            // setting if you use soft decoder
            playingParam.softDecoderFirst = !playingParam.softDecoderFirst
            softDecoderFirstMenuItem?.isChecked = playingParam.softDecoderFirst
            getPlayService()?.switchDecoder()
        } else if (id == R.id.autoPlay) {
            autoPlayMenuItem?.let {
                // print the original check status
                LogUtil.d(TAG, "autoPlayMenuItem?.isChecked = ${it.isChecked}")
                if (!it.isChecked) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val songs = getFavoriteSongs()
                        withContext(Dispatchers.Main) {
                            it.isChecked = mPresenter.setAutoPlayStatusAndAction(songs)
                        }
                    }
                } else {
                    mPresenter.stopAutoPlay()
                    it.isChecked = false
                }
            }
        } else if (id == R.id.privacyPolicy) {
            PrivacyPolicyUtil.startPrivacyPolicyActivity(
                activity,
                MyPlayerConstants.PrivacyPolicyActivityRequestCode
            )
        } else if (id == R.id.exit) {
            closeFragment()
        } else if (id == R.id.audioTrack) {
            // if there are audio tracks
            item.subMenu?.let {
                for (i in 0 until it.size) {
                    val mItem = it[i]
                    // audio track index start from 1 for user interface
                    if (i + 1 == playingParam.currentAudioTrackIndexPlayed) {
                        mItem.isCheckable = true
                        mItem.isChecked = true
                    } else {
                        mItem.isCheckable = false
                    }
                }
            }
        } else if (id == R.id.audioTrack1) {
            mPresenter.setAudioTrackAndChannel(1, currentChannelPlayed)
        } else if (id == R.id.audioTrack2) {
            mPresenter.setAudioTrackAndChannel(2, currentChannelPlayed)
        } else if (id == R.id.audioTrack3) {
            mPresenter.setAudioTrackAndChannel(3, currentChannelPlayed)
        } else if (id == R.id.audioTrack4) {
            mPresenter.setAudioTrackAndChannel(4, currentChannelPlayed)
        } else if (id == R.id.audioTrack5) {
            mPresenter.setAudioTrackAndChannel(5, currentChannelPlayed)
        } else if (id == R.id.audioTrack6) {
            mPresenter.setAudioTrackAndChannel(6, currentChannelPlayed)
        } else if (id == R.id.audioTrack7) {
            mPresenter.setAudioTrackAndChannel(7, currentChannelPlayed)
        } else if (id == R.id.audioTrack8) {
            mPresenter.setAudioTrackAndChannel(8, currentChannelPlayed)
        } else if (id == R.id.channel) {
            val mediaUri = mPresenter.mediaUri
            val numberOfAudioTracks = mPresenter.getNumberOfAudioTracks()
            if (mediaUri != null && Uri.EMPTY != mediaUri && numberOfAudioTracks > 0) {
                leftChannelMenuItem?.isEnabled = true
                rightChannelMenuItem?.isEnabled = true
                stereoChannelMenuItem?.isEnabled = true
                if (playingParam.preparedStatus != 0) {
                    if (currentChannelPlayed == CommonConstants.LEFT_CHANNEL) {
                        leftChannelMenuItem?.isCheckable = true
                        leftChannelMenuItem?.isChecked = true
                    } else {
                        leftChannelMenuItem?.isCheckable = false
                        leftChannelMenuItem?.isChecked = false
                    }
                    if (currentChannelPlayed == CommonConstants.RIGHT_CHANNEL) {
                        rightChannelMenuItem?.isCheckable = true
                        rightChannelMenuItem?.isChecked = true
                    } else {
                        rightChannelMenuItem?.isCheckable = false
                        rightChannelMenuItem?.isChecked = false
                    }
                    if (currentChannelPlayed == CommonConstants.STEREO) {
                        stereoChannelMenuItem?.isCheckable = true
                        stereoChannelMenuItem?.isChecked = true
                    } else {
                        stereoChannelMenuItem?.isCheckable = false
                        stereoChannelMenuItem?.isChecked = false
                    }
                } else {
                    leftChannelMenuItem?.isCheckable = false
                    leftChannelMenuItem?.isChecked = false
                    rightChannelMenuItem?.isCheckable = false
                    rightChannelMenuItem?.isChecked = false
                    stereoChannelMenuItem?.isCheckable = false
                    stereoChannelMenuItem?.isChecked = false
                }
            } else {
                leftChannelMenuItem?.isEnabled = false
                rightChannelMenuItem?.isEnabled = false
                stereoChannelMenuItem?.isEnabled = false
            }
        } else if (id == R.id.leftChannel) {
            mPresenter.playLeftChannel()
        } else if (id == R.id.rightChannel) {
            mPresenter.playRightChannel()
        } else if (id == R.id.stereoChannel) {
            mPresenter.playStereoChannel()
        } else if (id == R.id.smileApps) {
            playSongs?.showSmileAppsActivity()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        LogUtil.i(TAG, "onStart")
        super.onStart()
        mPresenter.playingParam.let {
            LogUtil.d(TAG, "onStart.preparedStatus = ${it.preparedStatus}")
            if (it.preparedStatus == 3) { // running in the background before
                // set to come back from background
                it.preparedStatus = 4
            }
        }
    }

    override fun onResume() {
        LogUtil.d(TAG, "onResume")
        super.onResume()
        myBannerAdView?.resume()
        activity?.let {
            val res = it.resources
            checkBannerAdByOrientation(res.configuration.orientation)
        }
        startAndBindPlayService()
        hideVideoImageButton?.post { hideVideoImageButton?.requestFocus() }
    }

    override fun onPause() {
        LogUtil.i(TAG, "onPause")
        super.onPause()
        myBannerAdView?.pause()
        bannerAdsLayout?.visibility = View.GONE
    }

    override fun onStop() {
        LogUtil.i(TAG, "onStop")
        super.onStop()
        mPresenter.playingParam.let {
            LogUtil.d(TAG, "onStop.isPlaySingleSong = ${it.isPlaySingleSong}")
            it.preparedStatus = 3 // running in background
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val logStr = "onConfigurationChanged"
        LogUtil.i(TAG, logStr)
        CommonUtil.closeMenu(mainMenu)
        activity?.let {actIt ->
            val screen = ScreenUtil.getScreenSize(actIt)
            screenSizeX = screen.x
            LogUtil.i(TAG, "$logStr.screenSizeX = $screenSizeX")
            screenSizeY = screen.y
            LogUtil.i(TAG, "$logStr.screenSizeY = $screenSizeY")
            myBannerAdView?.destroy()
            bannerLinearLayout?.also {layoutIt ->
                layoutIt.visibility = View.VISIBLE // Show Banner Ad
                showBannerAd()
            }
        }
        setOrientationImageButton(newConfig.orientation)
        setButtonsPositionAndSize(newConfig)
        checkBannerAdByOrientation(newConfig.orientation)
        super.onConfigurationChanged(newConfig)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        LogUtil.i(TAG, "onSaveInstanceState")
        mPresenter.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        LogUtil.i(TAG, "onDestroy")
        MySingleton.clearSingleton()
        // cancel the timer
        mPresenter.removeMsgFromDurationBarHandler()
        controllerTimerHandler.removeCallbacksAndMessages(null)
        myBannerAdView?.destroy()
        nativeTemplate?.release()
        unbindAndStopPlayService()
        fragmentView?.viewTreeObserver?.removeOnGlobalFocusChangeListener(focusChangeListener)
        super.onDestroy()
    }

    private fun checkBannerAdByOrientation(orientation: Int) {
        /*
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            bannerAdsLayout?.visibility = View.GONE
        } else {
            CommonUtil.setVisible(bannerAdsLayout, nativeAdViewVisibility)
        }
        */
        CommonUtil.setVisible(bannerAdsLayout, nativeAdViewVisibility)
    }

    fun onBackPressed() {
        LogUtil.d(TAG, "onBackPressed")
        val exitAppTimer = ExitAppTimer.getInstance(1000) // singleton class
        if (exitAppTimer.canExit()) {
            closeFragment()
        } else {
            exitAppTimer.start()
            ScreenUtil.showToast(activity, getString(R.string.backKeyToExitApp),
                toastTextSize, Toast.LENGTH_SHORT)
        }
    }

    fun setMainMenu() {
        LogUtil.i(TAG, "setMainMenu")
        softDecoderFirstMenuItem?.isVisible = true    // always visible
        // val isVisible = !mPresenter.playingParam.isPlaySingleSong // no more playing single song
        // val isVisible = true
        autoPlayMenuItem?.isVisible = true
        audioMenuItem?.isVisible = true
        audioTrackMenuItem?.isVisible = true
        // val channelMenuItem = mainMenu?.findItem(R.id.channel)
        channelMenuItem?.isVisible = true
        val privacyPolicyMenuItem = mainMenu?.findItem(R.id.privacyPolicy)
        privacyPolicyMenuItem?.isVisible = true
        setupMenuItems() // abstract method
    }

    private fun setMediaRouteButtonVisible() {
        LogUtil.i(TAG, "setMediaRouteButtonVisible")
        mediaRouteButton?.visibility =
            if (castContext != null)
            View.VISIBLE else View.GONE
    }

    private fun setMediaRouteButtonView(buttonMarginLeft: Int, imageButtonHeight: Int) {
        LogUtil.i(TAG, "setMediaRouteButtonView.castContext = $castContext")
        if (castContext == null) return
        try {
            mediaRouteButton = fragmentView?.findViewById(R.id.media_route_button)
            mediaRouteButton?.let {
                activity?.let { actIt ->
                    val ctx = actIt.applicationContext
                    CastButtonFactory.setUpMediaRouteButton(ctx, it)
                    setMediaRouteButtonVisible()
                }
            }
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.cast)
            val buttonDrawable: Drawable =
                bitmap.scale(imageButtonHeight, imageButtonHeight)
                    .toDrawable(resources)
            mediaRouteButton?.setRemoteIndicatorDrawable(buttonDrawable)
            val linearParam = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, imageButtonHeight)
            linearParam.setMargins(buttonMarginLeft, 0, 0, 0)
            mediaRouteButton?.layoutParams = linearParam
        } catch (ex: Exception) {
            LogUtil.e(TAG, "setMediaRouteButtonView.Exception", ex)
        }
    }

    private fun setButtonsPositionAndSize(config: Configuration) {
        LogUtil.i(TAG, "setButtonsPositionAndSize")
        var buttonMarginLeft = (50.0f * fontScale).toInt() // 60 pixels = 20dp on Nexus 5
        var buttonMarginLeft2 = buttonMarginLeft
        // val screenSize = ScreenUtil.getScreenSize(activity)
        // LogUtil.d(TAG, "screenSize.x = ${screenSize.x}, screenSize.y = ${screenSize.y}, buttonMarginLeft = $buttonMarginLeft")
        LogUtil.d(TAG, "screenSize.x = $screenSizeX, screenSize.y = $screenSizeX, buttonMarginLeft = $buttonMarginLeft")

        val audioControllerViewLP = audioControllerView?.layoutParams as ConstraintLayout.LayoutParams
        audioControllerViewLP.matchConstraintPercentHeight = 0.24f
        val emptyLinearLayout = fragmentView?.findViewById<LinearLayout>(R.id.emptyLinearLayout)
        val emptyLinearLayoutLP = emptyLinearLayout?.layoutParams as ConstraintLayout.LayoutParams
        emptyLinearLayoutLP.matchConstraintPercentHeight = 0.03f

        val barParams = brightAdjustSeekbar?.layoutParams
        barParams?.width = screenSizeX * 2 / 3

        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            buttonMarginLeft =
                (buttonMarginLeft.toFloat() * (screenSizeX.toFloat() / screenSizeY.toFloat())).toInt()
            audioControllerViewLP.matchConstraintPercentHeight = 0.40f
            emptyLinearLayoutLP.matchConstraintPercentHeight = 0.05f
            barParams?.width = screenSizeX / 2
        }

        if (buttonMarginLeft<0) buttonMarginLeft = 0
        LogUtil.d(TAG, "buttonMarginLeft = $buttonMarginLeft")
        val buttonNum = 8 // 8 buttons
        var imageButtonHeight = (textFontSize * 1.2f).toInt()
        LogUtil.d(TAG, "imageButtonHeight = $imageButtonHeight")
        // added to avoid crashing on some devices
        if (imageButtonHeight <= 0 ) imageButtonHeight = (24.0f * 1.2f).toInt()
        //
        val maxWidth = buttonNum * imageButtonHeight + (buttonNum - 1) * buttonMarginLeft
        if (maxWidth > screenSizeX) {
            LogUtil.d(TAG, "maxWidth > screenSize.x")
            // greater than the width of screen
            buttonMarginLeft = (screenSizeX - 10 - buttonNum * imageButtonHeight) / (buttonNum-1)
        }
        if (buttonMarginLeft<0) buttonMarginLeft = 0
        LogUtil.d(TAG, "buttonMarginLeft = $buttonMarginLeft")
        val buttonNum2 = 8
        val maxWidth2 = buttonNum2 * imageButtonHeight + (buttonNum2 - 1) * buttonMarginLeft2
        if (maxWidth2 > screenSizeX) {
            // greater than the width of screen
            buttonMarginLeft2 = (screenSizeX - 10 - buttonNum2 * imageButtonHeight) / (buttonNum2 - 1)
        }
        if (buttonMarginLeft2<0) buttonMarginLeft2 = 0
        LogUtil.d(TAG, "buttonMarginLeft2 = $buttonMarginLeft2")

        val linearParam = LinearLayout.LayoutParams(imageButtonHeight, imageButtonHeight)
        linearParam.setMargins(0, 0, 0, 0)
        volumeImageButton?.layoutParams = linearParam

        linearParam.setMargins(buttonMarginLeft, 0, 0, 0)
        previousMediaImageButton?.layoutParams = linearParam
        playMediaImageButton?.layoutParams = linearParam
        replayMediaImageButton?.layoutParams = linearParam
        stopMediaImageButton?.layoutParams = linearParam
        nextMediaImageButton?.layoutParams = linearParam
        heartImageButton?.layoutParams = linearParam
        actionMenuView?.layoutParams = linearParam

        val tempBitmap = BitmapFactory.decodeResource(resources, R.drawable.circle_and_three_dots)
        val iconDrawable: Drawable =
            tempBitmap.scale(imageButtonHeight, imageButtonHeight)
                .toDrawable(resources)
        actionMenuView?.overflowIcon = iconDrawable // set icon of three dots for ActionMenuView
        actionMenuImageButton?.layoutParams = linearParam

        linearParam.setMargins(0, 0, 0, 0)
        orientationImageButton?.apply {
            layoutParams = linearParam
            visibility = if (deviceType == ScreenUtil.DEVICE_TYPE_PHONE)
                View.VISIBLE else View.GONE
        }

        linearParam.setMargins(buttonMarginLeft2, 0, 0, 0)
        repeatImageButton?.layoutParams = linearParam
        switchToMusicImageButton?.layoutParams = linearParam
        switchToVocalImageButton?.layoutParams = linearParam
        hideVideoImageButton?.layoutParams = linearParam
        audioChannelImageButton?.layoutParams = linearParam
        audioTrackImageButton?.layoutParams = linearParam

        setMediaRouteButtonView(buttonMarginLeft2, imageButtonHeight)

        supportToolbar?.layoutParams?.height = previousMediaImageButton?.layoutParams?.height!!
        bannerAdsLayout = fragmentView?.findViewById(R.id.bannerAdsLayout)
        val bannerAdsLayoutLP = bannerAdsLayout?.layoutParams as ConstraintLayout.LayoutParams
        val nativeAdLayout: FrameLayout? = fragmentView?.findViewById(R.id.nativeAdLayout)
        val nativeAdLayoutLP = nativeAdLayout?.layoutParams as ConstraintLayout.LayoutParams
        val bannerHeightPercent = bannerAdsLayoutLP.matchConstraintPercentHeight
        LogUtil.d(TAG,"bannerHeightPercent = $bannerHeightPercent")
        val heightPercent = 1.0f - bannerHeightPercent - imageButtonHeight * 3.30f / screenSizeY
        LogUtil.d(TAG, "heightPercent = $heightPercent")
        nativeAdLayoutLP.matchConstraintPercentHeight = (heightPercent * 100.0f).toInt() / 100.0f
        LogUtil.d(TAG, "nativeAdLayoutLP.matchConstraintPercentHeight = " +
                nativeAdLayoutLP.matchConstraintPercentHeight)

        // setting the width and the margins for nativeAdTemplateView
        val layoutParams = nativeAdTemplateView?.layoutParams as MarginLayoutParams
        // 6 buttons and 5 gaps
        layoutParams.width = imageButtonHeight * 6 + buttonMarginLeft * 5
        layoutParams.setMargins(0, 0, 0, 0)
        nativeAdTemplateView?.layoutParams = layoutParams
        //
    }

    private fun closeFragment() {
        val pm = mPresenter.playingParam
        LogUtil.i(TAG, "closeFragment.isPlaySingleSong = " + pm.isPlaySingleSong)
        playSongs?.returnToPrevious(pm.isPlaySingleSong)
    }

    private fun showBannerAd() {
        LogUtil.d(TAG, "showBannerAd")
        activity?.let { actIt ->
            myBannerAdView?.destroy()
            myBannerAdView = (actIt.application as SmileAppBase)
                .showBannerAd(actIt, bannerLinearLayout)
            LogUtil.d(TAG, "showBannerAd.myBannerAdView = $myBannerAdView")
            myBannerAdView?.showBannerAdView(0) // AdMob first
        }
    }

    fun showSupportToolbarAudioControlSetTimer() {
        LogUtil.d(TAG, "showSupportToolbarAudioControlSetTimer")
        showSupportToolbarAudioControl()
        setTimerToHideSupportAudioControl()   // reset the timer
    }

    private fun showSupportToolbarAudioControl() {
        LogUtil.d(TAG, "showSupportToolbarAudioControl")
        // bannerLinearLayout?.visibility = View.GONE
        bannerAdsLayout?.visibility = View.GONE
        hideNativeAd()
        supportToolbar?.visibility = View.VISIBLE
        nativeAdsFrameLayout?.visibility = nativeAdViewVisibility
        audioControllerView?.visibility = View.VISIBLE
    }

    private fun hideSupportToolbarAndAudioController() {
        LogUtil.d(TAG, "hideSupportToolbarAndAudioController.context = $context")
        if (context == null) return
        if (playerViewLinearLayout?.visibility == View.VISIBLE) {
            supportToolbar?.visibility = View.GONE
            audioControllerView?.visibility = View.GONE
            nativeAdsFrameLayout?.visibility = nativeAdViewVisibility
            CommonUtil.closeMenu(mainMenu)
            activity?.let { actIt ->
                val res = actIt.resources
                checkBannerAdByOrientation(res.configuration.orientation)
            }
        }
        fragmentView?.let { it.post { it.requestFocus() } }
    }

    open fun switchToMusicVisibility(): Int {
        return View.VISIBLE
    }

    open fun switchToVocalVisibility(): Int {
        return View.VISIBLE
    }

    open fun audioChannelVisibility(): Int {
        return View.VISIBLE
    }

    private fun audioTrackListener() {
        val logStr = "audioTrackImageButton"
        audioTrackImageButton?.setOnClickListener {
            mPresenter.playingParam.apply {
                LogUtil.d(TAG, "$logStr.currentAudioTrackIndexPlayed = $currentAudioTrackIndexPlayed")
                currentAudioTrackIndexPlayed++
                LogUtil.d(TAG, "$logStr.currentAudioTrackIndexPlayed = $currentAudioTrackIndexPlayed")
                val numAudioTracks = mPresenter.getNumberOfAudioTracks()
                LogUtil.d(TAG, "$logStr.numAudioTracks = $numAudioTracks")
                if (currentAudioTrackIndexPlayed > numAudioTracks)
                    currentAudioTrackIndexPlayed = 1
                val str: String? =
                    when (currentAudioTrackIndexPlayed) {
                        1 -> activity?.getString(R.string.audioTrack1String)
                        2 -> activity?.getString(R.string.audioTrack2String)
                        3 -> activity?.getString(R.string.audioTrack3String)
                        4 -> activity?.getString(R.string.audioTrack4String)
                        5 -> activity?.getString(R.string.audioTrack5String)
                        6 -> activity?.getString(R.string.audioTrack6String)
                        7 -> activity?.getString(R.string.audioTrack7String)
                        8 -> activity?.getString(R.string.audioTrack8String)
                        else -> activity?.getString(R.string.unknown)
                    }
                ScreenUtil.showToast(activity, str, toastTextSize,
                    Toast.LENGTH_SHORT)
                mPresenter.setAudioTrackAndChannel(currentAudioTrackIndexPlayed,
                    currentChannelPlayed)
            }
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = audioTrackImageButton
            fragmentView?.requestFocus()
        }
    }

    private fun onBrightSeekBarProgressChanged(progress: Int, fromUser: Boolean) {
        val logStr = "onBrightSeekBarProgressChanged"
        LogUtil.d(TAG, "$logStr.progress = $progress, fromUser = $fromUser")

        // Protect against zero max (use 100 as fallback)
        // val max = brightAdjustSeekbar?.max?.takeIf { it > 0 } ?: MAX_BRIGHTNESS
        val rawBrightness = progress.toFloat() / MAX_BRIGHTNESS.toFloat() // 0.0 .. 1.0
        // avoid a completely-black screen accidentally; allow 0.0 if you prefer
        val brightness = when {
            rawBrightness <= 0f -> 0.01f
            rawBrightness > 1f -> 1.0f
            else -> rawBrightness
        }

        // Apply to this window only (no system permission required)
        activity?.window?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = brightness // -1 = system default, 0..1 = explicit brightness
            win.attributes = lp
            LogUtil.d(TAG, "$logStr.applied screenBrightness = $brightness (max = $MAX_BRIGHTNESS)")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnClickEvents() {
        brightAdjustSeekbar?.let {
            it.max = MAX_BRIGHTNESS
            val currentBrightnessFloat: Float = run {
                // Try per-window brightness first
                val lp = activity?.window?.attributes
                val windowBrightness = lp?.screenBrightness
                    ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                if (windowBrightness >= 0f) {
                    // explicit per-window brightness (0..1)
                    windowBrightness.coerceIn(0f, 1f)
                } else {
                    // window uses system default -> read system brightness (0..255)
                    try {
                        val sysBrightnessInt = Settings.System.getInt(requireContext().contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS)
                        (sysBrightnessInt.coerceIn(0, 255) / 255.0f).coerceIn(0f, 1f)
                    } catch (ex: Exception) {
                        // fallback to full bright on failure
                        1.0f
                    }
                }
            }
            it.progress = (currentBrightnessFloat * it.max).toInt()
            it.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    LogUtil.d(TAG, "brightAdjustSeekbar.onProgressChanged.progress = $progress")
                    // update the duration on controller UI
                    onBrightSeekBarProgressChanged(progress, fromUser)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        }
        volumeImageButton?.setOnClickListener {
            LogUtil.d(TAG,"volumeImageButton.onClick")
            mPresenter.playingParam.let { pIt->
                LogUtil.d(TAG,"volumeImageButton.onClick.currentVolume = ${pIt.currentVolume}")
                if (pIt.currentVolume > 0.0f) {
                    pIt.currentVolume = 0.0f
                    volumeImageButton?.setImageResource(R.drawable.volume)
                } else {
                    pIt.currentVolume = 1.0f
                    volumeImageButton?.setImageResource(R.drawable.non_volume)
                }
                getPlayService()?.setAudioVolume(pIt.currentVolume)
            }
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = volumeImageButton
            fragmentView?.post { fragmentView?.requestFocus() }
        }
        previousMediaImageButton?.setOnClickListener {
            mPresenter.playPreviousSong()
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = previousMediaImageButton
            fragmentView?.post { fragmentView?.requestFocus() }
        }
        playMediaImageButton?.setOnClickListener {
            if (mPresenter.playingParam.currentPlaybackState ==
                PlaybackStateCompat.STATE_PLAYING) {
                mPresenter.pausePlay()
            } else {
                mPresenter.startPlay()
            }
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = playMediaImageButton
            fragmentView?.post { fragmentView?.requestFocus() }
        }
        replayMediaImageButton?.setOnClickListener {
            mPresenter.replayMedia()
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = replayMediaImageButton
            fragmentView?.post { fragmentView?.requestFocus() }
        }
        stopMediaImageButton?.setOnClickListener {
            mPresenter.stopPlay(MyPlayerConstants.STOPPED_BY_USER)
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = stopMediaImageButton
            fragmentView?.post { fragmentView?.requestFocus() }
        }
        nextMediaImageButton?.setOnClickListener {
            mPresenter.playNextSong()
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = nextMediaImageButton
            fragmentView?.post { fragmentView?.requestFocus() }
        }
        heartImageButton?.setOnClickListener {
            // add this media file to my favorite
            val index = mPresenter.playingParam.currentSongIndex
            LogUtil.d(TAG,"heartImageButton.onClick.currentSongIndex = $index")
            if (index>=0 && MySingleton.orderedSongs.size>index) {
                activity?.let { actIt ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        if (DatabaseUtil.addSongToFavorites(actIt, getFavDatabaseName(),
                            MySingleton.orderedSongs[index])) {
                            withContext(Dispatchers.Main) {
                                ScreenUtil.showToast(
                                    actIt,
                                    getString(R.string.add_to_favorites),
                                    textFontSize, Toast.LENGTH_SHORT)
                            }
                        }
                    }
                }
            }
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = heartImageButton
            fragmentView?.post { fragmentView?.requestFocus() }
        }

        orientationImageButton?.setOnClickListener {
            activity?.let { actIt ->
                val res = actIt.resources
                val org = res.configuration.orientation
                val orientation = if (org == Configuration.ORIENTATION_PORTRAIT)
                    Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
                LogUtil.d(TAG, "orientationImageButton.onClick.orientation = $orientation")
                CommonUtil.setScreenOrientation(actIt, orientation)
                CommonUtil.disableButtonForSometime(it)
                lastFocusView = orientationImageButton
                fragmentView?.post { fragmentView?.requestFocus() }
            }
        }
        repeatImageButton?.setOnClickListener {
            mPresenter.setRepeatSongStatus()
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = repeatImageButton
            fragmentView?.post { fragmentView?.requestFocus() }
        }
        switchToMusicImageButton?.setOnClickListener {
            mPresenter.switchAudioToMusic()
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = switchToMusicImageButton
            fragmentView?.post { fragmentView?.requestFocus() }
        }
        switchToVocalImageButton?.setOnClickListener {
            mPresenter.switchAudioToVocal()
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = switchToVocalImageButton
            fragmentView?.post { fragmentView?.requestFocus() }
        }
        hideVideoImageButton?.setOnClickListener {
            LogUtil.d(TAG, "hideVideoImageButton.setOnClickListener")
            if (playerViewLinearLayout?.visibility==View.VISIBLE) {
                hidePlayerView()
            } else {
                showPlayerView()
            }
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = hideVideoImageButton
            fragmentView?.post { fragmentView?.requestFocus() }
        }

        audioChannelImageButton?.setOnClickListener {
            audioChannelButtonListener()
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = audioChannelImageButton
            fragmentView?.post { fragmentView?.requestFocus() }
        }
        audioTrackListener()

        actionMenuImageButton?.setOnClickListener {
            LogUtil.d(TAG, "actionMenuImageButton.setOnClickListener")
            actionMenuView?.showOverflowMenu()
            softDecoderFirstMenuItem?.isChecked = mPresenter.playingParam.softDecoderFirst
            autoPlayMenuItem?.isChecked = mPresenter.playingParam.isAutoPlay
            setTimerToHideSupportAudioControl()   // reset the timer
            CommonUtil.disableButtonForSometime(it)
            lastFocusView = actionMenuImageButton
            fragmentView?.post { fragmentView?.requestFocus() }
        }
        actionMenuView?.setOnMenuItemClickListener { item: MenuItem? ->
            item?.let { itemIt->
                onOptionsItemSelected(itemIt)
                // lastFocusView = actionMenuView
                fragmentView?.post { fragmentView?.requestFocus() }
            } == true
        }
        playerDurationSeekbar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                LogUtil.d(TAG, "playerDurationSeekbar.onProgressChanged.progress = $progress")
                // update the duration on controller UI
                onDurationSeekBarProgressChanged(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        supportToolbar?.let {
            it.setOnClickListener { _: View ->
                LogUtil.d(TAG, "supportToolbar.onClick")
                if (it.isVisible) {
                    hideSupportToolbarAndAudioController()
                } else {
                    showSupportToolbarAudioControlSetTimer()
                    lastFocusView?.let { last ->
                        last.post { last.requestFocus() }
                    } ?: run {
                        actionMenuImageButton?.post { actionMenuImageButton?.requestFocus() }
                        lastFocusView = actionMenuImageButton
                    }
                }
            }
        }
        playerViewLinearLayout?.let { it->
            it.setOnClickListener {
                LogUtil.d(TAG, "playerViewLinearLayout.onClick()")
                if (playerViewLinearLayout?.visibility == View.VISIBLE) {
                    supportToolbar?.performClick()
                }
            }
            it.setOnTouchListener { _, motionEvent ->
                val playService = getPlayService() ?: return@setOnTouchListener false
                val posX = motionEvent.x    // keep changing if there is new motionEvent
                val posY = motionEvent.y    // keep changing if there is new motionEvent
                LogUtil.d(TAG, "setOnTouchListener.motionEvent.x = $posX")
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        LogUtil.d(TAG, "setOnTouchListener.ACTION_DOWN.posX = $posX")
                        oldMotionEventX = posX
                        oldMotionEventY = posY
                        currentAudioPosition = mPresenter.playingParam.currentAudioPosition
                        mPresenter.removeMsgFromDurationBarHandler()
                    }
                    MotionEvent.ACTION_UP -> {
                        LogUtil.d(TAG, "setOnTouchListener.ACTION_UP")
                        mPresenter.startDurationBarHandler()
                    }
                    MotionEvent.ACTION_MOVE -> run {
                        if (!playService.isSeekable()) return@run
                        mPresenter.playingParam.apply {
                            if (currentPlaybackState != PlaybackStateCompat.STATE_PLAYING
                                        && currentPlaybackState != PlaybackStateCompat.STATE_PAUSED) {
                                LogUtil.d(TAG, "setOnTouchListener.ACTION_MOVE.not playing and not paused")
                                return@run
                            }
                        }
                        if (posX <= 0 || posX >= screenSizeX) {
                            LogUtil.d(TAG, "setOnTouchListener.ACTION_MOVE.out of the screen size")
                            return@run
                        }
                        if (posY <= 0 || posY >= screenSizeY) {
                            LogUtil.d(TAG, "setOnTouchListener.ACTION_MOVE.out of the screen size")
                            return@run
                        }
                        LogUtil.d(TAG, "setOnTouchListener.ACTION_MOVE.posX = $posX")
                        LogUtil.d(TAG, "setOnTouchListener.ACTION_MOVE.oldMotionEventX = $oldMotionEventX")
                        val disX = posX - oldMotionEventX
                        val disY = posY - oldMotionEventY
                        LogUtil.d(TAG, "setOnTouchListener.ACTION_MOVE.screenSizeX = $screenSizeX")
                        LogUtil.d(TAG, "setOnTouchListener.ACTION_MOVE.disX = $disX")
                        LogUtil.d(TAG, "setOnTouchListener.ACTION_MOVE.disY = $disY")
                        if (abs(disX) <= 20.0f) {
                            LogUtil.d(TAG, "setOnTouchListener.ACTION_MOVE.disX is too small")
                            return@run
                        }
                        if (abs(disY) >= 30.0f) {
                            LogUtil.d(TAG, "setOnTouchListener.ACTION_MOVE.disY is too big")
                            return@run
                        }
                        val duration = playService.getMediaDuration()
                        LogUtil.d(TAG, "setOnTouchListener.ACTION_MOVE.duration = $duration")
                        if (duration > 0) {
                            // val currentTime = playService.getCurrentPosition()
                            var progress = currentAudioPosition + ((disX / screenSizeX) * duration).toInt()
                            if (progress < 0) {
                                progress = 0
                            } else if (progress > duration - 2000) {
                                // less than 2 seconds before the end
                                progress = duration - 2000
                            }
                            LogUtil.d(TAG, "setOnTouchListener.ACTION_MOVE.currentAudioPosition = $currentAudioPosition")
                            LogUtil.d(TAG, "setOnTouchListener.ACTION_MOVE.progress = $progress")
                            // onDurationSeekBarProgressChanged(progress.toInt(),
                            //     true)
                            playService.setPlayerTime(progress)
                            updatePlayerDurationSeekbarProgress(progress.toInt())
                            showSupportToolbarAudioControl() // show the player buttons
                        }
                    }
                    MotionEvent.ACTION_OUTSIDE -> {
                        LogUtil.d(TAG, "setOnTouchListener.ACTION_OUTSIDE")
                    }
                    else -> {
                        LogUtil.d(TAG, "setOnTouchListener.else")
                    }
                }
                false
            }
        }
    }

    // implementing PlayerBasePresenter.BasePresentView
    override fun setImageButtonStatus() {
        LogUtil.i(TAG, "setImageButtonStatus")
        val pm = mPresenter.playingParam
        if (pm.currentVolume > 0.0f) volumeImageButton?.setImageResource(R.drawable.non_volume)
        else volumeImageButton?.setImageResource(R.drawable.volume)
        switchToMusicImageButton?.visibility = switchToMusicVisibility()
        switchToVocalImageButton?.visibility = switchToVocalVisibility()
        audioChannelImageButton?.visibility = audioChannelVisibility()
        activity?.let { actIt ->
            val res = actIt.resources
            setOrientationImageButton(res.configuration.orientation)
        }
        // repeatImageButton
        when (pm.repeatStatus) {
            MyPlayerConstants.NoRepeatPlaying -> {
                // no repeat but show symbol of repeat all song with transparent background
                repeatImageButton?.setImageResource(R.drawable.repeat_no)
            }
            MyPlayerConstants.RepeatOneSong ->                 // repeat one song
                repeatImageButton?.setImageResource(R.drawable.repeat_one)
            MyPlayerConstants.RepeatAllSongs ->                 // repeat all song list
                repeatImageButton?.setImageResource(R.drawable.repeat_all)
        }
        repeatImageButton?.visibility = if (pm.isPlaySingleSong) View.GONE else View.VISIBLE

        hideVideoImageButton?.apply {
            setImageResource(if (playerViewLinearLayout?.visibility==View.VISIBLE) R.drawable.hide_video
                else R.drawable.show_video)
            visibility = if (pm.isPlaySingleSong) View.GONE else View.VISIBLE
        }
    }

    override fun playButtonOnPauseButtonOff() {
        playMediaImageButton?.setImageResource(R.drawable.play_media_button_image)
    }

    override fun playButtonOffPauseButtonOn() {
        playMediaImageButton?.setImageResource(R.drawable.pause_media_button_image)
    }

    override fun initPlayerDurationSeekbar(duration: Float) {
        LogUtil.d(TAG, "initPlayerDurationSeekbar")
        playerDurationSeekbar?.progress = 0
        updateDurationTextView(duration)
    }

    override fun updateDurationTextView(duration: Float) {
        LogUtil.d(TAG, "updateDurationTextView")
        var durationTmp = duration
        playerDurationSeekbar?.max = durationTmp.toInt()
        durationTmp /= 1000.0f // seconds
        val minutes = (durationTmp / 60.0f).toInt() // minutes
        val seconds = durationTmp.toInt() - minutes * 60
        val durationString = String.format(Locale.ENGLISH,"%3d:%02d",
            minutes, seconds)
        durationTimeTextView?.text = durationString
    }

    override fun onDurationSeekBarProgressChanged(progress: Int, fromUser: Boolean) {
        val msgStr = "onDurationSeekBarProgressChanged"
        LogUtil.d(TAG, msgStr)
        val playService = getPlayService() ?: return
        LogUtil.d(TAG, "$msgStr.progress = $progress")
        val positionTime = progress / 1000.0f // seconds
        val minutes = (positionTime / 60.0f).toInt() // minutes
        val seconds = positionTime.toInt() - (minutes * 60)
        val playingTimeString = String.format(
            Locale.ENGLISH,
            "%3d:%02d", minutes, seconds
        )

        playingTimeTextView?.text = playingTimeString
        mPresenter.playingParam.currentAudioPosition = progress.toLong()
        LogUtil.d(TAG, "$msgStr.fromUser = $fromUser")
        if (fromUser) {
            val isSeekable = playService.isSeekable()
            LogUtil.d(TAG, "$msgStr.isSeekable = $isSeekable")
            if (isSeekable) {
                LogUtil.d(TAG, "$msgStr.playService.setPlayerTime()")
                playService.setPlayerTime(progress.toLong())
            }
        }
    }

    override fun updatePlayerDurationSeekbarProgress(progress: Int) {
        val msgString = "updatePlayerDurationSeekbarProgress"
        LogUtil.d(TAG, "$msgString.progress = $progress")
        playerDurationSeekbar?.progress = progress
    }

    override fun updateVolumeSeekBarProgress() {
        // volumeSeekBar?.setProgressAndThumb(mPresenter.currentProgressForVolumeSeekBar)
    }

    override fun showNativeAndHideBannerAd(): Boolean {
        val msgStr = "showNativeAndHideBannerAd"
        val playService = getPlayService() ?: return false
        if (playerViewLinearLayout?.visibility == View.VISIBLE) {
            LogUtil.d(TAG, "${msgStr}.View.VISIBLE")
            val numVideoTracks =  mPresenter.numberOfVideoTracks
            LogUtil.d(TAG, "${msgStr}.numVideoTracks = $numVideoTracks")
            mPresenter.playingParam.let {
                LogUtil.d(TAG, "${msgStr}.playbackState = ${it.currentPlaybackState}")
                if (it.currentPlaybackState != PlaybackStateCompat.STATE_PLAYING
                    || numVideoTracks == 0 || isCasting()) {
                    // Not playing, No video tracks, or casting session is available
                    nativeAdViewVisibility = View.VISIBLE
                    bannerAdsLayout?.visibility = View.GONE // hide the banner ad
                    nativeTemplate?.showNativeAd()
                } else {
                    hideNativeAd()
                    activity?.let { actIt ->
                        val res = actIt.resources
                        checkBannerAdByOrientation(res.configuration.orientation)
                    }
                }
            }
        } else {
            LogUtil.d(TAG, "${msgStr}.View.INVISIBLE")
            // show the banner ad if in the right place
            activity?.let { act ->
                val res = act.resources
                checkBannerAdByOrientation(res.configuration.orientation)
            }
        }

        return nativeTemplate != null
    }

    override fun hideNativeAd() {
        LogUtil.d(TAG, "hideNativeAd")
        nativeAdViewVisibility = View.GONE
        nativeTemplate?.hideNativeAd()
    }

    override fun showBufferingMessage() {
        LogUtil.d(TAG, "showBufferingMessage")
        messageAreaLinearLayout?.visibility = View.VISIBLE
        bufferingStringTextView?.startAnimation(animationText)
    }

    override fun dismissBufferingMessage() {
        LogUtil.d(TAG, "dismissBufferingMessage")
        animationText?.cancel()
        messageAreaLinearLayout?.visibility = View.GONE
    }

    override fun buildAudioTrackMenuItem(audioTrackNumber: Int) {
        LogUtil.d(TAG, "buildAudioTrackMenuItem.audioTrackNumber = $audioTrackNumber")
        // build R.id.audioTrack submenu
        audioTrackMenuItem?.subMenu?.let {
            var index = 0
            while (index < audioTrackNumber) {
                // audio track index start from 1 for user interface
                it[index].isVisible = true
                index++
            }
            for (j in index until it.size) {
                it[j].isVisible = false
            }
        }
    }

    override fun setTimerToHideSupportAudioControl() {
        LogUtil.d(TAG, "setTimerToHideSupportAndAudioController")
        if (playerViewLinearLayout?.visibility == View.VISIBLE) {
            controllerTimerHandler.removeCallbacksAndMessages(null)
            // 10 seconds
            controllerTimerHandler.postDelayed(controllerTimerRunnable,
                PLAY_VIEW_TIMEOUT.toLong())
        }
    }

    override fun showMusicAndVocalIsNotSet() {
        LogUtil.d(TAG, "showMusicAndVocalIsNotSet")
        ScreenUtil.showToast(activity, getString(R.string.musicAndVocalNotSet),
            toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT
        )
    }

    override fun hidePlayerView() {
        LogUtil.d(TAG, "hidePlayerView")
        playerViewLinearLayout?.visibility = View.INVISIBLE
        hideNativeAd()
        setImageButtonStatus()
        // must be after statement of playerViewLinearLayout.visibility = View.INVISIBLE
        controllerTimerHandler.removeCallbacksAndMessages(null) // cancel the timer
        playBaseFragmentFunc?.baseHidePlayerView()
        mPresenter.playingParam.isPlayerViewVisible = false
        if (deviceType == ScreenUtil.DEVICE_TYPE_PHONE) {
            activity?.let { actIt ->
                CommonUtil.setScreenOrientation(actIt, Configuration.ORIENTATION_PORTRAIT)
            }
        }
    }

    override fun showPlayerView() {
        LogUtil.i(TAG, "showPlayerView")
        playerViewLinearLayout?.visibility = View.VISIBLE
        mPresenter.run {
            if ( (playingParam.currentPlaybackState != PlaybackStateCompat.STATE_PLAYING) ||
                (numberOfVideoTracks == 0) ) {
                // not playing then show ads or only music is being played, show native ads
                showNativeAndHideBannerAd()
            }
        }
        setImageButtonStatus()
        // must be after statement of playerViewLinearLayout.visibility = View.VISIBLE
        setTimerToHideSupportAudioControl()   // reset the timer
        playBaseFragmentFunc?.baseShowPlayerView()
        mPresenter.playingParam.isPlayerViewVisible = true
        if (deviceType == ScreenUtil.DEVICE_TYPE_PHONE) {
            LogUtil.d(TAG, "showPlayerView.orgOrientation = $orgOrientation")
            activity?.let { actIt ->
                 CommonUtil.setScreenOrientation(actIt, orgOrientation)
            }
        }
        LogUtil.d(TAG, "showPlayerView.fragmentView?.requestFocus()")
        fragmentView?.post {
            fragmentView?.requestFocus()
            hideVideoImageButton?.post { hideVideoImageButton?.requestFocus() }
        }
    }

    override fun showToastNoFilesSelected() {
        activity?.let {
            ScreenUtil.showToast(it,
                it.getString(R.string.noFilesSelectedString),
                toastTextSize, ScreenUtil.FontSize_Pixel_Type,
                Toast.LENGTH_SHORT
            )
        }
    }

    override fun showToastNoPrevious() {
        activity?.let {
            ScreenUtil.showToast(it,
                it.getString(R.string.noPreviousSongString),
                toastTextSize, ScreenUtil.FontSize_Pixel_Type,
                Toast.LENGTH_SHORT
            )
        }
    }

    override fun showToastNoNext() {
        activity?.let {
            ScreenUtil.showToast(it,
                it.getString(R.string.noNextSongString),
                toastTextSize, ScreenUtil.FontSize_Pixel_Type,
                Toast.LENGTH_SHORT
            )
        }
    }

    override fun showToastNotSupported() {
        activity?.let {
            ScreenUtil.showToast(it,
                it.getString(R.string.formatNotSupportedString),
                toastTextSize, ScreenUtil.FontSize_Pixel_Type,
                Toast.LENGTH_SHORT
            )
        }
    }

    override fun isActivityFinishing(): Boolean {
        return activity?.isFinishing ?: true
    }

    override fun getRunActivity(): AppCompatActivity? {
        return activity as? AppCompatActivity
    }
    // end of implementing PlayerBasePresenter.BasePresentView

    private suspend fun getFavoriteSongs(): ArrayList<SongInfo> {
        LogUtil.d(TAG, "getFavoriteSongs")
        activity?.let {
            return DatabaseUtil.readSavedFavorites(it,
                getFavDatabaseName(),true)
        }
        return ArrayList()
    }

    private fun setOrientationImageButton(orientation : Int) {
        orientationImageButton?.let {
            it.rotation = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 0.0f else 90.0f
            LogUtil.d(TAG, "setOrientationImageButton.rotation == ${it.rotation}")
            it.setImageResource(R.drawable.phone_portrait)
        }
    }
}