package com.smile.karaokeplayer;

import android.Manifest;
import android.content.Context;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.smile.smilelibraries.privacy_policy.PrivacyPolicyUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.io.File;
import java.util.Locale;

import static com.smile.karaokeplayer.Utilities.ExternalStorageUtil.isExternalStorageReadable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = new String(".MainActivity");
    private static final String LOG_TAG = new String("MediaSessionCompatTag");
    private static final int PERMISSION_REQUEST_CODE = 0x11;
    private static final int PrivacyPolicyActivityRequestCode = 10;

    private float textFontSize;
    private float fontScale;
    // private Toolbar supportToolbar;  // use customized ToolBar
    private ActionBar supportToolbar;   // use default ActionBar

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
    // submenu of channel
    private MenuItem leftChannelMenuItem;
    private MenuItem rightChannelMenuItem;
    private MenuItem stereoChannelMenuItem;

    private VideoSurfaceView videoSurfaceView;

    private MediaSessionCompat mediaSessionCompat;
    // private PlaybackStateCompat.Builder playbackStateBuilder;    // no need if use MediaSessionConnector
    private MediaControllerCompat mediaControllerCompat;
    private MediaControllerCallback mediaControllerCallback;
    private MediaControllerCompat.TransportControls mediaTransportControls;
    private MediaSessionConnector mediaSessionConnector;

    private PlayerView videoPlayerView;
    private DataSource.Factory dataSourceFactory;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private SimpleExoPlayer exoPlayer;

    private boolean isAutoPlay;
    private boolean hasPermissionForExternalStorage;
    private int mCurrentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, SmileApplication.FontSize_Scale_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(this, SmileApplication.FontSize_Scale_Type, 0.0f);

        // int colorDarkOrange = ContextCompat.getColor(KaraokeApp.AppContext, R.color.darkOrange);
        // int colorRed = ContextCompat.getColor(KaraokeApp.AppContext, R.color.red);
        // int colorDarkRed = ContextCompat.getColor(SmileApplication.AppContext, R.color.darkRed);
        // int colorDarkGreen = ContextCompat.getColor(KaraokeApp.AppContext, R.color.darkGreen);

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

        initExoPlayer();

        initMediaSessionCompat();

        isAutoPlay = false;
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

        // use default ActionBar
        final Context wrapper = supportToolbar.getThemedContext();
        //

        final float fScale = fontScale;
        ScreenUtil.buildActionViewClassMenu(this, wrapper, menu, fScale, SmileApplication.FontSize_Scale_Type);

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
        // submenu of channel
        leftChannelMenuItem = menu.findItem(R.id.leftChannel);
        rightChannelMenuItem = menu.findItem(R.id.rightChannel);
        stereoChannelMenuItem = menu.findItem(R.id.stereoChannel);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (!hasPermissionForExternalStorage) {
            Toast.makeText(getApplicationContext(), "PERMISSION_DENIED", Toast.LENGTH_SHORT).show();
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
                if (isExternalStorageReadable()) {
                    // has readable external storage
                    Toast.makeText(this, "Has readable external storage", Toast.LENGTH_LONG).show();
                    String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                    Log.d(TAG, "Public root directory: " + externalPath);
                    String filePath = externalPath + "/Song/perfume_h264.mp4";
                    Log.d(TAG, "File path: " + filePath);
                    File songFile = new File(filePath);
                    if (songFile.exists()) {
                        Log.d(TAG, "perfume_h264.mp4 exists");
                        Uri mediaUri = Uri.parse("file://" + filePath);
                        mediaTransportControls.prepareFromUri(mediaUri, null);
                    } else {
                        Log.d(TAG, "perfume_h264.mp4 does not exist");
                    }
                } else {
                    Toast.makeText(this, "Does not Have readable external storage", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Does not Have readable external storage.");
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
                Toast.makeText(getApplicationContext(), "PERMISSION_DENIED", Toast.LENGTH_SHORT).show();
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
    public void onBackPressed() {
        exitApplication();
    }

    private void exitApplication() {
        finish();
    }

    private void initExoPlayer() {
        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getPackageName()));
        trackSelector = new DefaultTrackSelector();
        trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();
        trackSelector.setParameters(trackSelectorParameters);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

        // no need. It will ve overridden by MediaSessionConnector
        // exoPlayer.addListener(new ExoPlayerEventListener());

        videoPlayerView.setPlayer(exoPlayer);
        videoPlayerView.requestFocus();
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
        playbackStateBuilder = new PlaybackStateCompat.Builder();
        setMediaPlaybackState(mCurrentState);
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
        */

        // MySessionCallback has methods that handle callbacks from a media controller
        // No need because it will be overridden by MediaSessionConnector
        // MediaSessionCallback mediaSessionCallback = new MediaSessionCallback();
        // mediaSessionCompat.setCallback(mediaSessionCallback);

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

    /*
    // No need if use MediaSessionConnector
    private void setMediaPlaybackState(int state) {
        // PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        } else {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
        }
        // playbackStateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        playbackStateBuilder.setState(state, exoPlayer.getContentPosition(), 1f);
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
    }
    */

    private class ExoPlayerEventListener implements Player.EventListener {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
                mediaTransportControls.prepare();
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

    /*
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
                MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
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
    */

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
