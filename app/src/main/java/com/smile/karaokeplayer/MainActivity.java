package com.smile.karaokeplayer;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.smile.smilelibraries.privacy_policy.PrivacyPolicyUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final String TAG = new String(".MainActivity");
    private final String LOG_TAG = new String("MediaSessionCompatTag");
    private final int PrivacyPolicyActivityRequestCode = 10;

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
    private MenuItem replayMenuItem;
    private MenuItem fforwardMenuItem;
    private MenuItem rewindMenuItem;
    private MenuItem toTvMenuItem;
    // submenu of audio
    // submenu of channel
    private MenuItem leftChannelMenuItem;
    private MenuItem rightChannelMenuItem;
    private MenuItem stereoChannelMenuItem;

    private MediaSessionCompat mediaSessionCompat;
    private PlaybackStateCompat.Builder playbackStateBuilder;
    private MediaControllerCompat mediaControllerCompat;
    private MediaControllerCallback mediaControllerCallback;
    private MediaControllerCompat.TransportControls mediaTransportControls;
    private MediaPlayer mediaPlayer;

    private boolean isAutoPlay;
    private int mCurrentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, SmileApplication.FontSize_Scale_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(this, SmileApplication.FontSize_Scale_Type, 0.0f);

        // int colorDarkOrange = ContextCompat.getColor(KaraokeApp.AppContext, R.color.darkOrange);
        // int colorRed = ContextCompat.getColor(KaraokeApp.AppContext, R.color.red);
        int colorDarkRed = ContextCompat.getColor(SmileApplication.AppContext, R.color.darkRed);
        // int colorDarkGreen = ContextCompat.getColor(KaraokeApp.AppContext, R.color.darkGreen);

        setContentView(R.layout.activity_main);

        /*
        // use customized ToolBar
        setSupportActionBar(supportToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitleTextView = findViewById(R.id.toolbarTitleTextView);
        ScreenUtil.resizeTextSize(toolbarTitleTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        //
        */

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
        playbackStateBuilder = new PlaybackStateCompat.Builder();
        setMediaPlaybackState(mCurrentState);
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build());

        // MySessionCallback has methods that handle callbacks from a media controller
        MediaSessionCallback mediaSessionCallback = new MediaSessionCallback();
        mediaSessionCompat.setCallback(mediaSessionCallback);
        mediaSessionCompat.setActive(true); // might need to find better place to put

        // Create a MediaControllerCompat
        mediaControllerCompat = new MediaControllerCompat(this, mediaSessionCompat);
        MediaControllerCompat.setMediaController(this, mediaControllerCompat);
        mediaControllerCallback = new MediaControllerCallback();
        mediaControllerCompat.registerCallback(mediaControllerCallback);
        mediaTransportControls = mediaControllerCompat.getTransportControls();

        mediaPlayer =  new MediaPlayer();
        try {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.demo_video);
            // mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            // afd.close();
            // Uri mediaUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.demo_video);
            Uri mediaUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.background_music);
            mediaPlayer.setDataSource(getApplicationContext(), mediaUri);
            mediaPlayer.prepare();
            mediaPlayer.setVolume(1.0f, 1.0f);
            mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        } catch (Exception ex) {
            Log.d(TAG, "Exception happened to MediaPlayer.");
            ex.printStackTrace();
        }

        isAutoPlay = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.menu_main, menu);

        /*
        // use customized ToolBar
        final int popupThemeId = supportToolbar.getPopupTheme();
        final Context wrapper = new ContextThemeWrapper(this, popupThemeId);
        */

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
        replayMenuItem = menu.findItem(R.id.replay);
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
    protected void onDestroy() {
        super.onDestroy();

        mediaSessionCompat.release();
        mediaSessionCompat = null;
        mediaTransportControls = null;
        if (mediaControllerCallback != null) {
            mediaControllerCompat.unregisterCallback(mediaControllerCallback);
        }
        mediaControllerCompat = null;
        playbackStateBuilder = null;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void setMediaPlaybackState(int state) {
        // PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        } else {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
        }
        playbackStateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
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

            /*
            switch( state.getState() ) {
                case PlaybackStateCompat.STATE_PLAYING: {
                    mCurrentState = PlaybackStateCompat.STATE_PLAYING;
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED: {
                    mCurrentState = STATE_PAUSED;
                    break;
                }
            }
            */

        }
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            super.onPlay();
            if (!mediaPlayer.isPlaying()) {
                try {
                    MediaControllerCompat controller = mediaSessionCompat.getController();
                    PlaybackStateCompat stateCompat = controller.getPlaybackState();
                    int state = stateCompat.getState();
                    if (state == PlaybackStateCompat.STATE_STOPPED) {
                        mediaPlayer.prepare();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                mediaPlayer.start();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            }
            Log.d(TAG, "MediaSessionCallback.onPlay() is called.");
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            }
            Log.d(TAG, "MediaSessionCallback.onPause() is called.");
        }

        @Override
        public void onStop() {
            super.onStop();
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            }
            Log.d(TAG, "MediaSessionCallback.onPause() is onStop.");
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            setMediaPlaybackState(PlaybackStateCompat.STATE_FAST_FORWARDING);
            Log.d(TAG, "MediaSessionCallback.onFastForward() is onStop.");
        }

        @Override
        public void onRewind() {
            super.onRewind();
            setMediaPlaybackState(PlaybackStateCompat.STATE_REWINDING);
            Log.d(TAG, "MediaSessionCallback.onRewind() is onStop.");
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            Log.d(TAG, "MediaSessionCallback.onPlayFromMediaId() is onPlayFromMediaId.");
        }
    }
}
