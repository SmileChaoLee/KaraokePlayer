package com.smile.karaokeplayer.Constants;

public final class PlayerConstants {
    private PlayerConstants() {
    }

    public static final String LOG_TAG = "MediaSessionCompatTag";
    public static final String PlayingParamOrigin = "PlayingParamOrigin";
    public static final String PlayingParamState = "PlayingParam";
    public static final String TrackSelectorParametersState = "TrackSelectorParameters";
    public static final String NumberOfVideoTracksState = "NumberOfVideoTracks";
    public static final String NumberOfAudioTracksState = "NumberOfAudioTracks";
    public static final String OrderedSongListState = "OrderedSongList";
    public static final String MediaUriState = "MediaUri";
    public static final String CanShowNotSupportedFormatState = "CanShowNotSupportedFormat";
    public static final String VideoTrackIndicesListState = "VideoTrackIndicesList";
    public static final String AudioTrackIndicesListState = "AudioTrackIndicesList";

    public static final int PrivacyPolicyActivityRequestCode = 10;
    public static final int FILE_READ_REQUEST_CODE = 1;
    public static final int SONG_LIST_ACTIVITY_CODE = 2;
    public static final int PlayerView_Timeout = 10000;  //  10 seconds
    public static final int NoVideoTrack = -1;
    public static final int NoAudioTrack = -1;
    public static final int NoAudioChannel = -1;
    public static final int MaxProgress = 100;
    public static final int PlayingMusic = 1;
    public static final int PlayingVocal = 2;
    public static final int NoRepeatPlaying = 0;
    public static final int RepeatOneSong = 1;
    public static final int RepeatAllSongs = 2;

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String IsPlaySingleSongState = "IsPlaySingleSong";
    public static final String SongInfoState = "SingleSongInfo";
    public static final String PlayerBaseActivityIntent = "PlayerBaseActivityIntent";
}
