package com.smile.karaokeplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.ResultReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.audio.AudioListener;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import com.smile.karaokeplayer.Models.SongListSQLite;
import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.Models.VerticalSeekBar;
import com.smile.karaokeplayer.audioprocessor_implement.StereoVolumeAudioProcessor;
import com.smile.smilelibraries.Models.ExitAppTimer;
import com.smile.smilelibraries.privacy_policy.PrivacyPolicyUtil;
import com.smile.smilelibraries.showing_instertitial_ads_utility.ShowingInterstitialAdsUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = new String(".MainActivity");
    private static final String LOG_TAG = new String("MediaSessionCompatTag");
    private static final int PERMISSION_REQUEST_CODE = 0x11;
    private static final int PrivacyPolicyActivityRequestCode = 10;
    private static final int FILE_READ_REQUEST_CODE = 1;
    private static final int SONG_LIST_ACTIVITY_CODE = 2;
    private static final int PlayerView_Timeout = 10000;  //  10 seconds
    private static final int noVideoRenderer = -1;
    private static final int noAudioTrack = -1;
    private static final int noAudioChannel = -1;
    private static final int maxProgress = 100;
    private static final float timesOfVolumeBarForPortrait = 1.5f;
    private static final int MusicOrVocalUnknown = 0;
    private static final int PlayingMusic = 1;
    private static final int PlayingVocal = 2;
    private static final int NoRepeatPlaying = 0;
    private static final int RepeatOneSong = 1;
    private static final int RepeatAllSongs = 2;

    private float textFontSize;
    private float fontScale;
    private float toastTextSize;
    private Toolbar supportToolbar;  // use customized ToolBar
    // private ActionBar supportToolbar;   // use default ActionBar
    private ImageButton volumeImageButton;
    private VerticalSeekBar volumeSeekBar;
    private ImageButton switchToMusicImageButton;
    private ImageButton switchToVocalImageButton;
    private ImageButton repeatImageButton;
    private int volumeSeekBarHeightForLandscape;

    private String accessExternalStoragePermissionDeniedString;
    private String noReadableExternalStorageString;
    private boolean hasPermissionForExternalStorage;

    private Menu mainMenu;
    // submenu of file
    private MenuItem autoPlayMenuItem;
    private MenuItem openMenuItem;
    private MenuItem closeMenuItem;
    // submenu of action
    private MenuItem playMenuItem;
    private MenuItem pauseMenuItem;
    private MenuItem stopMenuItem;
    private MenuItem replayMenuItem;
    private MenuItem toTvMenuItem;
    // submenu of audio
    private MenuItem audioTrackMenuItem;
    // submenu of channel
    private MenuItem leftChannelMenuItem;
    private MenuItem rightChannelMenuItem;
    private MenuItem stereoChannelMenuItem;

    private MediaSessionCompat mediaSessionCompat;
    private MediaControllerCompat mediaControllerCompat;
    private MediaControllerCallback mediaControllerCallback;
    private MediaControllerCompat.TransportControls mediaTransportControls;
    private MediaSessionConnector mediaSessionConnector;

    private PlayerView videoPlayerView;

    private DataSource.Factory dataSourceFactory;
    private StereoVolumeAudioProcessor stereoVolumeAudioProcessor;
    private RenderersFactory renderersFactory;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private SimpleExoPlayer exoPlayer;
    private Uri mediaUri;
    private MediaSource mediaSource;
    private AudioAttributes.Builder audioAttributesBuilder;

    private int numberOfVideoRenderers;
    private int numberOfAudioRenderers;

    private SortedMap<String, Integer> videoRendererIndexMap;
    private SortedMap<String, Integer> audioRendererIndexMap;
    private ArrayList<SongInfo> publicSongList;

    private PlayingParameters playingParam;

    private LinearLayout messageLinearLayout;
    private TextView bufferingStringTextView;
    private Animation animationText;

    private boolean canShowNotSupportedFormat;
    private int colorRed;
    private int colorTransparent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate() is called");
        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, SmileApplication.FontSize_Scale_Type, null);
        Log.d(TAG, "defaultTextFontSize = " + defaultTextFontSize);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        Log.d(TAG, "textFontSize = " + textFontSize);
        fontScale = ScreenUtil.suitableFontScale(this, SmileApplication.FontSize_Scale_Type, 0.0f);
        Log.d(TAG, "fontScale = " + fontScale);
        toastTextSize = 0.7f * textFontSize;

        accessExternalStoragePermissionDeniedString = getString(R.string.accessExternalStoragePermissionDeniedString);
        noReadableExternalStorageString = getString(R.string.noReadableExternalStorageString);

        colorRed = ContextCompat.getColor(SmileApplication.AppContext, R.color.red);
        colorTransparent = ContextCompat.getColor(SmileApplication.AppContext, android.R.color.transparent);

        initializeVariables(savedInstanceState);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Video player view
        videoPlayerView = findViewById(R.id.videoPlayerView);
        videoPlayerView.setVisibility(View.VISIBLE);
        //
        initExoPlayer();
        initMediaSessionCompat();

        // use custom toolbar
        supportToolbar = findViewById(R.id.custom_toolbar);
        setSupportActionBar(supportToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        supportToolbar.setVisibility(View.VISIBLE);
        supportToolbar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int visibility = view.getVisibility();
                if (visibility == View.VISIBLE) {
                    videoPlayerView.hideController();
                }
                return false;
            }
        });

        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        // get default height of volumeBar from dimen.xml
        volumeSeekBarHeightForLandscape = volumeSeekBar.getLayoutParams().height;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // if orientation is portrait, then double the height of volumeBar
            volumeSeekBar.getLayoutParams().height = (int) ((float)volumeSeekBarHeightForLandscape * timesOfVolumeBarForPortrait);
        }
        // uses dimens.xml for different devices' sizes
        volumeSeekBar.setVisibility(View.INVISIBLE); // default is not showing
        volumeSeekBar.setMax(maxProgress);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                volumeSeekBar.setProgressAndThumb(i);
                // float currentVolume = (float)i / (float)maxProgress;
                float currentVolume = 1.0f;
                if (i < maxProgress) {
                    currentVolume = (float)(1.0f - (Math.log(maxProgress - i) / Math.log(maxProgress)));
                }
                playingParam.setCurrentVolume(currentVolume);
                setAudioVolume(currentVolume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        int currentProgress = (int)(playingParam.getCurrentVolume() * maxProgress);
        volumeSeekBar.setProgressAndThumb(currentProgress);

        int imageButtonHeight = (int)(textFontSize * 1.5f);
        volumeImageButton = supportToolbar.findViewById(R.id.volumeImageButton);
        volumeImageButton.getLayoutParams().height = imageButtonHeight;
        volumeImageButton.getLayoutParams().width = imageButtonHeight;
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
        switchToMusicImageButton = findViewById(R.id.switchToMusicImageButton);
        switchToMusicImageButton.getLayoutParams().height = imageButtonHeight;
        switchToMusicImageButton.getLayoutParams().width = imageButtonHeight;
        switchToMusicImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchAudioToMusic();
            }
        });

        switchToVocalImageButton = findViewById(R.id.switchToVocalImageButton);
        switchToVocalImageButton.getLayoutParams().height = imageButtonHeight;
        switchToVocalImageButton.getLayoutParams().width = imageButtonHeight;
        switchToVocalImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchAudioToVocal();
            }
        });

        setImageButtonEnabledStatus(playingParam.isAutoPlay());

        repeatImageButton = findViewById(R.id.repeatImageButton);
        repeatImageButton.getLayoutParams().height = imageButtonHeight;
        repeatImageButton.getLayoutParams().width = imageButtonHeight;
        repeatImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            int repeatStatus = playingParam.getRepeatStatus();
                switch (repeatStatus) {
                    case NoRepeatPlaying:
                        // switch to repeat all
                        repeatImageButton.setImageResource(R.drawable.repeat_all_white);
                        playingParam.setRepeatStatus(RepeatAllSongs);
                        break;
                    case RepeatAllSongs:
                        // switch to repeat on song
                        repeatImageButton.setImageResource(R.drawable.repeat_one_white);
                        playingParam.setRepeatStatus(RepeatOneSong);
                        break;
                    case RepeatOneSong:
                        // switch to no repeat but show symbol of repeat all song with transparent background
                        repeatImageButton.setImageResource(R.drawable.repeat_all_white);
                        playingParam.setRepeatStatus(NoRepeatPlaying);
                        break;
                }
                if (playingParam.getRepeatStatus() == NoRepeatPlaying) {
                    repeatImageButton.setBackgroundColor(colorTransparent);
                } else {
                    repeatImageButton.setBackgroundColor(colorRed);
                }
            }
        });

        int volumeSeekBarHeight = (int)(textFontSize * 2.0f);
        volumeSeekBar.getLayoutParams().width = volumeSeekBarHeight;
        supportToolbar.getLayoutParams().height = volumeImageButton.getLayoutParams().height + volumeSeekBar.getLayoutParams().height;
        Log.d(TAG, "supportToolbar = " + supportToolbar.getLayoutParams().height);

        // message area
        messageLinearLayout = findViewById(R.id.messageLinearLayout);
        messageLinearLayout.setVisibility(View.GONE);
        bufferingStringTextView = findViewById(R.id.bufferingStringTextView);
        ScreenUtil.resizeTextSize(bufferingStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        animationText = new AlphaAnimation(0.0f,1.0f);
        animationText.setDuration(500);
        animationText.setStartOffset(0);
        animationText.setRepeatMode(Animation.REVERSE);
        animationText.setRepeatCount(Animation.INFINITE);
        //

        // the original places for the two following statements
        // initExoPlayer();
        // initMediaSessionCompat();

        hasPermissionForExternalStorage = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                String permissions[] = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            }
        }

        if (mediaUri != null) {
            mediaTransportControls.prepareFromUri(mediaUri, null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.menu_main, menu);
        mainMenu = menu;

        // use default ActionBar
        // final Context wrapper = supportToolbar.getThemedContext();
        // or use
        // use custom toolbar
        /*
        final int popupThemeId = supportToolbar.getPopupTheme();
        final Context wrapper = new ContextThemeWrapper(this, popupThemeId);
        ScreenUtil.buildActionViewClassMenu(this, wrapper, menu, fontScale, SmileApplication.FontSize_Scale_Type);
        */

        /**
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // API >= 18 works on SpannableString on MenuItems with app:showAsAction="always"
            // and MenuItems with app:showAsAction="never"
            ScreenUtil.resizeMenuTextSize(menu, fontScale);
        } else {
            // API < 18 does not work on SpannableString on MenuItems with app:showAsAction="always"
            // only MenuItems with app:showAsAction="never" works on SpannableString
            // MenuItems with app:showAsAction="never" use menu_text_style (in style.xml)
            // MenuItems with app:showAsAction="always" use dimens
            int menuSize = menu.size();
            MenuItem mItem;
            Menu subMenu;
            for (int i=0; i<menuSize; i++) {
                mItem = menu.getItem(i);
                subMenu = mItem.getSubMenu();
                ScreenUtil.resizeMenuTextSize(subMenu, fontScale);
            }
        }
         */

        // according to the above explanations, the following statement will fit every situation
        ScreenUtil.resizeMenuTextSize(menu, fontScale);

        // submenu of file
        autoPlayMenuItem = menu.findItem(R.id.autoPlay);
        openMenuItem = menu.findItem(R.id.open);
        closeMenuItem = menu.findItem(R.id.close);
        // submenu of action
        playMenuItem = menu.findItem(R.id.play);
        pauseMenuItem = menu.findItem(R.id.pause);
        stopMenuItem = menu.findItem(R.id.stop);
        replayMenuItem = menu.findItem(R.id.replay);
        toTvMenuItem = menu.findItem(R.id.toTV);

        // submenu of audio
        audioTrackMenuItem = menu.findItem(R.id.audioTrack);
        // submenu of channel
        leftChannelMenuItem = menu.findItem(R.id.leftChannel);
        rightChannelMenuItem = menu.findItem(R.id.rightChannel);
        stereoChannelMenuItem = menu.findItem(R.id.stereoChannel);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (!hasPermissionForExternalStorage) {
            ScreenUtil.showToast(this, accessExternalStoragePermissionDeniedString, toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
            return super.onOptionsItemSelected(item);
        }

        videoPlayerView.setControllerShowTimeoutMs(-1); // cancel the time out

        boolean isAutoPlay;
        int currentChannelPlayed;
        int mCurrentState;

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
                    closeMenuItem.setEnabled(false);
                } else {
                    autoPlayMenuItem.setChecked(false);
                    openMenuItem.setEnabled(true);
                    closeMenuItem.setEnabled(true);
                }
                break;
            case R.id.autoPlay:
                // item.isChecked() return the previous value
                isAutoPlay = !playingParam.isAutoPlay();
                canShowNotSupportedFormat = true;
                if (isAutoPlay) {
                    readPublicSongList();
                    if ( (publicSongList != null) && (publicSongList.size() > 0) ) {
                        playingParam.setAutoPlay(true);
                        playingParam.setPublicSongIndex(0);
                        // start playing video from list
                        // if (exoPlayer.getPlaybackState() != Player.STATE_IDLE) {
                        if (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_NONE) {
                            // media is playing or prepared
                            // exoPlayer.stop();// no need   // will go to onPlayerStateChanged()
                            Log.d(TAG, "isAutoPlay is true and exoPlayer.stop().");

                        }
                        startAutoPlay();
                    } else {
                        String msg = getString(R.string.noPlaylistString);
                        ScreenUtil.showToast(this, msg, toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
                    }
                } else {
                    playingParam.setAutoPlay(isAutoPlay);
                }
                setImageButtonEnabledStatus(playingParam.isAutoPlay());
                break;
            case R.id.songList:
                Intent songListIntent = new Intent(this, SongListActivity.class);
                startActivityForResult(songListIntent, SONG_LIST_ACTIVITY_CODE);
                break;
            case R.id.open:
                if (!playingParam.isAutoPlay()) {
                    // isMediaSourcePrepared = false;
                    /*
                    if (ExternalStorageUtil.isExternalStorageReadable()) {
                        // has readable external storage
                        selectFileToOpen();
                    } else {
                        ScreenUtil.showToast(this, noReadableExternalStorageString, toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
                        Log.d(TAG, noReadableExternalStorageString);
                    }
                    */
                    selectFileToOpen();
                }
                break;
            case R.id.close:
                stopPlay();
                break;
            case R.id.privacyPolicy:
                PrivacyPolicyUtil.startPrivacyPolicyActivity(this, SmileApplication.PrivacyPolicyUrl, PrivacyPolicyActivityRequestCode);
                break;
            case R.id.exit:
                showAdAndExitApplication();
                break;
            case R.id.action:
                if ( (mediaSource != null) && (numberOfAudioRenderers>0) ) {
                    Log.d(TAG, "R.id.action --> mediaSource is not null.");
                    playMenuItem.setEnabled(true);
                    pauseMenuItem.setEnabled(true);
                    stopMenuItem.setEnabled(true);
                    replayMenuItem.setEnabled(true);
                    toTvMenuItem.setEnabled(true);
                    mCurrentState = playingParam.getCurrentPlaybackState();
                    if (mCurrentState == PlaybackStateCompat.STATE_PLAYING) {
                        playMenuItem.setCheckable(true);
                        playMenuItem.setChecked(true);
                    } else {
                        playMenuItem.setCheckable(false);
                    }
                    if (mCurrentState == PlaybackStateCompat.STATE_PAUSED) {
                        pauseMenuItem.setCheckable(true);
                        pauseMenuItem.setChecked(true);
                    } else {
                        pauseMenuItem.setCheckable(false);
                    }
                    if ((mediaSource != null) && (mCurrentState == PlaybackStateCompat.STATE_NONE)) {
                        stopMenuItem.setCheckable(true);
                        stopMenuItem.setChecked(true);
                    } else {
                        stopMenuItem.setCheckable(false);
                    }
                    // toTvMenuItem
                } else {
                    Log.d(TAG, "R.id.action --> mediaSource is null.");
                    playMenuItem.setEnabled(false);
                    pauseMenuItem.setEnabled(false);
                    stopMenuItem.setEnabled(false);
                    replayMenuItem.setEnabled(false);
                    toTvMenuItem.setEnabled(false);
                }
                break;
            case R.id.play:
                startPlay();
                break;
            case R.id.pause:
                pausePlay();
                break;
            case R.id.stop:
                // if (mCurrentState != PlaybackStateCompat.STATE_STOPPED) {
                // mCurrentState = PlaybackStateCompat.STATE_STOPPED when finished playing
                stopPlay();
                break;
            case R.id.replay:
                replayMedia();
                break;
            case R.id.toTV:
                break;
            case R.id.audioTrack:
                // if there are audio tracks
                SubMenu subMenu = item.getSubMenu();
                // use default ActionBar
                // final Context wrapper = supportToolbar.getThemedContext();
                // or
                // use custom toolbar
                final int popupThemeId = supportToolbar.getPopupTheme();
                final Context wrapper = new ContextThemeWrapper(this, popupThemeId);
                ScreenUtil.buildActionViewClassMenu(this, wrapper, subMenu, fontScale, SmileApplication.FontSize_Scale_Type);

                // check if MenuItems of audioTrack need CheckBox
                for (int i=0; i<subMenu.size(); i++) {
                    MenuItem mItem = subMenu.getItem(i);
                    if (i == playingParam.getCurrentAudioRendererPlayed()) {
                        mItem.setCheckable(true);
                        mItem.setChecked(true);
                    } else {
                        mItem.setCheckable(false);
                    }
                }
                //

                break;
            case R.id.channel:
                if ( (mediaSource != null) && (numberOfAudioRenderers>0) ) {
                    leftChannelMenuItem.setEnabled(true);
                    rightChannelMenuItem.setEnabled(true);
                    stereoChannelMenuItem.setEnabled(true);
                    if (playingParam.isMediaSourcePrepared()) {
                        currentChannelPlayed = playingParam.getCurrentChannelPlayed();
                        if (currentChannelPlayed == SmileApplication.leftChannel) {
                            leftChannelMenuItem.setCheckable(true);
                            leftChannelMenuItem.setChecked(true);
                        } else {
                            leftChannelMenuItem.setCheckable(false);
                            leftChannelMenuItem.setChecked(false);
                        }
                        if (currentChannelPlayed == SmileApplication.rightChannel) {
                            rightChannelMenuItem.setCheckable(true);
                            rightChannelMenuItem.setChecked(true);
                        } else {
                            rightChannelMenuItem.setCheckable(false);
                            rightChannelMenuItem.setChecked(false);
                        }
                        if (currentChannelPlayed == SmileApplication.stereoChannel) {
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
                playingParam.setCurrentChannelPlayed(SmileApplication.leftChannel);
                setAudioVolume(playingParam.getCurrentVolume());
                break;
            case R.id.rightChannel:
                playingParam.setCurrentChannelPlayed(SmileApplication.rightChannel);
                setAudioVolume(playingParam.getCurrentVolume());
                break;
            case R.id.stereoChannel:
                playingParam.setCurrentChannelPlayed(SmileApplication.stereoChannel);
                setAudioVolume(playingParam.getCurrentVolume());
                break;
        }

        videoPlayerView.setControllerShowTimeoutMs(PlayerView_Timeout); // cancel the time out

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        Log.d(TAG,"MainActivity-->onStart() is called.");
        super.onStart();
    }
    @Override
    protected void onResume() {
        Log.d(TAG,"MainActivity-->onResume() is called.");
        super.onResume();
    }
    @Override
    protected void onPause() {
        Log.d(TAG,"MainActivity-->onPause() is called.");
        super.onPause();
    }
    @Override
    protected void onStop() {
        Log.d(TAG,"MainActivity-->onStop() is called.");
        super.onStop();
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Log.d(TAG,"MainActivity-->onConfigurationChanged() is called.");
        super.onConfigurationChanged(newConfig);
        // mainMenu.close();
        closeMenu(mainMenu);

        // reset the heights of volumeBar and supportToolbar
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // if orientation is portrait, then double the height of volumeBar
            volumeSeekBar.getLayoutParams().height = (int) ((float)volumeSeekBarHeightForLandscape * timesOfVolumeBarForPortrait);
        } else {
            volumeSeekBar.getLayoutParams().height = volumeSeekBarHeightForLandscape;
        }
        supportToolbar.getLayoutParams().height = volumeImageButton.getLayoutParams().height + volumeSeekBar.getLayoutParams().height;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                hasPermissionForExternalStorage = false;
                ScreenUtil.showToast(this, accessExternalStoragePermissionDeniedString, toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
            } else {
                hasPermissionForExternalStorage = true;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"MainActivity-->onDestroy() is called.");
        releaseMediaSessionCompat();
        releaseExoPlayer();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG,"MainActivity-->onSaveInstanceState() is called.");
        outState.putParcelable("MediaUri", mediaUri);
        playingParam.setCurrentAudioPosition(exoPlayer.getContentPosition());
        outState.putParcelable("PlayingParameters", playingParam);
        outState.putBoolean("CanShowNotSupportedFormat", canShowNotSupportedFormat);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == FILE_READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            mediaUri = null;
            if (data != null) {
                mediaUri = data.getData();
                Log.i(TAG, "Uri: " + mediaUri.toString());

                if ( (mediaUri == null) || (Uri.EMPTY.equals(mediaUri)) ) {
                    return;
                }

                playingParam.setCurrentVideoRendererPlayed(0);

                int currentAudioRederer = 0;
                playingParam.setMusicAudioRenderer(currentAudioRederer);
                playingParam.setVocalAudioRenderer(currentAudioRederer);
                playingParam.setCurrentAudioRendererPlayed(currentAudioRederer);

                playingParam.setMusicAudioChannel(SmileApplication.leftChannel);
                playingParam.setVocalAudioChannel(SmileApplication.stereoChannel);
                playingParam.setCurrentChannelPlayed(SmileApplication.stereoChannel);

                playingParam.setCurrentAudioPosition(0);
                playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);
                playingParam.setMediaSourcePrepared(false);

                // music or vocal is unknown
                playingParam.setMusicOrVocalOrNoSetting(MusicOrVocalUnknown);

                mediaTransportControls.prepareFromUri(mediaUri, null);
            }
            return;
        }
    }

    @Override
    public void onBackPressed() {
        ExitAppTimer exitAppTimer = ExitAppTimer.getInstance(1000); // singleton class
        if (exitAppTimer.canExit()) {
            showAdAndExitApplication();
        } else {
            exitAppTimer.start();
            ScreenUtil.showToast(this, getString(R.string.backKeyToExitApp), toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
        }
        Log.d(TAG, "onBackPressed() is called");
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

    private void setImageButtonEnabledStatus(boolean isAutoPlay) {
        if (isAutoPlay) {
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
    }

    private void showAdAndExitApplication() {
        if (SmileApplication.InterstitialAd != null) {
            // free version
            int entryPoint = 0; //  no used
            ShowingInterstitialAdsUtil.ShowAdAsyncTask showAdAsyncTask =
                    SmileApplication.InterstitialAd.new ShowAdAsyncTask(this
                            , entryPoint
                            , new ShowingInterstitialAdsUtil.AfterDismissFunctionOfShowAd() {
                        @Override
                        public void executeAfterDismissAds(int endPoint) {
                            exitApplication();
                        }
                    });
            showAdAsyncTask.execute();
        } else {
            exitApplication();
        }
    }

    private void exitApplication() {
        finish();
    }

    private void initializePlayingParam() {
        playingParam = new PlayingParameters();
        playingParam.setAutoPlay(false);
        playingParam.setMediaSourcePrepared(false);
        playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);

        playingParam.setCurrentVideoRendererPlayed(0);

        playingParam.setMusicAudioRenderer(0);
        playingParam.setVocalAudioRenderer(0);
        playingParam.setCurrentAudioRendererPlayed(playingParam.getMusicAudioRenderer());
        playingParam.setMusicAudioChannel(SmileApplication.leftChannel);     // default
        playingParam.setVocalAudioChannel(SmileApplication.stereoChannel);   // default
        playingParam.setCurrentChannelPlayed(playingParam.getMusicAudioChannel());
        playingParam.setCurrentAudioPosition(0);
        playingParam.setCurrentVolume(1.0f);

        playingParam.setPublicSongIndex(0);
        playingParam.setPlayingPublic(true);

        playingParam.setMusicOrVocalOrNoSetting(0); // no music and vocal setting
        playingParam.setRepeatStatus(NoRepeatPlaying);    // no repeat playing songs
    }

    private void initializeVariables(Bundle savedInstanceState) {

        numberOfVideoRenderers = 0;
        numberOfAudioRenderers = 0;
        videoRendererIndexMap = new TreeMap<>();
        audioRendererIndexMap = new TreeMap<>();

        if (savedInstanceState == null) {
            mediaUri = null;
            initializePlayingParam();
            canShowNotSupportedFormat = false;
        } else {
            mediaUri = savedInstanceState.getParcelable("MediaUri");
            playingParam = savedInstanceState.getParcelable("PlayingParameters");
            canShowNotSupportedFormat = savedInstanceState.getBoolean("CanShowNotSupportedFormat");
            if (playingParam == null) {
                initializePlayingParam();
            }
        }
    }

    private void selectFileToOpen() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // intent.setType("video/*");

        startActivityForResult(intent, FILE_READ_REQUEST_CODE);
    }

    private void showBufferingMessage() {
        messageLinearLayout.setVisibility(View.VISIBLE);
        bufferingStringTextView.startAnimation(animationText);
    }
    private void dismissBufferingMessage() {
        messageLinearLayout.setVisibility(View.GONE);
        animationText.cancel();
    }

    private void initExoPlayer() {

        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getPackageName()));
        trackSelector = new DefaultTrackSelector();
        // trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory());
        trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();
        trackSelector.setParameters(trackSelectorParameters);

        // Tell ExoPlayer to use FfmpegAudioRenderer
        // renderersFactory = new DefaultRenderersFactory(this).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);

        renderersFactory = new DefaultRenderersFactory(this) {
            @Override
            protected AudioProcessor[] buildAudioProcessors() {
                Log.d(TAG,"DefaultRenderersFactory.buildAudioProcessors() is called.");

                // Customized AudioProcessor
                stereoVolumeAudioProcessor = new StereoVolumeAudioProcessor();
                AudioProcessor[] audioProcessors = new AudioProcessor[] {stereoVolumeAudioProcessor};

                return audioProcessors;
                // return super.buildAudioProcessors();
            }
        }
        // .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);
        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        // .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);

        // renderersFactory = new DefaultRenderersFactory(this).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        // renderersFactory = new DefaultRenderersFactory(this).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);

        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, renderersFactory, trackSelector);

        // no need. It will ve overridden by MediaSessionConnector
        exoPlayer.addListener(new ExoPlayerEventListener());
        exoPlayer.addAudioListener(new AudioListener() {
            @Override
            public void onAudioSessionId(int audioSessionId) {
                Log.d(TAG, "addAudioListener.onAudioSessionId() is called.");
                Log.d(TAG, "addAudioListener.audioSessionId = " + audioSessionId);

            }

            @Override
            public void onAudioAttributesChanged(AudioAttributes audioAttributes) {
                Log.d(TAG, "addAudioListener.onAudioAttributesChanged() is called.");
            }

            @Override
            public void onVolumeChanged(float volume) {
                Log.d(TAG, "addAudioListener.onVolumeChanged() is called.");
            }
        });

        audioAttributesBuilder = new AudioAttributes.Builder();
        audioAttributesBuilder.setUsage(C.USAGE_MEDIA).setContentType(C.CONTENT_TYPE_MOVIE);
        exoPlayer.setAudioAttributes(audioAttributesBuilder.build(), true);

        videoPlayerView.setPlayer(exoPlayer);
        videoPlayerView.requestFocus();

        videoPlayerView.setControllerShowTimeoutMs(PlayerView_Timeout);
        videoPlayerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int visibility) {
                if (visibility == View.VISIBLE) {
                    // use custom toolbar
                    supportToolbar.setVisibility(View.VISIBLE);
                } else {
                    // use custom toolbar
                    supportToolbar.setVisibility(View.GONE);
                    // mainMenu.close();
                    closeMenu(mainMenu);
                }
                volumeSeekBar.setVisibility(View.INVISIBLE);
            }
        });

        // Log.d(TAG, "FfmpegLibrary.isAvailable() = " + FfmpegLibrary.isAvailable());

    }

    private void releaseExoPlayer() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private void initMediaSessionCompat() {
        // Create a MediaSessionCompat
        mediaSessionCompat = new MediaSessionCompat(this, LOG_TAG);

        // Do not let MediaButtons restart the player when the app is not visible
        mediaSessionCompat.setMediaButtonReceiver(null);
        mediaSessionCompat.setActive(true); // might need to find better place to put

        // Create a MediaControllerCompat
        mediaControllerCompat = new MediaControllerCompat(this, mediaSessionCompat);
        MediaControllerCompat.setMediaController(this, mediaControllerCompat);
        mediaControllerCallback = new MediaControllerCallback();
        mediaControllerCompat.registerCallback(mediaControllerCallback);
        mediaTransportControls = mediaControllerCompat.getTransportControls();

        mediaSessionConnector = new MediaSessionConnector(mediaSessionCompat);
        mediaSessionConnector.setPlayer(exoPlayer);
        mediaSessionConnector.setPlaybackPreparer(new PlaybackPreparer());
    }

    private void releaseMediaSessionCompat() {
        mediaSessionCompat.setActive(false);
        mediaSessionCompat.release();
        mediaSessionCompat = null;
        mediaTransportControls = null;
        if (mediaControllerCallback != null) {
            mediaControllerCompat.unregisterCallback(mediaControllerCallback);
            mediaControllerCallback = null;
        }
        mediaControllerCompat = null;
        mediaSessionConnector = null;
    }

    private void readPublicSongList() {
        SongListSQLite songListSQLite = new SongListSQLite(SmileApplication.AppContext);
        if (songListSQLite != null) {
            publicSongList = songListSQLite.readPlaylist();
            songListSQLite.closeDatabase();
            songListSQLite = null;
        } else {
            publicSongList = new ArrayList<>();
            Log.d(TAG, "Read database unsuccessfully --> " + publicSongList.size());
        }
    }

    private void startAutoPlay() {

        if (isFinishing()) {
            // activity is being destroyed
            return;
        }

        // activity is not being destroyed then check
        // if there are still songs to be play
        boolean hasSongs = false;   // no more songs to play
        if (hasSongs) {
            playingParam.setPlayingPublic(false);   // playing next ordered song
        } else {
            playingParam.setPlayingPublic(true);   // playing next public song on list
        }

        SongInfo songInfo = null;
        if (playingParam.isPlayingPublic())  {
            int publicSongListSize = 0;
            if (publicSongList != null) {
                publicSongListSize = publicSongList.size();
            }
            if (publicSongListSize <= 0) {
                // no public songs
                ScreenUtil.showToast(this, getString(R.string.noPlaylistString), toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
                playingParam.setAutoPlay(false);    // cancel auto play
            } else {
                // There are public songs to be played
                boolean stillPlayNext = true;
                int repeatStatus = playingParam.getRepeatStatus();
                int publicSongIndex = playingParam.getPublicSongIndex();
                switch (repeatStatus) {
                    case NoRepeatPlaying:
                        // no repeat
                        if (publicSongIndex >= publicSongListSize) {
                            // stop playing
                            playingParam.setAutoPlay(false);
                            stopPlay();
                            stillPlayNext = false;  // stop here and do not go to next
                        }
                        break;
                    case RepeatOneSong:
                        // repeat one song
                        if ( (publicSongIndex > 0) && (publicSongIndex <= publicSongListSize) ) {
                            publicSongIndex--;
                        }
                        break;
                    case RepeatAllSongs:
                        // repeat all songs
                        if (publicSongIndex >= publicSongListSize) {
                            publicSongIndex = 0;
                        }
                        break;
                }

                if (stillPlayNext) {    // still play the next song
                    songInfo = publicSongList.get(publicSongIndex);

                    playingParam.setMusicOrVocalOrNoSetting(PlayingVocal);  // presume vocal
                    playingParam.setCurrentVideoRendererPlayed(0);

                    playingParam.setMusicAudioRenderer(songInfo.getMusicTrackNo());
                    playingParam.setMusicAudioChannel(songInfo.getMusicChannel());

                    playingParam.setVocalAudioRenderer(songInfo.getVocalTrackNo());
                    playingParam.setCurrentAudioRendererPlayed(playingParam.getVocalAudioRenderer());
                    playingParam.setVocalAudioChannel(songInfo.getVocalChannel());
                    playingParam.setCurrentChannelPlayed(playingParam.getVocalAudioChannel());

                    playingParam.setCurrentAudioPosition(0);
                    playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);
                    playingParam.setMediaSourcePrepared(false);

                    publicSongIndex++;  // set next index of playlist that will be played
                    playingParam.setPublicSongIndex(publicSongIndex);

                    String filePath = songInfo.getFilePath();
                    Log.i(TAG, "filePath : " + filePath);
                    // File songFile = new File(filePath);
                    // if (songFile.exists()) {
                    // mediaUri = Uri.fromFile(new File(filePath));
                    // mediaUri = Uri.parse("file://" + filePath);
                    mediaUri = Uri.parse(filePath);
                    Log.i(TAG, "mediaUri from filePath : " + mediaUri);
                    mediaTransportControls.prepareFromUri(mediaUri, null);
                }
                Log.d(TAG, "startAutoPlay() finished --> " + publicSongIndex--);
                // }
            }
        } else {
            // play next song that user has ordered
            playingParam.setMusicOrVocalOrNoSetting(PlayingMusic);  // presume music
            if (mediaSource != null) {
                if (playingParam.getRepeatStatus() != NoRepeatPlaying) {
                    // repeat playing this mediaSource
                } else {
                    // next song that user ordered
                }
            }
            Log.d(TAG, "startAutoPlay() finished --> ordered song.");
        }
        setImageButtonEnabledStatus(playingParam.isAutoPlay());
    }

    private void startPlay() {
        if ( (mediaSource != null) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PLAYING) ) {
            // no media file opened or playing has been stopped
            mediaTransportControls.play();
        }
    }

    private void pausePlay() {
        if ( (mediaSource != null) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) ) {
            // no media file opened or playing has been stopped
            mediaTransportControls.pause();
        }
    }

    private void stopPlay() {
        if (playingParam.isAutoPlay()) {
            // auto play
            startAutoPlay();
        } else {
            if (playingParam.getRepeatStatus() == NoRepeatPlaying) {
                // no repeat playing
                if ((mediaSource != null) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_NONE)) {
                    // media file opened or playing has been stopped
                    mediaTransportControls.stop();
                }
            } else {
                replayMedia();
            }
        }
    }

    private void replayMedia() {
        if ( (mediaSource != null) && (numberOfAudioRenderers>0) ) {
            if (playingParam.isMediaSourcePrepared()) {
                // song is playing, paused, or finished playing
                // cannot do the following statement (exoPlayer.setPlayWhenReady(false); )
                // because it will send Play.STATE_ENDED event after the playing has finished
                // but the playing was stopped in the middle of playing then wo'nt send
                // Play.STATE_ENDED event
                // exoPlayer.setPlayWhenReady(false);
                long currentAudioPosition = 0;
                playingParam.setCurrentAudioPosition(currentAudioPosition);
                exoPlayer.seekTo(currentAudioPosition);
                setProperAudioTrackAndChannel();
                exoPlayer.retry();
                exoPlayer.setPlayWhenReady(true);
                Log.d(TAG, "replayMedia()--> exoPlayer.seekTo(currentAudioPosition).");
            } else {
                // song was stopped by user
                mediaTransportControls.prepare();   // prepare and play
                Log.d(TAG, "replayMedia()--> mediaTransportControls.prepare().");
            }
        }

        Log.d(TAG, "replayMedia() is called.");
    }

    private void setAudioTrackAndChannel(int audioRenderer, int audioChannel) {
        if (numberOfAudioRenderers > 0) {
            // select audio renderer
            playingParam.setCurrentAudioRendererPlayed(audioRenderer);

            // select audio channel
            playingParam.setCurrentChannelPlayed(audioChannel);
            setAudioVolume(playingParam.getCurrentVolume());
        }
    }

    private void switchAudioToVocal() {
        int vocalAudioRenderer = playingParam.getVocalAudioRenderer();
        int vocalAudioChannel = playingParam.getVocalAudioChannel();
        playingParam.setMusicOrVocalOrNoSetting(PlayingVocal);
        setAudioTrackAndChannel(vocalAudioRenderer, vocalAudioChannel);
    }

    private void switchAudioToMusic() {
        int musicAudioRenderer = playingParam.getMusicAudioRenderer();
        int musicAudioChannel = playingParam.getMusicAudioChannel();
        playingParam.setMusicOrVocalOrNoSetting(PlayingMusic);
        setAudioTrackAndChannel(musicAudioRenderer, musicAudioChannel);
    }

    private void setAudioVolume(float volume) {
        if (numberOfAudioRenderers > 0) {
            // get current channel
            int currentChannelPlayed = playingParam.getCurrentChannelPlayed();
            //
            if (currentChannelPlayed == SmileApplication.leftChannel) {
                stereoVolumeAudioProcessor.setVolume(volume, 0.0f);
            } else if (currentChannelPlayed == SmileApplication.rightChannel) {
                stereoVolumeAudioProcessor.setVolume(0.0f, volume);
            } else {
                stereoVolumeAudioProcessor.setVolume(volume, volume);
            }
            playingParam.setCurrentVolume(volume);
        }
    }

    private void setProperAudioTrackAndChannel() {
        if (playingParam.isAutoPlay()) {
            if (playingParam.isPlayingPublic()) {
                switchAudioToVocal();
            } else {
                switchAudioToMusic();
            }
        } else {
            // not auto playing media, means using open menu to open a media
            switchAudioToVocal();   // music and vocal are the same in this case
        }
    }

    private class ExoPlayerEventListener implements Player.EventListener {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

            Log.d(TAG,"Player.EventListener.onPlayerStateChanged is called.");
            Log.d(TAG, "Playback state = " + playbackState);

            if (playbackState == Player.STATE_BUFFERING) {
                showBufferingMessage();
                return;
            }
            dismissBufferingMessage();

            if (playbackState == Player.STATE_ENDED) {
                if (playingParam.isAutoPlay()) {
                    // start playing next video from list
                    startAutoPlay();
                } else {
                    // stop or end of playing
                    if (playingParam.getRepeatStatus() != NoRepeatPlaying) {
                        replayMedia();
                    }
                }
                Log.d(TAG, "Playback state = Player.STATE_ENDED after startAutoPlay()");
                return;
            }
            if (playbackState == Player.STATE_IDLE) {
                // There is bug here
                // The listener will get twice of (Player.STATE_IDLE)
                // when user stop playing using ExoPlayer.stop()
                // so do not put startAutoPlay() inside this event
                if (mediaSource != null) {
                    playingParam.setMediaSourcePrepared(false);
                    Log.d(TAG, "Song was stopped by user.");
                }
                return;
            }
            if (playbackState == Player.STATE_READY) {
                if (!playingParam.isMediaSourcePrepared()) {
                    // the first time of Player.STATE_READY means prepared

                    DefaultTrackSelector.Parameters parameters = trackSelector.getParameters();
                    DefaultTrackSelector.ParametersBuilder parametersBuilder = parameters.buildUpon();
                    // trackSelector.setParameters(parametersBuilder.build());  // for the testing
                    // or trackSelector.setParameters(parametersBuilder);  // for the testing

                    int numAudioRenderer = 0;
                    int numVideoRenderer = 0;
                    int numUnknownRenderer = 0;
                    String rendererName = "";

                    videoRendererIndexMap.clear();
                    audioRendererIndexMap.clear();

                    MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                    if (mappedTrackInfo != null) {
                        int rendererCount = mappedTrackInfo.getRendererCount();
                        Log.d(TAG, "mappedTrackInfo-->rendererCount = " + rendererCount);
                        for (int rendererIndex = 0; rendererIndex < rendererCount; rendererIndex++) {
                            TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);
                            if (trackGroupArray != null) {
                                int arraySize = trackGroupArray.length;
                                for (int groupIndex = 0; groupIndex < arraySize; groupIndex++) {
                                    TrackGroup trackGroup = trackGroupArray.get(groupIndex);
                                    if (trackGroup != null) {
                                        int groupSize = trackGroup.length;
                                        for (int trackIndex = 0; trackIndex < groupSize; trackIndex++) {
                                            Format format = trackGroup.getFormat(trackIndex);
                                            int rendererType = mappedTrackInfo.getRendererType(rendererIndex);
                                            switch (rendererType) {
                                                case C.TRACK_TYPE_AUDIO:
                                                    Log.d(TAG, "The audio renderer index = " + rendererIndex);
                                                    // track starts from 1
                                                    rendererName = getString(R.string.audioTrackString) + " " + (numAudioRenderer + 1);
                                                    audioRendererIndexMap.put(rendererName, rendererIndex);
                                                    int stereoMode = format.stereoMode;
                                                    Log.d(TAG, "Format.stereoMode = " + stereoMode);
                                                    // parametersBuilder.setRendererDisabled(rendererIndex, true); // for testing
                                                    numAudioRenderer++;
                                                    break;
                                                case C.TRACK_TYPE_VIDEO:
                                                    Log.d(TAG, "The video renderer index = " + rendererIndex);
                                                    rendererName = getString(R.string.videoTrackString) + " " + (numVideoRenderer + 1);
                                                    videoRendererIndexMap.put(rendererName, rendererIndex);
                                                    // parametersBuilder.setRendererDisabled(rendererIndex, true); // for testing
                                                    numVideoRenderer++;
                                                    break;
                                                default:
                                                    numUnknownRenderer++;
                                                    break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "mappedTrackInfo is null.");
                    }

                    trackSelector.setParameters(parametersBuilder.build());

                    // build R.id.audioTrack submenu
                    if (audioTrackMenuItem.hasSubMenu()) {
                        SubMenu subMenu = audioTrackMenuItem.getSubMenu();
                        subMenu.clear();
                        for (String audioTrackString : audioRendererIndexMap.keySet()) {
                            subMenu.add(audioTrackString);
                        }
                    } else {
                        ((Menu)audioTrackMenuItem).addSubMenu("Text title");
                    }

                    Log.d(TAG, "audioRenderer = " + numAudioRenderer);
                    Log.d(TAG, "videoRenderer = " + numVideoRenderer);
                    Log.d(TAG, "unknownRenderer = " + numUnknownRenderer);

                    numberOfVideoRenderers = numVideoRenderer;
                    if (numberOfVideoRenderers == 0) {
                        playingParam.setCurrentVideoRendererPlayed(noVideoRenderer);
                    }
                    numberOfAudioRenderers = numAudioRenderer;
                    if (numberOfAudioRenderers == 0) {
                        playingParam.setCurrentAudioRendererPlayed(noAudioTrack);
                        playingParam.setCurrentChannelPlayed(noAudioChannel);
                    } else {
                        int audioRenderer = playingParam.getCurrentAudioRendererPlayed();
                        int audioChannel = playingParam.getCurrentChannelPlayed();
                        setAudioTrackAndChannel(audioRenderer, audioChannel);
                    }
                }

                playingParam.setMediaSourcePrepared(true);

                return;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            switch (error.type) {
                case ExoPlaybackException.TYPE_SOURCE:
                    Log.e(TAG, "TYPE_SOURCE: " + error.getSourceException().getMessage());
                    break;

                case ExoPlaybackException.TYPE_RENDERER:
                    Log.e(TAG, "TYPE_RENDERER: " + error.getRendererException().getMessage());
                    break;

                case ExoPlaybackException.TYPE_UNEXPECTED:
                    Log.e(TAG, "TYPE_UNEXPECTED: " + error.getUnexpectedException().getMessage());
                    break;
            }
            Log.d(TAG,"Player.EventListener.onPlayerError() is called.");

            String formatNotSupportedString = getString(R.string.formatNotSupportedString);
            if (playingParam.isAutoPlay()) {
                // go to next one in the list
                if (canShowNotSupportedFormat) {
                    // only show once
                    ScreenUtil.showToast(getApplicationContext(), formatNotSupportedString, toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
                    canShowNotSupportedFormat = false;
                }
                startAutoPlay();
            } else {
                ScreenUtil.showToast(getApplicationContext(), formatNotSupportedString, toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
            }
        }
    }

    private class MediaControllerCallback extends MediaControllerCompat.Callback {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if( state == null ) {
                return;
            }

            int currentState = state.getState();
            switch (currentState) {
                case PlaybackStateCompat.STATE_NONE:
                    // initial state and when playing is stopped by user
                    Log.d(TAG, "PlaybackStateCompat.STATE_NONE");
                    if (mediaSource != null) {
                        Log.d(TAG, "MediaControllerCallback--> Song was stopped by user.");
                    }
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    // when finished playing
                    Log.d(TAG, "PlaybackStateCompat.STATE_STOPPED");
                    break;
            }
            playingParam.setCurrentPlaybackState(currentState);
            Log.d(TAG, "MediaControllerCallback.onPlaybackStateChanged() is called. " + currentState);
        }
    }

    private class PlaybackPreparer implements MediaSessionConnector.PlaybackPreparer {

        @Override
        public long getSupportedPrepareActions() {
            long supportedPrepareActions = PlaybackPreparer.ACTIONS;
            Log.d(TAG, "MediaSessionConnector.PlaybackPreparer.getSupportedPrepareActions() is called.");
            return supportedPrepareActions;
        }

        @Override
        public void onPrepare() {
            exoPlayer.prepare(mediaSource);
            exoPlayer.setPlayWhenReady(true);
            Log.d(TAG, "MediaSessionConnector.PlaybackPreparer.onPrepare() is called.");
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "MediaSessionConnector.PlaybackPreparer.onPrepareFromMediaId() is called.");
        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            Log.d(TAG, "MediaSessionConnector.PlaybackPreparer.onPrepareFromSearch() is called.");
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            Log.d(TAG, "Uri = " + uri);
            // MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            // DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory().setMp4ExtractorFlags ( Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS);
            // mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(uri);
            playingParam.setMediaSourcePrepared(false);
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            exoPlayer.prepare(mediaSource);
            exoPlayer.seekTo(playingParam.getCurrentAudioPosition());
            int playbackState = playingParam.getCurrentPlaybackState();
            switch (playbackState) {
                case PlaybackStateCompat.STATE_PAUSED:
                    exoPlayer.setPlayWhenReady(false);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    exoPlayer.setPlayWhenReady(false);
                    break;
                default:
                    exoPlayer.setPlayWhenReady(true);  // start playing when ready
                    break;
            }

            Log.d(TAG, "MediaSessionConnector.PlaybackPreparer.onPrepareFromUri() is called--> " + playbackState);
        }

        @Override
        public boolean onCommand(Player player, ControlDispatcher controlDispatcher, String command, Bundle extras, ResultReceiver cb) {
            return false;
        }
    }
}
