package com.smile.karaokeplayer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import com.smile.karaokeplayer.Presenters.VLCPlayerPresenter;

import org.videolan.libvlc.util.VLCVideoLayout;

public class VLCPlayerActivity extends PlayerBaseActivity implements VLCPlayerPresenter.PresentView{

    private static final String TAG = new String("VLCPlayerActivity");
    private static final boolean ENABLE_SUBTITLES = true;
    private static final boolean USE_TEXTURE_VIEW = false;

    private VLCPlayerPresenter mPresenter;
    private VLCVideoLayout videoVLCPlayerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate() is called.");

        mPresenter = new VLCPlayerPresenter(this, this);
        Intent callingIntent = getIntent();
        mPresenter.initializeVariables(savedInstanceState, callingIntent);

        setPlayerBasePresenter(mPresenter);   // set presenter to PlayBasePresenter object

        super.onCreate(savedInstanceState);

        mPresenter.initVLCPlayer();   // must be before volumeSeekBar settings
        mPresenter.initMediaSessionCompat();

        // Video player view
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        videoVLCPlayerView = new VLCVideoLayout(this);
        videoVLCPlayerView.setLayoutParams(layoutParams);
        videoVLCPlayerView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        playerViewLinearLayout.addView(videoVLCPlayerView);

        videoVLCPlayerView.setVisibility(View.VISIBLE);

        int currentProgress = mPresenter.setCurrentProgressForVolumeSeekBar();
        volumeSeekBar.setProgressAndThumb(currentProgress);

        mPresenter.playTheSongThatWasPlayedBeforeActivityCreated();
    }

    @Override
    protected void onStart() {
        Log.d(TAG,"onStart() is called.");
        super.onStart();
        videoVLCPlayerView.requestFocus();
        mPresenter.attachPlayerViews(videoVLCPlayerView, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);
    }

    @Override
    protected void onStop() {
        Log.d(TAG,"onStop() is called.");
        super.onStop();
        mPresenter.detachPlayerViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy() is called.");
        mPresenter.releaseMediaSessionCompat();
        mPresenter.releaseVLCPlayer();
    }
}
