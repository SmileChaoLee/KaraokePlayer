package exoplayer.Listeners;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;

import exoplayer.Presenters.ExoPlayerPresenter;

public class ExoPlayerListener implements Player.Listener {

    private static final String TAG = "ExoPlayerListener";
    private final ExoPlayerPresenter presenter;
    private final ExoPlayer exoPlayer;

    public ExoPlayerListener(Activity activity, ExoPlayerPresenter presenter) {
        this.presenter = presenter;
        exoPlayer = this.presenter.getExoPlayer();
        Log.d(TAG, "ExoPlayerListener is created.");
    }

    @Override
    public synchronized void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
        Log.d(TAG, "onPlayWhenReadyChanged() --> playWhenReady = " + playWhenReady
                        + ", reason = " + reason);
    }

    @Override
    public synchronized void onPlaybackStateChanged(int state) {
        switch (state) {
            case Player.STATE_BUFFERING:
                Log.d(TAG, "onPlaybackStateChanged()--> Player.STATE_BUFFERING--> playWhenReady = " + exoPlayer.getPlayWhenReady() );
                break;
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
        Log.d(TAG,"onIsPlayingChanged() --> isPlaying = " + isPlaying);
    }

    @Override
    public void onPlayerErrorChanged(@Nullable PlaybackException error) {
        Log.d(TAG,"onPlayerErrorChanged() --> error = " + error);
    }

    @Override
    public synchronized void onPlayerError(PlaybackException error) {
        Log.d(TAG,"onPlayerError() --> error = " + error);
    }
}
