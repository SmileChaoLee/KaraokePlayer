package com.smile.karaokeplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.Models.VerticalSeekBar;
import com.smile.karaokeplayer.Presenters.PlayerBasePresenter;
import com.smile.karaokeplayer.Utilities.DataOrContentAccessUtil;
import com.smile.nativetemplates_models.GoogleAdMobNativeTemplate;
import com.smile.smilelibraries.Models.ExitAppTimer;
import com.smile.smilelibraries.privacy_policy.PrivacyPolicyUtil;
import com.smile.smilelibraries.showing_banner_ads_utility.SetBannerAdViewForAdMobOrFacebook;
import com.smile.smilelibraries.showing_instertitial_ads_utility.ShowingInterstitialAdsUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

public abstract class PlayerBaseActivity extends AppCompatActivity implements PlayerBasePresenter.BasePresentView {

    private static final String TAG = "PlayerBaseActivity";
    private PlayerBasePresenter mPresenter;

    protected float textFontSize;
    protected float fontScale;
    protected float toastTextSize;
    protected LinearLayout playerViewLinearLayout;
    protected Toolbar supportToolbar;  // use customized ToolBar

    private ActionMenuView actionMenuView;
    private LinearLayout audioControllerView;
    protected VerticalSeekBar volumeSeekBar;
    private ImageButton volumeImageButton;
    private ImageButton previousMediaImageButton;
    private ImageButton playMediaImageButton;
    private ImageButton replayMediaImageButton;
    private ImageButton pauseMediaImageButton;
    private ImageButton stopMediaImageButton;
    private ImageButton nextMediaImageButton;

    private TextView playingTimeTextView;
    protected AppCompatSeekBar player_duration_seekbar;
    private TextView durationTimeTextView;

    private ImageButton repeatImageButton;
    private ImageButton switchToMusicImageButton;
    private ImageButton switchToVocalImageButton;

    private ImageButton actionMenuImageButton;
    private int volumeSeekBarHeightForLandscape;

    private LinearLayout bannerLinearLayout;
    private SetBannerAdViewForAdMobOrFacebook myBannerAdView;
    private GoogleAdMobNativeTemplate nativeTemplate;

    // private AdView bannerAdView;
    private LinearLayout message_area_LinearLayout;
    private TextView bufferingStringTextView;
    private Animation animationText;
    private FrameLayout nativeAdsFrameLayout;
    private int nativeAdViewVisibility;
    private com.google.android.ads.nativetemplates.TemplateView nativeAdTemplateView;

    protected Menu mainMenu;
    // submenu of file
    private MenuItem autoPlayMenuItem;
    private MenuItem openMenuItem;
    // submenu of audio
    private MenuItem audioTrackMenuItem;
    // submenu of channel
    private MenuItem leftChannelMenuItem;
    private MenuItem rightChannelMenuItem;
    private MenuItem stereoChannelMenuItem;

    private final Handler controllerTimerHandler = new Handler(Looper.getMainLooper());
    private final Runnable controllerTimerRunnable = new Runnable() {
        @Override
        public void run() {
            controllerTimerHandler.removeCallbacksAndMessages(null);
            if (mPresenter != null) {
                PlayingParameters playingParam = mPresenter.getPlayingParam();
                if (playingParam != null) {
                    if (playingParam.isMediaSourcePrepared()) {
                        if (supportToolbar.getVisibility() == View.VISIBLE) {
                            // hide supportToolbar
                            hideSupportToolbarAndAudioController();
                        }
                    } else {
                        showSupportToolbarAndAudioController();
                    }
                }
            }
        }
    };

    protected abstract PlayerBasePresenter getPlayerBasePresenter();
    protected abstract void setMediaRouteButtonView(int buttonMarginLeft, int imageButtonHeight);
    public abstract void setMediaRouteButtonVisible(boolean isVisible);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate() is called.");

        SmileApplication.InterstitialAd = new ShowingInterstitialAdsUtil(this, SmileApplication.facebookAds, SmileApplication.googleInterstitialAd);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_base);

        mPresenter = getPlayerBasePresenter();
        if (mPresenter == null) {
            Log.d(TAG, "mPresenter is null so exit activity.");
            returnToPrevious();
            return;
        }

        textFontSize = mPresenter.getTextFontSize();
        fontScale = mPresenter.getFontScale();
        toastTextSize = mPresenter.getToastTextSize();

        final PlayingParameters playingParam = mPresenter.getPlayingParam();

        // Video player view
        playerViewLinearLayout = findViewById(R.id.playerViewLinearLayout);

        // use custom toolbar
        supportToolbar = findViewById(R.id.custom_toolbar);
        supportToolbar.setVisibility(View.VISIBLE);
        setSupportActionBar(supportToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        actionMenuView = supportToolbar.findViewById(R.id.actionMenuViewLayout); // main menu

        audioControllerView = findViewById(R.id.audioControllerView);
        //
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        // get default height of volumeBar from dimen.xml
        // uses dimens.xml for different devices' sizes
        volumeSeekBarHeightForLandscape = volumeSeekBar.getLayoutParams().height;
        volumeImageButton = findViewById(R.id.volumeImageButton);
        previousMediaImageButton = findViewById(R.id.previousMediaImageButton);
        playMediaImageButton = findViewById(R.id.playMediaImageButton);
        pauseMediaImageButton = findViewById(R.id.pauseMediaImageButton);
        if (playingParam.getCurrentPlaybackState()==PlaybackStateCompat.STATE_PLAYING) {
            playButtonOffPauseButtonOn();
        } else {
            playButtonOnPauseButtonOff();
        }

        replayMediaImageButton = findViewById(R.id.replayMediaImageButton);
        stopMediaImageButton = findViewById(R.id.stopMediaImageButton);
        nextMediaImageButton = findViewById(R.id.nextMediaImageButton);

        repeatImageButton = findViewById(R.id.repeatImageButton);
        switchToMusicImageButton = findViewById(R.id.switchToMusicImageButton);
        switchToVocalImageButton = findViewById(R.id.switchToVocalImageButton);
        actionMenuImageButton = findViewById(R.id.actionMenuImageButton);

        bannerLinearLayout = findViewById(R.id.bannerLinearLayout);
        // bannerLinearLayout.setGravity(Gravity.TOP);

        String testString = "";
        // for debug mode
        if (com.smile.karaokeplayer.BuildConfig.DEBUG) {
            testString = "IMG_16_9_APP_INSTALL#";
        }
        String facebookBannerID = testString + SmileApplication.facebookBannerID;
        //
        myBannerAdView = new SetBannerAdViewForAdMobOrFacebook(this, null, bannerLinearLayout
                , SmileApplication.googleAdMobBannerID, facebookBannerID);
        myBannerAdView.showBannerAdViewFromAdMobOrFacebook(SmileApplication.AdProvider);

        // message area
        message_area_LinearLayout = findViewById(R.id.message_area_LinearLayout);
        message_area_LinearLayout.setVisibility(View.GONE);
        bufferingStringTextView = findViewById(R.id.bufferingStringTextView);
        ScreenUtil.resizeTextSize(bufferingStringTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        animationText = new AlphaAnimation(0.0f,1.0f);
        animationText.setDuration(500);
        animationText.setStartOffset(0);
        animationText.setRepeatMode(Animation.REVERSE);
        animationText.setRepeatCount(Animation.INFINITE);

        float durationTextSize = textFontSize * 0.6f;
        playingTimeTextView = findViewById(R.id.playingTimeTextView);
        playingTimeTextView.setText("000:00");
        ScreenUtil.resizeTextSize(playingTimeTextView, durationTextSize, ScreenUtil.FontSize_Pixel_Type);

        player_duration_seekbar = findViewById(R.id.player_duration_seekbar);
        if (com.smile.karaokeplayer.BuildConfig.DEBUG) {
            Log.d(TAG, "com.smile.karaokeplayer.BuildConfig.FLAVOR = " + com.smile.karaokeplayer.BuildConfig.FLAVOR );
            if (!com.smile.karaokeplayer.BuildConfig.FLAVOR.toLowerCase().equals("exoplayer")) {
                player_duration_seekbar.setBackgroundColor(Color.BLUE);
            }
        }

        durationTimeTextView = findViewById(R.id.durationTimeTextView);
        durationTimeTextView.setText("000:00");
        ScreenUtil.resizeTextSize(durationTimeTextView, durationTextSize, ScreenUtil.FontSize_Pixel_Type);

        nativeAdsFrameLayout = findViewById(R.id.nativeAdsFrameLayout);
        nativeAdViewVisibility = nativeAdsFrameLayout.getVisibility();

        String nativeAdvancedId0 = "ca-app-pub-8354869049759576/7985456524";     // real ad unit id
        nativeAdTemplateView = findViewById(R.id.nativeAdTemplateView);
        FrameLayout nativeAdsFrameLayout = findViewById(R.id.nativeAdsFrameLayout);
        nativeAdTemplateView = findViewById(R.id.nativeAdTemplateView);
        nativeTemplate = new GoogleAdMobNativeTemplate(this, nativeAdsFrameLayout
                , nativeAdvancedId0, nativeAdTemplateView);

        setImageButtonStatus();
        setButtonsPositionAndSize(getResources().getConfiguration());
        setOnClickEvents();

        showNativeAndBannerAd();

        Log.d(TAG,"onCreate() is finished.");
    }

    @Override
    protected void onStart() {
        Log.d(TAG,"onStart() is called.");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume() is called.");
        super.onResume();
        if (myBannerAdView != null) {
            myBannerAdView.resume();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG,"onPause() is called.");
        super.onPause();
        if (myBannerAdView != null) {
            myBannerAdView.pause();
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG,"onStop() is called.");
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu() is called");
        // Inflate the menu; this adds items to the action bar if it is present.

        // mainMenu = menu;
        mainMenu = actionMenuView.getMenu();
        getMenuInflater().inflate(R.menu.menu_main, mainMenu);

        // final Context wrapper = new ContextThemeWrapper(this, R.style.menu_text_style);
        // or
        final int popupThemeId = supportToolbar.getPopupTheme();
        final Context wrapper = new ContextThemeWrapper(this, popupThemeId);

        // ScreenUtil.buildActionViewClassMenu(this, wrapper, mainMenu, fontScale, SmileApplication.FontSize_Scale_Type);
        ScreenUtil.resizeMenuTextIconSize(wrapper, mainMenu, fontScale);

        // submenu of file
        autoPlayMenuItem = mainMenu.findItem(R.id.autoPlay);
        openMenuItem = mainMenu.findItem(R.id.open);

        // submenu of audio
        audioTrackMenuItem = mainMenu.findItem(R.id.audioTrack);
        // submenu of channel
        leftChannelMenuItem = mainMenu.findItem(R.id.leftChannel);
        rightChannelMenuItem = mainMenu.findItem(R.id.rightChannel);
        stereoChannelMenuItem = mainMenu.findItem(R.id.stereoChannel);

        PlayingParameters playingParam = mPresenter.getPlayingParam();
        if (playingParam.isPlaySingleSong()) {
            autoPlayMenuItem.setVisible(false);
            MenuItem songListMenuItem = mainMenu.findItem(R.id.songList);
            songListMenuItem.setVisible(false);
            openMenuItem.setVisible(false);
            audioTrackMenuItem.setVisible(false);
            MenuItem channelMenuItem = mainMenu.findItem(R.id.channel);
            channelMenuItem.setVisible(false);
            MenuItem privacyPolicyMenuItem = mainMenu.findItem(R.id.privacyPolicy);
            privacyPolicyMenuItem.setVisible(false);
        }

        //
        switch (com.smile.karaokeplayer.BuildConfig.FLAVOR.toLowerCase()) {
            case "exoplayer" :
                break;
            case "vlcplayer" :
                break;
            case "musicplayer" :
                audioTrackMenuItem.setVisible(false);
            case "videoplayer" :
                MenuItem channelMenuItem = mainMenu.findItem(R.id.channel);
                channelMenuItem.setVisible(false);
                break;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        PlayingParameters playingParam = mPresenter.getPlayingParam();
        int currentChannelPlayed = playingParam.getCurrentChannelPlayed();

        if (item.hasSubMenu()) {
            SubMenu subMenu = item.getSubMenu();
            subMenu.clearHeader();
        }
        int id = item.getItemId();
        switch (id) {
            case R.id.autoPlay:
                // item.isChecked() return the previous value
                mPresenter.setAutoPlayStatusAndAction();
                break;
            case R.id.songList:
                Intent songListIntent = new Intent(getApplicationContext(), SongListActivity.class);
                Class childClass = getClass();
                Log.d(TAG, "childClass = " + childClass);
                if (childClass != null) {
                    Intent playerBaseActivityIntent = new Intent(getApplicationContext(), childClass);
                    songListIntent.putExtra(PlayerConstants.PlayerBaseActivityIntent, playerBaseActivityIntent);
                    startActivityForResult(songListIntent, PlayerConstants.SONG_LIST_ACTIVITY_CODE);
                }
                break;
            case R.id.open:
                if (!playingParam.isAutoPlay()) {
                    // isMediaSourcePrepared = false;
                    DataOrContentAccessUtil.selectFileToOpen(this, PlayerConstants.FILE_READ_REQUEST_CODE);
                }
                break;
            case R.id.privacyPolicy:
                PrivacyPolicyUtil.startPrivacyPolicyActivity(this, PlayerConstants.PrivacyPolicyActivityRequestCode);
                break;
            case R.id.exit:
                showInterstitialAd(true);
                break;
            case R.id.audioTrack:
                // if there are audio tracks
                SubMenu subMenu = item.getSubMenu();
                // check if MenuItems of audioTrack need CheckBox
                for (int i=0; i<subMenu.size(); i++) {
                    MenuItem mItem = subMenu.getItem(i);
                    // audio track index start from 1 for user interface
                    if ( (i+1) == playingParam.getCurrentAudioTrackIndexPlayed() ) {
                        mItem.setCheckable(true);
                        mItem.setChecked(true);
                    } else {
                        mItem.setCheckable(false);
                    }
                }
                //
                break;
            case R.id.audioTrack1:
                mPresenter.setAudioTrackAndChannel(1, currentChannelPlayed);
                break;
            case R.id.audioTrack2:
                mPresenter.setAudioTrackAndChannel(2, currentChannelPlayed);
                break;
            case R.id.audioTrack3:
                mPresenter.setAudioTrackAndChannel(3, currentChannelPlayed);
                break;
            case R.id.audioTrack4:
                mPresenter.setAudioTrackAndChannel(4, currentChannelPlayed);
                break;
            case R.id.audioTrack5:
                mPresenter.setAudioTrackAndChannel(5, currentChannelPlayed);
                break;
            case R.id.audioTrack6:
                mPresenter.setAudioTrackAndChannel(6, currentChannelPlayed);
                break;
            case R.id.audioTrack7:
                mPresenter.setAudioTrackAndChannel(7, currentChannelPlayed);
                break;
            case R.id.audioTrack8:
                mPresenter.setAudioTrackAndChannel(8, currentChannelPlayed);
                break;
            case R.id.channel:
                Uri mediaUri = mPresenter.getMediaUri();
                int numberOfAudioTracks = mPresenter.getNumberOfAudioTracks();
                if (mediaUri != null && !Uri.EMPTY.equals(mediaUri) && numberOfAudioTracks>0) {
                    leftChannelMenuItem.setEnabled(true);
                    rightChannelMenuItem.setEnabled(true);
                    stereoChannelMenuItem.setEnabled(true);
                    if (playingParam.isMediaSourcePrepared()) {
                        if (currentChannelPlayed == CommonConstants.LeftChannel) {
                            leftChannelMenuItem.setCheckable(true);
                            leftChannelMenuItem.setChecked(true);
                        } else {
                            leftChannelMenuItem.setCheckable(false);
                            leftChannelMenuItem.setChecked(false);
                        }
                        if (currentChannelPlayed == CommonConstants.RightChannel) {
                            rightChannelMenuItem.setCheckable(true);
                            rightChannelMenuItem.setChecked(true);
                        } else {
                            rightChannelMenuItem.setCheckable(false);
                            rightChannelMenuItem.setChecked(false);
                        }
                        if (currentChannelPlayed == CommonConstants.StereoChannel) {
                            stereoChannelMenuItem.setCheckable(true);
                            stereoChannelMenuItem.setChecked(true);
                        } else {
                            stereoChannelMenuItem.setCheckable(false);
                            stereoChannelMenuItem.setChecked(false);
                        }
                    } else {
                        leftChannelMenuItem.setCheckable(false);
                        leftChannelMenuItem.setChecked(false);
                        rightChannelMenuItem.setCheckable(false);
                        rightChannelMenuItem.setChecked(false);
                        stereoChannelMenuItem.setCheckable(false);
                        stereoChannelMenuItem.setChecked(false);
                    }
                } else {
                    leftChannelMenuItem.setEnabled(false);
                    rightChannelMenuItem.setEnabled(false);
                    stereoChannelMenuItem.setEnabled(false);
                }

                break;
            case R.id.leftChannel:
                mPresenter.playLeftChannel();
                break;
            case R.id.rightChannel:
                mPresenter.playRightChannel();
                break;
            case R.id.stereoChannel:
                mPresenter.playStereoChannel();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Log.d(TAG,"onConfigurationChanged() is called.");
        super.onConfigurationChanged(newConfig);
        closeMenu(mainMenu);
        setButtonsPositionAndSize(newConfig);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG,"onSaveInstanceState() is called.");
        mPresenter.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == PlayerConstants.FILE_READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData()
            if (data != null) {
                Uri mediaUri = data.getData();
                Log.i(TAG, "Uri: " + mediaUri);
                if ( (mediaUri == null) || (Uri.EMPTY.equals(mediaUri)) ) {
                    return;
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(mediaUri, takeFlags);
                    }

                    // testing code
                    // the following codes need android.permission.READ_EXTERNAL_STORAGE
                    // and android.permission.WRITE_EXTERNAL_STORAGE
                    /*
                    Uri resultUri = null;
                    try {
                        String filePath = ExternalStorageUtil.getUriRealPath(this, mediaUri);
                        if (filePath != null) {
                            if (!filePath.isEmpty()) {
                                File songFile = new File(filePath);
                                resultUri = Uri.fromFile(songFile);
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    Log.d(TAG, "resultUri = " + resultUri);
                    mediaUri = resultUri;
                    //  end of testing code
                    */

                    mPresenter.playSelectedSongFromStorage(mediaUri);
                } catch (Exception ex) {
                    Log.d(TAG, "Failed to add persistable permission of mediaUri");
                    ex.printStackTrace();
                }
            }
            return;
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy() is called.");
        if (mPresenter != null) {
            mPresenter.releaseMediaSessionCompat();
        }
        if (myBannerAdView != null) {
            myBannerAdView.destroy();
            myBannerAdView = null;
        }
        if (nativeTemplate != null) {
            nativeTemplate.release();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        ExitAppTimer exitAppTimer = ExitAppTimer.getInstance(1000); // singleton class
        if (exitAppTimer.canExit()) {
            showInterstitialAd(true);
        } else {
            exitAppTimer.start();
            ScreenUtil.showToast(this, getString(R.string.backKeyToExitApp), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
        }

        Log.d(TAG, "onBackPressed() is called");
    }

    private void returnToPrevious() {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);    // can bundle some data to previous activity
        // setResult(Activity.RESULT_OK);   // no bundle data
        finish();
    }

    private void showSupportToolbarAndAudioController() {
        supportToolbar.setVisibility(View.VISIBLE);
        audioControllerView.setVisibility(View.VISIBLE);
        nativeAdsFrameLayout.setVisibility(nativeAdViewVisibility);
    }

    private void hideSupportToolbarAndAudioController() {
        supportToolbar.setVisibility(View.GONE);
        audioControllerView.setVisibility(View.GONE);
        nativeAdsFrameLayout.setVisibility(nativeAdViewVisibility);
        closeMenu(mainMenu);
    }

    private void setOnClickEvents() {
        volumeSeekBar.setVisibility(View.INVISIBLE); // default is not showing
        volumeSeekBar.setMax(PlayerConstants.MaxProgress);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                volumeSeekBar.setProgressAndThumb(i);
                mPresenter.setAudioVolumeInsideVolumeSeekBar(i);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        volumeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int volumeSeekBarVisibility = volumeSeekBar.getVisibility();
                if ( (volumeSeekBarVisibility == View.GONE) || (volumeSeekBarVisibility == View.INVISIBLE) ) {
                    volumeSeekBar.setVisibility(View.VISIBLE);
                    nativeAdsFrameLayout.setVisibility(View.GONE);
                } else {
                    volumeSeekBar.setVisibility(View.INVISIBLE);
                    nativeAdsFrameLayout.setVisibility(nativeAdViewVisibility);
                }
            }
        });

        previousMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.playPreviousSong();
            }
        });

        playMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.startPlay();
            }
        });

        pauseMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.pausePlay();
            }
        });

        replayMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.replayMedia();
            }
        });

        stopMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.stopPlay();
            }
        });

        nextMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.playNextSong();
            }
        });

        repeatImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.setRepeatSongStatus();
            }
        });

        switchToMusicImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.switchAudioToMusic();
            }
        });

        switchToVocalImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.switchAudioToVocal();
            }
        });

        actionMenuImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionMenuView.showOverflowMenu();
                PlayingParameters playingParam = mPresenter.getPlayingParam();
                if (playingParam.isAutoPlay()) {
                    autoPlayMenuItem.setChecked(true);
                    openMenuItem.setEnabled(false);
                } else {
                    autoPlayMenuItem.setChecked(false);
                    openMenuItem.setEnabled(true);
                }
            }
        });

        actionMenuView.setOnMenuItemClickListener(new ActionMenuView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });


        player_duration_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // update the duration on controller UI
                mPresenter.onDurationSeekBarProgressChanged(seekBar, progress, fromUser);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        supportToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int visibility = v.getVisibility();
                if (visibility == View.VISIBLE) {
                    // use custom toolbar
                    hideSupportToolbarAndAudioController();
                    Log.d(TAG, "supportToolbar.onClick() is called --> View.VISIBLE.");
                } else {
                    // use custom toolbar
                    showSupportToolbarAndAudioController();
                    setTimerToHideSupportAndAudioController();
                    Log.d(TAG, "supportToolbar.onClick() is called --> View.INVISIBLE.");
                }
                volumeSeekBar.setVisibility(View.INVISIBLE);

                Log.d(TAG, "supportToolbar.onClick() is called.");
            }
        });

        playerViewLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "playerViewLinearLayout.onClick() is called.");
                supportToolbar.performClick();
            }
        });
    }

    public void setButtonsPositionAndSize(Configuration config) {
        int buttonMarginLeft = (int)(60.0f * fontScale);    // 60 pixels = 20dp on Nexus 5
        Log.d(TAG, "buttonMarginLeft = " + buttonMarginLeft);
        Point screenSize = ScreenUtil.getScreenSize(this);
        Log.d(TAG, "screenSize.x = " + screenSize.x);
        Log.d(TAG, "screenSize.y = " + screenSize.y);
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            buttonMarginLeft = (int)((float)buttonMarginLeft * ((float)screenSize.x / (float)screenSize.y));
            Log.d(TAG, "buttonMarginLeft = " + buttonMarginLeft);
        }

        ViewGroup.MarginLayoutParams layoutParams;
        int imageButtonHeight = (int)(textFontSize * 1.5f);
        int buttonNum = 6;  // 6 buttons
        int maxWidth = buttonNum * imageButtonHeight + (buttonNum-1) * buttonMarginLeft;
        if (maxWidth > screenSize.x) {
            // greater than the width of screen
            buttonMarginLeft = (screenSize.x-10-(buttonNum*imageButtonHeight)) / (buttonNum-1);
        }

        layoutParams = (ViewGroup.MarginLayoutParams) volumeSeekBar.getLayoutParams();
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(0, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) volumeImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(0, 0, 0, 0);

        //
        layoutParams = (ViewGroup.MarginLayoutParams) previousMediaImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        FrameLayout playPauseButtonFrameLayout = findViewById(R.id.playPauseButtonFrameLayout);
        layoutParams = (ViewGroup.MarginLayoutParams) playPauseButtonFrameLayout.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) replayMediaImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) stopMediaImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) nextMediaImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        //
        layoutParams = (ViewGroup.MarginLayoutParams) repeatImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(0, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) switchToMusicImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) switchToVocalImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        setMediaRouteButtonView(buttonMarginLeft, imageButtonHeight);

        layoutParams = (ViewGroup.MarginLayoutParams) actionMenuImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) actionMenuView.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        Bitmap tempBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.circle_and_three_dots);
        Drawable iconDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(tempBitmap, imageButtonHeight, imageButtonHeight, true));
        actionMenuView.setOverflowIcon(iconDrawable);   // set icon of three dots for ActionMenuView
        // supportToolbar.setOverflowIcon(iconDrawable);   // set icon of three dots for toolbar

        // reset the heights of volumeBar and supportToolbar
        final float timesOfVolumeBarForPortrait = 1.5f;
        if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // if orientation is portrait, then double the height of volumeBar
            volumeSeekBar.getLayoutParams().height = (int) ((float)volumeSeekBarHeightForLandscape * timesOfVolumeBarForPortrait);
        } else {
            volumeSeekBar.getLayoutParams().height = volumeSeekBarHeightForLandscape;
        }
        supportToolbar.getLayoutParams().height = volumeImageButton.getLayoutParams().height;

        LinearLayout bannerAds_toobar_layout = findViewById(R.id.bannerAds_toobar_layout);
        ConstraintLayout.LayoutParams bannerToolbarLayoutLP = (ConstraintLayout.LayoutParams)bannerAds_toobar_layout.getLayoutParams();
        FrameLayout message_nativeAd_Layout = findViewById(R.id.message_nativeAd_Layout);
        ConstraintLayout.LayoutParams messageNativeAdLayoutLP = (ConstraintLayout.LayoutParams)message_nativeAd_Layout.getLayoutParams();
        float bannerToobarHeightPercent = bannerToolbarLayoutLP.matchConstraintPercentHeight;
        Log.d(TAG, "bannerToobarHeightPercent = " + bannerToobarHeightPercent);
        float heightPercent = 1.0f - bannerToobarHeightPercent - (imageButtonHeight*3.0f/screenSize.y);
        Log.d(TAG, "heightPercent = " + heightPercent);
        messageNativeAdLayoutLP.matchConstraintPercentHeight = ((int)(heightPercent*100.0f)) / 100.0f;
        Log.d(TAG, "messageNativeAdLayoutLP.matchConstraintPercentHeight = " + messageNativeAdLayoutLP.matchConstraintPercentHeight);

        // setting the width and the margins for nativeAdTemplateView
        layoutParams = (ViewGroup.MarginLayoutParams) nativeAdTemplateView.getLayoutParams();
        // 6 buttons and 5 gaps
        int nativeViewWidth = imageButtonHeight * 6 + buttonMarginLeft * 5;
        layoutParams.width = nativeViewWidth;
        layoutParams.setMargins(0, 0, 0, 0);
        //
    }

    public void closeMenu(Menu menu) {
        if (menu == null) {
            return;
        }
        MenuItem menuItem;
        Menu subMenu;
        int mSize = menu.size();
        for (int i=0; i<mSize; i++) {
            menuItem = menu.getItem(i);
            subMenu = menuItem.getSubMenu();
            closeMenu(subMenu);
        }
        menu.close();
    }

    // implementing PlayerBasePresenter.BasePresentView

    @Override
    public void setImageButtonStatus() {
        PlayingParameters playingParam = mPresenter.getPlayingParam();
        boolean isAutoPlay = playingParam.isAutoPlay();
        switchToMusicImageButton.setEnabled(true);
        switchToMusicImageButton.setVisibility(View.VISIBLE);
        switchToVocalImageButton.setEnabled(true);
        switchToVocalImageButton.setVisibility(View.VISIBLE);

        // repeatImageButton
        int repeatStatus = playingParam.getRepeatStatus();
        switch (repeatStatus) {
            case PlayerConstants.NoRepeatPlaying:
                // no repeat but show symbol of repeat all song with transparent background
                repeatImageButton.setImageResource(R.drawable.repeat_all_white);
                break;
            case PlayerConstants.RepeatOneSong:
                // repeat one song
                repeatImageButton.setImageResource(R.drawable.repeat_one_white);
                break;
            case PlayerConstants.RepeatAllSongs:
                // repeat all song list
                repeatImageButton.setImageResource(R.drawable.repeat_all_white);
                break;
        }
        if (repeatStatus == PlayerConstants.NoRepeatPlaying) {
            repeatImageButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.transparentDark));
        } else {
            repeatImageButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
        }
    }

    @Override
    public void playButtonOnPauseButtonOff() {
        playMediaImageButton.setVisibility(View.VISIBLE);
        pauseMediaImageButton.setVisibility(View.GONE);
    }

    @Override
    public void playButtonOffPauseButtonOn() {
        playMediaImageButton.setVisibility(View.GONE);
        pauseMediaImageButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void setPlayingTimeTextView(String durationString) {
        playingTimeTextView.setText(durationString);
    }

    @Override
    public void update_Player_duration_seekbar(float duration) {
        player_duration_seekbar.setProgress(0);
        player_duration_seekbar.setMax((int)duration);
        duration /= 1000.0f;   // seconds
        int minutes = (int)(duration / 60.0f);    // minutes
        int seconds = (int)duration - (minutes * 60);
        String durationString = String.format("%3d:%02d", minutes, seconds);
        durationTimeTextView.setText(durationString);
    }

    @Override
    public void showMusicAndVocalIsNotSet() {
        ScreenUtil.showToast(this, getString(R.string.musicAndVocalNotSet), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
        Log.d(TAG, "showMusicAndVocalIsNotSet is called.");
    }

    @Override
    public void update_Player_duration_seekbar_progress(int progress) {
        player_duration_seekbar.setProgress(progress);
    }

    @Override
    public void showNativeAndBannerAd() {
        Log.d(TAG, "showNativeAndBannerAd() is called.");
        nativeAdViewVisibility = View.VISIBLE;
        nativeTemplate.showNativeAd();
        bannerLinearLayout.setVisibility(View.VISIBLE);    // Show Banner Ad
    }

    @Override
    public void hideNativeAndBannerAd() {
        Log.d(TAG, "hideNativeAndBannerAd() is called.");
        nativeAdViewVisibility = View.GONE;
        nativeTemplate.hideNativeAd();
        bannerLinearLayout.setVisibility(View.GONE);    // hide Banner Ad
    }

    @Override
    public void showBufferingMessage() {
        message_area_LinearLayout.setVisibility(View.VISIBLE);
        if (animationText != null) {
            bufferingStringTextView.startAnimation(animationText);
        }
    }

    @Override
    public void dismissBufferingMessage() {
        if (animationText != null) {
            animationText.cancel();
        }
        message_area_LinearLayout.setVisibility(View.GONE);
    }

    @Override
    public void buildAudioTrackMenuItem(int audioTrackNumber) {
        // build R.id.audioTrack submenu
        if (audioTrackMenuItem != null) {
            SubMenu subMenu = audioTrackMenuItem.getSubMenu();
            int index=0;
            for (index = 0; index < audioTrackNumber; index++) {
                // audio track index start from 1 for user interface
                subMenu.getItem(index).setVisible(true);
            }
            for (int j=index; j<subMenu.size(); j++) {
                subMenu.getItem(j).setVisible(false);
            }
        }
    }

    @Override
    public void setTimerToHideSupportAndAudioController() {
        controllerTimerHandler.removeCallbacksAndMessages(null);
        controllerTimerHandler.postDelayed(controllerTimerRunnable, PlayerConstants.PlayerView_Timeout); // 10 seconds
    }

    @Override
    public void showInterstitialAd(boolean isReturnToPrevious) {
        if (isReturnToPrevious) {
            returnToPrevious();
        }
        if (SmileApplication.InterstitialAd != null) {
            // free version
            int entryPoint = 0; //  no used
            ShowingInterstitialAdsUtil.ShowInterstitialAdThread showAdAsyncTask =
                    SmileApplication.InterstitialAd.new ShowInterstitialAdThread(entryPoint, SmileApplication.AdProvider);
            showAdAsyncTask.startShowAd();
        }
    }

    // end of implementing PlayerBasePresenter.BasePresentView
}
