package com.smile.karaokeplayer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.ui.PlayerView;
import com.smile.karaokeplayer.Presenters.ExoPlayerPresenter;

public class ExoPlayerActivity extends PlayerBaseActivity implements ExoPlayerPresenter.PresentView{

    private static final String TAG = "ExoPlayerActivity";

    private ExoPlayerPresenter mPresenter;
    private PlayerView videoExoPlayerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate() is called.");

        mPresenter = new ExoPlayerPresenter(this, this);
        Intent callingIntent = getIntent();
        mPresenter.initializeVariables(savedInstanceState, callingIntent);

        setPlayerBasePresenter(mPresenter);   // set presenter to PlayBasePresenter object

        super.onCreate(savedInstanceState);

        mPresenter.initExoPlayer();   // must be before volumeSeekBar settings
        mPresenter.initMediaSessionCompat();

        // Video player view
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        videoExoPlayerView = new com.google.android.exoplayer2.ui.PlayerView(this);
        videoExoPlayerView.setLayoutParams(layoutParams);
        videoExoPlayerView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        playerViewLinearLayout.addView(videoExoPlayerView);

        videoExoPlayerView.setVisibility(View.VISIBLE);
        videoExoPlayerView.setPlayer(mPresenter.getExoPlayer());
        videoExoPlayerView.setUseArtwork(true);
        videoExoPlayerView.setUseController(false);
        videoExoPlayerView.requestFocus();

        int currentProgress = mPresenter.setCurrentProgressForVolumeSeekBar();
        volumeSeekBar.setProgressAndThumb(currentProgress);

        mPresenter.playTheSongThatWasPlayedBeforeActivityCreated();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy() is called.");
        mPresenter.releaseMediaSessionCompat();
        mPresenter.releaseExoPlayer();
    }
}
