package videoplayer.Presenters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Presenters.BasePlayerPresenter;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.util.DisplayManager;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;

import videoplayer.Callbacks.VLCMediaControllerCallback;
import videoplayer.Callbacks.VLCMediaSessionCallback;
import videoplayer.Listeners.VLCPlayerEventListener;
import videoplayer.utilities.FileSelectUtil;
import videoplayer.utilities.UriUtil;

public class VLCPlayerPresenter extends BasePlayerPresenter {

    private static final String TAG = "VLCPlayerPresenter";

    private final BasePresentView presentView;
    // private final Context callingContext;
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

    // public interface VLCPlayerPresentView extends BasePresentView {
    // }

    public VLCPlayerPresenter(Activity activity, BasePresentView presentView) {
        super(activity, presentView);
        mActivity = activity;
        // this.callingContext = activity;
        this.presentView = presentView;
        // this.mActivity = (Activity)(this.presentView);
        this.audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
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
        // mLibVLC = new LibVLC(callingContext, args);
        // mLibVLC = new LibVLC(callingContext);
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
        tempUri = super.getValidatedUri(tempUri);

        return tempUri;

        /*
        // removed on 2020-12-21
        // because of using file picker to select file
        // so the uri is already file uri

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
        */
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
        outState.putIntegerArrayList("VideoTrackIndexList", videoTrackIndicesList);
        outState.putIntegerArrayList("AudioTrackIndexList", audioTrackIndicesList);

        super.saveInstanceState(outState);
    }

    @Override
    public Intent createSelectFilesToOpenIntent() {
        return FileSelectUtil.selectFileToOpenIntent(mActivity, false);
    }

    @Override
    public ArrayList<Uri> getUrisListFromIntentPresenter(Intent data) {
        // return UriUtil.getUrisListFromIntent(callingContext, data);
        return UriUtil.getUrisListFromIntent(mActivity, data);
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
                getVlcPlayingMediaInfoAndSetAudioActionSubMenu();
                onlyMusicShowNativeAndBannerAd();
            }
        };
        handler.postDelayed(runnable, 1000); // delay 1 seconds
    }

    private void getVlcPlayingMediaInfoAndSetAudioActionSubMenu() {
        Log.d(TAG, "getVlcPlayingMediaInfoAndSetAudioActionSubMenu()");
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
                    Log.d(TAG, "audioTrack id = " + track.id);
                    Log.d(TAG, "audioTrack.channels = " + audioTrack.channels);
                    Log.d(TAG, "audioTrack.rate = " + audioTrack.rate);
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
