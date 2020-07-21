package com.smile.karaokeplayer.Listeners;

import android.content.Context;
import android.net.Uri;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.Presenters.ExoPlayerPresenter;
import com.smile.karaokeplayer.R;
import com.smile.smilelibraries.utilities.ScreenUtil;

public class ExoPlayerEventListener implements Player.EventListener {

    private static final String TAG = "ExoPlayerEventListener";
    private final Context callingContext;
    private final ExoPlayerPresenter mPresenter;
    private final float toastTextSize;

    public ExoPlayerEventListener(Context context, ExoPlayerPresenter presenter) {
        callingContext = context;
        mPresenter = presenter;
        toastTextSize = mPresenter.getToastTextSize();
    }

    @Override
    public synchronized void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        Log.d(TAG, "onPlayerStateChanged() is called.");
        Log.d(TAG, "Playback state = " + playbackState);

        PlayingParameters playingParam = mPresenter.getPlayingParam();
        Uri mediaUri = mPresenter.getMediaUri();

        switch (playbackState) {
            case Player.STATE_BUFFERING:
                if (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) {
                    mPresenter.getPresentView().hideNativeAd();
                }
                mPresenter.getPresentView().showBufferingMessage();
                return;
            case Player.STATE_READY:
                if (!playingParam.isMediaSourcePrepared()) {
                    // the first time of Player.STATE_READY means prepared
                    mPresenter.getPlayingMediaInfoAndSetAudioActionSubMenu();
                    playingParam = mPresenter.getPlayingParam();
                }
                playingParam.setMediaSourcePrepared(true);

                int numberOfVideoTracks = mPresenter.getNumberOfVideoTracks();
                if (numberOfVideoTracks == 0) {
                    // no video is being played, show native ads
                    mPresenter.getPresentView().showNativeAd();
                } else {
                    // video is being played, hide native ads
                    if (playWhenReady) {
                        // playing
                        mPresenter.getPresentView().hideNativeAd();
                    }
                }
                break;
            case Player.STATE_ENDED:
                // playing is finished
                if (playingParam.isAutoPlay()) {
                    // start playing next video from list
                    mPresenter.startAutoPlay();
                } else {
                    // end of playing
                    if (playingParam.getRepeatStatus() != PlayerConstants.NoRepeatPlaying) {
                        mPresenter.replayMedia();
                    } else {
                        mPresenter.getPresentView().showNativeAd();
                    }
                }
                Log.d(TAG, "Playback state = Player.STATE_ENDED after startAutoPlay()");
                break;
            case Player.STATE_IDLE:
                // There is bug here
                // The listener will get twice of (Player.STATE_IDLE)
                // when user stop playing using ExoPlayer.stop()
                // so do not put startAutoPlay() inside this event
                if (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) {
                    playingParam.setMediaSourcePrepared(false);
                    Log.d(TAG, "Song was stopped by user (by stopPlay()).");
                }
                if (!playingParam.isAutoPlay()) {
                    // not auto play
                    mPresenter.getPresentView().showNativeAd();
                }
                break;
        }

        mPresenter.getPresentView().dismissBufferingMessage();
    }

    @Override
    public synchronized void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        Log.d(TAG,"Player.EventListener.onTracksChanged() is called.");
    }
    @Override
    public synchronized void onIsPlayingChanged(boolean isPlaying) {
        Log.d(TAG,"Player.EventListener.onIsPlayingChanged() is called.");
    }

    @Override
    public synchronized void onPlayerError(ExoPlaybackException error) {
        Log.d(TAG,"Player.EventListener.onPlayerError() is called.");
        switch (error.type) {
            case ExoPlaybackException.TYPE_SOURCE:
                Log.d(TAG, "TYPE_SOURCE: " + error.getSourceException().getMessage());
                break;

            case ExoPlaybackException.TYPE_RENDERER:
                Log.d(TAG, "TYPE_RENDERER: " + error.getRendererException().getMessage());
                break;

            case ExoPlaybackException.TYPE_UNEXPECTED:
                Log.d(TAG, "TYPE_UNEXPECTED: " + error.getUnexpectedException().getMessage());
                break;
        }

        PlayingParameters playingParam = mPresenter.getPlayingParam();

        String formatNotSupportedString = callingContext.getString(R.string.formatNotSupportedString);
        if (playingParam.isAutoPlay()) {
            // go to next one in the list
            if (mPresenter.isCanShowNotSupportedFormat()) {
                // only show once
                mPresenter.setCanShowNotSupportedFormat(false);
                ScreenUtil.showToast(callingContext, formatNotSupportedString, toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
            }
            mPresenter.startAutoPlay();
        } else {
            ScreenUtil.showToast(callingContext, formatNotSupportedString, toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
        }

        mPresenter.setMediaUri(null);
    }

    @Override
    public synchronized void onPositionDiscontinuity(int reason) {
        Log.d(TAG,"Player.EventListener.onPositionDiscontinuity() is called.");
    }

    @Override
    public synchronized void onSeekProcessed() {
        Log.d(TAG,"Player.EventListener.onSeekProcessed() is called.");
    }
}
