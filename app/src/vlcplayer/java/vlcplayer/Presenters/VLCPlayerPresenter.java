package vlcplayer.Presenters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Presenters.PlayerBasePresenter;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.util.DisplayManager;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.File;
import java.util.ArrayList;

import vlcplayer.Callbacks.VLCMediaControllerCallback;
import vlcplayer.Callbacks.VLCMediaSessionCallback;
import vlcplayer.Listeners.VLCPlayerEventListener;
import vlcplayer.utilities.ExternalStorageUtil;

public class VLCPlayerPresenter extends PlayerBasePresenter {

    private static final String TAG = "VLCPlayerPresenter";

    private final VLCPlayerPresentView presentView;
    private final Context callingContext;
    private final Activity mActivity;
    private final AudioManager audioManager;
    private final int mStreamType = AudioManager.STREAM_MUSIC;

    private final int vlcMaxVolume = 100;

    private MediaControllerCompat mediaControllerCompat;
    private VLCMediaControllerCallback mediaControllerCallback;

    private LibVLC mLibVLC;
    private MediaPlayer vlcPlayer;

    // instances of the following members have to be saved when configuration changed
    private ArrayList<Integer> videoTrackIndicesList = new ArrayList<>();
    private ArrayList<Integer> audioTrackIndicesList = new ArrayList<>();

    public interface VLCPlayerPresentView extends BasePresentView {
    }

    public VLCPlayerPresenter(Context context, VLCPlayerPresentView presentView) {
        super(context, presentView);
        this.callingContext = context;
        this.presentView = presentView;
        this.mActivity = (Activity)(this.presentView);
        this.audioManager = (AudioManager) callingContext.getSystemService(Context.AUDIO_SERVICE);
        // set volume control stream to STREAM_MUSIC
        mActivity.setVolumeControlStream(mStreamType);
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

    public VLCPlayerPresentView getPresentView() {
        return presentView;
    }

    public void attachPlayerViews(VLCVideoLayout videoVLCPlayerView, @Nullable DisplayManager dm, boolean enableSUBTITLES, boolean use_TEXTURE_VIEW) {
        vlcPlayer.attachViews(videoVLCPlayerView, dm, enableSUBTITLES, use_TEXTURE_VIEW);
    }

    public void detachPlayerViews() {
        vlcPlayer.detachViews();
    }

    public void initVLCPlayer() {
        // final ArrayList<String> args = new ArrayList<>();
        // args.add("-vvv");
        // mLibVLC = new LibVLC(callingContext, args);
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
        videoTrackIndicesList = new ArrayList<>();
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
            int audioTrackIdPlayed = vlcPlayer.getAudioTrack(); // currently played audio track
            Log.d(TAG, "vlcPlayer.getAudioTrack() = " + audioTrackIdPlayed);
            Log.d(TAG, "audioTrackIdPlayed = " + audioTrackIdPlayed);
            int audioTrackIndex = 1;    // default audio track index
            int audioChannel = CommonConstants.StereoChannel;
            if (playingParam.isAutoPlay() || playingParam.isPlaySingleSong() || playingParam.isInSongList()) {
                audioTrackIndex = playingParam.getCurrentAudioTrackIndexPlayed();
                audioChannel = playingParam.getCurrentChannelPlayed();
                Log.d(TAG, "Auto play or playing single song.");
            } else {
                for (int index = 0; index< audioTrackIndicesList.size(); index++) {
                    int audioId = audioTrackIndicesList.get(index);
                    if (audioId == audioTrackIdPlayed) {
                        audioTrackIndex = index + 1;
                        break;
                    }
                }
                // for open media. do not know the music track and vocal track
                // guess
                audioTrackIdPlayed = 1;
                if (numberOfAudioTracks >= 2) {
                    // more than 2 audio tracks
                    audioChannel = CommonConstants.StereoChannel;
                    playingParam.setVocalAudioTrackIndex(audioTrackIdPlayed);
                    playingParam.setVocalAudioChannel(audioChannel);
                    playingParam.setMusicAudioTrackIndex(2);
                    playingParam.setMusicAudioChannel(audioChannel);
                } else {
                    // only one track
                    audioChannel = CommonConstants.LeftChannel;
                    playingParam.setVocalAudioTrackIndex(audioTrackIdPlayed);
                    playingParam.setVocalAudioChannel(audioChannel);
                    playingParam.setMusicAudioTrackIndex(audioTrackIdPlayed);
                    playingParam.setMusicAudioChannel(CommonConstants.RightChannel);
                }
            }
            setAudioTrackAndChannel(audioTrackIndex, audioChannel);

            // build R.id.audioTrack submenu
            presentView.buildAudioTrackMenuItem(audioTrackIndicesList.size());

            // for testing
            // Media media = vlcPlayer.getMedia();  // for version 3.1.12
            IMedia media = vlcPlayer.getMedia();    // for version above 3.3.0
            int trackCount = media.getTrackCount();
            for (int i=0; i<trackCount; i++) {
                // Media.Track track = media.getTrack(i);   // for version 3.1.12
                // if (track.type == Media.Track.Type.Audio) {  // for version 3.1.12
                IMedia.Track track = media.getTrack(i);
                if (track.type == IMedia.Track.Type.Audio) {
                    // audio
                    IMedia.AudioTrack audioTrack = (IMedia.AudioTrack)track;
                    Log.d(TAG, "audioTrack id = " + track.id);
                    Log.d(TAG, "audioTrack.channels = " + audioTrack.channels);
                    Log.d(TAG, "audioTrack.rate = " + audioTrack.rate);
                }
            }
            //
        }

        // update the duration on controller UI
        float duration = vlcPlayer.getLength();
        presentView.update_Player_duration_seekbar(duration);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initializeVariables(Bundle savedInstanceState, Intent callingIntent) {
        Log.d(TAG, "VLCPlayerPresenter --> initializeVariables()");
        super.initializeVariables(savedInstanceState, callingIntent);
        if (savedInstanceState == null) {
            videoTrackIndicesList = new ArrayList<>();
            audioTrackIndicesList = new ArrayList<>();
        } else {
            videoTrackIndicesList = (ArrayList<Integer>)savedInstanceState.getSerializable(PlayerConstants.VideoTrackIndicesListState);
            audioTrackIndicesList = (ArrayList<Integer>)savedInstanceState.getSerializable(PlayerConstants.AudioTrackIndicesListState);
        }
    }

    @Override
    public boolean isSeekable() {
        boolean result = super.isSeekable();
        return result;
    }

    @Override
    public void setPlayerTime(int progress) {
        vlcPlayer.setTime(progress);
    }

    @Override
    public void setAudioVolume(float volume) {
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
        vlcPlayer.setVolume((int) (volume * vlcMaxVolume));
        playingParam.setCurrentVolume(volume);

        /*
        // added on 2020-08-10 for testing
        // works
        int maxStreamVolume = audioManager.getStreamMaxVolume(mStreamType);
        audioManager.setStreamVolume(mStreamType, (int) (volume * maxStreamVolume),0);
        //
        */
    }

    @Override
    public void setAudioVolumeInsideVolumeSeekBar(int i) {
        // needed to put inside the presenter
        float currentVolume = 1.0f;
        if (i < PlayerConstants.MaxProgress) {
            currentVolume = (float)i / (float)PlayerConstants.MaxProgress;
        }
        setAudioVolume(currentVolume);
        //
    }

    @Override
    public int getCurrentProgressForVolumeSeekBar() {
        int currentProgress;
        float currentVolume = playingParam.getCurrentVolume();
        if ( currentVolume >= 1.0f) {
            currentProgress = PlayerConstants.MaxProgress;
        } else {
            // percentage of 100
            currentProgress = (int) (currentVolume * PlayerConstants.MaxProgress);
        }

        return currentProgress;
    }

    @Override
    public void setAudioTrackAndChannel(int audioTrackIndex, int audioChannel) {
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
        Log.d(TAG, "VLCPlayerPresenter.getValidatedUri() is called.");
        super.getValidatedUri(tempUri);

        Uri resultUri = null;
        try {
            String filePath = ExternalStorageUtil.getUriRealPath(callingContext, tempUri);
            Log.d(TAG, "VLCPlayerPresenter.getValidatedUri() --> filePath == " + filePath);
            if (filePath != null) {
                if (!filePath.isEmpty()) {
                    File songFile = new File(filePath);
                    resultUri = Uri.fromFile(songFile);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return resultUri;
    }

    @Override
    protected void specificPlayerReplayMedia(long currentAudioPosition) {
        vlcPlayer.setTime(currentAudioPosition); // use time to set position
        switchAudioToVocal();
        if (!vlcPlayer.isPlaying()) {
            vlcPlayer.play();
        }
    }

    @Override
    public void initMediaSessionCompat() {
        // Create a MediaSessionCompat
        Log.d(TAG, "VLCPlayerPresenter.initMediaSessionCompat() is called.");
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
        Log.d(TAG, "VLCPlayerPresenter.releaseMediaSessionCompat() is called.");
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
