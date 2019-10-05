package com.smile.karaokeplayer.Models;

import android.os.Parcel;
import android.os.Parcelable;

public class PlayingParameters implements Parcelable {

    private int currentPlaybackState;
    private boolean isAutoPlay;
    private boolean isMediaSourcePrepared;
    private int currentVideoRendererPlayed;
    private int musicAudioChannel;
    private int vocalAudioChannel;
    private int currentChannelPlayed;
    private int musicAudioRenderer;
    private int vocalAudioRenderer;
    private int currentAudioRendererPlayed;
    private long currentAudioPosition;
    private float currentVolume;
    private int publicSongIndex;
    private boolean isPlayingPublic;
    private int musicOrVocalOrNoSetting;
    private int repeatStatus;

    public PlayingParameters(int currentPlaybackState, boolean isAutoPlay, boolean isMediaSourcePrepared,
                             int currentVideoRendererPlayed, int musicAudioChannel, int vocalAudioChannel,
                             int currentChannelPlayed, int musicAudioRenderer, int vocalAudioRenderer,
                             int currentAudioRendererPlayed, long currentAudioPosition, float currentVolume,
                             int publicSongIndex, boolean isPlayingPublic, int musicOrVocalOrNoSetting,
                             int repeatStatus) {
        this.currentPlaybackState = currentPlaybackState;
        this.isAutoPlay = isAutoPlay;
        this.isMediaSourcePrepared = isMediaSourcePrepared;
        this.currentVideoRendererPlayed = currentVideoRendererPlayed;
        this.musicAudioChannel = musicAudioChannel;
        this.vocalAudioChannel = vocalAudioChannel;
        this.currentChannelPlayed = currentChannelPlayed;
        this.musicAudioRenderer = musicAudioRenderer;
        this.vocalAudioRenderer = vocalAudioRenderer;
        this.currentAudioRendererPlayed = currentAudioRendererPlayed;
        this.currentAudioPosition = currentAudioPosition;
        this.currentVolume = currentVolume;
        this.publicSongIndex = publicSongIndex;
        this.isPlayingPublic = isPlayingPublic;
        this.musicOrVocalOrNoSetting = musicOrVocalOrNoSetting;
        this.repeatStatus = repeatStatus;
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

    public boolean isMediaSourcePrepared() {
        return isMediaSourcePrepared;
    }

    public void setMediaSourcePrepared(boolean mediaSourcePrepared) {
        isMediaSourcePrepared = mediaSourcePrepared;
    }

    public int getCurrentVideoRendererPlayed() {
        return currentVideoRendererPlayed;
    }

    public void setCurrentVideoRendererPlayed(int currentVideoRendererPlayed) {
        this.currentVideoRendererPlayed = currentVideoRendererPlayed;
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

    public int getMusicAudioRenderer() {
        return musicAudioRenderer;
    }

    public void setMusicAudioRenderer(int musicAudioRenderer) {
        this.musicAudioRenderer = musicAudioRenderer;
    }

    public int getVocalAudioRenderer() {
        return vocalAudioRenderer;
    }

    public void setVocalAudioRenderer(int vocalAudioRenderer) {
        this.vocalAudioRenderer = vocalAudioRenderer;
    }

    public int getCurrentAudioRendererPlayed() {
        return currentAudioRendererPlayed;
    }

    public void setCurrentAudioRendererPlayed(int currentAudioRendererPlayed) {
        this.currentAudioRendererPlayed = currentAudioRendererPlayed;
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

    public int getPublicSongIndex() {
        return publicSongIndex;
    }

    public void setPublicSongIndex(int publicSongIndex) {
        this.publicSongIndex = publicSongIndex;
    }

    public boolean isPlayingPublic() {
        return isPlayingPublic;
    }

    public void setPlayingPublic(boolean playingPublic) {
        isPlayingPublic = playingPublic;
    }

    public int getMusicOrVocalOrNoSetting() {
        return musicOrVocalOrNoSetting;
    }

    public void setMusicOrVocalOrNoSetting(int musicOrVocalOrNoSetting) {
        this.musicOrVocalOrNoSetting = musicOrVocalOrNoSetting;
    }

    public int getRepeatStatus() {
        return repeatStatus;
    }

    public void setRepeatStatus(int repeatStatus) {
        this.repeatStatus = repeatStatus;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.currentPlaybackState);
        dest.writeByte(this.isAutoPlay ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isMediaSourcePrepared ? (byte) 1 : (byte) 0);
        dest.writeInt(this.currentVideoRendererPlayed);
        dest.writeInt(this.musicAudioChannel);
        dest.writeInt(this.vocalAudioChannel);
        dest.writeInt(this.currentChannelPlayed);
        dest.writeInt(this.musicAudioRenderer);
        dest.writeInt(this.vocalAudioRenderer);
        dest.writeInt(this.currentAudioRendererPlayed);
        dest.writeLong(this.currentAudioPosition);
        dest.writeFloat(this.currentVolume);
        dest.writeInt(this.publicSongIndex);
        dest.writeByte(this.isPlayingPublic ? (byte) 1 : (byte) 0);
        dest.writeInt(this.musicOrVocalOrNoSetting);
        dest.writeInt(this.repeatStatus);
    }

    protected PlayingParameters(Parcel in) {
        this.currentPlaybackState = in.readInt();
        this.isAutoPlay = in.readByte() != 0;
        this.isMediaSourcePrepared = in.readByte() != 0;
        this.currentVideoRendererPlayed = in.readInt();
        this.musicAudioChannel = in.readInt();
        this.vocalAudioChannel = in.readInt();
        this.currentChannelPlayed = in.readInt();
        this.musicAudioRenderer = in.readInt();
        this.vocalAudioRenderer = in.readInt();
        this.currentAudioRendererPlayed = in.readInt();
        this.currentAudioPosition = in.readLong();
        this.currentVolume = in.readFloat();
        this.publicSongIndex = in.readInt();
        this.isPlayingPublic = in.readByte() != 0;
        this.musicOrVocalOrNoSetting = in.readInt();
        this.repeatStatus = in.readInt();
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
}
