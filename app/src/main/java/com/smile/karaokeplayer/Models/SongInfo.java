package com.smile.karaokeplayer.Models;

import android.os.Parcel;
import android.os.Parcelable;

public class SongInfo implements Parcelable {
    private int id;
    private String songName;
    private String filePath;
    private int musicTrackNo;
    private int musicChannel;
    private int vocalTrackNo;
    private int vocalChannel;

    public SongInfo(int id, String songName, String filePath, int musicTrackNo, int musicChannel, int vocalTrackNo, int vocalChannel) {
        this.id = id;
        this.songName = songName;
        this.filePath = filePath;
        this.musicTrackNo = musicTrackNo;
        this.musicChannel = musicChannel;
        this.vocalTrackNo = vocalTrackNo;
        this.vocalChannel = vocalChannel;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getMusicTrackNo() {
        return musicTrackNo;
    }

    public void setMusicTrackNo(int musicTrackNo) {
        this.musicTrackNo = musicTrackNo;
    }

    public void setMusicChannel(int musicChannel) {
        this.musicChannel = musicChannel;
    }

    public int getVocalTrackNo() {
        return vocalTrackNo;
    }

    public void setVocalTrackNo(int vocalTrackNo) {
        this.vocalTrackNo = vocalTrackNo;
    }

    public int getMusicChannel() {
        return musicChannel;
    }

    public int getVocalChannel() {
        return vocalChannel;
    }

    public void setVocalChannel(int vocalChannel) {
        this.vocalChannel = vocalChannel;
    }

    @Override
    public String toString() {
        return "SongInfo{" +
                "id='" + id + '\'' +
                ", songName='" + songName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", musicTrackNo=" + musicTrackNo +
                ", musicChannel=" + musicChannel +
                ", vocalTrackNo=" + vocalTrackNo +
                ", vocalChannel=" + vocalChannel +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.songName);
        dest.writeString(this.filePath);
        dest.writeInt(this.musicTrackNo);
        dest.writeInt(this.musicChannel);
        dest.writeInt(this.vocalTrackNo);
        dest.writeInt(this.vocalChannel);
    }

    protected SongInfo(Parcel in) {
        this.id = in.readInt();
        this.songName = in.readString();
        this.filePath = in.readString();
        this.musicTrackNo = in.readInt();
        this.musicChannel = in.readInt();
        this.vocalTrackNo = in.readInt();
        this.vocalChannel = in.readInt();
    }

    public static final Creator<SongInfo> CREATOR = new Creator<SongInfo>() {
        @Override
        public SongInfo createFromParcel(Parcel source) {
            return new SongInfo(source);
        }

        @Override
        public SongInfo[] newArray(int size) {
            return new SongInfo[size];
        }
    };
}
