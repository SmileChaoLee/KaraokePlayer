package exoplayer.Callbacks;

import android.annotation.SuppressLint;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import exoplayer.Presenters.ExoPlayerPresenter;

public class ExoMediaControllerCallback extends MediaControllerCompat.Callback {

    private static final String TAG = "ExoMediaControllerCallback";
    private final ExoPlayerPresenter mPresenter;

    public ExoMediaControllerCallback(ExoPlayerPresenter presenter) {
        mPresenter = presenter;
    }

    @SuppressLint("LongLogTag")
    @Override
    public synchronized void onPlaybackStateChanged(PlaybackStateCompat state) {
        Log.d(TAG, "onPlaybackStateChanged() --> state = " + state);
        super.onPlaybackStateChanged(state);
        if( state == null ) {
            return;
        }
        mPresenter.updateStatusAndUi(state);
    }
}
