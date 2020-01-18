package com.smile.karaokeplayer.Presenters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.smile.karaokeplayer.Callbacks.VLCMediaControllerCallback;
import com.smile.karaokeplayer.Callbacks.VLCMediaSessionCallback;
import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Listeners.VLCPlayerEventListener;
import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.R;
import com.smile.karaokeplayer.Utilities.ExternalStorageUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.DisplayManager;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.File;
import java.util.ArrayList;

public class VLCPlayerPresenter extends PlayerBasePresenter {

    private static final String TAG = new String("VLCPlayerPresenter");

    private final PresentView presentView;
    private final Context callingContext;
    private final Activity mActivity;

    private MediaControllerCompat mediaControllerCompat;
    private VLCMediaControllerCallback mediaControllerCallback;

    private LibVLC mLibVLC;
    private MediaPlayer vlcPlayer;

    // instances of the following members have to be saved when configuration changed
    private ArrayList<Integer> videoTrackIndicesList;
    private ArrayList<Integer> audioTrackIndicesList;

    public interface PresentView extends PlayerBasePresenter.PresentView {
    }

    public VLCPlayerPresenter(Context context, PresentView presentView) {
        super(context, presentView);
        this.callingContext = context;
        this.presentView = presentView;
        this.mActivity = (Activity)(this.presentView);
    }

    public ArrayList<Integer> getAudioTrackIndicesList() {
        return audioTrackIndicesList;
    }
    public void setAudioTrackIndicesList(ArrayList<Integer> audioTrackIndicesList) {
        this.audioTrackIndicesList = audioTrackIndicesList;
    }

    public ArrayList<Integer> getVideoTrackIndicesList() {
        return videoTrackIndicesList;
    }
    public void setVideoTrackIndicesList(ArrayList<Integer> videoTrackIndicesList) {
        this.videoTrackIndicesList = videoTrackIndicesList;
    }

    public PresentView getPresentView() {
        return presentView;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initializeVariables(Bundle savedInstanceState, Intent callingIntent) {
        super.initializeVariables(savedInstanceState, callingIntent);
        if (savedInstanceState == null) {
            videoTrackIndicesList = new ArrayList<>();
            audioTrackIndicesList = new ArrayList<>();
        } else {
            videoTrackIndicesList = (ArrayList<Integer>)savedInstanceState.getSerializable(PlayerConstants.VideoTrackIndicesListState);
            audioTrackIndicesList = (ArrayList<Integer>)savedInstanceState.getSerializable(PlayerConstants.AudioTrackIndicesListState);
        }
    }

    public void attachPlayerViews(VLCVideoLayout videoVLCPlayerView, @Nullable DisplayManager dm, boolean enableSUBTITLES, boolean use_TEXTURE_VIEW) {
        vlcPlayer.attachViews(videoVLCPlayerView, dm, enableSUBTITLES, use_TEXTURE_VIEW);
    }

    public void detachPlayerViews() {
        vlcPlayer.detachViews();
    }

    public void initVLCPlayer() {
        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        mLibVLC = new LibVLC(callingContext, args);
        mLibVLC = new LibVLC(callingContext);
        vlcPlayer = new MediaPlayer(mLibVLC);
        vlcPlayer.setEventListener(new VLCPlayerEventListener(callingContext, this));
    }

    public void releaseVLCPlayer() {
        if (vlcPlayer != null) {
            vlcPlayer.stop();
            vlcPlayer.detachViews();
            vlcPlayer.release();
            vlcPlayer = null;
        }
        if (mLibVLC != null) {
            mLibVLC.release();
            mLibVLC = null;
        }
    }

    public MediaPlayer getVlcPlayer() {
        return vlcPlayer;
    }

    public void setMediaPlaybackState(int state) {
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        } else {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
        }
        playbackStateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        // playbackStateBuilder.setState(state, vlcPlayer.getTime(), 1f);
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
    }

    public MediaControllerCompat getMediaControllerCompat() {
        return mediaControllerCompat;
    }

    public void getPlayingMediaInfoAndSetAudioActionSubMenu() {
        MediaPlayer.TrackDescription videoDis[] = vlcPlayer.getVideoTracks();
        int videoTrackId;
        String videoTrackName;
        videoTrackIndicesList.clear();
        if (videoDis != null) {
            // because it is null sometimes
            for (int i = 0; i < videoDis.length; i++) {
                videoTrackId = videoDis[i].id;
                videoTrackName = videoDis[i].name;
                Log.d(TAG, "videoDis[i].id = " + videoTrackId);
                Log.d(TAG, "videoDis[i].name = " + videoTrackName);
                // exclude disabled
                if (videoTrackId >=0 ) {
                    // enabled audio track
                    videoTrackIndicesList.add(videoTrackId);
                }
            }
        }
        numberOfVideoTracks = videoTrackIndicesList.size();
        Log.d(TAG, "numberOfVideoTracks = " + numberOfVideoTracks);
        if (numberOfVideoTracks == 0) {
            playingParam.setCurrentVideoTrackIndexPlayed(PlayerConstants.NoVideoTrack);
        } else {
            // set which video track to be played
            int videoTrackIdPlayed = vlcPlayer.getVideoTrack();
            playingParam.setCurrentVideoTrackIndexPlayed(videoTrackIdPlayed);
        }

        //
        MediaPlayer.TrackDescription audioDis[] = vlcPlayer.getAudioTracks();
        int audioTrackId;
        String audioTrackName;
        audioTrackIndicesList.clear();
        if (audioDis != null) {
            // because it is null sometimes
            for (int i = 0; i < audioDis.length; i++) {
                audioTrackId = audioDis[i].id;
                audioTrackName = audioDis[i].name;
                Log.d(TAG, "audioDis[i].id = " + audioTrackId);
                Log.d(TAG, "audioDis[i].name = " + audioTrackName);
                // exclude disabled
                if (audioTrackId >=0 ) {
                    // enabled audio track
                    audioTrackIndicesList.add(audioTrackId);
                }
            }
        }
        numberOfAudioTracks = audioTrackIndicesList.size();
        Log.d(TAG, "numberOfAudioTracks = " + numberOfAudioTracks);
        if (numberOfAudioTracks == 0) {
            playingParam.setCurrentAudioTrackIndexPlayed(PlayerConstants.NoAudioTrack);
            playingParam.setCurrentChannelPlayed(PlayerConstants.NoAudioChannel);
        } else {
            int audioTrackIdPlayed = vlcPlayer.getAudioTrack();
            Log.d(TAG, "vlcPlayer.getAudioTrack() = " + audioTrackIdPlayed);
            Log.d(TAG, "audioTrackIdPlayed = " + audioTrackIdPlayed);
            int audioTrackIndex = 1;    // default audio track index
            int audioChannel = CommonConstants.StereoChannel;
            if (playingParam.isAutoPlay() || playingParam.isPlaySingleSong()) {
                audioTrackIndex = playingParam.getCurrentAudioTrackIndexPlayed();
                audioChannel = playingParam.getCurrentChannelPlayed();
            } else {
                for (int index = 0; index< audioTrackIndicesList.size(); index++) {
                    int audioId = audioTrackIndicesList.get(index);
                    if (audioId == audioTrackIdPlayed) {
                        audioTrackIndex = index + 1;
                        break;
                    }
                }
                // for open media. do not know the music track and vocal track
                playingParam.setMusicAudioTrackIndex(audioTrackIndex);
                playingParam.setMusicAudioChannel(audioChannel);
                playingParam.setVocalAudioTrackIndex(audioTrackIndex);
                playingParam.setVocalAudioChannel(audioChannel);
            }
            setAudioTrackAndChannel(audioTrackIndex, audioChannel);

            // for testing
            Media media = vlcPlayer.getMedia();
            int trackCount = media.getTrackCount();
            for (int i=0; i<trackCount; i++) {
                Media.Track track = media.getTrack(i);
                if (track.type == Media.Track.Type.Audio) {
                    // audio
                    Media.AudioTrack audioTrack = (Media.AudioTrack)track;
                    Log.d(TAG, "audioTrack.channels = " + audioTrack.channels);
                    Log.d(TAG, "audioTrack.rate = " + audioTrack.rate);
                }
            }
            //

            // build R.id.audioTrack submenu
            presentView.buildAudioTrackMenuItem(audioTrackIndicesList.size());
        }

        // update the duration on controller UI
        float duration = vlcPlayer.getLength();
        presentView.update_Player_duration_seekbar(duration);
    }

    @Override
    public void setPlayerTime(int progress) {
        super.setPlayerTime(progress);
        vlcPlayer.setTime(progress);
    }

    @Override
    public void setAudioVolume(float volume) {
        super.setAudioVolume(volume);

        int audioChannel = playingParam.getCurrentChannelPlayed();
        float leftVolume = volume;
        float rightVolume = volume;
        switch (audioChannel) {
            case CommonConstants.LeftChannel:
                rightVolume = 0;
                break;
            case CommonConstants.RightChannel:
                leftVolume = 0;
                break;
            case CommonConstants.StereoChannel:
                leftVolume = rightVolume;
                break;
        }
        int vlcMaxVolume = 100;
        vlcPlayer.setVolume((int) (volume * vlcMaxVolume));
        playingParam.setCurrentVolume(volume);
    }

    @Override
    public void setAudioTrackAndChannel(int audioTrackIndex, int audioChannel) {
        super.setAudioTrackAndChannel(audioTrackIndex, audioChannel);

        if (numberOfAudioTracks > 0) {
            // select audio track
            if (audioTrackIndex<=0) {
                Log.d(TAG, "No such audio Track Index = " + audioTrackIndex);
                return;
            }
            if (audioTrackIndex>numberOfAudioTracks) {
                Log.d(TAG, "No such audio Track Index = " + audioTrackIndex);
                // set to first track
                audioTrackIndex = 1;
            }
            int indexInArrayList = audioTrackIndex - 1;

            int audioTrackId = audioTrackIndicesList.get(indexInArrayList);
            vlcPlayer.setAudioTrack(audioTrackId);

            playingParam.setCurrentAudioTrackIndexPlayed(audioTrackIndex);

            // select audio channel
            playingParam.setCurrentChannelPlayed(audioChannel);

            setAudioVolume(playingParam.getCurrentVolume());
        }
    }

    @Override
    public Uri getValidatedUri(Uri tempUri) {
        super.getValidatedUri(tempUri);

        Uri resultUri = null;
        String filePath = ExternalStorageUtil.getUriRealPath(callingContext, tempUri);
        if (filePath != null) {
            if (!filePath.isEmpty()) {
                File songFile = new File(filePath);
                resultUri = Uri.fromFile(songFile);
            }
        }

        return resultUri;
    }

    @Override
    public void replayMedia() {
        super.replayMedia();

        Log.d(TAG, "replayMedia() is called.");
        if ( (mediaUri == null) || (Uri.EMPTY.equals(mediaUri)) || (numberOfAudioTracks<=0) ) {
            return;
        }

        long currentAudioPosition = 0;
        playingParam.setCurrentAudioPosition(currentAudioPosition);
        if (playingParam.isMediaSourcePrepared()) {
            vlcPlayer.setTime(currentAudioPosition); // use time to set position
            setProperAudioTrackAndChannel();
            if (!vlcPlayer.isPlaying()) {
                vlcPlayer.play();
            }
            Log.d(TAG, "replayMedia()--> vlcPlayer.seekTo(currentAudioPosition).");
        } else {
            Bundle playingParamOriginExtras = new Bundle();
            playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
            mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);   // prepare and startPlay
            Log.d(TAG, "replayMedia()--> mediaTransportControls.prepareFromUri().");
        }
    }

    @Override
    public void initMediaSessionCompat() {
        // Create a MediaSessionCompat
        super.initMediaSessionCompat();

        VLCMediaSessionCallback mediaSessionCallback = new VLCMediaSessionCallback(this, mLibVLC, vlcPlayer);
        mediaSessionCompat.setCallback(mediaSessionCallback);
        setMediaPlaybackState(playingParam.getCurrentPlaybackState());

        // Create a MediaControllerCompat
        mediaControllerCompat = new MediaControllerCompat(callingContext, mediaSessionCompat);
        MediaControllerCompat.setMediaController(mActivity, mediaControllerCompat);
        mediaControllerCallback = new VLCMediaControllerCallback(this);
        mediaControllerCompat.registerCallback(mediaControllerCallback);
        mediaTransportControls = mediaControllerCompat.getTransportControls();
    }

    @Override
    public void releaseMediaSessionCompat() {
        super.releaseMediaSessionCompat();

        if (mediaControllerCallback != null) {
            mediaControllerCompat.unregisterCallback(mediaControllerCallback);
            mediaControllerCallback = null;
        }
        mediaControllerCompat = null;
    }

    @Override
    public void saveInstanceState(@NonNull Bundle outState) {
        if (vlcPlayer != null) {
            playingParam.setCurrentAudioPosition(vlcPlayer.getTime());
        } else {
            playingParam.setCurrentAudioPosition(0);
        }
        outState.putIntegerArrayList("VideoTrackIndexList", videoTrackIndicesList);
        outState.putIntegerArrayList("AudioTrackIndexList", audioTrackIndicesList);

        super.saveInstanceState(outState);
    }
}
