package com.smile.karaokeplayer;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
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

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    private boolean isAutoPlay;

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
        mediaSession = new MediaSessionCompat(this, LOG_TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Do not let MediaButtons restart the player when the app is not visible
        mediaSession.setMediaButtonReceiver(null);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());

        // MySessionCallback has methods that handle callbacks from a media controller
        MediaSessionCallback mediaSessionCallback = new MediaSessionCallback();
        mediaSession.setCallback(mediaSessionCallback);

        // Create a MediaControllerCompat
        MediaControllerCompat mediaController = new MediaControllerCompat(this, mediaSession);
        MediaControllerCompat.setMediaController(this, mediaController);

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

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.autoPlay) {
            // item.isChecked() return the previous value
            isAutoPlay = !isAutoPlay;
            item.setChecked(isAutoPlay);
        }
        if (id == R.id.open) {

        }
        if (id == R.id.close) {

        }
        if (id == R.id.privacyPolicy) {
            PrivacyPolicyUtil.startPrivacyPolicyActivity(this, SmileApplication.PrivacyPolicyUrl, PrivacyPolicyActivityRequestCode);
        }
        if (id == R.id.exit) {
            finish();
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
        mediaSession.release();
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            super.onPlay();
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onStop() {
            super.onStop();
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
        }

        @Override
        public void onRewind() {
            super.onRewind();
        }
    }
}
