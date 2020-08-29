package vlcplayer.Callbacks;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import com.smile.karaokeplayer.Models.PlayingParameters;

import vlcplayer.Presenters.VLCPlayerPresenter;

public class VLCMediaControllerCallback extends MediaControllerCompat.Callback {

    private static final String TAG = "VLCMediaControllerCallback";
    private final VLCPlayerPresenter mPresenter;

    public VLCMediaControllerCallback(VLCPlayerPresenter presenter) {
        mPresenter = presenter;
    }

    @SuppressLint("LongLogTag")
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
                // initial state or playing is finished
                Log.d(TAG, "PlaybackStateCompat.STATE_NONE");
                if (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) {
                    Log.d(TAG, "MediaControllerCallback--> Song was finished.");
                    mPresenter.getPresentView().showNativeAndBannerAd();
                    mPresenter.startAutoPlay(); // added on 2020-08-16
                }
                mPresenter.getPresentView().playButtonOnPauseButtonOff();
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                // when playing
                Log.d(TAG, "PlaybackStateCompat.STATE_PLAYING");
                if (!playingParam.isMediaSourcePrepared()) {
                    // first playing
                    playingParam.setMediaSourcePrepared(true);  // has been prepared
                    final Handler handler = new Handler(Looper.getMainLooper());
                    final Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            handler.removeCallbacksAndMessages(null);
                            mPresenter.getPlayingMediaInfoAndSetAudioActionSubMenu();

                            int numberOfVideoTracks = mPresenter.getNumberOfVideoTracks();
                            if (numberOfVideoTracks == 0) {
                                // no video is being played, show native ads
                                mPresenter.getPresentView().showNativeAndBannerAd();
                            } else {
                                // video is being played, hide native ads
                                mPresenter.getPresentView().hideNativeAndBannerAd();
                            }
                        }
                    };
                    handler.postDelayed(runnable, 1000); // delay 1 seconds
                }
                mPresenter.getPresentView().playButtonOffPauseButtonOn();
                // set up a timer for supportToolbar's visibility
                mPresenter.getPresentView().setTimerToHideSupportAndAudioController();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                Log.d(TAG, "PlaybackStateCompat.STATE_PAUSED");
                mPresenter.getPresentView().playButtonOnPauseButtonOff();
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                Log.d(TAG, "PlaybackStateCompat.STATE_STOPPED");
                if (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) {
                    Log.d(TAG, "MediaControllerCallback--> Song was stopped by user.");
                }
                mPresenter.getPresentView().playButtonOnPauseButtonOff();
                break;
        }

        playingParam.setCurrentPlaybackState(currentState);

        Log.d(TAG, "MediaControllerCallback.onPlaybackStateChanged() is called. " + currentState);
    }
}
