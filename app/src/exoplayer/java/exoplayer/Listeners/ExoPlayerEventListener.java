package exoplayer.Listeners;

import android.app.Activity;
import android.net.Uri;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.smile.karaokeplayer.Models.PlayingParameters;
import exoplayer.Presenters.ExoPlayerPresenter;
import com.smile.karaokeplayer.R;
import com.smile.smilelibraries.utilities.ScreenUtil;

public class ExoPlayerEventListener implements Player.EventListener {

    private static final String TAG = "ExoPlayerEventListener";
    private final Activity mActivity;
    private final ExoPlayerPresenter mPresenter;
    private final float toastTextSize;

    public ExoPlayerEventListener(Activity activity, ExoPlayerPresenter presenter) {
        mActivity = activity;
        mPresenter = presenter;
        toastTextSize = mPresenter.getToastTextSize();
    }

    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
        Log.d(TAG, "onPlayWhenReadyChanged() --> playWhenReady = " + playWhenReady
                        + ", reason = " + reason);
        if (playWhenReady) {
            // playing
            Log.d(TAG, "onPlayWhenReadyChanged()-->numberOfVideoTracks != 0-->hideNativeAndBannerAd()");
            mPresenter.getPresentView().hideNativeAndBannerAd();
        } else {
            Log.d(TAG, "onPlayWhenReadyChanged() --> playWhenReady=false -->showNativeAndBannerAd()");
            mPresenter.getPresentView().showNativeAndBannerAd();
        }
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        Log.d(TAG, "onPlaybackStateChanged()() --> Playback state = " + state);
        PlayingParameters playingParam = mPresenter.getPlayingParam();
        Uri mediaUri = mPresenter.getMediaUri();
        switch (state) {
            case Player.STATE_BUFFERING:
                if (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) {
                    Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_BUFFERING --> hideNativeAndBannerAd()");
                    mPresenter.getPresentView().hideNativeAndBannerAd();
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
                    Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_READY --> numberOfVideoTracks == 0 --> showNativeAndBannerAd()");
                    mPresenter.getPresentView().showNativeAndBannerAd();
                } else {
                    // video is being played, hide native ads
                    boolean playWhenReady = mPresenter.getExoPlayer().getPlayWhenReady();
                    if (playWhenReady) {
                        // playing
                        Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_READY --> numberOfVideoTracks != 0 --> hideNativeAndBannerAd()");
                        mPresenter.getPresentView().hideNativeAndBannerAd();
                    }
                }
                break;
            case Player.STATE_ENDED:
                // playing is finished
                Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_ENDED --> showNativeAndBannerAd()");
                mPresenter.getPresentView().showNativeAndBannerAd(); // removed for testing
                mPresenter.startAutoPlay(); // added on 2020-08-16
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
                // if (!playingParam.isAutoPlay()) {
                // not auto play
                Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_IDLE --> showNativeAndBannerAd()");
                mPresenter.getPresentView().showNativeAndBannerAd(); // removed for testing
                // }
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

        String formatNotSupportedString = mActivity.getString(R.string.formatNotSupportedString);
        if (mPresenter.isCanShowNotSupportedFormat()) {
            // only show once
            mPresenter.setCanShowNotSupportedFormat(false);
            ScreenUtil.showToast(mActivity, formatNotSupportedString, toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
        }
        mPresenter.startAutoPlay();
        mPresenter.setMediaUri(null);
    }

    @Override
    public synchronized void onPositionDiscontinuity(int reason) {
        Log.d(TAG,"Player.EventListener.onPositionDiscontinuity() is called.");
    }
}
