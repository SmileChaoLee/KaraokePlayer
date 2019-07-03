package com.smile.karaokeplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ResultReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import com.smile.karaokeplayer.audioprocessor_implement.StereoVolumeAudioProcessor;
import com.smile.smilelibraries.privacy_policy.PrivacyPolicyUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.io.File;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.smile.karaokeplayer.Utilities.ExternalStorageUtil.isExternalStorageReadable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = new String(".MainActivity");
    private static final String LOG_TAG = new String("MediaSessionCompatTag");
    private static final int PERMISSION_REQUEST_CODE = 0x11;
    private static final int PrivacyPolicyActivityRequestCode = 10;
    private static final int FILE_READ_REQUEST_CODE = 1;

    private float textFontSize;
    private float fontScale;
    private float toastTextSize;
    // private Toolbar supportToolbar;  // use customized ToolBar
    private ActionBar supportToolbar;   // use default ActionBar

    private String accessExternalStoragePermissionDeniedString;
    private String noReadableExternalStorageString;

    private Menu mainMenu;
    // submenu of file
    private MenuItem autoPlayMenuItem;
    // submenu of action
    private MenuItem playMenuItem;
    private MenuItem pauseMenuItem;
    private MenuItem stopMenuItem;
    private MenuItem fforwardMenuItem;
    private MenuItem rewindMenuItem;
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
    private MediaSource mediaSource;
    private AudioAttributes.Builder audioAttributesBuilder;

    private SortedMap<String, Integer> videoRendererIndexMap;
    private SortedMap<String, Integer> audioRendererIndexMap;

    private boolean hasPermissionForExternalStorage;
    private int mCurrentState;
    private boolean isMediaSourcePrepared;
    private boolean isAutoPlay;

    private static final int leftChannel = 0;
    private static final int rightChannel = 1;
    private static final int stereoChannel = 2;
    private int currentChannelPlayed = stereoChannel;

    private float currentPosition = 0.0f;
    private float currentVolume = 1.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, SmileApplication.FontSize_Scale_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(this, SmileApplication.FontSize_Scale_Type, 0.0f);
        toastTextSize = 0.8f * textFontSize;

        accessExternalStoragePermissionDeniedString = getString(R.string.accessExternalStoragePermissionDeniedString);
        noReadableExternalStorageString = getString(R.string.noReadableExternalStorageString);

        // int colorDarkOrange = ContextCompat.getColor(KaraokeApp.AppContext, R.color.darkOrange);
        // int colorRed = ContextCompat.getColor(KaraokeApp.AppContext, R.color.red);
        // int colorDarkRed = ContextCompat.getColor(SmileApplication.AppContext, R.color.darkRed);
        // int colorDarkGreen = ContextCompat.getColor(KaraokeApp.AppContext, R.color.darkGreen);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // use default ActionBar
        setTitle(String.format(Locale.getDefault(), ""));
        supportToolbar = getSupportActionBar();
        TextView titleView = new TextView(this);
        titleView.setText(supportToolbar.getTitle());
        titleView.setTextColor(Color.WHITE);
        ScreenUtil.resizeTextSize(titleView, textFontSize, SmileApplication.FontSize_Scale_Type);
        supportToolbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        supportToolbar.setCustomView(titleView);
        //

        // Video player view
        videoPlayerView = findViewById(R.id.videoPlayView);
        //

        videoRendererIndexMap = new TreeMap<>();
        audioRendererIndexMap = new TreeMap<>();

        initExoPlayer();
        initMediaSessionCompat();

        isAutoPlay = false;
        isMediaSourcePrepared = false;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.menu_main, menu);
        mainMenu = menu;

        // use default ActionBar
        final Context wrapper = supportToolbar.getThemedContext();
        //
        ScreenUtil.buildActionViewClassMenu(this, wrapper, menu, fontScale, SmileApplication.FontSize_Scale_Type);

        // submenu of file
        autoPlayMenuItem = menu.findItem(R.id.autoPlay);
        // submenu of action
        playMenuItem = menu.findItem(R.id.play);
        pauseMenuItem = menu.findItem(R.id.pause);
        stopMenuItem = menu.findItem(R.id.stop);
        fforwardMenuItem = menu.findItem(R.id.fforward);
        rewindMenuItem = menu.findItem(R.id.rewind);
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

        int id = item.getItemId();

        switch (id) {
            case R.id.file:
                autoPlayMenuItem.setCheckable(true);
                if (isAutoPlay) {
                    autoPlayMenuItem.setChecked(true);
                }
                break;
            case R.id.autoPlay:
                // item.isChecked() return the previous value
                isAutoPlay = !isAutoPlay;
                if (isAutoPlay) {
                    // start playing video from list
                }
                break;
            case R.id.open:
                isMediaSourcePrepared = false;
                if (isExternalStorageReadable()) {
                    // has readable external storage
                    /*
                    ScreenUtil.showToast(this, "Has readable external storage", toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
                    String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                    Log.d(TAG, "Public root directory: " + externalPath);
                    String filePath = externalPath + "/Song/perfume_h264.mp4";
                    Log.d(TAG, "File path: " + filePath);
                    File songFile = new File(filePath);
                    if (songFile.exists()) {
                        Log.d(TAG, "File exists");
                        Uri mediaUri = Uri.parse("file://" + filePath);
                        mediaTransportControls.prepareFromUri(mediaUri, null);
                    } else {
                        Log.d(TAG, "File does not exist");
                    }
                    */
                    selectFileToOpen();
                } else {
                    ScreenUtil.showToast(this, noReadableExternalStorageString, toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
                    Log.d(TAG, noReadableExternalStorageString);
                }
                break;
            case R.id.close:
                break;
            case R.id.privacyPolicy:
                PrivacyPolicyUtil.startPrivacyPolicyActivity(this, SmileApplication.PrivacyPolicyUrl, PrivacyPolicyActivityRequestCode);
                break;
            case R.id.exit:
                finish();
                break;
            case R.id.action:
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
                if (mCurrentState == PlaybackStateCompat.STATE_STOPPED) {
                    stopMenuItem.setCheckable(true);
                    stopMenuItem.setChecked(true);
                } else {
                    stopMenuItem.setCheckable(false);
                }
                if (mCurrentState == PlaybackStateCompat.STATE_FAST_FORWARDING) {
                    fforwardMenuItem.setCheckable(true);
                    fforwardMenuItem.setChecked(true);
                } else {
                    fforwardMenuItem.setCheckable(false);
                }
                if (mCurrentState == PlaybackStateCompat.STATE_REWINDING) {
                    rewindMenuItem.setCheckable(true);
                    rewindMenuItem.setChecked(true);
                } else {
                    rewindMenuItem.setCheckable(false);
                }
                // toTvMenuItem
                break;
            case R.id.play:
                if (mCurrentState != PlaybackStateCompat.STATE_PLAYING) {
                    item.setCheckable(true);
                    item.setChecked(true);
                    mediaTransportControls.play();
                }
                break;
            case R.id.pause:
                if (mCurrentState != PlaybackStateCompat.STATE_PAUSED) {
                    item.setCheckable(true);
                    item.setChecked(true);
                    mediaTransportControls.pause();
                }
                break;
            case R.id.stop:
                if (mCurrentState != PlaybackStateCompat.STATE_STOPPED) {
                    item.setCheckable(true);
                    item.setChecked(true);
                    mediaTransportControls.stop();
                }
                break;
            case R.id.replay:
                break;
            case R.id.audioTrack:
                // if there are audio tracks
                SubMenu subMenu = item.getSubMenu();
                final Context wrapper = supportToolbar.getThemedContext();
                ScreenUtil.buildActionViewClassMenu(this, wrapper, subMenu, fontScale, SmileApplication.FontSize_Scale_Type);
                break;
            case R.id.channel:
                if (isMediaSourcePrepared) {
                    if (currentChannelPlayed == leftChannel) {
                        leftChannelMenuItem.setCheckable(true);
                        leftChannelMenuItem.setChecked(true);
                    } else {
                        leftChannelMenuItem.setCheckable(false);
                        leftChannelMenuItem.setChecked(false);
                    }
                    if (currentChannelPlayed == rightChannel) {
                        rightChannelMenuItem.setCheckable(true);
                        rightChannelMenuItem.setChecked(true);
                    } else {
                        rightChannelMenuItem.setCheckable(false);
                        rightChannelMenuItem.setChecked(false);
                    }
                    if (currentChannelPlayed == stereoChannel) {
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
                break;
            case R.id.leftChannel:
                currentChannelPlayed = leftChannel;
                stereoVolumeAudioProcessor.setVolume(currentVolume, 0.0f);
                break;
            case R.id.rightChannel:
                currentChannelPlayed = rightChannel;
                stereoVolumeAudioProcessor.setVolume(0.0f, currentVolume);
                break;
            case R.id.stereoChannel:
                currentChannelPlayed = stereoChannel;
                stereoVolumeAudioProcessor.setVolume(currentVolume, currentVolume);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
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

        releaseMediaSessionCompat();
        releaseExoPlayer();
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
            Uri mediaUri = null;
            if (data != null) {
                mediaUri = data.getData();
                Log.i(TAG, "Uri: " + mediaUri.toString());
                mediaTransportControls.prepareFromUri(mediaUri, null);
            }
        }
    }

    @Override
    public void onBackPressed() {
        exitApplication();
    }

    private void exitApplication() {
        finish();
    }

    private void selectFileToOpen() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only videos, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("video/*");

        startActivityForResult(intent, FILE_READ_REQUEST_CODE);
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
        }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);

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

        // Log.d(TAG, "FfmpegLibrary.isAvailable() = " + FfmpegLibrary.isAvailable());

    }

    private void releaseExoPlayer() {
        if (exoPlayer != null) {
            // exoPlayer.removeListener();
            // exoPlayer.removeVideoListener();
            // exoPlayer.removeAudioListener();
            // exoPlayer.removeAnalyticsListener();
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private void initMediaSessionCompat() {
        // Create a MediaSessionCompat
        mediaSessionCompat = new MediaSessionCompat(this, LOG_TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mediaSessionCompat.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Do not let MediaButtons restart the player when the app is not visible
        mediaSessionCompat.setMediaButtonReceiver(null);
        mCurrentState = PlaybackStateCompat.STATE_NONE;

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        /*
        // PlaybackStateCompat is already defined and mapped in MediaSessionConnector
        setMediaPlaybackState(mCurrentState);
        */

        // MySessionCallback has methods that handle callbacks from a media controller
        // No need because it will be overridden by MediaSessionConnector
        /*
        MediaSessionCallback mediaSessionCallback = new MediaSessionCallback();
        mediaSessionCompat.setCallback(mediaSessionCallback);
        */

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

        // playbackStateBuilder = null; // no need if use MediaSessionConnector
    }

    // No need if use MediaSessionConnector
    private void setMediaPlaybackState(int state) {
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        } else {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
        }
        // playbackStateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        playbackStateBuilder.setState(state, exoPlayer.getContentPosition(), 1f);
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
    }
    //

    private class ExoPlayerEventListener implements Player.EventListener {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.d(TAG,"Player.EventListener.onPlayerStateChanged is called.");
            if (playbackState == Player.STATE_ENDED) {
                isMediaSourcePrepared = false;
                return;
            }
            if (playbackState == Player.STATE_READY) {
                if (!isMediaSourcePrepared) {
                    // the first time of Player.STATE_READY means prepared

                    currentChannelPlayed = stereoChannel;
                    currentVolume = 1.0f;
                    currentPosition = 0.0f;

                    exoPlayer.setVolume(currentVolume);

                    DefaultTrackSelector.Parameters parameters = trackSelector.getParameters();
                    DefaultTrackSelector.ParametersBuilder parametersBuilder = parameters.buildUpon();
                    // trackSelector.setParameters(parametersBuilder.build());  // for the testing
                    // or trackSelector.setParameters(parametersBuilder);  // for the testing

                    int audioRenderer = 0;
                    int videoRenderer = 0;
                    int unknownRenderer = 0;
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
                                                    rendererName = "Audio_" + audioRenderer;
                                                    audioRendererIndexMap.put(rendererName, rendererIndex);
                                                    int stereoMode = format.stereoMode;
                                                    Log.d(TAG, "Format.stereoMode = " + stereoMode);
                                                    // parametersBuilder.setRendererDisabled(rendererIndex, true); // for testing
                                                    audioRenderer++;
                                                    break;
                                                case C.TRACK_TYPE_VIDEO:
                                                    Log.d(TAG, "The video renderer index = " + rendererIndex);
                                                    rendererName = "Video_" + videoRenderer;
                                                    videoRendererIndexMap.put(rendererName, rendererIndex);
                                                    // parametersBuilder.setRendererDisabled(rendererIndex, true); // for testing
                                                    videoRenderer++;
                                                    break;
                                                default:
                                                    unknownRenderer++;
                                                    break;
                                            }
                                            Log.d(TAG, "Format = " + format);
                                            Log.d(TAG, "Format.sampleMimeType = " + format.sampleMimeType);
                                            Log.d(TAG, "Format.containerMimeType = " + format.containerMimeType);
                                            Log.d(TAG, "Format.label = " + format.label);
                                            Log.d(TAG, "Format.codecs = " + format.codecs);
                                            Log.d(TAG, "Format.language = " + format.language);
                                            Log.d(TAG, "Format.channelCount = " + format.channelCount);
                                            Log.d(TAG, "Format.accessibilityChannel = " + format.accessibilityChannel);
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
                    }

                    Log.d(TAG, "audioRenderer = " + audioRenderer);
                    Log.d(TAG, "videoRenderer = " + videoRenderer);
                    Log.d(TAG, "unknownRenderer = " + unknownRenderer);
                }

                isMediaSourcePrepared = true;

                return;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {

        }
    }

    private class MediaControllerCallback extends MediaControllerCompat.Callback {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            Log.d(TAG, "MediaControllerCallback.onPlaybackStateChanged() is called.");
            super.onPlaybackStateChanged(state);
            if( state == null ) {
                return;
            }
            mCurrentState = state.getState();
        }
    }

    // Already defined in MediaSessionConnector if use MediaSessionConnector
    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
        }

        @Override
        public void onPrepare() {
            super.onPrepare();

            exoPlayer.seekTo(0);
            exoPlayer.setPlayWhenReady(false);
            setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);

            Log.d(TAG, "MediaSessionCallback.onPrepare() is called.");
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            super.onPrepareFromUri(uri, extras);
            try {
                mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                exoPlayer.prepare(mediaSource);
                exoPlayer.setPlayWhenReady(false);  // do not start playing
                setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);
            } catch (Exception ex) {
                Log.d(TAG, "Exception happened to onPrepareFromUri() of MediaSessionCallback.");
                ex.printStackTrace();
            }

            Log.d(TAG, "MediaSessionCallback.onPrepareFromUri() is called.");
        }

        @Override
        public void onPlay() {
            super.onPlay();
            MediaControllerCompat controller = mediaSessionCompat.getController();
            PlaybackStateCompat stateCompat = controller.getPlaybackState();
            int state = stateCompat.getState();
            if (state != PlaybackStateCompat.STATE_PLAYING) {
                int exoPlayerState = exoPlayer.getPlaybackState();
                if (exoPlayerState == Player.STATE_READY) {
                    exoPlayer.setPlayWhenReady(true);
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }
            }
            Log.d(TAG, "MediaSessionCallback.onPlay() is called.");
        }

        @Override
        public void onPause() {
            super.onPause();
            MediaControllerCompat controller = mediaSessionCompat.getController();
            PlaybackStateCompat stateCompat = controller.getPlaybackState();
            int state = stateCompat.getState();
            if (state != PlaybackStateCompat.STATE_PAUSED) {
                exoPlayer.setPlayWhenReady(false);
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            }
            Log.d(TAG, "MediaSessionCallback.onPause() is called.");
        }

        @Override
        public void onStop() {
            super.onStop();
            MediaControllerCompat controller = mediaSessionCompat.getController();
            PlaybackStateCompat stateCompat = controller.getPlaybackState();
            int state = stateCompat.getState();
            if (state != PlaybackStateCompat.STATE_STOPPED) {
                // exoPlayer.stop();
                exoPlayer.setPlayWhenReady(false);
                exoPlayer.seekTo(0);
                exoPlayer.retry();
                setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            }
            Log.d(TAG, "MediaSessionCallback.onStop() is called.");
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            setMediaPlaybackState(PlaybackStateCompat.STATE_FAST_FORWARDING);
            Log.d(TAG, "MediaSessionCallback.onFastForward() is called.");
        }

        @Override
        public void onRewind() {
            super.onRewind();
            setMediaPlaybackState(PlaybackStateCompat.STATE_REWINDING);
            Log.d(TAG, "MediaSessionCallback.onRewind() is called.");
        }
    }
    //

    private class PlaybackPreparer implements MediaSessionConnector.PlaybackPreparer {

        @Override
        public long getSupportedPrepareActions() {
            long supportedPrepareActions = PlaybackPreparer.ACTIONS;
            Log.d(TAG, "MediaSessionConnector.PlaybackPreparer.getSupportedPrepareActions() is called.");
            return supportedPrepareActions;
        }

        @Override
        public void onPrepare() {
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

            // MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            // DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory().setMp4ExtractorFlags ( Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS);
            // MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(uri);
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            exoPlayer.prepare(mediaSource);
            exoPlayer.setPlayWhenReady(false);  // do not start playing

            Log.d(TAG, "MediaSessionConnector.PlaybackPreparer.onPrepareFromUri() is called.");
        }

        @Override
        public boolean onCommand(Player player, ControlDispatcher controlDispatcher, String command, Bundle extras, ResultReceiver cb) {
            return false;
        }
    }
}
