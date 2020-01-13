package com.smile.karaokeplayer.Presenters;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSeekBar;

import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.Models.SongListSQLite;

import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;

public class VLCPlayerPresenter {

    private static final String TAG = new String("PlayerPresenter");

    private PresentView presentView;

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
    private SongInfo songInfo;

    public interface PresentView {
        void playButtonOnPauseButtonOff();
        void playButtonOffPauseButtonOn();
        void startAutoPlay();
        void replayMedia();
        void showBufferingMessage();
        void dismissBufferingMessage();
        void setMediaPlaybackState(int state);
        void hideNativeAds();
        void showNativeAds();
        AppCompatSeekBar getPlayer_duration_seekbar();
        MediaSessionCompat getMediaSessionCompat();
        void setAudioVolume(float volume);
        void getPlayingMediaInfoAndSetAudioActionSubMenu();
        void setTimerToHideSupportAndAudioController();
    }

    public VLCPlayerPresenter(PresentView presentView) {
        this.presentView = presentView;
    }

    public SongInfo getSongInfo() {
        return songInfo;
    }
    public void setSongInfo(SongInfo songInfo) {
        this.songInfo = songInfo;
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
        playingParam.setAutoPlay(false);
        playingParam.setMediaSourcePrepared(false);
        playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);

        playingParam.setCurrentVideoTrackIndexPlayed(0);

        playingParam.setMusicAudioTrackIndex(1);
        playingParam.setVocalAudioTrackIndex(1);
        playingParam.setCurrentAudioTrackIndexPlayed(playingParam.getMusicAudioTrackIndex());
        playingParam.setMusicAudioChannel(CommonConstants.LeftChannel);     // default
        playingParam.setVocalAudioChannel(CommonConstants.StereoChannel);   // default
        playingParam.setCurrentChannelPlayed(playingParam.getMusicAudioChannel());
        playingParam.setCurrentAudioPosition(0);
        playingParam.setCurrentVolume(1.0f);

        playingParam.setPublicNextSongIndex(0);
        playingParam.setPlayingPublic(true);

        playingParam.setMusicOrVocalOrNoSetting(0); // no music and vocal setting
        playingParam.setRepeatStatus(PlayerConstants.NoRepeatPlaying);    // no repeat playing songs
        playingParam.setPlaySingleSong(true);     // default
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
            songInfo = null;    // default
            if (arguments != null) {
                playingParam.setPlaySingleSong(arguments.getBoolean(PlayerConstants.IsPlaySingleSongState));
                songInfo = arguments.getParcelable(PlayerConstants.SongInfoState);
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
            songInfo = savedInstanceState.getParcelable(PlayerConstants.SongInfoState);
        }
    }

    public ArrayList<SongInfo> readPublicSongList(Context callingContext) {
        ArrayList<SongInfo> playlist;
        SongListSQLite songListSQLite = new SongListSQLite(callingContext);
        if (songListSQLite != null) {
            playlist = songListSQLite.readPlaylist();
            songListSQLite.closeDatabase();
            songListSQLite = null;
        } else {
            playlist = new ArrayList<>();
            Log.d(TAG, "Read database unsuccessfully --> " + playlist.size());
        }

        return playlist;
    }

    private void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("NumberOfVideoTracks", numberOfVideoTracks);
        outState.putInt("NumberOfAudioTracks", numberOfAudioTracks);
        outState.putIntegerArrayList("VideoTrackIndexList", videoTrackIndicesList);
        outState.putIntegerArrayList("AudioTrackIndexList", audioTrackIndicesList);
        outState.putParcelableArrayList("PublicSongList", publicSongList);
        outState.putParcelable("MediaUri", mediaUri);
        outState.putBoolean("CanShowNotSupportedFormat", canShowNotSupportedFormat);
        outState.putParcelable(PlayerConstants.SongInfoState, songInfo);
        outState.putParcelable("PlayingParameters", playingParam);
    }

    public void onSaveInstanceState(@NonNull Bundle outState, MediaPlayer vlcPlayer) {
        playingParam.setCurrentAudioPosition(vlcPlayer.getTime());
        onSaveInstanceState(outState);
    }
}
