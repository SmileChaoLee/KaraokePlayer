package com.smile.karaokeplayer.constants

object PlayerConstants {
    const val LOG_TAG = "MediaSessionCompatTag"
    const val PlayingParamOrigin = "PlayingParamOrigin"
    const val PlayingParamState = "PlayingParam"
    const val TrackSelectorParametersState = "TrackSelectorParameters"
    const val NumberOfVideoTracksState = "NumberOfVideoTracks"
    const val NumberOfAudioTracksState = "NumberOfAudioTracks"
    const val OrderedSongListState = "OrderedSongList"
    const val MediaUriState = "MediaUri"
    const val CanShowNotSupportedFormatState = "CanShowNotSupportedFormat"
    const val AudioTrackIndicesListState = "AudioTrackIndicesList"
    const val PrivacyPolicyActivityRequestCode = 10
    const val PlayerView_Timeout = 10000 //  10 seconds
    const val NoAudioTrack = -1
    const val NoAudioChannel = -1
    const val MaxProgress = 100
    const val NoRepeatPlaying = 0 // Player.REPEAT_MODE_OFF
    const val RepeatOneSong = 1 // Player.REPEAT_MODE_ONE
    const val RepeatAllSongs = 2 // Player.REPEAT_MODE_ALL

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    const val IsPlaySingleSongState = "IsPlaySingleSong"
    const val SongInfoState = "SingleSongInfo"

    const val Uri_List: String = "Uri_List"
}