package videoplayer.Callbacks;

import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Models.PlayingParameters;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;

import videoplayer.Presenters.VLCPlayerPresenter;

public class VLCMediaSessionCallback extends MediaSessionCompat.Callback {

    private static final String TAG = "VLCMediaSessionCallback";
    private final VLCPlayerPresenter presenter;
    private final LibVLC libVLC;
    private final MediaPlayer vlcPlayer;

    public VLCMediaSessionCallback(VLCPlayerPresenter presenter, LibVLC libVLC, MediaPlayer mediaPlayer) {
        this.presenter = presenter;
        this.libVLC = libVLC;
        vlcPlayer = mediaPlayer;
    }

    @Override
    public synchronized void onCommand(String command, Bundle extras, ResultReceiver cb) {
        super.onCommand(command, extras, cb);
    }

    @Override
    public synchronized void onPrepare() {
        super.onPrepare();
        Log.d(TAG, "onPrepare() is called.");
    }

    @Override
    public synchronized void onPrepareFromMediaId(String mediaId, Bundle extras) {
        super.onPrepareFromMediaId(mediaId, extras);
        Log.d(TAG, "onPrepareFromMediaId() is called.");
    }

    @Override
    public synchronized void onPrepareFromUri(Uri uri, Bundle extras) {
        Log.d(TAG, "onPrepareFromUri() --> uri = " + uri);
        super.onPrepareFromUri(uri, extras);

        PlayingParameters playingParam = presenter.getPlayingParam();
        playingParam.setMediaPrepared(false);

        long currentAudioPosition = playingParam.getCurrentAudioPosition();
        float currentVolume = playingParam.getCurrentVolume();
        int playbackState = playbackState = playingParam.getCurrentPlaybackState();
        if (extras != null) {
            Log.d(TAG, "extras is not null.");
            PlayingParameters playingParamOrigin = extras.getParcelable(PlayerConstants.PlayingParamOrigin);
            if (playingParamOrigin != null) {
                Log.d(TAG, "playingParamOrigin is not null.");
                playbackState = playingParamOrigin.getCurrentPlaybackState();
                currentAudioPosition = playingParamOrigin.getCurrentAudioPosition();
                currentVolume = playingParamOrigin.getCurrentVolume();
            }
        }
        presenter.setAudioVolume(currentVolume);
        vlcPlayer.setTime(currentAudioPosition); // use time to set position
        try {
            switch (playbackState) {
                case PlaybackStateCompat.STATE_PAUSED:
                    Log.d(TAG, "onPrepareFromUri() --> PlaybackStateCompat.STATE_PAUSED");
                    vlcPlayer.pause();
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    Log.d(TAG, "onPrepareFromUri() --> PlaybackStateCompat.STATE_STOPPED");
                    vlcPlayer.stop();
                    break;
                case PlaybackStateCompat.STATE_PLAYING:
                    Log.d(TAG, "onPrepareFromUri() --> PlaybackStateCompat.STATE_PLAYING");
                case PlaybackStateCompat.STATE_NONE:
                    Log.d(TAG, "onPrepareFromUri() --> PlaybackStateCompat.STATE_NONE");
                    // start playing when ready or just start new playing
                    final IMedia media = new Media(libVLC, uri);
                    vlcPlayer.setMedia(media);
                    vlcPlayer.play();
                    media.release();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid mediaId");
        }
    }

    @Override
    public synchronized void onPlay() {
        super.onPlay();
        Log.d(TAG, "onPlay() is called.");
        MediaControllerCompat controller = presenter.getMediaControllerCompat();
        PlaybackStateCompat stateCompat = controller.getPlaybackState();
        int state = stateCompat.getState();
        if (state != PlaybackStateCompat.STATE_PLAYING) {
            int playerState = vlcPlayer.getPlayerState();
            if (!vlcPlayer.isPlaying()) {
                vlcPlayer.play();
            }
        }
    }

    @Override
    public synchronized void onPlayFromMediaId(String mediaId, Bundle extras) {
        super.onPlayFromMediaId(mediaId, extras);
        Log.d(TAG, "onPlayFromMediaId() is called.");
    }

    @Override
    public synchronized void onPlayFromUri(Uri uri, Bundle extras) {
        super.onPlayFromUri(uri, extras);
        Log.d(TAG, "onPlayFromUri() is called.");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() is called.");
        MediaControllerCompat controller = presenter.getMediaControllerCompat();
        PlaybackStateCompat stateCompat = controller.getPlaybackState();
        int state = stateCompat.getState();
        if (state != PlaybackStateCompat.STATE_PAUSED) {
            vlcPlayer.pause();
        }
    }

    @Override
    public synchronized void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() is called.");
        MediaControllerCompat controller = presenter.getMediaControllerCompat();
        PlaybackStateCompat stateCompat = controller.getPlaybackState();
        int state = stateCompat.getState();
        if (state != PlaybackStateCompat.STATE_STOPPED) {
            vlcPlayer.stop();
        }
    }

    @Override
    public synchronized void onFastForward() {
        super.onFastForward();
        Log.d(TAG, "onFastForward() is called.");
        presenter.setMediaPlaybackState(PlaybackStateCompat.STATE_FAST_FORWARDING);
    }

    @Override
    public synchronized void onRewind() {
        super.onRewind();
        Log.d(TAG, "onRewind() is called.");
        presenter.setMediaPlaybackState(PlaybackStateCompat.STATE_REWINDING);
    }
}
