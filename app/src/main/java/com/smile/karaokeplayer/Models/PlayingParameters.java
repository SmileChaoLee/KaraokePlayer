package com.smile.karaokeplayer.Models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.media.session.PlaybackStateCompat;

import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;

public class PlayingParameters implements Parcelable {

    private int currentPlaybackState;
    private boolean isAutoPlay;
    private boolean isMediaPrepared;
    private int currentVideoTrackIndexPlayed;
    private int musicAudioChannel;
    private int vocalAudioChannel;
    private int currentChannelPlayed;
    private int musicAudioTrackIndex;
    private int vocalAudioTrackIndex;
    private int currentAudioTrackIndexPlayed;
    private long currentAudioPosition;
    private float currentVolume;
    private int currentSongIndex;
    private int repeatStatus;
    private boolean isPlaySingleSong;
    private boolean isInSongList;
    private int numOfPlayedSongs;

    public PlayingParameters(int currentPlaybackState, boolean isAutoPlay, boolean isMediaPrepared,
                             int currentVideoTrackIndexPlayed, int musicAudioChannel, int vocalAudioChannel,
                             int currentChannelPlayed, int musicAudioTrackIndex, int vocalAudioTrackIndex,
                             int currentAudioTrackIndexPlayed, long currentAudioPosition, float currentVolume,
                             int currentSongIndex, int repeatStatus, boolean isPlaySingleSong,
                             boolean isInSongList, int numOfPlayedSongs) {
        this.currentPlaybackState = currentPlaybackState;
        this.isAutoPlay = isAutoPlay;
        this.isMediaPrepared = isMediaPrepared;
        this.currentVideoTrackIndexPlayed = currentVideoTrackIndexPlayed;
        this.musicAudioChannel = musicAudioChannel;
        this.vocalAudioChannel = vocalAudioChannel;
        this.currentChannelPlayed = currentChannelPlayed;
        this.musicAudioTrackIndex = musicAudioTrackIndex;
        this.vocalAudioTrackIndex = vocalAudioTrackIndex;
        this.currentAudioTrackIndexPlayed = currentAudioTrackIndexPlayed;
        this.currentAudioPosition = currentAudioPosition;
        this.currentVolume = currentVolume;
        this.currentSongIndex = currentSongIndex;
        this.repeatStatus = repeatStatus;
        this.isPlaySingleSong = isPlaySingleSong;
        this.isInSongList = isInSongList;
        this.numOfPlayedSongs = numOfPlayedSongs;
    }

    public PlayingParameters() {
    }

    public int getCurrentPlaybackState() {
        return currentPlaybackState;
    }
    public void setCurrentPlaybackState(int currentPlaybackState) {
        this.currentPlaybackState = currentPlaybackState;
    }

    public boolean isAutoPlay() {
        return isAutoPlay;
    }
    public void setAutoPlay(boolean autoPlay) {
        isAutoPlay = autoPlay;
    }

    public boolean isMediaPrepared() {
        return isMediaPrepared;
    }
    public void setMediaPrepared(boolean isMediaPrepared) {
        this.isMediaPrepared = isMediaPrepared;
    }

    public int getCurrentVideoTrackIndexPlayed() {
        return currentVideoTrackIndexPlayed;
    }
    public void setCurrentVideoTrackIndexPlayed(int currentVideoTrackIndexPlayed) {
        this.currentVideoTrackIndexPlayed = currentVideoTrackIndexPlayed;
    }

    public int getMusicAudioChannel() {
        return musicAudioChannel;
    }
    public void setMusicAudioChannel(int musicAudioChannel) {
        this.musicAudioChannel = musicAudioChannel;
    }

    public int getVocalAudioChannel() {
        return vocalAudioChannel;
    }
    public void setVocalAudioChannel(int vocalAudioChannel) {
        this.vocalAudioChannel = vocalAudioChannel;
    }

    public int getCurrentChannelPlayed() {
        return currentChannelPlayed;
    }
    public void setCurrentChannelPlayed(int currentChannelPlayed) {
        this.currentChannelPlayed = currentChannelPlayed;
    }

    public int getMusicAudioTrackIndex() {
        return musicAudioTrackIndex;
    }
    public void setMusicAudioTrackIndex(int musicAudioTrackIndex) {
        this.musicAudioTrackIndex = musicAudioTrackIndex;
    }

    public int getVocalAudioTrackIndex() {
        return vocalAudioTrackIndex;
    }
    public void setVocalAudioTrackIndex(int vocalAudioTrackIndex) {
        this.vocalAudioTrackIndex = vocalAudioTrackIndex;
    }

    public int getCurrentAudioTrackIndexPlayed() {
        return currentAudioTrackIndexPlayed;
    }
    public void setCurrentAudioTrackIndexPlayed(int currentAudioTrackIndexPlayed) {
        this.currentAudioTrackIndexPlayed = currentAudioTrackIndexPlayed;
    }

    public long getCurrentAudioPosition() {
        return currentAudioPosition;
    }
    public void setCurrentAudioPosition(long currentAudioPosition) {
        this.currentAudioPosition = currentAudioPosition;
    }

    public float getCurrentVolume() {
        return currentVolume;
    }
    public void setCurrentVolume(float currentVolume) {
        this.currentVolume = currentVolume;
    }

    public int getCurrentSongIndex() {
        return currentSongIndex;
    }
    public void setCurrentSongIndex(int currentSongIndex) {
        this.currentSongIndex = currentSongIndex;
    }

    public int getRepeatStatus() {
        return repeatStatus;
    }
    public void setRepeatStatus(int repeatStatus) {
        this.repeatStatus = repeatStatus;
    }

    public boolean isPlaySingleSong() {
        return isPlaySingleSong;
    }
    public void setPlaySingleSong(boolean isPlaySingleSong) {
        this.isPlaySingleSong = isPlaySingleSong;
    }

    public boolean isInSongList() {
        return isInSongList;
    }
    public void setInSongList(boolean isInSongList) {
        this.isInSongList = isInSongList;
    }

    public int getNumOfPlayedSongs() {
        return numOfPlayedSongs;
    }
    public void setNumOfPlayedSongs(int numOfPlayedSongs) {
        this.numOfPlayedSongs = numOfPlayedSongs;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.currentPlaybackState);
        dest.writeByte(this.isAutoPlay ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isMediaPrepared ? (byte) 1 : (byte) 0);
        dest.writeInt(this.currentVideoTrackIndexPlayed);
        dest.writeInt(this.musicAudioChannel);
        dest.writeInt(this.vocalAudioChannel);
        dest.writeInt(this.currentChannelPlayed);
        dest.writeInt(this.musicAudioTrackIndex);
        dest.writeInt(this.vocalAudioTrackIndex);
        dest.writeInt(this.currentAudioTrackIndexPlayed);
        dest.writeLong(this.currentAudioPosition);
        dest.writeFloat(this.currentVolume);
        dest.writeInt(this.currentSongIndex);
        dest.writeInt(this.repeatStatus);
        dest.writeByte(this.isPlaySingleSong ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isInSongList ? (byte) 1 : (byte) 0);
        dest.writeInt(this.numOfPlayedSongs);
    }

    protected PlayingParameters(Parcel in) {
        this.currentPlaybackState = in.readInt();
        this.isAutoPlay = in.readByte() != 0;
        this.isMediaPrepared = in.readByte() != 0;
        this.currentVideoTrackIndexPlayed = in.readInt();
        this.musicAudioChannel = in.readInt();
        this.vocalAudioChannel = in.readInt();
        this.currentChannelPlayed = in.readInt();
        this.musicAudioTrackIndex = in.readInt();
        this.vocalAudioTrackIndex = in.readInt();
        this.currentAudioTrackIndexPlayed = in.readInt();
        this.currentAudioPosition = in.readLong();
        this.currentVolume = in.readFloat();
        this.currentSongIndex = in.readInt();
        this.repeatStatus = in.readInt();
        this.isPlaySingleSong = in.readByte() != 0;
        this.isInSongList = in.readByte() != 0;
        this.numOfPlayedSongs = in.readInt();
    }

    public static final Creator<PlayingParameters> CREATOR = new Creator<PlayingParameters>() {
        @Override
        public PlayingParameters createFromParcel(Parcel source) {
            return new PlayingParameters(source);
        }

        @Override
        public PlayingParameters[] newArray(int size) {
            return new PlayingParameters[size];
        }
    };

    public void initializePlayingParameters() {
        setAutoPlay(false);
        setMediaPrepared(false);
        setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);

        setCurrentVideoTrackIndexPlayed(0);

        setMusicAudioTrackIndex(1);
        setVocalAudioTrackIndex(1);
        setCurrentAudioTrackIndexPlayed(getMusicAudioTrackIndex());
        setMusicAudioChannel(CommonConstants.LeftChannel);     // default
        setVocalAudioChannel(CommonConstants.StereoChannel);   // default
        setCurrentChannelPlayed(getMusicAudioChannel());
        setCurrentAudioPosition(0);
        setCurrentVolume(1.0f);

        setCurrentSongIndex(-1);    // no playing

        setRepeatStatus(PlayerConstants.NoRepeatPlaying);    // no repeat playing songs
        setPlaySingleSong(false);    // default
        setInSongList(false);
    }
}
