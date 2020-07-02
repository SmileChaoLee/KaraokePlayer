package com.smile.karaokeplayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.smile.karaokeplayer.Presenters.ExoPlayerPresenter;

public class ExoPlayerActivity extends PlayerBaseActivity implements ExoPlayerPresenter.PresentView{
    private static final String TAG = "ExoPlayerActivity";

    private ExoPlayerPresenter mPresenter;
    private SimpleExoPlayer exoPlayer;
    private PlayerView videoExoPlayerView;
    private CastPlayer castPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate() is called.");

        mPresenter = new ExoPlayerPresenter(this, this);
        Intent callingIntent = getIntent();
        mPresenter.initializeVariables(savedInstanceState, callingIntent);

        setPlayerBasePresenter(mPresenter);   // set presenter to PlayBasePresenter object

        super.onCreate(savedInstanceState);

        mPresenter.initExoPlayerAndCastPlayer();   // must be before volumeSeekBar settings
        mPresenter.initMediaSessionCompat();

        exoPlayer = mPresenter.getExoPlayer();
        castPlayer = mPresenter.getCastPlayer();

        // Video player view
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        videoExoPlayerView = new com.google.android.exoplayer2.ui.PlayerView(this);
        videoExoPlayerView.setLayoutParams(layoutParams);
        videoExoPlayerView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        playerViewLinearLayout.addView(videoExoPlayerView);

        videoExoPlayerView.setVisibility(View.VISIBLE);
        videoExoPlayerView.setPlayer(exoPlayer);
        videoExoPlayerView.setUseArtwork(true);
        videoExoPlayerView.setUseController(false);
        videoExoPlayerView.requestFocus();

        int currentProgress = mPresenter.setCurrentProgressForVolumeSeekBar();
        volumeSeekBar.setProgressAndThumb(currentProgress);

        mPresenter.playTheSongThatWasPlayedBeforeActivityCreated();

        if (castPlayer != null && exoPlayer != null) {
            mPresenter.setCurrentPlayer(castPlayer.isCastSessionAvailable() ? castPlayer : exoPlayer);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy() is called.");
        mPresenter.releaseMediaSessionCompat();
        mPresenter.releaseExoPlayerAndCastPlayer();
        videoExoPlayerView.setPlayer(null);
    }

    @Override
    public void setCurrentPlayerToPlayerView() {
        Player currentPlayer = mPresenter.getCurrentPlayer();
        if (currentPlayer == null) {
            return;
        }

        if (currentPlayer == exoPlayer) {
            // videoExoPlayerView.setVisibility(View.VISIBLE);
            // videoExoPlayerView.setPlayer(exoPlayer);
            // castControlView.hide();
            Log.d(TAG, "Current player is exoPlayer." );
        } else /* currentPlayer == castPlayer */ {
            // videoExoPlayerView.setVisibility(View.INVISIBLE);
            // castControlView.show();
            Log.d(TAG, "Current player is castPlayer." );
        }
    }
}
