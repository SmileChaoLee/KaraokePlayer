package com.smile.karaokeplayer.Models;

import android.os.Parcel;
import android.os.Parcelable;

public class SongInfo implements Parcelable {
    private String songNo;
    private String songName;
    private String path;
    private String fileName;
    private int musicTrackNo;
    private int vocalTrackNo;
    private int musicChannel;
    private int vocalChannel;

    public SongInfo(String songNo, String songName, String path, String fileName, int musicTrackNo, int vocalTrackNo, int musicChannel, int vocalChannel) {
        this.songNo = songNo;
        this.songName = songName;
        this.path = path;
        this.fileName = fileName;
        this.musicTrackNo = musicTrackNo;
        this.vocalTrackNo = vocalTrackNo;
        this.musicChannel = musicChannel;
        this.vocalChannel = vocalChannel;
    }

    public String getSongNo() {
        return songNo;
    }

    public void setSongNo(String songNo) {
        this.songNo = songNo;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getMusicTrackNo() {
        return musicTrackNo;
    }

    public void setMusicTrackNo(int musicTrackNo) {
        this.musicTrackNo = musicTrackNo;
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

    public void setMusicChannel(int musicChannel) {
        this.musicChannel = musicChannel;
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
                "songNo='" + songNo + '\'' +
                ", songName='" + songName + '\'' +
                ", path='" + path + '\'' +
                ", fileName='" + fileName + '\'' +
                ", musicTrackNo=" + musicTrackNo +
                ", vocalTrackNo=" + vocalTrackNo +
                ", musicChannel=" + musicChannel +
                ", vocalChannel=" + vocalChannel +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.songNo);
        dest.writeString(this.songName);
        dest.writeString(this.path);
        dest.writeString(this.fileName);
        dest.writeInt(this.musicTrackNo);
        dest.writeInt(this.vocalTrackNo);
        dest.writeInt(this.musicChannel);
        dest.writeInt(this.vocalChannel);
    }

    protected SongInfo(Parcel in) {
        this.songNo = in.readString();
        this.songName = in.readString();
        this.path = in.readString();
        this.fileName = in.readString();
        this.musicTrackNo = in.readInt();
        this.vocalTrackNo = in.readInt();
        this.musicChannel = in.readInt();
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
