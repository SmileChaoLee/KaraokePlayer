package com.smile.karaokeplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Gravity;
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

import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.Models.VerticalSeekBar;
import com.smile.karaokeplayer.Presenters.ExoPlayerPresenter;
import com.smile.karaokeplayer.Utilities.DataOrContentAccessUtil;
import com.smile.smilelibraries.Models.ExitAppTimer;
import com.smile.smilelibraries.privacy_policy.PrivacyPolicyUtil;
import com.smile.smilelibraries.showing_instertitial_ads_utility.ShowingInterstitialAdsUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.ArrayList;

public class PlayerBaseActivity extends AppCompatActivity implements ExoPlayerPresenter.PresentView{

    private static final String TAG = new String("PlayerBaseActivity");
    private static final int PERMISSION_REQUEST_CODE = 0x11;

    private ExoPlayerPresenter mPresenter;
    private float textFontSize;
    private float fontScale;
    private float toastTextSize;
    private String accessExternalStoragePermissionDeniedString;
    private boolean hasPermissionForExternalStorage;

    private Toolbar supportToolbar;  // use customized ToolBar
    private ActionMenuView actionMenuView;
    private VerticalSeekBar volumeSeekBar;
    private ImageButton volumeImageButton;
    private ImageButton previousMediaImageButton;
    private ImageButton playMediaImageButton;
    private ImageButton replayMediaImageButton;
    private ImageButton pauseMediaImageButton;
    private ImageButton stopMediaImageButton;
    private ImageButton nextMediaImageButton;
    private ImageButton repeatImageButton;
    private ImageButton switchToMusicImageButton;
    private ImageButton switchToVocalImageButton;
    private ImageButton actionMenuImageButton;
    private int volumeSeekBarHeightForLandscape;

    private Menu mainMenu;
    // submenu of file
    private MenuItem autoPlayMenuItem;
    private MenuItem openMenuItem;
    // submenu of audio
    private MenuItem audioTrackMenuItem;
    // submenu of channel
    private MenuItem leftChannelMenuItem;
    private MenuItem rightChannelMenuItem;
    private MenuItem stereoChannelMenuItem;

    private PlayerView videoExoPlayerView;

    private LinearLayout linearLayout_for_ads;
    private LinearLayout messageLinearLayout;
    private TextView bufferingStringTextView;
    private Animation animationText;
    private LinearLayout nativeAdsLinearLayout;
    private TextView nativeAdsStringTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate() is called.");

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(getApplicationContext(), ScreenUtil.FontSize_Pixel_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(getApplicationContext(), defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(getApplicationContext(), ScreenUtil.FontSize_Pixel_Type, 0.0f);
        toastTextSize = 0.7f * textFontSize;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_exoplayer);

        Intent callingIntent = getIntent();
        mPresenter = new ExoPlayerPresenter(this, this);
        mPresenter.initializeVariables(savedInstanceState, callingIntent);

        final PlayingParameters playingParam = mPresenter.getPlayingParam();

        // Video player view
        videoExoPlayerView = findViewById(R.id.videoExoPlayerView);
        videoExoPlayerView.setVisibility(View.VISIBLE);

        // use custom toolbar
        supportToolbar = findViewById(R.id.custom_toolbar);
        supportToolbar.setVisibility(View.VISIBLE);
        setSupportActionBar(supportToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        //
        previousMediaImageButton = findViewById(R.id.previousMediaImageButton);
        playMediaImageButton = findViewById(R.id.playMediaImageButton);
        pauseMediaImageButton = findViewById(R.id.pauseMediaImageButton);
        if (playingParam.getCurrentPlaybackState()==PlaybackStateCompat.STATE_PLAYING) {
            playMediaImageButton.setVisibility(View.GONE);
            pauseMediaImageButton.setVisibility(View.VISIBLE);
        } else {
            playMediaImageButton.setVisibility(View.VISIBLE);
            pauseMediaImageButton.setVisibility(View.GONE);
        }
        replayMediaImageButton = findViewById(R.id.replayMediaImageButton);
        stopMediaImageButton = findViewById(R.id.stopMediaImageButton);
        nextMediaImageButton = findViewById(R.id.nextMediaImageButton);

        //
        repeatImageButton = findViewById(R.id.repeatImageButton);
        switchToMusicImageButton = findViewById(R.id.switchToMusicImageButton);
        switchToVocalImageButton = findViewById(R.id.switchToVocalImageButton);
        actionMenuImageButton = findViewById(R.id.actionMenuImageButton);

        actionMenuView = supportToolbar.findViewById(R.id.actionMenuViewLayout); // main menu
        actionMenuView.setOnMenuItemClickListener(new ActionMenuView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });

        linearLayout_for_ads = findViewById(R.id.linearLayout_for_ads);
        if (!SmileApplication.googleAdMobBannerID.isEmpty()) {
            try {
                AdView bannerAdView = new AdView(this);
                bannerAdView.setAdSize(AdSize.BANNER);
                bannerAdView.setAdUnitId(SmileApplication.googleAdMobBannerID);
                linearLayout_for_ads.addView(bannerAdView);
                AdRequest adRequest = new AdRequest.Builder().build();
                bannerAdView.loadAd(adRequest);
                linearLayout_for_ads.setGravity(Gravity.TOP);
                linearLayout_for_ads.setVisibility(View.VISIBLE);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            linearLayout_for_ads.setVisibility(View.GONE);
        }

        // message area
        messageLinearLayout = findViewById(R.id.messageLinearLayout);
        messageLinearLayout.setVisibility(View.GONE);
        bufferingStringTextView = findViewById(R.id.bufferingStringTextView);
        ScreenUtil.resizeTextSize(bufferingStringTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        animationText = new AlphaAnimation(0.0f,1.0f);
        animationText.setDuration(500);
        animationText.setStartOffset(0);
        animationText.setRepeatMode(Animation.REVERSE);
        animationText.setRepeatCount(Animation.INFINITE);

        nativeAdsLinearLayout = findViewById(R.id.nativeAdsLinearLayout);
        nativeAdsStringTextView = findViewById(R.id.nativeAdsStringTextView);
        ScreenUtil.resizeTextSize(nativeAdsStringTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);

        float durationTextSize = textFontSize * 0.6f;
        TextView exo_position_TextView = findViewById(R.id.exo_position);
        ScreenUtil.resizeTextSize(exo_position_TextView, durationTextSize, ScreenUtil.FontSize_Pixel_Type);

        TextView exo_duration_TextView = findViewById(R.id.exo_duration);
        ScreenUtil.resizeTextSize(exo_duration_TextView, durationTextSize, ScreenUtil.FontSize_Pixel_Type);

        mPresenter.initExoPlayer();   // must be before volumeSeekBar settings
        videoExoPlayerView.setPlayer(mPresenter.getExoPlayer());
        videoExoPlayerView.requestFocus();
        mPresenter.initMediaSessionCompat();

        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        // get default height of volumeBar from dimen.xml
        volumeSeekBarHeightForLandscape = volumeSeekBar.getLayoutParams().height;
        // uses dimens.xml for different devices' sizes
        volumeSeekBar.setVisibility(View.INVISIBLE); // default is not showing
        volumeSeekBar.setMax(PlayerConstants.MaxProgress);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                volumeSeekBar.setProgressAndThumb(i);
                // float currentVolume = (float)i / (float)MaxProgress;
                float currentVolume = 1.0f;
                if (i < PlayerConstants.MaxProgress) {
                    currentVolume = (float)(1.0f - (Math.log(PlayerConstants.MaxProgress - i) / Math.log(PlayerConstants.MaxProgress)));
                }
                playingParam.setCurrentVolume(currentVolume);
                mPresenter.setAudioVolume(currentVolume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        int currentProgress;
        float currentVolume = playingParam.getCurrentVolume();
        if ( currentVolume >= 1.0f) {
            currentProgress = PlayerConstants.MaxProgress;
        } else {
            currentProgress = PlayerConstants.MaxProgress - (int)Math.pow(PlayerConstants.MaxProgress, (1-currentVolume));
            currentProgress = Math.max(0, currentProgress);
        }
        volumeSeekBar.setProgressAndThumb(currentProgress);
        volumeImageButton = findViewById(R.id.volumeImageButton);

        setImageButtonStatus();
        setButtonsPositionAndSize(getResources().getConfiguration());

        setOnClickEvents();

        showNativeAds();

        Uri mediaUri = mPresenter.getMediaUri();
        if (mediaUri==null || Uri.EMPTY.equals(mediaUri)) {
            if (playingParam.isPlaySingleSong()) {
                SongInfo singleSongInfo = mPresenter.getSingleSongInfo();
                if (singleSongInfo == null) {
                    Log.d(TAG, "singleSongInfo is null");
                } else {
                    Log.d(TAG, "singleSongInfo is not null");
                    playingParam.setAutoPlay(false);
                    mPresenter.playSingleSong(singleSongInfo);
                }
            }
        } else {
            int playbackState = playingParam.getCurrentPlaybackState();
            Log.d(TAG, "onActivityCreated() --> playingParam.getCurrentPlaybackState() = " + playbackState);
            if (playbackState != PlaybackStateCompat.STATE_NONE) {
                // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
                // pass the saved instance of playingParam to
                // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)
                Bundle playingParamOriginExtras = new Bundle();
                playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
                MediaControllerCompat.TransportControls mediaTransportControls = mPresenter.getMediaTransportControls();
                mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
            }
        }

        hasPermissionForExternalStorage = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                String permissions[] = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                hasPermissionForExternalStorage = false;
                ScreenUtil.showToast(this, accessExternalStoragePermissionDeniedString, toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_LONG);
            } else {
                hasPermissionForExternalStorage = true;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu() is called");
        // Inflate the menu; this adds items to the action bar if it is present.

        // mainMenu = menu;
        mainMenu = actionMenuView.getMenu();
        getMenuInflater().inflate(R.menu.menu_main, mainMenu);

        // according to the above explanations, the following statement will fit every situation
        ScreenUtil.resizeMenuTextSize(mainMenu, fontScale);

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
            MenuItem fileMenuItem = mainMenu.findItem(R.id.file);
            fileMenuItem.setVisible(false);
            audioTrackMenuItem.setVisible(false);
            MenuItem channelMenuItem = mainMenu.findItem(R.id.channel);
            channelMenuItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        PlayingParameters playingParam = mPresenter.getPlayingParam();

        boolean isAutoPlay;
        int currentChannelPlayed = playingParam.getCurrentChannelPlayed();

        if (item.hasSubMenu()) {
            SubMenu subMenu = item.getSubMenu();
            subMenu.clearHeader();
        }
        int id = item.getItemId();
        switch (id) {
            case R.id.file:
                autoPlayMenuItem.setCheckable(true);
                if (playingParam.isAutoPlay()) {
                    autoPlayMenuItem.setChecked(true);
                    openMenuItem.setEnabled(false);
                } else {
                    autoPlayMenuItem.setChecked(false);
                    openMenuItem.setEnabled(true);
                }
                break;
            case R.id.autoPlay:
                // item.isChecked() return the previous value
                isAutoPlay = !playingParam.isAutoPlay();
                // boolean canShowNotSupportedFormat = true;
                mPresenter.setCanShowNotSupportedFormat(true);
                if (isAutoPlay) {
                    ArrayList<SongInfo> publicSongList = DataOrContentAccessUtil.readPublicSongList(this);
                    mPresenter.setPublicSongList(publicSongList);
                    if ( (publicSongList != null) && (publicSongList.size() > 0) ) {
                        playingParam.setAutoPlay(true);
                        playingParam.setPublicNextSongIndex(0);
                        // start playing video from list
                        // if (exoPlayer.getPlaybackState() != Player.STATE_IDLE) {
                        if (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_NONE) {
                            // media is playing or prepared
                            // exoPlayer.stop();// no need   // will go to onPlayerStateChanged()
                            Log.d(TAG, "isAutoPlay is true and exoPlayer.stop().");

                        }
                        mPresenter.startAutoPlay();
                    } else {
                        String msg = getString(R.string.noPlaylistString);
                        ScreenUtil.showToast(this, msg, toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                    }
                } else {
                    playingParam.setAutoPlay(isAutoPlay);
                }
                setImageButtonStatus();
                break;
            case R.id.songList:
                Intent songListIntent = new Intent(this, SongListActivity.class);
                startActivityForResult(songListIntent, PlayerConstants.SONG_LIST_ACTIVITY_CODE);
                break;
            case R.id.open:
                if (!playingParam.isAutoPlay()) {
                    // isMediaSourcePrepared = false;
                    DataOrContentAccessUtil.selectFileToOpen(this, PlayerConstants.FILE_READ_REQUEST_CODE);
                }
                break;
            case R.id.privacyPolicy:
                PrivacyPolicyUtil.startPrivacyPolicyActivity(this, CommonConstants.PrivacyPolicyUrl, PlayerConstants.PrivacyPolicyActivityRequestCode);
                break;
            case R.id.exit:
                showAdAndExitActivity();
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
                playingParam.setCurrentChannelPlayed(CommonConstants.LeftChannel);
                mPresenter.setAudioVolume(playingParam.getCurrentVolume());
                break;
            case R.id.rightChannel:
                playingParam.setCurrentChannelPlayed(CommonConstants.RightChannel);
                mPresenter.setAudioVolume(playingParam.getCurrentVolume());
                break;
            case R.id.stereoChannel:
                playingParam.setCurrentChannelPlayed(CommonConstants.StereoChannel);
                mPresenter.setAudioVolume(playingParam.getCurrentVolume());
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
        mPresenter.onSaveInstanceState(outState);
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
                Log.i(TAG, "Uri: " + mediaUri.toString());

                if ( (mediaUri == null) || (Uri.EMPTY.equals(mediaUri)) ) {
                    return;
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(mediaUri, takeFlags);
                    }
                } catch (Exception ex) {
                    Log.d(TAG, "Failed to add persistable permission of mediaUri");
                    ex.printStackTrace();
                }

                mPresenter.setMediaUri(mediaUri);

                PlayingParameters playingParam = mPresenter.getPlayingParam();
                playingParam.setCurrentVideoTrackIndexPlayed(0);
                int currentAudioRederer = 0;
                playingParam.setMusicAudioTrackIndex(currentAudioRederer);
                playingParam.setVocalAudioTrackIndex(currentAudioRederer);
                playingParam.setCurrentAudioTrackIndexPlayed(currentAudioRederer);
                playingParam.setMusicAudioChannel(CommonConstants.LeftChannel);
                playingParam.setVocalAudioChannel(CommonConstants.StereoChannel);
                playingParam.setCurrentChannelPlayed(CommonConstants.StereoChannel);
                playingParam.setCurrentAudioPosition(0);
                playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);
                playingParam.setMediaSourcePrepared(false);
                // music or vocal is unknown
                playingParam.setMusicOrVocalOrNoSetting(PlayerConstants.MusicOrVocalUnknown);
                // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
                // pass the saved instance of playingParam to
                // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)

                Bundle playingParamOriginExtras = new Bundle();
                playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
                MediaControllerCompat.TransportControls mediaTransportControls = mPresenter.getMediaTransportControls();
                mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
            }
            return;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy() is called.");
        mPresenter.releaseMediaSessionCompat();
        mPresenter.releaseExoPlayer();
    }

    @Override
    public void onBackPressed() {
        ExitAppTimer exitAppTimer = ExitAppTimer.getInstance(1000); // singleton class
        if (exitAppTimer.canExit()) {
            showAdAndExitActivity();
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

    private void showAdAndExitActivity() {
        returnToPrevious();
        if (SmileApplication.InterstitialAd != null) {
            // free version
            int entryPoint = 0; //  no used
            ShowingInterstitialAdsUtil.ShowAdAsyncTask showAdAsyncTask =
                    SmileApplication.InterstitialAd.new ShowAdAsyncTask(entryPoint
                            , new ShowingInterstitialAdsUtil.AfterDismissFunctionOfShowAd() {
                        @Override
                        public void executeAfterDismissAds(int endPoint) {
                            // returnToPrevious();
                        }
                    });
            showAdAsyncTask.execute();
        } else {
            // returnToPrevious();
        }
    }

    private void setButtonsPositionAndSize(Configuration config) {
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
    }

    private void setOnClickEvents() {

        volumeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int visibility = volumeSeekBar.getVisibility();
                if ( (visibility == View.GONE) || (visibility == View.INVISIBLE) ) {
                    volumeSeekBar.setVisibility(View.VISIBLE);
                } else {
                    volumeSeekBar.setVisibility(View.INVISIBLE);
                }
            }
        });

        previousMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<SongInfo> publicSongList = mPresenter.getPublicSongList();
                PlayingParameters playingParam = mPresenter.getPlayingParam();
                if ( publicSongList==null || !playingParam.isAutoPlay() || playingParam.isPlaySingleSong()) {
                    return;
                }
                int publicSongListSize = publicSongList.size();
                int nextIndex = playingParam.getPublicNextSongIndex();
                int repeatStatus = playingParam.getRepeatStatus();
                nextIndex = nextIndex - 2;
                switch (repeatStatus) {
                    case PlayerConstants.RepeatOneSong:
                        // because in startAutoPlay() will subtract 1 from next index
                        nextIndex++;
                        if (nextIndex == 0) {
                            // go to last song
                            nextIndex = publicSongListSize;
                        }
                        break;
                    case PlayerConstants.RepeatAllSongs:
                        if (nextIndex < 0) {
                            // is going to play the last one
                            nextIndex = publicSongListSize - 1; // the last one
                        }
                        break;
                    case PlayerConstants.NoRepeatPlaying:
                    default:
                        break;
                }
                playingParam.setPublicNextSongIndex(nextIndex);

                mPresenter.startAutoPlay();
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
                ArrayList<SongInfo> publicSongList = mPresenter.getPublicSongList();
                PlayingParameters playingParam = mPresenter.getPlayingParam();
                if ( publicSongList==null || !playingParam.isAutoPlay() || playingParam.isPlaySingleSong()) {
                    return;
                }
                int publicSongListSize = publicSongList.size();
                int nextIndex = playingParam.getPublicNextSongIndex();
                int repeatStatus = playingParam.getRepeatStatus();
                if (repeatStatus == PlayerConstants.RepeatOneSong) {
                    nextIndex++;
                }
                if (nextIndex > publicSongListSize) {
                    // it is playing the last one right now
                    // so it is going to play the first one
                    nextIndex = 0;
                }
                playingParam.setPublicNextSongIndex(nextIndex);

                mPresenter.startAutoPlay();
            }
        });

        repeatImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayingParameters playingParam = mPresenter.getPlayingParam();
                int repeatStatus = playingParam.getRepeatStatus();
                switch (repeatStatus) {
                    case PlayerConstants.NoRepeatPlaying:
                        // switch to repeat one song
                        playingParam.setRepeatStatus(PlayerConstants.RepeatOneSong);
                        break;
                    case PlayerConstants.RepeatOneSong:
                        // switch to repeat song list
                        playingParam.setRepeatStatus(PlayerConstants.RepeatAllSongs);
                        break;
                    case PlayerConstants.RepeatAllSongs:
                        // switch to no repeat
                        playingParam.setRepeatStatus(PlayerConstants.NoRepeatPlaying);
                        break;
                }
                setImageButtonStatus();
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
            }
        });

        supportToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int visibility = v.getVisibility();
                if (visibility == View.VISIBLE) {
                    videoExoPlayerView.hideController();
                }
            }
        });

        videoExoPlayerView.setControllerShowTimeoutMs(PlayerConstants.PlayerView_Timeout);
        videoExoPlayerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int visibility) {
                if (visibility == View.VISIBLE) {
                    // use custom toolbar
                    supportToolbar.setVisibility(View.VISIBLE);
                } else {
                    // use custom toolbar
                    supportToolbar.setVisibility(View.GONE);
                    closeMenu(mainMenu);
                }
                volumeSeekBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void closeMenu(Menu menu) {
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

    // implement ExoPlayerPresenter.PresentView

    @Override
    public void setImageButtonStatus() {
        PlayingParameters playingParam = mPresenter.getPlayingParam();
        boolean isAutoPlay = playingParam.isAutoPlay();
        if ( isAutoPlay || playingParam.isPlaySingleSong() ) {
            switchToMusicImageButton.setEnabled(true);
            switchToMusicImageButton.setVisibility(View.VISIBLE);
            switchToVocalImageButton.setEnabled(true);
            switchToVocalImageButton.setVisibility(View.VISIBLE);
        } else {
            switchToMusicImageButton.setEnabled(false);
            switchToMusicImageButton.setVisibility(View.GONE);
            switchToVocalImageButton.setEnabled(false);
            switchToVocalImageButton.setVisibility(View.GONE);
        }

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
    public void showNativeAds() {
        // simulate showing native ad
        if (BuildConfig.DEBUG) {
            nativeAdsLinearLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideNativeAds() {
        // simulate hide native ad
        Log.d(TAG, "hideNativeAds() is called.");
        if (BuildConfig.DEBUG) {
            nativeAdsLinearLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void showBufferingMessage() {
        messageLinearLayout.setVisibility(View.VISIBLE);
        bufferingStringTextView.startAnimation(animationText);
    }

    @Override
    public void dismissBufferingMessage() {
        messageLinearLayout.setVisibility(View.GONE);
        animationText.cancel();
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
    //
}
