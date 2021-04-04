package exoplayer.Listeners;

import android.app.Activity;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.smile.karaokeplayer.Models.PlayingParameters;
import exoplayer.Presenters.ExoPlayerPresenter;
import exoplayer.Presenters.ExoPlayerPresenter.ExoPlayerPresentView;

public class ExoPlayerEventListener implements Player.EventListener {

    private static final String TAG = "ExoPlayerEventListener";
    private final ExoPlayerPresenter presenter;
    private final SimpleExoPlayer exoPlayer;

    public ExoPlayerEventListener(Activity activity, ExoPlayerPresenter presenter) {
        this.presenter = presenter;
        exoPlayer = this.presenter.getExoPlayer();
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
                return;
            case Player.STATE_READY:
                Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_READY--> playWhenReady = " + exoPlayer.getPlayWhenReady() );
                break;
            case Player.STATE_ENDED:
                // playing is finished and send PlaybackStateCompat.STATE_STOPPED
                // to MediaControllerCallback
                Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_ENDED--> playWhenReady = " + exoPlayer.getPlayWhenReady() );
                break;
            case Player.STATE_IDLE:
                // user stops the playing and send PlaybackStateCompat.STATE_NONE
                // to MediaControllerCallback
                Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_IDLE--> playWhenReady = " + exoPlayer.getPlayWhenReady() );
                break;
            default:
                Log.d(TAG, "onPlaybackStateChanged() --> Playback state (Default) = " + state);
                break;
        }
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
    }

    @Override
    public synchronized void onPositionDiscontinuity(int reason) {
        Log.d(TAG,"Player.EventListener.onPositionDiscontinuity() is called.");
    }
}
