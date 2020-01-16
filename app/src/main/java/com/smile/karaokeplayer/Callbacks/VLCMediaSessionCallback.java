package com.smile.karaokeplayer.Callbacks;

import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.Presenters.VLCPlayerPresenter;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

public class VLCMediaSessionCallback extends MediaSessionCompat.Callback {

    private static final String TAG = new String("VLCMediaSessionCallback");
    private final VLCPlayerPresenter mPresenter;
    private final LibVLC mLibVLC;
    private final MediaPlayer vlcPlayer;

    public VLCMediaSessionCallback(VLCPlayerPresenter presenter, LibVLC libVLC, MediaPlayer mediaPlayer) {
        mPresenter = presenter;
        mLibVLC = libVLC;
        vlcPlayer = mediaPlayer;
    }

    @Override
    public void onCommand(String command, Bundle extras, ResultReceiver cb) {
        super.onCommand(command, extras, cb);
    }

    @Override
    public void onPrepare() {
        super.onPrepare();
        Log.d(TAG, "onPrepare() is called.");
    }

    @Override
    public void onPrepareFromMediaId(String mediaId, Bundle extras) {
        super.onPrepareFromMediaId(mediaId, extras);
        Log.d(TAG, "onPrepareFromMediaId() is called.");
    }

    @Override
    public void onPrepareFromUri(Uri uri, Bundle extras) {
        Log.d(TAG, "onPrepareFromUri() is called.");
        super.onPrepareFromUri(uri, extras);

        PlayingParameters playingParam = mPresenter.getPlayingParam();
        playingParam.setMediaSourcePrepared(false);

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
        mPresenter.setAudioVolume(currentVolume);
        vlcPlayer.setTime(currentAudioPosition); // use time to set position
        try {
            switch (playbackState) {
                case PlaybackStateCompat.STATE_PAUSED:
                    vlcPlayer.pause();
                    Log.d(TAG, "onPrepareFromUri() --> PlaybackStateCompat.STATE_PAUSED");
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    vlcPlayer.stop();
                    Log.d(TAG, "onPrepareFromUri() --> PlaybackStateCompat.STATE_STOPPED");
                    break;
                case PlaybackStateCompat.STATE_PLAYING:
                case PlaybackStateCompat.STATE_NONE:
                    // start playing when ready or just start new playing
                    final Media mediaSource = new Media(mLibVLC, uri);
                    vlcPlayer.setMedia(mediaSource);
                    vlcPlayer.play();
                    mediaSource.release();
                    Log.d(TAG, "onPrepareFromUri() --> PlaybackStateCompat.STATE_PLAYING");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid mediaId");
        }
    }

    @Override
    public void onPlay() {
        super.onPlay();
        Log.d(TAG, "onPlay() is called.");
        MediaControllerCompat controller = mPresenter.getMediaSessionCompat().getController();
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
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        super.onPlayFromMediaId(mediaId, extras);
        Log.d(TAG, "onPlayFromMediaId() is called.");
    }

    @Override
    public void onPlayFromUri(Uri uri, Bundle extras) {
        super.onPlayFromUri(uri, extras);
        Log.d(TAG, "onPlayFromUri() is called.");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() is called.");
        MediaControllerCompat controller = mPresenter.getMediaSessionCompat().getController();
        PlaybackStateCompat stateCompat = controller.getPlaybackState();
        int state = stateCompat.getState();
        if (state != PlaybackStateCompat.STATE_PAUSED) {
            vlcPlayer.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() is called.");
        MediaControllerCompat controller = mPresenter.getMediaSessionCompat().getController();
        PlaybackStateCompat stateCompat = controller.getPlaybackState();
        int state = stateCompat.getState();
        if (state != PlaybackStateCompat.STATE_STOPPED) {
            vlcPlayer.stop();
        }
    }

    @Override
    public void onFastForward() {
        super.onFastForward();
        Log.d(TAG, "onFastForward() is called.");
        mPresenter.setMediaPlaybackState(PlaybackStateCompat.STATE_FAST_FORWARDING);
    }

    @Override
    public void onRewind() {
        super.onRewind();
        Log.d(TAG, "onRewind() is called.");
        mPresenter.setMediaPlaybackState(PlaybackStateCompat.STATE_REWINDING);
    }
}
