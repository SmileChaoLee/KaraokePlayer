package videoplayer.Listeners;

import android.app.Activity;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import org.videolan.libvlc.MediaPlayer;
import videoplayer.Presenters.VLCPlayerPresenter;

public class VLCPlayerEventListener implements MediaPlayer.EventListener {
    private static final String TAG = "VLCPlayerEventListener";
    private final VLCPlayerPresenter presenter;
    private final MediaPlayer vlcPlayer;

    public VLCPlayerEventListener(Activity activity, VLCPlayerPresenter presenter) {
        this.presenter = presenter;
        vlcPlayer = this.presenter.getVlcPlayer();
    }

    @Override
    public synchronized void onEvent(MediaPlayer.Event event) {
        // Log.d(TAG, "onEvent() is called and MediaPlayer.Event event = " + event);
        // PlayingParameters playingParam = presenter.getPlayingParam();
        switch(event.type) {
            case MediaPlayer.Event.Buffering:
                Log.d(TAG, "onEvent()-->Buffering.");
                if (!vlcPlayer.isPlaying()) {
                    Log.d(TAG, "onEvent()-->Buffering()-->setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING)");
                    presenter.setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
                }
                break;
            case MediaPlayer.Event.Playing:
                Log.d(TAG, "onEvent()-->Playing.");
                presenter.setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                break;
            case MediaPlayer.Event.Paused:
                Log.d(TAG, "onEvent()-->Paused.");
                presenter.setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                break;
            case MediaPlayer.Event.Stopped:
                // Event.Stopped is for user 1. stop the playing by user
                // 2. after end of the playing (Event.EndReached)
                Log.d(TAG, "onEvent()-->Stopped-->getLength() = " + vlcPlayer.getLength());
                if (vlcPlayer.getLength() == 0) {
                    // no legal media format
                    presenter.setMediaPlaybackState(PlaybackStateCompat.STATE_ERROR);
                } else {
                    presenter.setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                }
                break;
            case MediaPlayer.Event.EndReached:
                // after this event, vlcPlayer will send out Event.Stopped to EventListener
                Log.d(TAG, "onEvent()-->EndReached");
                presenter.setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);
                break;
            case MediaPlayer.Event.Opening:
                Log.d(TAG, "onEvent()-->Opening.");
                break;
            case MediaPlayer.Event.PositionChanged:
                // Log.d(TAG, "onEvent()-->PositionChanged.");
                break;
            case MediaPlayer.Event.TimeChanged:
                // Log.d(TAG, "onEvent()-->TimeChanged.");
                presenter.getPresentView().update_Player_duration_seekbar_progress((int)vlcPlayer.getTime());
                break;
            case MediaPlayer.Event.EncounteredError:
                Log.d(TAG, "onEvent()-->EncounteredError.");
                presenter.setMediaPlaybackState(PlaybackStateCompat.STATE_ERROR);
                break;
            default:
                Log.d(TAG, "onEvent()-->default-->event.type = " + event.type);
                break;
        }
    }
}
