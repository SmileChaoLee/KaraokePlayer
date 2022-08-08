package com.smile.karaokeplayer.models

import android.content.res.Configuration
import android.os.Parcelable
import android.support.v4.media.session.PlaybackStateCompat
import com.smile.karaokeplayer.constants.CommonConstants
import com.smile.karaokeplayer.constants.PlayerConstants
import kotlinx.parcelize.Parcelize

@Parcelize
class PlayingParameters (
        var currentPlaybackState: Int, var isAutoPlay: Boolean, var isMediaPrepared: Boolean,
        var isPlaySingleSong: Boolean, var isInSongList: Boolean,
        var musicAudioTrackIndex: Int, var vocalAudioTrackIndex: Int,
        var musicAudioChannel: Int, var vocalAudioChannel: Int,
        var currentAudioTrackIndexPlayed: Int, var currentChannelPlayed: Int,
        var currentAudioPosition: Long, var currentVolume: Float, var currentSongIndex: Int,
        var repeatStatus: Int, var orientationStatus: Int) : Parcelable {

        constructor() : this(PlaybackStateCompat.STATE_NONE, false,
                false, false, false,
                1, 1, CommonConstants.LeftChannel,
                CommonConstants.RightChannel, 1, CommonConstants.LeftChannel,
                0, 1.0f, -1,
                PlayerConstants.NoRepeatPlaying, Configuration.ORIENTATION_PORTRAIT)
        /*
        fun initializePlayingParameters() {
                currentPlaybackState = PlaybackStateCompat.STATE_NONE
                isAutoPlay = false
                isMediaPrepared = false
                isPlaySingleSong = false // default
                isInSongList = false
                musicAudioTrackIndex = 1
                vocalAudioTrackIndex = 1
                musicAudioChannel = CommonConstants.LeftChannel // default
                vocalAudioChannel = CommonConstants.RightChannel // default
                currentAudioTrackIndexPlayed = musicAudioTrackIndex
                currentChannelPlayed = musicAudioChannel
                currentAudioPosition = 0
                currentVolume = 1.0f
                currentSongIndex = -1 // no playing
                repeatStatus = PlayerConstants.NoRepeatPlaying // no repeat playing songs
                orientationStatus = Configuration.ORIENTATION_PORTRAIT
        }
        */
}
