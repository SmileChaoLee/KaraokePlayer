package com.smile.karaokeplayer.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.smile.karaokeplayer.constants.CommonConstants;

public class SongInfoOld implements Parcelable {
    private int id;
    private String songName;
    private String filePath;
    private int musicTrackNo;
    private int musicChannel;
    private int vocalTrackNo;
    private int vocalChannel;
    private String included;   // is included in playlist

    public SongInfoOld() {
        songName = "";
        filePath = "";
        musicTrackNo = 1;
        musicChannel = CommonConstants.RightChannel;
        vocalTrackNo = 1;
        vocalChannel = CommonConstants.LeftChannel;
        included = "1"; // default is included in playlist
    }

    public SongInfoOld(SongInfoOld songInfo) {
        id = songInfo.id;
        songName = songInfo.songName;
        filePath = songInfo.filePath;
        musicTrackNo = songInfo.musicTrackNo;
        musicChannel = songInfo.musicChannel;
        vocalTrackNo = songInfo.vocalTrackNo;
        vocalChannel = songInfo.vocalChannel;
        included = songInfo.included;
    }

    public SongInfoOld(int id, String songName, String filePath, int musicTrackNo, int musicChannel, int vocalTrackNo, int vocalChannel, String included) {
        this.id = id;
        this.songName = songName;
        this.filePath = filePath;
        this.musicTrackNo = musicTrackNo;
        this.musicChannel = musicChannel;
        this.vocalTrackNo = vocalTrackNo;
        this.vocalChannel = vocalChannel;
        this.included = included;
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

    public String getIncluded() {
        return included;
    }

    public void setIncluded(String included) {
        this.included = included;
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
                ", included=" + included +
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
        dest.writeString(this.included);
    }

    protected SongInfoOld(Parcel in) {
        this.id = in.readInt();
        this.songName = in.readString();
        this.filePath = in.readString();
        this.musicTrackNo = in.readInt();
        this.musicChannel = in.readInt();
        this.vocalTrackNo = in.readInt();
        this.vocalChannel = in.readInt();
        this.included = in.readString();
    }

    public static final Creator<SongInfoOld> CREATOR = new Creator<SongInfoOld>() {
        @Override
        public SongInfoOld createFromParcel(Parcel source) {
            return new SongInfoOld(source);
        }

        @Override
        public SongInfoOld[] newArray(int size) {
            return new SongInfoOld[size];
        }
    };
}
