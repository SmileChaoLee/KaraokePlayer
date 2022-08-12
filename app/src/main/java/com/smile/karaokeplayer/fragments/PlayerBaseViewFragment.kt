package com.smile.karaokeplayer.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.ads.nativetemplates.TemplateView
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.constants.CommonConstants
import com.smile.karaokeplayer.constants.PlayerConstants
import com.smile.karaokeplayer.models.VerticalSeekBar
import com.smile.karaokeplayer.presenters.BasePlayerPresenter
import com.smile.karaokeplayer.presenters.BasePlayerPresenter.BasePresentView
import com.smile.nativetemplates_models.GoogleAdMobNativeTemplate
import com.smile.smilelibraries.Models.ExitAppTimer
import com.smile.smilelibraries.privacy_policy.PrivacyPolicyUtil
import com.smile.smilelibraries.showing_banner_ads_utility.SetBannerAdViewForAdMobOrFacebook
import com.smile.smilelibraries.showing_interstitial_ads_utility.ShowingInterstitialAdsUtil
import com.smile.smilelibraries.utilities.ScreenUtil
import java.util.*

private const val TAG: String = "PlayerBaseViewFragment"

abstract class PlayerBaseViewFragment : Fragment(), BasePresentView {

    private lateinit var mPresenter: BasePlayerPresenter
    private lateinit var selectSongsToPlayActivityLauncher: ActivityResultLauncher<Intent>

    protected lateinit var fragmentView: View
    protected var textFontSize = 0f
    protected var fontScale = 0f
    protected var toastTextSize = 0f
    protected lateinit var playerViewLinearLayout: LinearLayout
    protected lateinit var supportToolbar // use customized ToolBar
            : Toolbar

    private lateinit var actionMenuView: ActionMenuView
    private lateinit var audioControllerView: LinearLayout
    protected lateinit var volumeSeekBar: VerticalSeekBar
    private lateinit var volumeImageButton: ImageButton
    private lateinit var previousMediaImageButton: ImageButton
    private lateinit var playMediaImageButton: ImageButton
    private lateinit var replayMediaImageButton: ImageButton
    private lateinit var pauseMediaImageButton: ImageButton
    private lateinit var stopMediaImageButton: ImageButton
    private lateinit var nextMediaImageButton: ImageButton

    private lateinit var playingTimeTextView: TextView
    protected lateinit var player_duration_seekbar: AppCompatSeekBar
    private lateinit var durationTimeTextView: TextView

    private lateinit var orientationImageButton: ImageButton
    private lateinit var repeatImageButton: ImageButton
    private lateinit var switchToMusicImageButton: ImageButton
    protected lateinit var switchToVocalImageButton: ImageButton

    private lateinit var actionMenuImageButton: ImageButton
    private var volumeSeekBarHeightForLandscape = 0

    private lateinit var bannerLinearLayout: LinearLayout
    private var myBannerAdView: SetBannerAdViewForAdMobOrFacebook? = null
    private var nativeTemplate: GoogleAdMobNativeTemplate? = null

    // private AdView bannerAdView;
    private lateinit var message_area_LinearLayout: LinearLayout
    private lateinit var bufferingStringTextView: TextView
    private lateinit var animationText: Animation
    private lateinit var nativeAdsFrameLayout: FrameLayout
    private var nativeAdViewVisibility = 0
    private lateinit var nativeAdTemplateView: TemplateView

    protected lateinit var mainMenu: Menu

    // submenu of file
    private lateinit var autoPlayMenuItem: MenuItem
    private lateinit var openMenuItem: MenuItem
    private lateinit var audioMenuItem: MenuItem

    // submenu of audio
    private lateinit var audioTrackMenuItem: MenuItem

    // submenu of channel
    private lateinit var leftChannelMenuItem: MenuItem
    private lateinit var rightChannelMenuItem: MenuItem
    private lateinit var stereoChannelMenuItem: MenuItem

    private val controllerTimerHandler = Handler(Looper.getMainLooper())
    private val controllerTimerRunnable = Runnable {
        controllerTimerHandler.removeCallbacksAndMessages(null)
        val playingParam = mPresenter.playingParam
        if (playingParam != null) {
            if (playingParam.isMediaPrepared) {
                if (supportToolbar.visibility == View.VISIBLE) {
                    // hide supportToolbar
                    hideSupportToolbarAndAudioController()
                }
            } else {
                showSupportToolbarAndAudioController()
            }
        }
    }

    abstract fun getPlayerBasePresenter(): BasePlayerPresenter?
    abstract fun setMediaRouteButtonView(buttonMarginLeft: Int, imageButtonHeight: Int)
    abstract fun setMediaRouteButtonVisible(isVisible: Boolean)
    abstract fun createIntentForSongListActivity(): Intent?
    abstract fun setMenuItemsVisibility()
    abstract fun setSwitchToVocalImageButtonVisibility()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() is called")
        super.onCreate(savedInstanceState)
        arguments?.let {
            Log.d(TAG, "arguments is not null")
        }
        // keep the screen on all the time, added on 2021-02-18
        activity?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        setHasOptionsMenu(true) // must have because it has menu

        BaseApplication.InterstitialAd = ShowingInterstitialAdsUtil(
            activity,
            BaseApplication.facebookAds,
            BaseApplication.googleInterstitialAd
        )

        val presenter = getPlayerBasePresenter()
        if (presenter == null) {
            Log.d(TAG, "presenter is null so exit activity.")
            returnToPrevious()
            return
        }
        mPresenter = presenter
        val callingIntent: Intent? = activity?.intent
        Log.d(TAG, "callingIntent = $callingIntent")
        mPresenter.initializeVariables(savedInstanceState, callingIntent)

        Log.d(TAG, "onCreate() is finished")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_player_base_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated() is called.")
        super.onViewCreated(view, savedInstanceState)

        fragmentView = view

        // val playingParam = mPresenter.playingParam
        textFontSize = mPresenter.textFontSize
        fontScale = mPresenter.fontScale
        toastTextSize = mPresenter.toastTextSize

        // Video player view
        playerViewLinearLayout = fragmentView.findViewById(R.id.playerViewLinearLayout)
        // use custom toolbar
        supportToolbar = fragmentView.findViewById(R.id.custom_toolbar)
        supportToolbar.visibility = View.VISIBLE

        activity?.let {
            val appActivity = it as AppCompatActivity
            appActivity.setSupportActionBar(supportToolbar)
            appActivity.supportActionBar?.setDisplayShowTitleEnabled(false)
        }
        actionMenuView = supportToolbar.findViewById(R.id.actionMenuViewLayout) // main menu
        audioControllerView = fragmentView.findViewById(R.id.audioControllerView)
        volumeSeekBar = fragmentView.findViewById(R.id.volumeSeekBar)
        volumeSeekBarHeightForLandscape = volumeSeekBar.layoutParams.height
        volumeImageButton = fragmentView.findViewById(R.id.volumeImageButton)
        previousMediaImageButton = fragmentView.findViewById(R.id.previousMediaImageButton)
        playMediaImageButton = fragmentView.findViewById(R.id.playMediaImageButton)
        pauseMediaImageButton = fragmentView.findViewById(R.id.pauseMediaImageButton)

        val playingParam = mPresenter.playingParam
        if (playingParam.currentPlaybackState == PlaybackStateCompat.STATE_PLAYING) {
            playButtonOffPauseButtonOn()
        } else {
            playButtonOnPauseButtonOff()
        }

        replayMediaImageButton = fragmentView.findViewById(R.id.replayMediaImageButton)
        stopMediaImageButton = fragmentView.findViewById(R.id.stopMediaImageButton)
        nextMediaImageButton = fragmentView.findViewById(R.id.nextMediaImageButton)

        orientationImageButton = fragmentView.findViewById(R.id.orientationImageButton)
        repeatImageButton = fragmentView.findViewById(R.id.repeatImageButton)
        switchToMusicImageButton = fragmentView.findViewById(R.id.switchToMusicImageButton)
        switchToVocalImageButton = fragmentView.findViewById(R.id.switchToVocalImageButton)
        actionMenuImageButton = fragmentView.findViewById(R.id.actionMenuImageButton)

        bannerLinearLayout = fragmentView.findViewById(R.id.bannerLinearLayout)
        bannerLinearLayout.visibility = View.VISIBLE // Show Banner Ad
        myBannerAdView = SetBannerAdViewForAdMobOrFacebook(
            activity,
            null,
            bannerLinearLayout,
            BaseApplication.googleAdMobBannerID,
            BaseApplication.facebookBannerID
        )
        myBannerAdView?.showBannerAdViewFromAdMobOrFacebook(BaseApplication.AdProvider)

        // message area
        message_area_LinearLayout = fragmentView.findViewById(R.id.message_area_LinearLayout)
        message_area_LinearLayout.visibility = View.GONE
        bufferingStringTextView = fragmentView.findViewById(R.id.bufferingStringTextView)
        ScreenUtil.resizeTextSize(
            bufferingStringTextView,
            textFontSize,
            ScreenUtil.FontSize_Pixel_Type
        )
        animationText = AlphaAnimation(0.0f, 1.0f)
        animationText.duration = 500
        animationText.startOffset = 0
        animationText.repeatMode = Animation.REVERSE
        animationText.repeatCount = Animation.INFINITE

        val durationTextSize = textFontSize * 0.6f
        playingTimeTextView = fragmentView.findViewById(R.id.playingTimeTextView)
        playingTimeTextView.text = "000:00"
        ScreenUtil.resizeTextSize(
            playingTimeTextView,
            durationTextSize,
            ScreenUtil.FontSize_Pixel_Type
        )

        player_duration_seekbar = fragmentView.findViewById(R.id.player_duration_seekbar)

        durationTimeTextView = fragmentView.findViewById(R.id.durationTimeTextView)
        durationTimeTextView.text = "000:00"
        ScreenUtil.resizeTextSize(
            durationTimeTextView,
            durationTextSize,
            ScreenUtil.FontSize_Pixel_Type
        )

        nativeAdsFrameLayout = fragmentView.findViewById(R.id.nativeAdsFrameLayout)
        nativeAdViewVisibility = nativeAdsFrameLayout.visibility
        nativeAdTemplateView = fragmentView.findViewById(R.id.nativeAdTemplateView)
        nativeTemplate = GoogleAdMobNativeTemplate(
            activity, nativeAdsFrameLayout, BaseApplication.googleAdMobNativeID, nativeAdTemplateView
        )

        setImageButtonStatus() // must before setButtonsPositionAndSize()
        setButtonsPositionAndSize(resources.configuration)
        // moved to onCreateOptionsMenu() because onCreateOptionsMenu() is after onViewCreated
        setOnClickEvents()

        showNativeAndBannerAd()

        selectSongsToPlayActivityLauncher = registerForActivityResult(
            StartActivityForResult()) { result: ActivityResult? ->
            Log.d(TAG, "selectSongsToPlayActivityLauncher.onActivityResult() is called.")
            result?.let {
                Log.d(TAG, "selectSongsToPlayActivityLauncher.result = $it")
                val resultCode = it.resultCode
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "selectSongsToPlayActivityLauncher.resultCode = Activity.RESULT_OK")
                    mPresenter.selectFileToOpenPresenter(it)
                }
            }
        }

        Log.d(TAG, "onViewCreated() is finished.")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Log.d(TAG, "onCreateOptionsMenu() is called")
        // Inflate the menu; this adds items to the action bar if it is present.
        // mainMenu = menu;

        mainMenu = actionMenuView.menu
        inflater.inflate(R.menu.menu_main, mainMenu)
        // final Context wrapper = new ContextThemeWrapper(this, R.style.menu_text_style);
        // or
        val popupThemeId = supportToolbar.popupTheme
        val wrapper: Context = ContextThemeWrapper(activity, popupThemeId)

        // ScreenUtil.buildActionViewClassMenu(activity, wrapper, mainMenu, fontScale, BaseApplication.FontSize_Scale_Type);
        ScreenUtil.resizeMenuTextIconSize(wrapper, mainMenu, fontScale)

        // submenu of file
        autoPlayMenuItem = mainMenu.findItem(R.id.autoPlay)
        openMenuItem = mainMenu.findItem(R.id.open)
        audioMenuItem = mainMenu.findItem(R.id.audio)
        // submenu of audio
        audioTrackMenuItem = mainMenu.findItem(R.id.audioTrack)
        // submenu of channel
        leftChannelMenuItem = mainMenu.findItem(R.id.leftChannel)
        rightChannelMenuItem = mainMenu.findItem(R.id.rightChannel)
        stereoChannelMenuItem = mainMenu.findItem(R.id.stereoChannel)

        val playingParam = mPresenter.playingParam
        if (playingParam.isPlaySingleSong) {
            autoPlayMenuItem.isVisible = false
            val songListMenuItem = mainMenu.findItem(R.id.songList)
            songListMenuItem.isVisible = false
            openMenuItem.isVisible = false
            audioMenuItem.isVisible = false
            audioTrackMenuItem.isVisible = false
            val channelMenuItem = mainMenu.findItem(R.id.channel)
            channelMenuItem.isVisible = false
            val privacyPolicyMenuItem = mainMenu.findItem(R.id.privacyPolicy)
            privacyPolicyMenuItem.isVisible = false
        }
        setMenuItemsVisibility() // abstract method

        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val playingParam = mPresenter.playingParam
        val currentChannelPlayed = playingParam.currentChannelPlayed

        if (item.hasSubMenu()) {
            val subMenu = item.subMenu
            subMenu!!.clearHeader()
        }
        val id = item.itemId
        if (id == R.id.autoPlay) {
            // item.isChecked() return the previous value
            mPresenter.setAutoPlayStatusAndAction()
        } else if (id == R.id.songList) {
            val songListIntent = createIntentForSongListActivity()
            startActivity(songListIntent)
        } else if (id == R.id.open) {
            // mPresenter.selectFileToOpenPresenter();
            val selectFileIntent = mPresenter.createSelectFilesToOpenIntent()
            selectSongsToPlayActivityLauncher.launch(selectFileIntent)
        } else if (id == R.id.privacyPolicy) {
            PrivacyPolicyUtil.startPrivacyPolicyActivity(
                activity,
                PlayerConstants.PrivacyPolicyActivityRequestCode
            )
        } else if (id == R.id.exit) {
            showInterstitialAd(true)
        } else if (id == R.id.audioTrack) {
            // if there are audio tracks
            item.subMenu?.let {
                for (i in 0 until it.size()) {
                    val mItem = it.getItem(i)
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
            val numberOfAudioTracks = mPresenter.numberOfAudioTracks
            if (mediaUri != null && Uri.EMPTY != mediaUri && numberOfAudioTracks > 0) {
                leftChannelMenuItem.isEnabled = true
                rightChannelMenuItem.isEnabled = true
                stereoChannelMenuItem.isEnabled = true
                if (playingParam.isMediaPrepared) {
                    if (currentChannelPlayed == CommonConstants.LeftChannel) {
                        leftChannelMenuItem.isCheckable = true
                        leftChannelMenuItem.isChecked = true
                    } else {
                        leftChannelMenuItem.isCheckable = false
                        leftChannelMenuItem.isChecked = false
                    }
                    if (currentChannelPlayed == CommonConstants.RightChannel) {
                        rightChannelMenuItem.isCheckable = true
                        rightChannelMenuItem.isChecked = true
                    } else {
                        rightChannelMenuItem.isCheckable = false
                        rightChannelMenuItem.isChecked = false
                    }
                    if (currentChannelPlayed == CommonConstants.StereoChannel) {
                        stereoChannelMenuItem.isCheckable = true
                        stereoChannelMenuItem.isChecked = true
                    } else {
                        stereoChannelMenuItem.isCheckable = false
                        stereoChannelMenuItem.isChecked = false
                    }
                } else {
                    leftChannelMenuItem.isCheckable = false
                    leftChannelMenuItem.isChecked = false
                    rightChannelMenuItem.isCheckable = false
                    rightChannelMenuItem.isChecked = false
                    stereoChannelMenuItem.isCheckable = false
                    stereoChannelMenuItem.isChecked = false
                }
            } else {
                leftChannelMenuItem.isEnabled = false
                rightChannelMenuItem.isEnabled = false
                stereoChannelMenuItem.isEnabled = false
            }
        } else if (id == R.id.leftChannel) {
            mPresenter.playLeftChannel()
        } else if (id == R.id.rightChannel) {
            mPresenter.playRightChannel()
        } else if (id == R.id.stereoChannel) {
            mPresenter.playStereoChannel()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        Log.d(TAG, "onResume() is called.")
        super.onResume()
        myBannerAdView?.resume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause() is called.")
        super.onPause()
        myBannerAdView?.pause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, "onConfigurationChanged() is called.")
        super.onConfigurationChanged(newConfig)
        closeMenu(mainMenu)
        setButtonsPositionAndSize(newConfig)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState() is called.")
        mPresenter.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() is called.")
        myBannerAdView?.destroy()
        nativeTemplate?.release()
        // clear the screen on, added on 2021-02-18
        activity?.window?.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        super.onDestroy()
    }

    fun onBackPressed() {
        Log.d(TAG, "onBackPressed() is called")
        val exitAppTimer = ExitAppTimer.getInstance(1000) // singleton class
        if (exitAppTimer.canExit()) {
            showInterstitialAd(true)
        } else {
            exitAppTimer.start()
            ScreenUtil.showToast(
                activity,
                getString(R.string.backKeyToExitApp),
                toastTextSize,
                ScreenUtil.FontSize_Pixel_Type,
                Toast.LENGTH_SHORT
            )
        }
    }

    private fun setButtonsPositionAndSize(config: Configuration) {
        var buttonMarginLeft = (60.0f * fontScale).toInt() // 60 pixels = 20dp on Nexus 5
        Log.d(TAG, "buttonMarginLeft = $buttonMarginLeft")
        val screenSize = ScreenUtil.getScreenSize(activity)
        Log.d(TAG, "screenSize.x = " + screenSize.x)
        Log.d(TAG, "screenSize.y = " + screenSize.y)
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            buttonMarginLeft =
                (buttonMarginLeft.toFloat() * (screenSize.x.toFloat() / screenSize.y.toFloat())).toInt()
            Log.d(TAG, "buttonMarginLeft = $buttonMarginLeft")
        }
        val imageButtonHeight = (textFontSize * 1.5f).toInt()
        val buttonNum = 6 // 6 buttons
        val maxWidth = buttonNum * imageButtonHeight + (buttonNum - 1) * buttonMarginLeft
        if (maxWidth > screenSize.x) {
            // greater than the width of screen
            buttonMarginLeft = (screenSize.x - 10 - buttonNum * imageButtonHeight) / (buttonNum - 1)
        }
        var layoutParams: MarginLayoutParams = volumeSeekBar.layoutParams as MarginLayoutParams
        layoutParams.width = imageButtonHeight
        layoutParams.setMargins(0, 0, 0, 0)
        layoutParams = volumeImageButton.layoutParams as MarginLayoutParams
        layoutParams.height = imageButtonHeight
        layoutParams.width = imageButtonHeight
        layoutParams.setMargins(0, 0, 0, 0)
        layoutParams = previousMediaImageButton.layoutParams as MarginLayoutParams
        layoutParams.height = imageButtonHeight
        layoutParams.width = imageButtonHeight
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0)
        val playPauseButtonFrameLayout: FrameLayout =
            fragmentView.findViewById(R.id.playPauseButtonFrameLayout)
        layoutParams = playPauseButtonFrameLayout.layoutParams as MarginLayoutParams
        layoutParams.height = imageButtonHeight
        layoutParams.width = imageButtonHeight
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0)
        layoutParams = replayMediaImageButton.layoutParams as MarginLayoutParams
        layoutParams.height = imageButtonHeight
        layoutParams.width = imageButtonHeight
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0)
        layoutParams = stopMediaImageButton.layoutParams as MarginLayoutParams
        layoutParams.height = imageButtonHeight
        layoutParams.width = imageButtonHeight
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0)
        layoutParams = nextMediaImageButton.layoutParams as MarginLayoutParams
        layoutParams.height = imageButtonHeight
        layoutParams.width = imageButtonHeight
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0)

        layoutParams = orientationImageButton.layoutParams as MarginLayoutParams
        layoutParams.height = imageButtonHeight
        layoutParams.width = imageButtonHeight
        layoutParams.setMargins(0, 0, 0, 0)
        layoutParams = repeatImageButton.layoutParams as MarginLayoutParams
        layoutParams.height = imageButtonHeight
        layoutParams.width = imageButtonHeight
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0)
        layoutParams = switchToMusicImageButton.layoutParams as MarginLayoutParams
        layoutParams.height = imageButtonHeight
        layoutParams.width = imageButtonHeight
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0)
        layoutParams = switchToVocalImageButton.layoutParams as MarginLayoutParams
        layoutParams.height = imageButtonHeight
        layoutParams.width = imageButtonHeight
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0)
        setMediaRouteButtonView(buttonMarginLeft, imageButtonHeight)
        layoutParams = actionMenuImageButton.layoutParams as MarginLayoutParams
        layoutParams.height = imageButtonHeight
        layoutParams.width = imageButtonHeight
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0)
        layoutParams = actionMenuView.layoutParams as MarginLayoutParams
        layoutParams.height = imageButtonHeight
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0)
        val tempBitmap = BitmapFactory.decodeResource(resources, R.mipmap.circle_and_three_dots)
        val iconDrawable: Drawable = BitmapDrawable(
            resources,
            Bitmap.createScaledBitmap(tempBitmap, imageButtonHeight, imageButtonHeight, true)
        )
        actionMenuView.overflowIcon = iconDrawable // set icon of three dots for ActionMenuView
        // supportToolbar.setOverflowIcon(iconDrawable);   // set icon of three dots for toolbar

        // reset the heights of volumeBar and supportToolbar
        val timesOfVolumeBarForPortrait = 1.5f
        if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // if orientation is portrait, then double the height of volumeBar
            volumeSeekBar.layoutParams.height =
                (volumeSeekBarHeightForLandscape.toFloat() * timesOfVolumeBarForPortrait).toInt()
        } else {
            volumeSeekBar.layoutParams.height = volumeSeekBarHeightForLandscape
        }
        supportToolbar.layoutParams.height = volumeImageButton.layoutParams.height
        val bannerAdsLayout: LinearLayout = fragmentView.findViewById(R.id.bannerAdsLayout)
        val bannerAdsLayoutLP = bannerAdsLayout.layoutParams as ConstraintLayout.LayoutParams
        val message_nativeAd_Layout: FrameLayout =
            fragmentView.findViewById(R.id.message_nativeAd_Layout)
        val messageNativeAdLayoutLP =
            message_nativeAd_Layout.layoutParams as ConstraintLayout.LayoutParams
        val bannerAdsLayoutHeightPercent = bannerAdsLayoutLP.matchConstraintPercentHeight
        Log.d(TAG,"bannerToolbarHeightPercent = $bannerAdsLayoutHeightPercent")
        val heightPercent =
            1.0f - bannerAdsLayoutHeightPercent - imageButtonHeight * 3.0f / screenSize.y
        Log.d(TAG, "heightPercent = $heightPercent")
        messageNativeAdLayoutLP.matchConstraintPercentHeight =
            (heightPercent * 100.0f).toInt() / 100.0f
        Log.d(TAG,
            "messageNativeAdLayoutLP.matchConstraintPercentHeight = " + messageNativeAdLayoutLP.matchConstraintPercentHeight
        )

        // setting the width and the margins for nativeAdTemplateView
        layoutParams = nativeAdTemplateView.layoutParams as MarginLayoutParams
        // 6 buttons and 5 gaps
        layoutParams.width = imageButtonHeight * 6 + buttonMarginLeft * 5
        layoutParams.setMargins(0, 0, 0, 0)
        //
    }

    private fun closeMenu(menu: Menu?) {
        menu?.let {
            var menuItem: MenuItem
            var subMenu: Menu?
            val mSize = menu.size()
            for (i in 0 until mSize) {
                menuItem = menu.getItem(i)
                subMenu = menuItem.subMenu
                closeMenu(subMenu)
            }
            menu.close()
        }
    }

    private fun returnToPrevious() {
        val returnIntent = Intent()
        activity?.setResult(Activity.RESULT_OK, returnIntent) // can bundle some data to previous activity
        // setResult(Activity.RESULT_OK);   // no bundle data
        activity?.finish()
    }

    private fun showSupportToolbarAndAudioController() {
        bannerLinearLayout.visibility = View.GONE
        supportToolbar.visibility = View.VISIBLE
        audioControllerView.visibility = View.VISIBLE
        nativeAdsFrameLayout.visibility = nativeAdViewVisibility
    }

    private fun hideSupportToolbarAndAudioController() {
        supportToolbar.visibility = View.GONE
        audioControllerView.visibility = View.GONE
        nativeAdsFrameLayout.visibility = nativeAdViewVisibility
        closeMenu(mainMenu)
        bannerLinearLayout.visibility = View.VISIBLE
    }

    private fun setOnClickEvents() {
        volumeSeekBar.visibility = View.INVISIBLE // default is not showing
        volumeSeekBar.max = PlayerConstants.MaxProgress
        volumeSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                volumeSeekBar.setProgressAndThumb(i)
                mPresenter.setAudioVolumeInsideVolumeSeekBar(i)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        volumeImageButton.setOnClickListener {
            val volumeSeekBarVisibility = volumeSeekBar.visibility
            if (volumeSeekBarVisibility == View.GONE || volumeSeekBarVisibility == View.INVISIBLE) {
                volumeSeekBar.visibility = View.VISIBLE
                nativeAdsFrameLayout.visibility = View.GONE
            } else {
                volumeSeekBar.visibility = View.INVISIBLE
                nativeAdsFrameLayout.visibility = nativeAdViewVisibility
            }
        }
        previousMediaImageButton.setOnClickListener { mPresenter.playPreviousSong() }
        playMediaImageButton.setOnClickListener { mPresenter.startPlay() }
        pauseMediaImageButton.setOnClickListener { mPresenter.pausePlay() }
        replayMediaImageButton.setOnClickListener { mPresenter.replayMedia() }
        stopMediaImageButton.setOnClickListener { mPresenter.stopPlay() }
        nextMediaImageButton.setOnClickListener { mPresenter.playNextSong() }
        orientationImageButton.setOnClickListener {
            val config = resources.configuration
            val orientation =
                if (config.orientation == Configuration.ORIENTATION_PORTRAIT) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
            Log.d(TAG,"orientationImageButton.onClick.orientation = $orientation")
            mPresenter.setOrientationStatus(orientation)
            setImageButtonStatus()
        }
        repeatImageButton.setOnClickListener { mPresenter.setRepeatSongStatus() }
        switchToMusicImageButton.setOnClickListener { mPresenter.switchAudioToMusic() }
        switchToVocalImageButton.setOnClickListener { mPresenter.switchAudioToVocal() }
        actionMenuImageButton.setOnClickListener {
            actionMenuView.showOverflowMenu()
            autoPlayMenuItem.isChecked = mPresenter.playingParam.isAutoPlay
        }
        actionMenuView.setOnMenuItemClickListener { item: MenuItem? ->
            onOptionsItemSelected(
                item!!
            )
        }
        player_duration_seekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // update the duration on controller UI
                mPresenter.onDurationSeekBarProgressChanged(seekBar, progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        supportToolbar.setOnClickListener { v: View ->
            val visibility = v.visibility
            if (visibility == View.VISIBLE) {
                // use custom toolbar
                hideSupportToolbarAndAudioController()
                Log.d(TAG, "supportToolbar.onClick() is called --> View.VISIBLE.")
            } else {
                // use custom toolbar
                showSupportToolbarAndAudioController()
                setTimerToHideSupportAndAudioController()
                Log.d(TAG, "supportToolbar.onClick() is called --> View.INVISIBLE.")
            }
            volumeSeekBar.visibility = View.INVISIBLE
            Log.d(TAG, "supportToolbar.onClick() is called.")
        }
        playerViewLinearLayout.setOnClickListener {
            Log.d(TAG, "playerViewLinearLayout.onClick() is called.")
            supportToolbar.performClick()
        }
    }

    // implementing PlayerBasePresenter.BasePresentView
    override fun setImageButtonStatus() {
        val playingParam = mPresenter.playingParam
        // boolean isAutoPlay = playingParam.isAutoPlay();
        switchToMusicImageButton.isEnabled = true
        switchToMusicImageButton.visibility = View.VISIBLE
        setSwitchToVocalImageButtonVisibility() // abstract method
        val orientation = playingParam.orientationStatus
        Log.d(TAG, "setImageButtonStatus.orientation = $orientation")
        orientationImageButton.rotation = if (orientation==Configuration.ORIENTATION_LANDSCAPE) 0.0f else 90.0f
        orientationImageButton.setImageResource(R.drawable.phone_portrait)
        // repeatImageButton
        var backgroundColor = R.color.red
        when (playingParam.repeatStatus) {
            PlayerConstants.NoRepeatPlaying -> {
                // no repeat but show symbol of repeat all song with transparent background
                repeatImageButton.setImageResource(R.drawable.repeat_all_white)
                backgroundColor = R.color.transparentDark
            }
            PlayerConstants.RepeatOneSong ->                 // repeat one song
                repeatImageButton.setImageResource(R.drawable.repeat_one_white)
            PlayerConstants.RepeatAllSongs ->                 // repeat all song list
                repeatImageButton.setImageResource(R.drawable.repeat_all_white)
        }
        activity?.let {
            repeatImageButton.setBackgroundColor(
                ContextCompat.getColor(
                    it.applicationContext,
                    backgroundColor
                )
            )
        }
    }

    override fun playButtonOnPauseButtonOff() {
        playMediaImageButton.visibility = View.VISIBLE
        pauseMediaImageButton.visibility = View.GONE
    }

    override fun playButtonOffPauseButtonOn() {
        playMediaImageButton.visibility = View.GONE
        pauseMediaImageButton.visibility = View.VISIBLE
    }

    override fun setPlayingTimeTextView(durationString: String?) {
        playingTimeTextView.text = durationString
    }

    override fun update_Player_duration_seekbar(duration: Float) {
        var durationTmp = duration
        player_duration_seekbar.progress = 0
        player_duration_seekbar.max = duration.toInt()
        durationTmp /= 1000.0f // seconds
        val minutes = (duration / 60.0f).toInt() // minutes
        val seconds = duration.toInt() - minutes * 60
        val durationString = String.format(Locale.ENGLISH, "%3d:%02d", minutes, seconds)
        durationTimeTextView.text = durationString
    }

    override fun showMusicAndVocalIsNotSet() {
        ScreenUtil.showToast(
            activity,
            getString(R.string.musicAndVocalNotSet),
            toastTextSize,
            ScreenUtil.FontSize_Pixel_Type,
            Toast.LENGTH_SHORT
        )
        Log.d(TAG, "showMusicAndVocalIsNotSet is called.")
    }

    override fun update_Player_duration_seekbar_progress(progress: Int) {
        player_duration_seekbar.progress = progress
    }

    override fun showNativeAndBannerAd() {
        Log.d(TAG, "showNativeAndBannerAd() is called.")
        nativeAdViewVisibility = View.VISIBLE
        nativeTemplate!!.showNativeAd()
    }

    override fun hideNativeAndBannerAd() {
        Log.d(TAG, "hideNativeAndBannerAd() is called.")
        nativeAdViewVisibility = View.GONE
        nativeTemplate!!.hideNativeAd()
    }

    override fun showBufferingMessage() {
        Log.d(TAG, "showBufferingMessage() is called.")
        message_area_LinearLayout.visibility = View.VISIBLE
        bufferingStringTextView.startAnimation(animationText)
    }

    override fun dismissBufferingMessage() {
        Log.d(TAG, "dismissBufferingMessage() is called.")
        animationText.cancel()
        message_area_LinearLayout.visibility = View.GONE
    }

    override fun buildAudioTrackMenuItem(audioTrackNumber: Int) {
        // build R.id.audioTrack submenu
        val subMenu = audioTrackMenuItem.subMenu
        var index = 0
        while (index < audioTrackNumber) {

            // audio track index start from 1 for user interface
            subMenu!!.getItem(index).isVisible = true
            index++
        }
        for (j in index until subMenu!!.size()) {
            subMenu.getItem(j).isVisible = false
        }
    }

    override fun setTimerToHideSupportAndAudioController() {
        controllerTimerHandler.removeCallbacksAndMessages(null)
        controllerTimerHandler.postDelayed(
            controllerTimerRunnable,
            PlayerConstants.PlayerView_Timeout.toLong()
        ) // 10 seconds
    }

    override fun showInterstitialAd(isExit: Boolean) {
        if (isExit) {
            returnToPrevious()
            val playingParam = mPresenter.playingParam
            if (playingParam.isPlaySingleSong) {
                return
            }
        }
        if (BaseApplication.InterstitialAd != null) {
            // free version
            val entryPoint = 0 //  no used
            val showAdAsyncTask = BaseApplication.InterstitialAd!!.ShowInterstitialAdThread(
                entryPoint,
                BaseApplication.AdProvider
            )
            showAdAsyncTask.startShowAd()
        }
    }

    override fun setScreenOrientation(orientation: Int) {
        activity?.requestedOrientation = if (orientation==Configuration.ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    // end of implementing PlayerBasePresenter.BasePresentView
}