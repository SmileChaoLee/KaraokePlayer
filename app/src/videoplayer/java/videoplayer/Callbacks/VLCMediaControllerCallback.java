package videoplayer.Callbacks;

import android.annotation.SuppressLint;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import videoplayer.Presenters.VLCPlayerPresenter;

public class VLCMediaControllerCallback extends MediaControllerCompat.Callback {

    private static final String TAG = "VLCMediaControllerCallback";
    private final VLCPlayerPresenter presenter;

    public VLCMediaControllerCallback(VLCPlayerPresenter presenter) {
        this.presenter = presenter;
    }

    @SuppressLint("LongLogTag")
    @Override
    public synchronized void onPlaybackStateChanged(PlaybackStateCompat state) {
        Log.d(TAG, "onPlaybackStateChanged() --> state = " + state);
        super.onPlaybackStateChanged(state);
        if( state == null ) {
            return;
        }
        presenter.updateStatusAndUi(state);
    }
}
