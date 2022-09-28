package videoplayer.Presenters;

import static android.media.AudioManager.GET_DEVICES_OUTPUTS;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.smile.karaokeplayer.constants.CommonConstants;
import com.smile.karaokeplayer.constants.PlayerConstants;
import com.smile.karaokeplayer.presenters.BasePlayerPresenter;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.util.DisplayManager;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;

import videoplayer.Callbacks.VLCMediaControllerCallback;
import videoplayer.Callbacks.VLCMediaSessionCallback;
import videoplayer.Listeners.VLCPlayerEventListener;

public class VLCPlayerPresenter extends BasePlayerPresenter {

    private static final String TAG = "VLCPlayerPresenter";

    private final BasePresentView presentView;
    private final Fragment mFragment;
    private final Activity mActivity;
    private final AudioManager audioManager;
    private final int mStreamType = AudioManager.STREAM_MUSIC;

    private final int vlcMaxVolume = 100;

    private MediaControllerCompat mediaControllerCompat;
    private VLCMediaControllerCallback mediaControllerCallback;

    private LibVLC mLibVLC;
    private MediaPlayer vlcPlayer;

    // instances of the following members have to be saved when configuration changed
    private ArrayList<Integer> audioTrackIndicesList = new ArrayList<>();

    public VLCPlayerPresenter(Fragment fragment, BasePresentView presentView) {
        super(fragment, presentView);
        mFragment = fragment;
        mActivity = fragment.getActivity();
        this.presentView = presentView;
        this.audioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        // set volume control stream to STREAM_MUSIC
        mActivity.setVolumeControlStream(mStreamType);
    }

    /*
    public ArrayList<Integer> getAudioTrackIndicesList() {
        return audioTrackIndicesList;
    }
    public void setAudioTrackIndicesList(ArrayList<Integer> audioTrackIndicesList) {
        this.audioTrackIndicesList = audioTrackIndicesList;
    }
    */

    public BasePresentView getPresentView() {
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
        // mLibVLC = new LibVLC(mActivity, args);
        mLibVLC = new LibVLC(mActivity);
        vlcPlayer = new MediaPlayer(mLibVLC);
        vlcPlayer.setEventListener(new VLCPlayerEventListener(mActivity, this));
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
        Log.d(TAG, "setMediaPlaybackState() = " + state);
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

    @Override
    @SuppressWarnings("unchecked")
    public void initializeVariables(Bundle savedInstanceState, Intent callingIntent) {
        Log.d(TAG, "VLCPlayerPresenter --> initializeVariables()");
        super.initializeVariables(savedInstanceState, callingIntent);
        if (savedInstanceState == null) {
            audioTrackIndicesList = new ArrayList<>();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                audioTrackIndicesList = (ArrayList<Integer>)savedInstanceState.getSerializable(PlayerConstants.AudioTrackIndicesListState, ArrayList.class);
            } else audioTrackIndicesList = (ArrayList<Integer>)savedInstanceState.getSerializable(PlayerConstants.AudioTrackIndicesListState);
            if (audioTrackIndicesList == null) audioTrackIndicesList = new ArrayList<>();
        }
    }

    @Override
    public boolean isSeekable() {
        return true;
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
        playingParam.setCurrentVolume(volume);
        // removed on 2022-08-29 for testing
        vlcPlayer.setVolume((int) (volume * vlcMaxVolume));

        // added on 2020-08-10 for testing
        // works
        /*
        int maxStreamVolume = audioManager.getStreamMaxVolume(mStreamType);
        audioManager.setStreamVolume(mStreamType, (int) (volume * maxStreamVolume),0);
        AudioDeviceInfo[] a = audioManager.getDevices(GET_DEVICES_OUTPUTS);
        Log.d(TAG, "setAudioVolume.a = " + a.length);
        for (AudioDeviceInfo temp : a) {
            int[] channels = temp.getChannelCounts();
            for (int i=0; i<channels.length; i++) {
                Log.d(TAG, "setAudioVolume.channels[" + i + "] = " + channels[i]);
            }
        }
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
        Log.d(TAG, "setAudioTrackAndChannel().audioTrackIndex = " + audioTrackIndex +
                ", audioChannel = " + audioChannel + "numberOfAudioTracks = " + numberOfAudioTracks);
        if (audioTrackIndex <= 0) {
            return;
        }
        if (numberOfAudioTracks > 0) {
            // select audio track
            if (audioTrackIndex > numberOfAudioTracks) {
                // set to first track
                audioTrackIndex = 1;
            }
            int audioTrackId = audioTrackIndicesList.get(audioTrackIndex - 1);
            vlcPlayer.setAudioTrack(audioTrackId);
            playingParam.setCurrentAudioTrackIndexPlayed(audioTrackIndex);
            // select audio channel
            playingParam.setCurrentChannelPlayed(audioChannel);
            setAudioVolume(playingParam.getCurrentVolume());
        }
    }

    @Override
    public void specificPlayerReplayMedia(long currentAudioPosition) {
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
        // setMediaPlaybackState(playingParam.getCurrentPlaybackState());   // removed on 2021-04-03 for testing

        // Create a MediaControllerCompat
        // mediaControllerCompat = new MediaControllerCompat(callingContext, mediaSessionCompat);
        mediaControllerCompat = new MediaControllerCompat(mActivity, mediaSessionCompat);
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
        outState.putIntegerArrayList("AudioTrackIndexList", audioTrackIndicesList);

        super.saveInstanceState(outState);
    }

    @Override
    public void switchAudioToMusic() {
        Log.d(TAG, "switchAudioToMusic() is called");
        int trackIndex;
        int channel;
        if (numberOfAudioTracks >= 2) {
            // has more than 2 audio tracks
            trackIndex = playingParam.getCurrentAudioTrackIndexPlayed();
            trackIndex++;
            if (trackIndex>numberOfAudioTracks) {
                trackIndex = 1; // the first audio track
            }
            playingParam.setCurrentAudioTrackIndexPlayed(trackIndex);
            playingParam.setCurrentChannelPlayed(CommonConstants.StereoChannel);
        } else {
            playingParam.setCurrentAudioTrackIndexPlayed(1);    // first audio track
            channel = playingParam.getCurrentChannelPlayed();
            if (channel == CommonConstants.LeftChannel) {
                playingParam.setCurrentChannelPlayed(CommonConstants.RightChannel);
            } else {
                playingParam.setCurrentChannelPlayed(CommonConstants.LeftChannel);
            }
        }
        int audioTrack = playingParam.getCurrentAudioTrackIndexPlayed();
        int audioChannel = playingParam.getCurrentChannelPlayed();

        setAudioTrackAndChannel(audioTrack, audioChannel);
    }

    @Override
    public void switchAudioToVocal() {
        // do nothing because it does not have this functionality yet
    }

    @Override
    public void startDurationSeekBarHandler() {
        // do nothing because no need
    }

    @Override
    public long getMediaDuration() {
        return vlcPlayer.getLength();
    }

    @Override
    public void getPlayingMediaInfoAndSetAudioActionSubMenu() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "getPlayingMediaInfoAndSetAudioActionSubMenu() --> run()");
                handler.removeCallbacksAndMessages(null);
                getMediaInfoSetAudioSubMenu();
                onlyMusicShowNativeAndBannerAd();
            }
        };
        handler.postDelayed(runnable, 1000); // delay 1 seconds
    }

    private void getMediaInfoSetAudioSubMenu() {
        Log.d(TAG, "getMediaInfoSetAudioSubMenu()");
        MediaPlayer.TrackDescription videoDis[] = vlcPlayer.getVideoTracks();
        int videoTrackId;
        String videoTrackName;
        numberOfVideoTracks = 0;
        if (videoDis != null) {
            // because it is null sometimes
            for (int i = 0; i < videoDis.length; i++) {
                videoTrackId = videoDis[i].id;
                videoTrackName = videoDis[i].name;
                Log.d(TAG, "getMediaInfoSetAudioSubMenu.videoDis[i].id = " + videoTrackId);
                Log.d(TAG, "getMediaInfoSetAudioSubMenu.videoDis[i].name = " + videoTrackName);
                // exclude disabled
                if (videoTrackId >=0 ) {
                    // enabled video track
                    numberOfVideoTracks++;
                }
            }
        }
        Log.d(TAG, "getMediaInfoSetAudioSubMenu.numberOfVideoTracks = " + numberOfVideoTracks);

        //
        int audioTrackId;
        String audioTrackName;
        audioTrackIndicesList.clear();
        MediaPlayer.TrackDescription audioDis[] = vlcPlayer.getAudioTracks();
        if (audioDis != null) {
            // because it is null sometimes
            for (int i = 0; i < audioDis.length; i++) {
                audioTrackId = audioDis[i].id;
                audioTrackName = audioDis[i].name;
                Log.d(TAG, "getMediaInfoSetAudioSubMenu.audioDis[i].id = " + audioTrackId);
                Log.d(TAG, "getMediaInfoSetAudioSubMenu.audioDis[i].name = " + audioTrackName);
                // exclude disabled
                if (audioTrackId >= 0 ) {
                    // enabled audio track
                    audioTrackIndicesList.add(audioTrackId);
                }
            }
        }
        numberOfAudioTracks = audioTrackIndicesList.size();
        Log.d(TAG, "getMediaInfoSetAudioSubMenu.numberOfAudioTracks = " + numberOfAudioTracks);
        if (numberOfAudioTracks == 0) {
            playingParam.setCurrentAudioTrackIndexPlayed(PlayerConstants.NoAudioTrack);
            playingParam.setCurrentChannelPlayed(PlayerConstants.NoAudioChannel);
        } else {
            int audioTrackIdPlayed = vlcPlayer.getAudioTrack(); // currently played audio track
            Log.d(TAG, "getMediaInfoSetAudioSubMenu.getAudioTrack() = " + audioTrackIdPlayed);
            Log.d(TAG, "getMediaInfoSetAudioSubMenu.audioTrackIdPlayed = " + audioTrackIdPlayed);
            int audioTrackIndex = 1;    // default audio track index
            int audioChannel = CommonConstants.StereoChannel;
            if (playingParam.isAutoPlay() || playingParam.isPlaySingleSong() || playingParam.isInSongList()) {
                audioTrackIndex = playingParam.getCurrentAudioTrackIndexPlayed();
                audioChannel = playingParam.getCurrentChannelPlayed();
                Log.d(TAG, "getMediaInfoSetAudioSubMenu.Auto play or playing single song.");
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
                int musicAudioTrack = 2;
                audioChannel = CommonConstants.StereoChannel;
                if (numberOfAudioTracks >= 2) {
                    // more than 2 audio tracks
                    musicAudioTrack = 2; // default music is the second track
                } else {
                    // only one track
                    musicAudioTrack = 1;
                }
                playingParam.setVocalAudioTrackIndex(audioTrackIdPlayed);
                playingParam.setVocalAudioChannel(audioChannel);
                playingParam.setMusicAudioTrackIndex(musicAudioTrack);    // default music is the second track
                playingParam.setMusicAudioChannel(audioChannel);
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
                    Log.d(TAG, "getMediaInfoSetAudioSubMenu.audioTrack id = " + track.id);
                    Log.d(TAG, "getMediaInfoSetAudioSubMenu.audioTrack.channels = " + audioTrack.channels);
                    Log.d(TAG, "getMediaInfoSetAudioSubMenu.audioTrack.rate = " + audioTrack.rate);
                }
            }
            //
        }

        // update the duration on controller UI
        presentView.update_Player_duration_seekbar(vlcPlayer.getLength());
    }

    @Override
    public void removeCallbacksAndMessages() {
    }
}
