package com.smile.karaokeplayer.Callbacks;

import android.net.Uri;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.Presenters.ExoPlayerPresenter;

public class ExoMediaControllerCallback extends MediaControllerCompat.Callback {

    private static final String TAG = new String("ExoMediaControllerCallback");
    private final ExoPlayerPresenter mPresenter;

    public ExoMediaControllerCallback(ExoPlayerPresenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public synchronized void onPlaybackStateChanged(PlaybackStateCompat state) {
        super.onPlaybackStateChanged(state);
        if( state == null ) {
            return;
        }

        PlayingParameters playingParam = mPresenter.getPlayingParam();
        Uri mediaUri = mPresenter.getMediaUri();

        int currentState = state.getState();
        switch (currentState) {
            case PlaybackStateCompat.STATE_NONE:
                // initial state and when playing is stopped by user
                Log.d(TAG, "PlaybackStateCompat.STATE_NONE");
                if (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) {
                    Log.d(TAG, "MediaControllerCallback--> Song was stopped by user.");
                }
                mPresenter.getPresentView().playButtonOnPauseButtonOff();
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                // when playing
                Log.d(TAG, "PlaybackStateCompat.STATE_PLAYING");
                mPresenter.startDurationSeekBarHandler();   // start updating duration seekbar
                mPresenter.getPresentView().playButtonOffPauseButtonOn();
                // set up a timer for supportToolbar's visibility
                mPresenter.getPresentView().setTimerToHideSupportAndAudioController();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                Log.d(TAG, "PlaybackStateCompat.STATE_PAUSED");
                mPresenter.getPresentView().playButtonOnPauseButtonOff();
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                // when finished playing
                Log.d(TAG, "PlaybackStateCompat.STATE_STOPPED");
                mPresenter.getPresentView().update_Player_duration_seekbar_progress((int)mPresenter.getExoPlayer().getDuration());
                mPresenter.getPresentView().playButtonOnPauseButtonOff();
                break;
        }

        playingParam.setCurrentPlaybackState(currentState);

        Log.d(TAG, "onPlaybackStateChanged() is called. " + currentState);
    }
}
