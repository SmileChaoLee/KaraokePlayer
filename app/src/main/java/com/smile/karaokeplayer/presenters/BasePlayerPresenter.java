package com.smile.karaokeplayer.presenters;

import android.app.Activity;
import android.content.ContentResolver;
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

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.smile.karaokeplayer.constants.CommonConstants;
import com.smile.karaokeplayer.constants.PlayerConstants;
import com.smile.karaokeplayer.models.PlayingParameters;
import com.smile.karaokeplayer.models.SongInfo;
import com.smile.karaokeplayer.models.SongListSQLite;
import com.smile.karaokeplayer.R;
import com.smile.karaokeplayer.utilities.DatabaseAccessUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.ArrayList;
import java.util.Locale;

public abstract class BasePlayerPresenter {

    private static final String TAG = "BasePlayerPresenter";
    private final Activity mActivity;
    private final BasePresentView mPresentView;
    protected final float textFontSize;
    protected final float fontScale;
    protected final float toastTextSize;
    protected MediaSessionCompat mediaSessionCompat;
    protected MediaControllerCompat.TransportControls mediaTransportControls;
    // instances of the following members have to be saved when configuration changed
    protected Uri mediaUri;
    protected int numberOfVideoTracks;
    protected int numberOfAudioTracks;
    protected ArrayList<SongInfo> orderedSongList;
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
        void showInterstitialAd(boolean isExit);
        void setScreenOrientation(int orientation);
        void hidePlayerView();
        void showPlayerView();
    }

    public abstract void setPlayerTime(int progress);
    public abstract void setAudioVolume(float volume);
    public abstract void setAudioVolumeInsideVolumeSeekBar(int i);
    public abstract int getCurrentProgressForVolumeSeekBar();
    public abstract void setAudioTrackAndChannel(int audioTrackIndex, int audioChannel);
    public abstract void specificPlayerReplayMedia(long currentAudioPosition);
    public abstract void switchAudioToMusic();
    public abstract void switchAudioToVocal();
    public abstract void startDurationSeekBarHandler();
    public abstract long getMediaDuration();
    public abstract void removeCallbacksAndMessages();
    public abstract void getPlayingMediaInfoAndSetAudioActionSubMenu();
    public abstract boolean isSeekable();

    public BasePlayerPresenter(Fragment fragment, BasePresentView presentView) {
        Log.d(TAG, "PlayerBasePresenter() constructor is called.");
        mActivity = fragment.getActivity();
        mPresentView = presentView;

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(mActivity, ScreenUtil.FontSize_Pixel_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(mActivity, defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(mActivity, ScreenUtil.FontSize_Pixel_Type, 0.0f);
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

    public Uri getMediaUri() {
        return mediaUri;
    }
    public void setMediaUri(Uri mediaUri) {
        this.mediaUri = mediaUri;
    }

    public int getNumberOfAudioTracks() {
        return numberOfAudioTracks;
    }

    public int getNumberOfVideoTracks() {
        return numberOfVideoTracks;
    }

    public boolean isCanShowNotSupportedFormat() {
        return canShowNotSupportedFormat;
    }
    public void setCanShowNotSupportedFormat(boolean canShowNotSupportedFormat) {
        this.canShowNotSupportedFormat = canShowNotSupportedFormat;
    }

    public PlayingParameters getPlayingParam() {
        return playingParam;
    }

    private void setPlayingParameters(SongInfo songInfo) {
        playingParam.setInSongList(songInfo.getIncluded().equals("1"));
        playingParam.setMusicAudioTrackIndex(songInfo.getMusicTrackNo());
        playingParam.setMusicAudioChannel(songInfo.getMusicChannel());
        playingParam.setVocalAudioTrackIndex(songInfo.getVocalTrackNo());
        playingParam.setVocalAudioChannel(songInfo.getVocalChannel());
        // Log.d(TAG, "startAutoPlay.songInfo = " + songInfo);
        // Log.d(TAG, "startAutoPlay.singleSongInfo = " + singleSongInfo);
        // songInfo.equals(singleSongInfo) = true, then use original audio track and channel
        Log.d(TAG, "startAutoPlay.songInfo == singleSongInfo) = " + (songInfo == singleSongInfo));
        if (songInfo != singleSongInfo) {
            playingParam.setCurrentAudioTrackIndexPlayed(songInfo.getVocalTrackNo());
            playingParam.setCurrentChannelPlayed(songInfo.getVocalChannel());
            singleSongInfo = songInfo;
        }
    }

    private void autoPlaySongList() {
        Log.d(TAG, "autoPlaySongList");
        canShowNotSupportedFormat = true;
        if ( (orderedSongList != null) && (orderedSongList.size() > 0) ) {
            // playingParam.setAutoPlay(true);
            playingParam.setCurrentSongIndex(-1); // next song that will be played, which the index is 0
            // start playing video from list
            startAutoPlay(false);
        } else {
            if (orderedSongList == null) Log.d(TAG, "autoPlaySongList.orderedSongList is null");
            else Log.d(TAG, "autoPlaySongList.orderedSongList.size() = " + orderedSongList.size());
        }
    }

    public void playSelectedUrisFromStorage(ArrayList<Uri> tempUriList) {

        if (tempUriList==null || tempUriList.size()==0) {
            return;
        }

        // clear orderedSongList but orderedSongList might be null
        ArrayList<SongInfo> songList = new ArrayList<>();
        for (Uri tempUri : tempUriList) {
            // searching song list for the information of tempUri
            SongListSQLite songListSQLite = new SongListSQLite(mActivity);
            SongInfo songInfo = songListSQLite.findOneSongByUriString(tempUri.toString()); // use the original Uri
            songListSQLite.closeDatabase();
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
            songList.add(songInfo);
        }
        playSongList(songList);
        playingParam.setAutoPlay(false);
        mPresentView.showPlayerView();
    }

    protected void playMediaFromUri(Uri uri) {
        Log.d(TAG, "playMediaFromUri.uri = " + uri);
        // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
        // pass the saved instance of playingParam to
        // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)
        Bundle playingParamOriginExtras = new Bundle();
        playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
        if (mediaTransportControls != null) {
            Log.d(TAG, "playMediaFromUri.mediaTransportControls is not null");
            mediaTransportControls.prepareFromUri(uri, playingParamOriginExtras);
        } else {
            Log.w(TAG, "playMediaFromUri.mediaTransportControls is null");
        }
    }

    public void initializePlayingParam() {
        playingParam = new PlayingParameters();
    }

    @SuppressWarnings("unchecked")
    public void initializeVariables(Bundle savedInstanceState, Intent callingIntent) {
        Log.d(TAG, "initializeVariables()");
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {    // API 33
                        singleSongInfo = arguments.getParcelable(PlayerConstants.SongInfoState, SongInfo.class);
                    } else singleSongInfo = arguments.getParcelable(PlayerConstants.SongInfoState);
                    Log.d(TAG, "initializeVariables.singleSongInfo = " + singleSongInfo);
                }
            }
            playingParam.setOrientationStatus(mActivity.getResources().getConfiguration().orientation);
        } else {
            // needed to be set
            numberOfVideoTracks = savedInstanceState.getInt(PlayerConstants.NumberOfVideoTracksState,0);
            numberOfAudioTracks = savedInstanceState.getInt(PlayerConstants.NumberOfAudioTracksState);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                orderedSongList = (ArrayList<SongInfo>)savedInstanceState.getSerializable(PlayerConstants.OrderedSongListState, ArrayList.class);
            } else orderedSongList = (ArrayList<SongInfo>)savedInstanceState.getSerializable(PlayerConstants.OrderedSongListState);
            Log.d(TAG, "initializeVariables.orderedSongList = " + orderedSongList);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mediaUri = savedInstanceState.getParcelable(PlayerConstants.MediaUriState,Uri.class);
            } else mediaUri = savedInstanceState.getParcelable(PlayerConstants.MediaUriState);
            if (Build.VERSION.SDK_INT >= 33) {
                playingParam = savedInstanceState.getParcelable(PlayerConstants.PlayingParamState, PlayingParameters.class);
            } else playingParam = savedInstanceState.getParcelable(PlayerConstants.PlayingParamState);
            Log.d(TAG, "initializeVariables.playingParam = " + playingParam);
            canShowNotSupportedFormat = savedInstanceState.getBoolean(PlayerConstants.CanShowNotSupportedFormatState);
            if (playingParam == null) {
                initializePlayingParam();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                singleSongInfo = savedInstanceState.getParcelable(PlayerConstants.SongInfoState, SongInfo.class);
            } else singleSongInfo = savedInstanceState.getParcelable(PlayerConstants.SongInfoState);
            Log.d(TAG, "initializeVariables.singleSongInfo = " + singleSongInfo);
        }
        setOrientationStatus(playingParam.getOrientationStatus());
    }

    public void onDurationSeekBarProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!isSeekable()) {
            return;
        }
        float positionTime = progress / 1000.0f;   // seconds
        int minutes = (int)(positionTime / 60.0f);    // minutes
        int seconds = (int)positionTime - (minutes * 60);
        String durationString = String.format(Locale.ENGLISH, "%3d:%02d", minutes, seconds);
        mPresentView.setPlayingTimeTextView(durationString);
        if (fromUser) {
            setPlayerTime(progress);
        }
        playingParam.setCurrentAudioPosition(progress);
    }

    public void playLeftChannel() {
        Log.d(TAG, "playLeftChannel() --> CommonConstants.LeftChannel = " + CommonConstants.LeftChannel);
        playingParam.setCurrentChannelPlayed(CommonConstants.LeftChannel);
        setAudioVolume(playingParam.getCurrentVolume());
    }

    public void playRightChannel() {
        Log.d(TAG, "playRightChannel() --> CommonConstants.RightChannel = " + CommonConstants.RightChannel);
        playingParam.setCurrentChannelPlayed(CommonConstants.RightChannel);
        setAudioVolume(playingParam.getCurrentVolume());
    }

    public void playStereoChannel() {
        Log.d(TAG, "playStereoChannel() --> CommonConstants.StereoChannel = " + CommonConstants.StereoChannel);
        playingParam.setCurrentChannelPlayed(CommonConstants.StereoChannel);
        setAudioVolume(playingParam.getCurrentVolume());
    }

    public void playSingleSong(SongInfo songInfo) {
        Log.d(TAG, "playSingleSong().");
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
        Log.d(TAG, "filePath = " + filePath);

        try {
            ContentResolver contentResolver = mActivity.getContentResolver();
            for (UriPermission perm : contentResolver.getPersistedUriPermissions()) {
                if (perm.getUri().equals(Uri.parse(filePath))) {
                    Log.d(TAG, "playSingleSong() has URI permission");
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        mediaUri = getValidatedUri(Uri.parse(filePath));

        Log.d(TAG, "mediaUri = " + mediaUri);
        if ((mediaUri == null) || (Uri.EMPTY.equals(mediaUri))) {
            return;
        }

        setPlayingParameters(songInfo);
        playingParam.setCurrentAudioPosition(0);
        playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);
        playingParam.setMediaPrepared(false);
        playMediaFromUri(mediaUri);
    }

    public void startAutoPlay(boolean isSelfFinished) {
        Log.d(TAG, "startAutoPlay");
        if (mActivity.isFinishing()) {
            // activity is being destroyed
            return;
        }

        int orderedSongListSize = 0;
        if (orderedSongList != null) {
            orderedSongListSize = orderedSongList.size();
        }

        Log.d(TAG, "startAutoPlay.orderedSongListSize = " + orderedSongListSize);

        boolean stillPlayNext = true;
        int repeatStatus = playingParam.getRepeatStatus();
        int currentSongIndex = playingParam.getCurrentSongIndex();
        int nextSongIndex = currentSongIndex + 1; // preparing the next
        Log.d(TAG, "startAutoPlay.playingParam.getCurrentSongIndex()+1 = " + nextSongIndex);

        if (orderedSongListSize == 0) {
            stillPlayNext = false;  // no more songs
        } else {
            switch (repeatStatus) {
                case PlayerConstants.NoRepeatPlaying:
                    // no repeat
                    if ((nextSongIndex >= orderedSongListSize) || (nextSongIndex < 0)) {
                        stillPlayNext = false;  // no more songs
                    }
                    break;
                case PlayerConstants.RepeatOneSong:
                    // repeat one song
                    Log.d(TAG, "startAutoPlay.RepeatOneSong");
                    if (isSelfFinished && (nextSongIndex > 0) && (nextSongIndex <= orderedSongListSize)) {
                        nextSongIndex--;
                        Log.d(TAG, "startAutoPlay.RepeatOneSong.nextSongIndex = " + nextSongIndex);
                    }
                    break;
                case PlayerConstants.RepeatAllSongs:
                    // repeat all songs
                    if (nextSongIndex >= orderedSongListSize) {
                        nextSongIndex = 0;
                    }
                    break;
            }
        }

        if (stillPlayNext) {    // still play the next song
            playSingleSong(orderedSongList.get(nextSongIndex));
            playingParam.setCurrentSongIndex(nextSongIndex);    // set nextSongIndex to currentSongIndex
            Log.d(TAG, "startAutoPlay.stillPlayNext.setCurrentSongIndex() = " + nextSongIndex);
        } else {
            Log.d(TAG, "startAutoPlay.not stillPlayNext");
            // added on 2021-07-7
            mPresentView.showNativeAndBannerAd();
            if (orderedSongListSize > 0) {
                mPresentView.showInterstitialAd(false);
            }
            //
        }

        mPresentView.setImageButtonStatus();
    }

    public void setAutoPlayStatusAndAction() {
        boolean isAutoPlay = playingParam.isAutoPlay();
        if (!isAutoPlay) {
            // previous is not auto play
            ArrayList<SongInfo> songList = DatabaseAccessUtil.readSavedSongList(mActivity, true);
            playingParam.setAutoPlay(playSongList(songList)); // must be above autoPlay savedSongList()
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
        mPresentView.setImageButtonStatus();
    }

    public boolean playSongList(ArrayList<SongInfo> songList) {
        if (songList.size() > 0) {
            orderedSongList = new ArrayList<>(songList);
            autoPlaySongList();
            return true;
        } else {
            ScreenUtil.showToast(mActivity, mActivity.getString(R.string.noPlaylistString)
                    , toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
            return false;
        }
    }

    public void selectFileToOpenPresenter(ActivityResult result) {
        Intent data = result.getData();
        ArrayList<Uri> uris;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            uris = data.getParcelableArrayListExtra(PlayerConstants.Uri_List, Uri.class);
        } else uris = data.getParcelableArrayListExtra(PlayerConstants.Uri_List);

        // ArrayList<Uri> uris = getUrisListFromIntentPresenter(data);
        if (uris != null && uris.size() > 0) {
            Log.d(TAG, "selectFileToOpenPresenter.uris.size() = " + uris.size());
            // There are files selected
            playSelectedUrisFromStorage(uris);
        } else {
            Log.d(TAG, "selectFileToOpenPresenter.uris is null or empty");
        }
    }

    public void playPreviousSong() {
        if ( orderedSongList == null) {    // added on 2020-12-08
            return;
        }
        int orderedSongListSize = orderedSongList.size();
        if (orderedSongListSize <= 1 ) {
            // only file in the play list
            ScreenUtil.showToast(mActivity, mActivity.getString(R.string.noPreviousSongString)
                    , toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
            return;
        }
        int currentIndex = playingParam.getCurrentSongIndex();
        int repeatStatus = playingParam.getRepeatStatus();
        // because in startAutoPlay(), the next song will be current index + 1
        int lastPreviousIndex = currentIndex - 2;
        switch (repeatStatus) {
            case PlayerConstants.NoRepeatPlaying:
            case PlayerConstants.RepeatOneSong:
                if (currentIndex <= 0) {
                    // no more previous
                    ScreenUtil.showToast(mActivity, mActivity.getString(R.string.noPreviousSongString)
                            , toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                    return;
                }
                // because in startAutoPlay(), the next song will be current index + 1
                currentIndex = lastPreviousIndex;
                break;
            case PlayerConstants.RepeatAllSongs:
                if (currentIndex <= 0) {
                    // is going to play the last one
                    currentIndex = orderedSongListSize - 2; // the last one
                } else {
                    // because in startAutoPlay(), the next song will be current index + 1
                    currentIndex = lastPreviousIndex;
                }
                break;
        }
        playingParam.setCurrentSongIndex(currentIndex);
        startAutoPlay(false);
    }

    public void playNextSong() {
        if (orderedSongList == null) {   // added on 2020-12-08
            return;
        }
        int orderedSongListSize = orderedSongList.size();
        int currentIndex = playingParam.getCurrentSongIndex();
        int repeatStatus = playingParam.getRepeatStatus();
        if (orderedSongListSize <= 1 ) {
            // only file in the play list
            ScreenUtil.showToast(mActivity, mActivity.getString(R.string.noNextSongString)
                    , toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
            return; // no more next
        }
        switch (repeatStatus) {
            case PlayerConstants.NoRepeatPlaying:
            case PlayerConstants.RepeatOneSong:
                if (currentIndex >= (orderedSongListSize-1)) {
                    ScreenUtil.showToast(mActivity, mActivity.getString(R.string.noNextSongString)
                            , toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                    return; // no more next
                }
                break;
            case PlayerConstants.RepeatAllSongs:
                break;
        }
        Log.d(TAG, "PlayerBasePresenter.getValidatedUri() is called.");
        startAutoPlay(false);    // go to next round
    }

    public Uri getValidatedUri(Uri tempUri) {
        Log.d(TAG, "getValidatedUri");
        return tempUri;
    }

    public void playTheSongThatWasPlayedBeforeActivityCreated() {
        Log.d(TAG, "playTheSongThatWasPlayedBeforeActivityCreated");
        if (mediaUri==null || Uri.EMPTY.equals(mediaUri)) {
            if (playingParam.isPlaySingleSong()) {
                // called by SongListActivity
                Log.d(TAG, "singleSongInfo = " + singleSongInfo);
                if (singleSongInfo != null) {
                    playingParam.setAutoPlay(false);
                    // added on 2020-12-08
                    // set orderedSongList that only contains song info from SongListActivity
                    orderedSongList = new ArrayList<>();
                    orderedSongList.add(singleSongInfo);
                    singleSongInfo = new SongInfo();    // reset for cycle playing
                    autoPlaySongList();
                }
            }
        } else {
            int playbackState = playingParam.getCurrentPlaybackState();
            Log.d(TAG, "playTheSongThatWasPlayedBeforeActivityCreated.playingParam.getCurrentPlaybackState() = " + playbackState);
            if (playbackState != PlaybackStateCompat.STATE_NONE) {
                playMediaFromUri(mediaUri);
            }
        }
    }

    public void setOrientationStatus(int orientation) {
        playingParam.setOrientationStatus(orientation);
        mPresentView.setScreenOrientation(orientation);
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
        mPresentView.setImageButtonStatus();
    }

    public void startPlay() {
        Log.d(TAG, "startPlay");
        int playbackState = playingParam.getCurrentPlaybackState();
        if (playbackState==PlaybackStateCompat.STATE_NONE
            || playbackState==PlaybackStateCompat.STATE_STOPPED) {
            // start playing the first song in the list
            Log.d(TAG, "startPlay.calling autoPlaySongList()");
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
                Log.d(TAG, "startPlay.mediaTransportControls.play() is called.");
            } else {
                // (playbackState == PlaybackStateCompat.STATE_STOPPED) or
                // (playbackState == PlaybackStateCompat.STATE_NONE)
                replayMedia();
                Log.d(TAG, "startPlay.replayMedia() is called.");
            }
        }
    }

    public void pausePlay() {
        Log.d(TAG, "pausePlay() is called.");
        if ( (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) ) {
            // no media file opened or playing has been stopped
            if (mediaTransportControls != null) {
                mediaTransportControls.pause();
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
            }
        }
    }

    public void replayMedia() {
        Log.d(TAG, "replayMedia() is called.");
        if ( (mediaUri == null) || (Uri.EMPTY.equals(mediaUri)) || (numberOfAudioTracks<=0) ) {
            return;
        }
        long currentAudioPosition = 0;
        playingParam.setCurrentAudioPosition(currentAudioPosition);
        if (playingParam.isMediaPrepared()) {
            Log.d(TAG, "replayMedia().specificPlayerReplayMedia(currentAudioPosition)");
            // song is playing, paused, or finished playing
            // cannot do the following statement (exoPlayer.setPlayWhenReady(false); )
            // because it will send Play.STATE_ENDED event after the playing has finished
            // but the playing was stopped in the middle of playing then won't send
            // Play.STATE_ENDED event
            // exoPlayer.setPlayWhenReady(false);
            specificPlayerReplayMedia(currentAudioPosition);
        } else {
            Log.d(TAG, "replayMedia().playMediaFromUri(mediaUri)");
            // song was stopped by user
            // mediaTransportControls.prepare();   // prepare and play
            // Log.d(TAG, "replayMedia()--> mediaTransportControls.prepare().");
            playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);
            playMediaFromUri(mediaUri);
        }
    }

    public void updateStatusAndUi(PlaybackStateCompat state) {
        int currentState = state.getState();
        playingParam.setCurrentPlaybackState(currentState);
        if (currentState == PlaybackStateCompat.STATE_BUFFERING) {
            Log.d(TAG, "updateStatusAndUi.PlaybackStateCompat.STATE_BUFFERING");
            mPresentView.hideNativeAndBannerAd();
            mPresentView.showBufferingMessage();
            return;
        }
        mPresentView.dismissBufferingMessage();
        switch (currentState) {
            case PlaybackStateCompat.STATE_NONE:
                // 1. initial state
                // 2. exoPlayer is stopped by user
                // 3. vlcPlayer finished playing (Event.EndReached)
                // 4. vlcPlayer is stopped by user
                Log.d(TAG, "updateStatusAndUi.PlaybackStateCompat.STATE_NONE");
                mPresentView.playButtonOnPauseButtonOff();
                removeCallbacksAndMessages();
                playingParam.setMediaPrepared(false);
                mPresentView.showNativeAndBannerAd();
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                // when playing
                Log.d(TAG, "updateStatusAndUi.PlaybackStateCompat.STATE_PLAYING");
                if (!playingParam.isMediaPrepared()) {
                    // the first time of Player.STATE_READY means prepared
                    getPlayingMediaInfoAndSetAudioActionSubMenu();
                }
                playingParam.setMediaPrepared(true);  // has been prepared
                startDurationSeekBarHandler();   // start updating duration seekbar
                // set up a timer for supportToolbar's visibility
                mPresentView.setTimerToHideSupportAndAudioController();
                mPresentView.playButtonOffPauseButtonOn();
                Log.d(TAG, "updateStatusAndUi.PlaybackStateCompat.STATE_PLAYING-->hideNativeAndBannerAd()");
                onlyMusicShowNativeAndBannerAd();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                // when playing is paused
                Log.d(TAG, "updateStatusAndUi.PlaybackStateCompat.STATE_PAUSED");
                mPresentView.playButtonOnPauseButtonOff();
                mPresentView.showNativeAndBannerAd();
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                // 1. exoPlayer finished playing
                // 3. after vlcPlayer finished playing
                Log.d(TAG, "updateStatusAndUi.PlaybackStateCompat.STATE_STOPPED");
                playingParam.setMediaPrepared(false);
                mPresentView.update_Player_duration_seekbar_progress((int) getMediaDuration());
                mPresentView.playButtonOnPauseButtonOff();
                removeCallbacksAndMessages();
                // nextSongOrShowNativeAndBannerAd(true);
                startAutoPlay(true);
                break;
            case PlaybackStateCompat.STATE_ERROR:
                String formatNotSupportedString = mActivity.getString(R.string.formatNotSupportedString);
                if (isCanShowNotSupportedFormat()) {
                    // only show once
                    setCanShowNotSupportedFormat(false);
                    ScreenUtil.showToast(mActivity, formatNotSupportedString, toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                }
                setMediaUri(null);
                // remove the song that is unable to be played
                Log.d(TAG, "updateStatusAndUi.PlaybackStateCompat.STATE_ERROR-->orderedSongList.size() = " + orderedSongList.size());
                int currentIndexOfList = playingParam.getCurrentSongIndex();
                Log.d(TAG, "updateStatusAndUi.PlaybackStateCompat.STATE_ERROR-->currentIndexOfList = " + currentIndexOfList);
                if (currentIndexOfList >= 0) {
                    orderedSongList.remove(currentIndexOfList);
                    Log.d(TAG, "updateStatusAndUi.PlaybackStateCompat.STATE_ERROR-->orderedSongList.remove("+currentIndexOfList+")");
                    playingParam.setCurrentSongIndex(--currentIndexOfList);
                }
                Log.d(TAG, "updateStatusAndUi.PlaybackStateCompat.STATE_ERROR-->orderedSongList.size() = " + orderedSongList.size());
                // nextSongOrShowNativeAndBannerAd(false);
                startAutoPlay(false);
                break;
            default:
                Log.d(TAG, "updateStatusAndUi.other PlaybackStateCompat");
        }
    }

    protected void onlyMusicShowNativeAndBannerAd() {
        if (getNumberOfVideoTracks() == 0) {
            // no video is being played, show native ads
            Log.d(TAG, "musicShowNativeAndBannerAd() --> getNumberOfVideoTracks() == 0 --> showNativeAndBannerAd()");
            mPresentView.showNativeAndBannerAd();
        } else {
            Log.d(TAG, "musicShowNativeAndBannerAd() --> getNumberOfVideoTracks() > 0 --> hideNativeAndBannerAd()");
            mPresentView.hideNativeAndBannerAd();
        }
    }

    public void initMediaSessionCompat() {
        Log.d(TAG, "PlayerBasePresenter.initMediaSessionCompat() is called.");
        // Create a MediaSessionCompat
        mediaSessionCompat = new MediaSessionCompat(mActivity, PlayerConstants.LOG_TAG);
        // Do not let MediaButtons restart the player when the app is not visible
        mediaSessionCompat.setMediaButtonReceiver(null);
        mediaSessionCompat.setActive(true); // might need to find better place to put
    }

    public void releaseMediaSessionCompat() {
        Log.d(TAG, "PlayerBasePresenter.releaseMediaSessionCompat() is called.");
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
        Log.d(TAG, "saveInstanceState.orderedSongList = " + orderedSongList);
        outState.putSerializable(PlayerConstants.OrderedSongListState, orderedSongList);
        outState.putParcelable(PlayerConstants.MediaUriState, mediaUri);
        outState.putParcelable(PlayerConstants.PlayingParamState, playingParam);
        outState.putBoolean(PlayerConstants.CanShowNotSupportedFormatState, canShowNotSupportedFormat);
        Log.d(TAG, "saveInstanceState.singleSongInfo = " + singleSongInfo);
        outState.putParcelable(PlayerConstants.SongInfoState, singleSongInfo);
    }
}
