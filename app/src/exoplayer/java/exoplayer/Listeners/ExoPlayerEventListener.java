package exoplayer.Listeners;

import android.app.Activity;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.smile.karaokeplayer.Models.PlayingParameters;
import exoplayer.Presenters.ExoPlayerPresenter;
import exoplayer.Presenters.ExoPlayerPresenter.ExoPlayerPresentView;
import com.smile.karaokeplayer.R;
import com.smile.smilelibraries.utilities.ScreenUtil;

public class ExoPlayerEventListener implements Player.EventListener {

    private static final String TAG = "ExoPlayerEventListener";
    private final Activity activity;
    private final ExoPlayerPresenter presenter;
    private final SimpleExoPlayer exoPlayer;
    private final ExoPlayerPresentView presentView;
    private final float toastTextSize;

    public ExoPlayerEventListener(Activity activity, ExoPlayerPresenter presenter) {
        this.activity = activity;
        this.presenter = presenter;
        exoPlayer = this.presenter.getExoPlayer();
        presentView = this.presenter.getPresentView();
        toastTextSize = this.presenter.getToastTextSize();
        Log.d(TAG, "ExoPlayerEventListener is created.");
    }

    @Override
    public synchronized void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
        Log.d(TAG, "onPlayWhenReadyChanged() --> playWhenReady = " + playWhenReady
                        + ", reason = " + reason);
    }

    @Override
    public synchronized void onPlaybackStateChanged(int state) {
        PlayingParameters playingParam = presenter.getPlayingParam();
        switch (state) {
            case Player.STATE_BUFFERING:
                Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_BUFFERING--> playWhenReady = " + exoPlayer.getPlayWhenReady() );
                if (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) {
                    Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_BUFFERING --> hideNativeAndBannerAd()");
                    presentView.hideNativeAndBannerAd();
                }
                presentView.showBufferingMessage();
                return;
            case Player.STATE_READY:
                Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_READY--> playWhenReady = " + exoPlayer.getPlayWhenReady() );
                if (!playingParam.isMediaSourcePrepared()) {
                    // the first time of Player.STATE_READY means prepared
                    presenter.getPlayingMediaInfoAndSetAudioActionSubMenu();
                    playingParam = presenter.getPlayingParam();
                }
                playingParam.setMediaSourcePrepared(true);
                break;
            case Player.STATE_ENDED:
                // playing is finished
                Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_ENDED--> playWhenReady = " + exoPlayer.getPlayWhenReady() );
                break;
            case Player.STATE_IDLE:
                Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_IDLE--> playWhenReady = " + exoPlayer.getPlayWhenReady() );
                break;
            default:
                Log.d(TAG, "onPlaybackStateChanged() --> Playback state (Default) = " + state);
                break;
        }

        presentView.dismissBufferingMessage();
    }

    @Override
    public synchronized void onIsPlayingChanged(boolean isPlaying) {
        Log.d(TAG,"Player.EventListener.onIsPlayingChanged() --> isPlaying = " + isPlaying);
    }

    @Override
    public synchronized void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        Log.d(TAG,"Player.EventListener.onTracksChanged() is called.");
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

        PlayingParameters playingParam = presenter.getPlayingParam();

        String formatNotSupportedString = activity.getString(R.string.formatNotSupportedString);
        if (presenter.isCanShowNotSupportedFormat()) {
            // only show once
            presenter.setCanShowNotSupportedFormat(false);
            ScreenUtil.showToast(activity, formatNotSupportedString, toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
        }
        presenter.startAutoPlay();
        presenter.setMediaUri(null);
    }

    @Override
    public synchronized void onPositionDiscontinuity(int reason) {
        Log.d(TAG,"Player.EventListener.onPositionDiscontinuity() is called.");
    }
}
