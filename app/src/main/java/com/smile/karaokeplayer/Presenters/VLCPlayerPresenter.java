package com.smile.karaokeplayer.Presenters;

import android.app.Activity;
import android.content.Context;
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

public class VLCPlayerPresenter {

    private static final String TAG = new String("VLCPlayerPresenter");

    private final PresentView presentView;
    private final Context callingContext;
    private final Activity mActivity;
    private final float toastTextSize;

    private MediaSessionCompat mediaSessionCompat;
    private MediaControllerCompat mediaControllerCompat;
    private VLCMediaControllerCallback mediaControllerCallback;
    private MediaControllerCompat.TransportControls mediaTransportControls;

    private LibVLC mLibVLC;
    private MediaPlayer vlcPlayer;

    // instances of the following members have to be saved when configuration changed
    private Uri mediaUri;
    // private MediaSource mediaSource;
    private int numberOfVideoTracks;
    private int numberOfAudioTracks;
    private ArrayList<Integer> videoTrackIndicesList;
    private ArrayList<Integer> audioTrackIndicesList;
    private ArrayList<SongInfo> publicSongList;
    private PlayingParameters playingParam;
    private boolean canShowNotSupportedFormat;
    private SongInfo singleSongInfo;

    public interface PresentView {
        void setImageButtonStatus();
        void playButtonOnPauseButtonOff();
        void playButtonOffPauseButtonOn();
        void showBufferingMessage();
        void dismissBufferingMessage();
        void hideNativeAds();
        void showNativeAds();
        AppCompatSeekBar getPlayer_duration_seekbar();
        void setTimerToHideSupportAndAudioController();
        void buildAudioTrackMenuItem(int audioTrackNumber);
        void update_Player_duration_seekbar(float duration);
    }

    public VLCPlayerPresenter(PresentView presentView, Context context) {
        this.presentView = presentView;
        this.callingContext = context;
        this.mActivity = ((Fragment)(this.presentView)).getActivity();

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(callingContext, ScreenUtil.FontSize_Pixel_Type, null);
        float textFontSize = ScreenUtil.suitableFontSize(callingContext, defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        toastTextSize = 0.7f * textFontSize;
    }

    public SongInfo getSingleSongInfo() {
        return singleSongInfo;
    }
    public void setSingleSongInfo(SongInfo singleSongInfo) {
        this.singleSongInfo = singleSongInfo;
    }

    public Uri getMediaUri() {
        return mediaUri;
    }
    public void setMediaUri(Uri mediaUri) {
        this.mediaUri = mediaUri;
    }

    public int getNumberOfAudioTracks() {
        return numberOfAudioTracks;
    }
    public void setNumberOfAudioTracks(int numberOfAudioTracks) {
        this.numberOfAudioTracks = numberOfAudioTracks;
    }

    public int getNumberOfVideoTracks() {
        return numberOfVideoTracks;
    }
    public void setNumberOfVideoTracks(int numberOfVideoTracks) {
        this.numberOfVideoTracks = numberOfVideoTracks;
    }

    public boolean isCanShowNotSupportedFormat() {
        return canShowNotSupportedFormat;
    }
    public void setCanShowNotSupportedFormat(boolean canShowNotSupportedFormat) {
        this.canShowNotSupportedFormat = canShowNotSupportedFormat;
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

    public ArrayList<SongInfo> getPublicSongList() {
        return publicSongList;
    }
    public void setPublicSongList(ArrayList<SongInfo> publicSongList) {
        this.publicSongList = publicSongList;
    }

    public PlayingParameters getPlayingParam() {
        return playingParam;
    }
    public void setPlayingParam(PlayingParameters playingParam) {
        this.playingParam = playingParam;
    }

    public PresentView getPresentView() {
        return presentView;
    }

    private void initializePlayingParam() {
        playingParam = new PlayingParameters();
        playingParam.initializePlayingParameters();
    }

    @SuppressWarnings("unchecked")
    public void initializeVariables(Bundle savedInstanceState, Bundle arguments) {

        if (savedInstanceState == null) {
            // new fragment is being created
            numberOfVideoTracks = 0;
            numberOfAudioTracks = 0;
            videoTrackIndicesList = new ArrayList<>();
            audioTrackIndicesList = new ArrayList<>();

            mediaUri = null;
            initializePlayingParam();
            canShowNotSupportedFormat = false;
            playingParam.setPlaySingleSong(false);  // default
            singleSongInfo = null;    // default
            if (arguments != null) {
                playingParam.setPlaySingleSong(arguments.getBoolean(PlayerConstants.IsPlaySingleSongState));
                singleSongInfo = arguments.getParcelable(PlayerConstants.SongInfoState);
            }
        } else {
            // needed to be set
            numberOfVideoTracks = savedInstanceState.getInt("NumberOfVideoTracks",0);
            numberOfAudioTracks = savedInstanceState.getInt("NumberOfAudioTracks");
            videoTrackIndicesList = savedInstanceState.getIntegerArrayList("VideoTrackIndexList");
            audioTrackIndicesList = savedInstanceState.getIntegerArrayList("AudioTrackIndexList");
            publicSongList = savedInstanceState.getParcelableArrayList("PublicSongList");

            mediaUri = savedInstanceState.getParcelable("MediaUri");
            playingParam = savedInstanceState.getParcelable("PlayingParameters");
            canShowNotSupportedFormat = savedInstanceState.getBoolean("CanShowNotSupportedFormat");
            if (playingParam == null) {
                initializePlayingParam();
            }
            singleSongInfo = savedInstanceState.getParcelable(PlayerConstants.SongInfoState);
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
        vlcPlayer.setEventListener(new VLCPlayerEventListener(callingContext, this, vlcPlayer));
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

    public void setPlayerTime(long progress) {
        vlcPlayer.setTime(progress);
    }

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
        int vlcMaxVolume = 100;
        vlcPlayer.setVolume((int) (volume * vlcMaxVolume));
        playingParam.setCurrentVolume(volume);
    }

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

    public void switchAudioToVocal() {
        int vocalAudioTrackIndex = playingParam.getVocalAudioTrackIndex();
        int vocalAudioChannel = playingParam.getVocalAudioChannel();
        playingParam.setMusicOrVocalOrNoSetting(PlayerConstants.PlayingVocal);
        setAudioTrackAndChannel(vocalAudioTrackIndex, vocalAudioChannel);
    }

    public void switchAudioToMusic() {
        int musicAudioTrackIndex = playingParam.getMusicAudioTrackIndex();
        int musicAudioChannel = playingParam.getMusicAudioChannel();
        playingParam.setMusicOrVocalOrNoSetting(PlayerConstants.PlayingMusic);
        setAudioTrackAndChannel(musicAudioTrackIndex, musicAudioChannel);
    }

    private void setProperAudioTrackAndChannel() {
        if (playingParam.isAutoPlay()) {
            if (playingParam.isPlayingPublic()) {
                switchAudioToVocal();
            } else {
                switchAudioToMusic();
            }
        } else {
            // not auto playing media, means using open menu to open a media
            switchAudioToVocal();   // music and vocal are the same in this case
        }
    }

    public void playSingleSong(SongInfo songInfo) {
        if (songInfo == null) {
            return;
        }

        String filePath = songInfo.getFilePath();
        if (filePath==null) {
            return;
        }
        filePath = filePath.trim();
        if (filePath.equals("")) {
            return;
        }

        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Have to add android:requestLegacyExternalStorage="true" in AndroidManifest.xml
            // to let devices that are above (included) API 29 can still use external storage
            mediaUri = null;
            filePath = ExternalStorageUtil.getUriRealPath(callingContext, Uri.parse(filePath));
            if (filePath != null) {
                if (!filePath.isEmpty()) {
                    File songFile = new File(filePath);
                    mediaUri = Uri.fromFile(songFile);
                }
            }
        // } else {
        //     mediaUri = Uri.parse(filePath);
        // }

        if (mediaUri==null || Uri.EMPTY.equals(mediaUri)) {
            Log.d(TAG, "Media Uri is empty.");
            ScreenUtil.showToast(callingContext, "Media Uri is empty.", toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
            return;
        }

        playingParam.setMusicOrVocalOrNoSetting(PlayerConstants.PlayingVocal);  // presume vocal
        playingParam.setCurrentVideoTrackIndexPlayed(0);

        playingParam.setMusicAudioTrackIndex(songInfo.getMusicTrackNo());
        playingParam.setMusicAudioChannel(songInfo.getMusicChannel());

        playingParam.setVocalAudioTrackIndex(songInfo.getVocalTrackNo());
        playingParam.setCurrentAudioTrackIndexPlayed(playingParam.getVocalAudioTrackIndex());
        playingParam.setVocalAudioChannel(songInfo.getVocalChannel());
        playingParam.setCurrentChannelPlayed(playingParam.getVocalAudioChannel());

        playingParam.setCurrentAudioPosition(0);
        playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);
        playingParam.setMediaSourcePrepared(false);

        // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
        // pass the saved instance of playingParam to
        Bundle playingParamOriginExtras = new Bundle();
        playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
        mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
    }

    public void startAutoPlay() {
        if (mActivity.isFinishing()) {
            // activity is being destroyed
            return;
        }

        // activity is not being destroyed then check
        // if there are still songs to be startPlay
        boolean hasSongs = false;   // no more songs to startPlay
        if (hasSongs) {
            playingParam.setPlayingPublic(false);   // playing next ordered song
        } else {
            playingParam.setPlayingPublic(true);   // playing next public song on list
        }

        SongInfo songInfo = null;
        if (playingParam.isPlayingPublic())  {
            int publicSongListSize = 0;
            if (publicSongList != null) {
                publicSongListSize = publicSongList.size();
            }
            if (publicSongListSize <= 0) {
                // no public songs
                ScreenUtil.showToast(callingContext, callingContext.getString(R.string.noPlaylistString), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                playingParam.setAutoPlay(false);    // cancel auto startPlay
            } else {
                // There are public songs to be played
                boolean stillPlayNext = true;
                int repeatStatus = playingParam.getRepeatStatus();
                int publicNextSongIndex = playingParam.getPublicNextSongIndex();
                switch (repeatStatus) {
                    case PlayerConstants.NoRepeatPlaying:
                        // no repeat
                        if ( (publicNextSongIndex >= publicSongListSize) || (publicNextSongIndex<0) ) {
                            // stop playing
                            playingParam.setAutoPlay(false);
                            stopPlay();
                            stillPlayNext = false;  // stop here and do not go to next
                        }
                        break;
                    case PlayerConstants.RepeatOneSong:
                        // repeat one song
                        Log.d(TAG, "startAutoPlay() --> RepeatOneSong");
                        if ( (publicNextSongIndex > 0) && (publicNextSongIndex <= publicSongListSize) ) {
                            publicNextSongIndex--;
                            Log.d(TAG, "startAutoPlay() --> RepeatOneSong --> publicSongIndex = " + publicNextSongIndex);
                        }
                        break;
                    case PlayerConstants.RepeatAllSongs:
                        // repeat all songs
                        Log.d(TAG, "startAutoPlay() --> RepeatAllSongs --> publicSongListSize = " + publicSongListSize);
                        Log.d(TAG, "startAutoPlay() --> RepeatAllSongs --> publicSongIndex = " + publicNextSongIndex);
                        if (publicNextSongIndex >= publicSongListSize) {
                            publicNextSongIndex = 0;
                        }
                        break;
                }

                if (stillPlayNext) {    // still startPlay the next song
                    songInfo = publicSongList.get(publicNextSongIndex);
                    playSingleSong(songInfo);
                    publicNextSongIndex++;  // set next index of playlist that will be played
                    playingParam.setPublicNextSongIndex(publicNextSongIndex);
                }
                Log.d(TAG, "Repeat status = " + repeatStatus);
                Log.d(TAG, "startAutoPlay() finished --> " + publicNextSongIndex--);
                // }
            }
        } else {
            // startPlay next song that user has ordered
            playingParam.setMusicOrVocalOrNoSetting(PlayerConstants.PlayingMusic);  // presume music
            if (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) {
                if (playingParam.getRepeatStatus() != PlayerConstants.NoRepeatPlaying) {
                    // repeat playing this mediaUri
                } else {
                    // next song that user ordered
                }
            }
            Log.d(TAG, "startAutoPlay() finished --> ordered song.");
        }
        presentView.setImageButtonStatus();
    }

    public void startPlay() {
        Log.d(TAG, "startPlay() is called.");
        int playbackState = playingParam.getCurrentPlaybackState();
        if ( (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playbackState != PlaybackStateCompat.STATE_PLAYING) ) {
            // no media file opened or playing has been stopped
            if ( (playbackState == PlaybackStateCompat.STATE_PAUSED)
                    || (playbackState == PlaybackStateCompat.STATE_REWINDING)
                    || (playbackState == PlaybackStateCompat.STATE_FAST_FORWARDING) ) {
                mediaTransportControls.play();
                Log.d(TAG, "startPlay() --> mediaTransportControls.startPlay() is called.");
            } else {
                // (playbackState == PlaybackStateCompat.STATE_STOPPED) or
                // (playbackState == PlaybackStateCompat.STATE_NONE)
                Log.d(TAG, "startPlay() --> replayMedia() is called.");
                replayMedia();
            }
        }
    }

    public void pausePlay() {
        if ( (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) ) {
            // no media file opened or playing has been stopped
            mediaTransportControls.pause();
        }
    }

    public void stopPlay() {
        if (playingParam.isAutoPlay()) {
            // auto startPlay
            startAutoPlay();
        } else {
            if (playingParam.getRepeatStatus() == PlayerConstants.NoRepeatPlaying) {
                // no repeat playing
                if ((mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_NONE)) {
                    mediaTransportControls.stop();
                    Log.d(TAG, "stopPlay() ---> mediaTransportControls.stop() is called.");
                }
            } else {
                replayMedia();
            }
        }
        Log.d(TAG, "stopPlay() is called.");
    }

    public void replayMedia() {
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

    public void initMediaSessionCompat() {
        // Create a MediaSessionCompat
        mediaSessionCompat = new MediaSessionCompat(callingContext, PlayerConstants.LOG_TAG);
        VLCMediaSessionCallback mediaSessionCallback = new VLCMediaSessionCallback(this, mLibVLC, vlcPlayer);
        mediaSessionCompat.setCallback(mediaSessionCallback);
        // Do not let MediaButtons restart the player when the app is not visible
        mediaSessionCompat.setMediaButtonReceiver(null);
        mediaSessionCompat.setActive(true); // might need to find better place to put
        setMediaPlaybackState(playingParam.getCurrentPlaybackState());

        // Create a MediaControllerCompat
        mediaControllerCompat = new MediaControllerCompat(callingContext, mediaSessionCompat);
        MediaControllerCompat.setMediaController(mActivity, mediaControllerCompat);
        mediaControllerCallback = new VLCMediaControllerCallback(this);
        mediaControllerCompat.registerCallback(mediaControllerCallback);
        mediaTransportControls = mediaControllerCompat.getTransportControls();
    }

    public void releaseMediaSessionCompat() {

        if (mediaSessionCompat != null) {
            mediaSessionCompat.setActive(false);
            mediaSessionCompat.release();
            mediaSessionCompat = null;
        }
        mediaTransportControls = null;
        if (mediaControllerCallback != null) {
            mediaControllerCompat.unregisterCallback(mediaControllerCallback);
            mediaControllerCallback = null;
        }
        mediaControllerCompat = null;
    }

    public MediaSessionCompat getMediaSessionCompat() {
        return mediaSessionCompat;
    }

    public MediaControllerCompat.TransportControls getMediaTransportControls() {
        return mediaTransportControls;
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

    public void onSaveInstanceState(@NonNull Bundle outState) {
        playingParam.setCurrentAudioPosition(vlcPlayer.getTime());
        outState.putInt("NumberOfVideoTracks", numberOfVideoTracks);
        outState.putInt("NumberOfAudioTracks", numberOfAudioTracks);
        outState.putIntegerArrayList("VideoTrackIndexList", videoTrackIndicesList);
        outState.putIntegerArrayList("AudioTrackIndexList", audioTrackIndicesList);
        outState.putParcelableArrayList("PublicSongList", publicSongList);
        outState.putParcelable("MediaUri", mediaUri);
        outState.putBoolean("CanShowNotSupportedFormat", canShowNotSupportedFormat);
        outState.putParcelable(PlayerConstants.SongInfoState, singleSongInfo);
        outState.putParcelable("PlayingParameters", playingParam);
    }
}
