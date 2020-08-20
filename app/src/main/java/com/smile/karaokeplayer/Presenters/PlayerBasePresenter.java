package com.smile.karaokeplayer.Presenters;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.Models.SongListSQLite;
import com.smile.karaokeplayer.R;
import com.smile.karaokeplayer.Utilities.DatabaseAccessUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;
import java.util.ArrayList;

public abstract class PlayerBasePresenter {

    private static final String TAG = "PlayerBasePresenter";

    private final Context callingContext;
    private final BasePresentView presentView;
    private final Activity mActivity;

    protected final float textFontSize;
    protected final float fontScale;
    protected final float toastTextSize;
    protected MediaSessionCompat mediaSessionCompat;
    protected MediaControllerCompat.TransportControls mediaTransportControls;

    // instances of the following members have to be saved when configuration changed
    protected Uri mediaUri;
    protected int numberOfVideoTracks;
    protected int numberOfAudioTracks;
    protected ArrayList<SongInfo> publicSongList;
    protected PlayingParameters playingParam;
    protected boolean canShowNotSupportedFormat;
    protected SongInfo singleSongInfo;    // when playing single song in songs list

    public interface BasePresentView {
        void setImageButtonStatus();
        void playButtonOnPauseButtonOff();
        void playButtonOffPauseButtonOn();
        void setPlayingTimeTextView(String durationString);
        void update_Player_duration_seekbar(float duration);
        void update_Player_duration_seekbar_progress(int progress);
        void showNativeAndBannerAd();
        void hideNativeAndBannerAd();
        void showBufferingMessage();
        void dismissBufferingMessage();
        void buildAudioTrackMenuItem(int audioTrackNumber);
        void setTimerToHideSupportAndAudioController();
        void showMusicAndVocalIsNotSet();
        void showInterstitialAd(boolean isReturnToPrevious);
    }

    public PlayerBasePresenter(Context context, BasePresentView presentView) {
        Log.d(TAG, "PlayerBasePresenter() constructor is called.");
        this.callingContext = context;
        this.presentView = presentView;
        this.mActivity = (Activity)(this.presentView);

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(callingContext, ScreenUtil.FontSize_Pixel_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(callingContext, defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(callingContext, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        toastTextSize = 0.7f * textFontSize;
    }

    public float getTextFontSize() {
        return textFontSize;
    }
    public float getFontScale() {
        return fontScale;
    }
    public float getToastTextSize() {
        return toastTextSize;
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

    public BasePresentView getPresentView() {
        return presentView;
    }

    public void initializePlayingParam() {
        playingParam = new PlayingParameters();
        playingParam.initializePlayingParameters();
    }

    @SuppressWarnings("unchecked")
    public void initializeVariables(Bundle savedInstanceState, Intent callingIntent) {
        if (savedInstanceState == null) {
            numberOfVideoTracks = 0;
            numberOfAudioTracks = 0;
            mediaUri = null;
            initializePlayingParam();
            canShowNotSupportedFormat = false;
            playingParam.setPlaySingleSong(false);  // default
            singleSongInfo = null;    // default
            if (callingIntent != null) {
                Bundle arguments = callingIntent.getExtras();
                if (arguments != null) {
                    playingParam.setPlaySingleSong(arguments.getBoolean(PlayerConstants.IsPlaySingleSongState));
                    singleSongInfo = arguments.getParcelable(PlayerConstants.SongInfoState);
                }
            }
        } else {
            // needed to be set
            numberOfVideoTracks = savedInstanceState.getInt(PlayerConstants.NumberOfVideoTracksState,0);
            numberOfAudioTracks = savedInstanceState.getInt(PlayerConstants.NumberOfAudioTracksState);
            publicSongList = savedInstanceState.getParcelableArrayList(PlayerConstants.PublicSongListState);
            mediaUri = savedInstanceState.getParcelable(PlayerConstants.MediaUriState);
            playingParam = savedInstanceState.getParcelable(PlayerConstants.PlayingParamState);
            canShowNotSupportedFormat = savedInstanceState.getBoolean(PlayerConstants.CanShowNotSupportedFormatState);
            if (playingParam == null) {
                initializePlayingParam();
            }
            singleSongInfo = savedInstanceState.getParcelable(PlayerConstants.SongInfoState);
        }
    }

    public void onDurationSeekBarProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!isSeekable()) {
            return;
        }
        float positionTime = progress / 1000.0f;   // seconds
        int minutes = (int)(positionTime / 60.0f);    // minutes
        int seconds = (int)positionTime - (minutes * 60);
        String durationString = String.format("%3d:%02d", minutes, seconds);
        presentView.setPlayingTimeTextView(durationString);
        if (fromUser) {
            setPlayerTime(progress);
        }
        playingParam.setCurrentAudioPosition(progress);
    }

    public boolean isSeekable() {
        return true;
    }

    public abstract void setPlayerTime(int progress);
    public abstract void setAudioVolume(float volume);
    public abstract void setAudioVolumeInsideVolumeSeekBar(int i);
    public abstract int getCurrentProgressForVolumeSeekBar();

    public void playLeftChannel() {
        playingParam.setCurrentChannelPlayed(CommonConstants.LeftChannel);
        setAudioVolume(playingParam.getCurrentVolume());
    }

    public void playRightChannel() {
        playingParam.setCurrentChannelPlayed(CommonConstants.RightChannel);
        setAudioVolume(playingParam.getCurrentVolume());
    }

    public void playStereoChannel() {
        playingParam.setCurrentChannelPlayed(CommonConstants.StereoChannel);
        setAudioVolume(playingParam.getCurrentVolume());
    }

    public abstract void setAudioTrackAndChannel(int audioTrackIndex, int audioChannel);

    public void switchAudioToVocal() {
        if (!playingParam.isInSongList()) {
            // not in the database and show message
            presentView.showMusicAndVocalIsNotSet();
        }
        setAudioTrackAndChannel(playingParam.getVocalAudioTrackIndex(), playingParam.getVocalAudioChannel());
    }

    public void switchAudioToMusic() {
        if (!playingParam.isInSongList()) {
            // not in the database and show message
            presentView.showMusicAndVocalIsNotSet();
        }
        setAudioTrackAndChannel(playingParam.getMusicAudioTrackIndex(), playingParam.getMusicAudioChannel());
    }

    protected void playMediaFromUri(Uri uri) {
        // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
        // pass the saved instance of playingParam to
        // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)
        Bundle playingParamOriginExtras = new Bundle();
        playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
        if (mediaTransportControls != null) {
            mediaTransportControls.prepareFromUri(uri, playingParamOriginExtras);
        }
    }

    private void setPlayingParameters(SongInfo songInfo) {
        playingParam.setInSongList( (songInfo.getIncluded().equals("1") ? true : false));
        playingParam.setMusicAudioTrackIndex(songInfo.getMusicTrackNo());
        playingParam.setMusicAudioChannel(songInfo.getMusicChannel());
        playingParam.setVocalAudioTrackIndex(songInfo.getVocalTrackNo());
        playingParam.setVocalAudioChannel(songInfo.getVocalChannel());
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                ContentResolver contentResolver = callingContext.getContentResolver();
                for (UriPermission perm : contentResolver.getPersistedUriPermissions()) {
                    if (perm.getUri().equals(Uri.parse(filePath))) {
                        Log.d(TAG, "playSingleSong() has URI permission");
                        break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        mediaUri = getValidatedUri(Uri.parse(filePath));

        Log.i(TAG, "mediaUri = " + mediaUri);
        if ((mediaUri == null) || (Uri.EMPTY.equals(mediaUri))) {
            return;
        }

        setPlayingParameters(songInfo);

        playingParam.setCurrentVideoTrackIndexPlayed(0);
        playingParam.setCurrentAudioTrackIndexPlayed(playingParam.getVocalAudioTrackIndex());
        playingParam.setCurrentChannelPlayed(playingParam.getVocalAudioChannel());
        playingParam.setCurrentAudioPosition(0);
        playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);
        playingParam.setMediaSourcePrepared(false);

        playMediaFromUri(mediaUri);
    }

    public void startAutoPlay() {
        if (mActivity.isFinishing()) {
            // activity is being destroyed
            return;
        }

        int publicSongListSize = 0;
        if (publicSongList != null) {
            publicSongListSize = publicSongList.size();
        }
        if (publicSongListSize == 0) {
            return;
        }

        boolean stillPlayNext = true;
        int repeatStatus = playingParam.getRepeatStatus();
        int publicNextSongIndex = playingParam.getPublicNextSongIndex();
        switch (repeatStatus) {
            case PlayerConstants.NoRepeatPlaying:
                // no repeat
                if ( (publicNextSongIndex >= publicSongListSize) || (publicNextSongIndex<0) ) {
                    // stop playing
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
                if (publicNextSongIndex >= publicSongListSize) {
                    publicNextSongIndex = 0;
                }
                break;
        }

        if (stillPlayNext) {    // still play the next song
            SongInfo songInfo = publicSongList.get(publicNextSongIndex);
            playSingleSong(songInfo);
            publicNextSongIndex++;  // set next index of playlist that will be played
            playingParam.setPublicNextSongIndex(publicNextSongIndex);
        }
        Log.d(TAG, "Repeat status = " + repeatStatus);
        Log.d(TAG, "startAutoPlay() finished --> " + publicNextSongIndex--);

        presentView.setImageButtonStatus();
    }

    public void setAutoPlayStatusAndAction() {
        boolean isAutoPlay = playingParam.isAutoPlay();
        if (!isAutoPlay) {
            // previous is not auto play
            ArrayList<SongInfo> songListTemp = DatabaseAccessUtil.readPublicSongList(callingContext);
            if (songListTemp.size() > 0) {
                publicSongList = new ArrayList<>(songListTemp);
                playingParam.setAutoPlay(true); // must be above autoPlayPublicSongList()
                autoPlaySongList();
            } else {
                ScreenUtil.showToast(callingContext, callingContext.getString(R.string.noPlaylistString)
                        , toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
            }
        } else {
            // previous is auto play
            int playbackState = playingParam.getCurrentPlaybackState();
            if (playbackState!=PlaybackStateCompat.STATE_NONE
                    && playbackState!=PlaybackStateCompat.STATE_STOPPED) {
                // not the following: (has not started, stopped, or finished)
                stopPlay();
            }
            playingParam.setAutoPlay(false);    // must be the last in this block
        }

        presentView.setImageButtonStatus();
    }

    private void autoPlaySongList() {
        canShowNotSupportedFormat = true;
        if ( (publicSongList != null) && (publicSongList.size() > 0) ) {
            // playingParam.setAutoPlay(true);
            playingParam.setPublicNextSongIndex(0); // next song that will be played
            // start playing video from list
            startAutoPlay();
        }
        /*
        // no need
        else {
            playingParam.setAutoPlay(false);
            ScreenUtil.showToast(callingContext, callingContext.getString(R.string.noPlaylistString)
                    , toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
        }
        */
    }

    public void playPreviousSong() {
        // if ( publicSongList==null || !playingParam.isAutoPlay() || playingParam.isPlaySingleSong()) {
        if ( publicSongList==null || playingParam.isPlaySingleSong()) {
            return;
        }
        int publicSongListSize = publicSongList.size();
        if (publicSongListSize <= 1 ) {
            // only file in the play list
            ScreenUtil.showToast(callingContext, callingContext.getString(R.string.noPreviousSongString)
                    , toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
            return;
        }
        int nextIndex = playingParam.getPublicNextSongIndex();
        int repeatStatus = playingParam.getRepeatStatus();
        nextIndex = nextIndex - 2;
        switch (repeatStatus) {
            case PlayerConstants.RepeatOneSong:
                // because in startAutoPlay() will subtract 1 from next index
                nextIndex++;
                if (nextIndex == 0) {
                    // go to last song
                    nextIndex = publicSongListSize;
                }
                break;
            case PlayerConstants.RepeatAllSongs:
                if (nextIndex < 0) {
                    // is going to play the last one
                    nextIndex = publicSongListSize - 1; // the last one
                }
                break;
            case PlayerConstants.NoRepeatPlaying:
                if (nextIndex < 0) {
                    ScreenUtil.showToast(callingContext, callingContext.getString(R.string.noPreviousSongString)
                            , toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                    return;
                }
                break;
        }
        playingParam.setPublicNextSongIndex(nextIndex);

        startAutoPlay();
    }

    public void playNextSong() {
        // if ( publicSongList==null || !playingParam.isAutoPlay() || playingParam.isPlaySingleSong()) {
        if (publicSongList == null || playingParam.isPlaySingleSong()) {
            return;
        }
        int publicSongListSize = publicSongList.size();
        if (publicSongListSize <= 1 ) {
            // only file in the play list
            ScreenUtil.showToast(callingContext, callingContext.getString(R.string.noNextSongString)
                    , toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
            return;
        }
        int nextIndex = playingParam.getPublicNextSongIndex();
        int repeatStatus = playingParam.getRepeatStatus();
        switch (repeatStatus) {
            case PlayerConstants.NoRepeatPlaying:
                if (nextIndex >= publicSongListSize) {
                    ScreenUtil.showToast(callingContext, callingContext.getString(R.string.noNextSongString)
                            , toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                    return; // no more next
                }
                break;
            case PlayerConstants.RepeatOneSong:
                nextIndex++;
                break;
            case PlayerConstants.RepeatAllSongs:
                break;
        }
        if (nextIndex > publicSongListSize) {
            // it is playing the last one right now
            // so it is going to play the first one
            nextIndex = 0;
        }
        playingParam.setPublicNextSongIndex(nextIndex);

        startAutoPlay();
    }

    public Uri getValidatedUri(Uri tempUri) {
        return tempUri;
    }

    public void playSelectedUrisFromStorage(ArrayList<Uri> tempUriList) {

        if (tempUriList==null || tempUriList.size()==0) {
            return;
        }

        // clear publicSongList but publicSongList might be null
        ArrayList<SongInfo> songListTemp = new ArrayList<>();
        for (Uri tempUri : tempUriList) {
            // searching song list for the information of tempUri
            SongInfo songInfo = null;
            SongListSQLite songListSQLite = new SongListSQLite(callingContext);
            if (songListSQLite != null) {
                songInfo = songListSQLite.findOneSongByUriString(tempUri.toString()); // use the original Uri
                songListSQLite.closeDatabase();
            }
            if (songInfo != null) {
                Log.d(TAG, "Found this song on song list.");
                songInfo.setIncluded("1");  // set to in the list
            } else {
                Log.d(TAG, "Could not find this song on song list.");
                songInfo = new SongInfo();
                songInfo.setSongName("");
                // has to be tempUri not mediaUri
                songInfo.setFilePath(tempUri.toString());
                int currentAudioTrack = 1;
                songInfo.setMusicTrackNo(currentAudioTrack);
                songInfo.setMusicChannel(CommonConstants.LeftChannel);
                songInfo.setVocalTrackNo(currentAudioTrack);
                songInfo.setVocalChannel(CommonConstants.RightChannel);
                // not in the list and unknown music and vocal setting
                songInfo.setIncluded("0");  // set to not in the list
            }
            songListTemp.add(songInfo);
        }

        if (songListTemp.size() > 0) {
            publicSongList = new ArrayList<>(songListTemp);
            playingParam.setAutoPlay(false);
            autoPlaySongList();
        }  else {
            playingParam.setAutoPlay(false);
            ScreenUtil.showToast(callingContext, callingContext.getString(R.string.noFilesSelectedString)
                    , toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
        }
    }

    public void playTheSongThatWasPlayedBeforeActivityCreated() {
        if (mediaUri==null || Uri.EMPTY.equals(mediaUri)) {
            if (playingParam.isPlaySingleSong()) {
                if (singleSongInfo == null) {
                    Log.d(TAG, "singleSongInfo is null");
                } else {
                    Log.d(TAG, "singleSongInfo is not null");
                    playingParam.setAutoPlay(false);
                    playSingleSong(singleSongInfo);
                }
            }
        } else {
            int playbackState = playingParam.getCurrentPlaybackState();
            Log.d(TAG, "onActivityCreated() --> playingParam.getCurrentPlaybackState() = " + playbackState);
            if (playbackState != PlaybackStateCompat.STATE_NONE) {
                playMediaFromUri(mediaUri);
            }
        }
    }

    public void setRepeatSongStatus() {
        int repeatStatus = playingParam.getRepeatStatus();
        switch (repeatStatus) {
            case PlayerConstants.NoRepeatPlaying:
                // switch to repeat one song
                playingParam.setRepeatStatus(PlayerConstants.RepeatOneSong);
                break;
            case PlayerConstants.RepeatOneSong:
                // switch to repeat song list
                playingParam.setRepeatStatus(PlayerConstants.RepeatAllSongs);
                break;
            case PlayerConstants.RepeatAllSongs:
                // switch to no repeat
                playingParam.setRepeatStatus(PlayerConstants.NoRepeatPlaying);
                break;
        }
        presentView.setImageButtonStatus();
    }

    public void startPlay() {
        Log.d(TAG, "startPlay() is called.");

        int playbackState = playingParam.getCurrentPlaybackState();
        if (playbackState==PlaybackStateCompat.STATE_NONE
            || playbackState==PlaybackStateCompat.STATE_STOPPED) {
            // start playing the first song in the list
            autoPlaySongList();
            return;
        }

        if ( (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playbackState != PlaybackStateCompat.STATE_PLAYING) ) {
            // no media file opened or playing has been stopped
            if ( (playbackState == PlaybackStateCompat.STATE_PAUSED)
                    || (playbackState == PlaybackStateCompat.STATE_REWINDING)
                    || (playbackState == PlaybackStateCompat.STATE_FAST_FORWARDING) ) {
                if (mediaTransportControls != null) {
                    mediaTransportControls.play();
                }
                Log.d(TAG, "startPlay() --> mediaTransportControls.play() is called.");
            } else {
                // (playbackState == PlaybackStateCompat.STATE_STOPPED) or
                // (playbackState == PlaybackStateCompat.STATE_NONE)
                replayMedia();
                Log.d(TAG, "startPlay() --> replayMedia() is called.");
            }
        }
    }

    public void pausePlay() {
        if ( (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) ) {
            // no media file opened or playing has been stopped
            if (mediaTransportControls != null) {
                mediaTransportControls.pause();
                presentView.showNativeAndBannerAd();
            }
        }
    }

    public void stopPlay() {
        Log.d(TAG, "stopPlay() is called.");
        if ((mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_NONE)) {
            // media file opened or playing has been stopped
            if (mediaTransportControls != null) {
                Log.d(TAG, "stopPlay() ---> mediaTransportControls.stop() is called.");
                mediaTransportControls.stop();
                if (playingParam.isPlaySingleSong()) {
                    presentView.showInterstitialAd(true);
                } else {
                    presentView.showInterstitialAd(false);
                }
            }
        }
        // removed the following on 2020-08-16 because app wants to keep the status
        // playingParam.setAutoPlay(false);    // no auto playing song list
    }

    protected abstract void specificPlayerReplayMedia(long currentAudioPosition);

    public void replayMedia() {
        if ( (mediaUri == null) || (Uri.EMPTY.equals(mediaUri)) || (numberOfAudioTracks<=0) ) {
            return;
        }

        long currentAudioPosition = 0;
        playingParam.setCurrentAudioPosition(currentAudioPosition);
        if (playingParam.isMediaSourcePrepared()) {
            // song is playing, paused, or finished playing
            // cannot do the following statement (exoPlayer.setPlayWhenReady(false); )
            // because it will send Play.STATE_ENDED event after the playing has finished
            // but the playing was stopped in the middle of playing then wo'nt send
            // Play.STATE_ENDED event
            // exoPlayer.setPlayWhenReady(false);
            specificPlayerReplayMedia(currentAudioPosition);
        } else {
            // song was stopped by user
            // mediaTransportControls.prepare();   // prepare and play
            // Log.d(TAG, "replayMedia()--> mediaTransportControls.prepare().");
            playMediaFromUri(mediaUri);
        }

        Log.d(TAG, "replayMedia() is called.");
    }

    public void initMediaSessionCompat() {
        // Create a MediaSessionCompat
        mediaSessionCompat = new MediaSessionCompat(callingContext, PlayerConstants.LOG_TAG);
        // Do not let MediaButtons restart the player when the app is not visible
        mediaSessionCompat.setMediaButtonReceiver(null);
        mediaSessionCompat.setActive(true); // might need to find better place to put
    }

    public void releaseMediaSessionCompat() {
        if (mediaSessionCompat != null) {
            mediaSessionCompat.setActive(false);
            mediaSessionCompat.release();
            mediaSessionCompat = null;
        }
        mediaTransportControls = null;
    }

    public MediaControllerCompat.TransportControls getMediaTransportControls() {
        return mediaTransportControls;
    }

    public void saveInstanceState(@NonNull Bundle outState) {
        outState.putInt(PlayerConstants.NumberOfVideoTracksState, numberOfVideoTracks);
        outState.putInt(PlayerConstants.NumberOfAudioTracksState, numberOfAudioTracks);
        outState.putParcelableArrayList(PlayerConstants.PublicSongListState, publicSongList);
        outState.putParcelable(PlayerConstants.MediaUriState, mediaUri);
        outState.putParcelable(PlayerConstants.PlayingParamState, playingParam);
        outState.putBoolean(PlayerConstants.CanShowNotSupportedFormatState, canShowNotSupportedFormat);
        outState.putParcelable(PlayerConstants.SongInfoState, singleSongInfo);
    }
}
