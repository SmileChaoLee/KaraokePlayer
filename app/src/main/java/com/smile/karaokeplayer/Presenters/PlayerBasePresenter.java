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
import com.smile.karaokeplayer.R;
import com.smile.karaokeplayer.Utilities.DataOrContentAccessUtil;
import com.smile.karaokeplayer.Utilities.ExternalStorageUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.io.File;
import java.util.ArrayList;

public class PlayerBasePresenter {

    private static final String TAG = new String("PlayerBasePresenter");

    private final Context callingContext;
    private final PresentView presentView;
    private final Activity mActivity;
    private final int maxNumberOfSongsPlayed = 5; // the point to enter showing ad
    private int numberOfSongsPlayed;

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

    public interface PresentView {
        void setImageButtonStatus();
        void playButtonOnPauseButtonOff();
        void playButtonOffPauseButtonOn();
        void setPlayingTimeTextView(String durationString);
        void update_Player_duration_seekbar(float duration);
        void update_Player_duration_seekbar_progress(int progress);
        void showNativeAd();
        void hideNativeAd();
        void showBufferingMessage();
        void dismissBufferingMessage();
        void buildAudioTrackMenuItem(int audioTrackNumber);
        void setTimerToHideSupportAndAudioController();
    }

    public PlayerBasePresenter(Context context, PresentView presentView) {
        this.callingContext = context;
        this.presentView = presentView;
        this.mActivity = (Activity)(this.presentView);

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(callingContext, ScreenUtil.FontSize_Pixel_Type, null);
        float textFontSize = ScreenUtil.suitableFontSize(callingContext, defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        toastTextSize = 0.7f * textFontSize;
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

    public PresentView getPresentView() {
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
        numberOfSongsPlayed = 0;    // start from 0
    }

    public int getNumberOfSongsPlayed() {
        return numberOfSongsPlayed;
    }
    public void setNumberOfSongsPlayed(int numberOfSongsPlayed) {
        this.numberOfSongsPlayed = numberOfSongsPlayed;
    }
    public int getMaxNumberOfSongsPlayed() {
        return maxNumberOfSongsPlayed;
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

    public void setPlayerTime(int progress) {
    }

    public void setAudioVolume(float volume) {
    }

    public void setAudioVolumeInsideVolumeSeekBar(int i) {
    }

    public int setCurrentProgressForVolumeSeekBar() {
        return 0;
    }

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

    public void setAudioTrackAndChannel(int audioTrackIndex, int audioChannel) {
    }

    public void switchAudioToVocal() {
        int vocalAudioTrackIndex = playingParam.getVocalAudioTrackIndex();
        int vocalAudioChannel = playingParam.getVocalAudioChannel();
        playingParam.setMusicOrVocalOrNoSetting(PlayerConstants.PlayingVocal);
        setAudioTrackAndChannel(vocalAudioTrackIndex, vocalAudioChannel);
    }

    public void switchAudioToMusic() {
        int musicAudioTrackIndex = playingParam.getMusicAudioTrackIndex();
        int musicAudioChannel = playingParam.getMusicAudioChannel();
        playingParam.setMusicOrVocalOrNoSetting(PlayerConstants.PlayingMusic);
        setAudioTrackAndChannel(musicAudioTrackIndex, musicAudioChannel);
    }

    protected void setProperAudioTrackAndChannel() {
        if (playingParam.isAutoPlay()) {
            if (playingParam.isPlayingPublic()) {
                switchAudioToVocal();
            } else {
                switchAudioToMusic();
            }
        } else {
            // not auto playing media, means using open menu to open a media
            switchAudioToVocal();   // music and vocal are the same in this case
        }
    }

    protected void playMediaFromUri(Uri uri) {
        // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
        // pass the saved instance of playingParam to
        // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)
        Bundle playingParamOriginExtras = new Bundle();
        playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
        if (mediaTransportControls != null) {
            mediaTransportControls.prepareFromUri(uri, playingParamOriginExtras);
            numberOfSongsPlayed++;
        }
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

        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        // Have to add android:requestLegacyExternalStorage="true" in AndroidManifest.xml
        // to let devices that are above (included) API 29 can still use external storage
        // mediaUri = null;
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
        /*
        try {
            filePath = ExternalStorageUtil.getUriRealPath(callingContext, Uri.parse(filePath));
            if (filePath != null) {
                if (!filePath.isEmpty()) {
                    File songFile = new File(filePath);
                    mediaUri = Uri.fromFile(songFile);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        */

        Log.i(TAG, "mediaUri = " + mediaUri);
        if ((mediaUri == null) || (Uri.EMPTY.equals(mediaUri))) {
            return;
        }

        playingParam.setMusicOrVocalOrNoSetting(PlayerConstants.PlayingVocal);  // presume vocal
        playingParam.setCurrentVideoTrackIndexPlayed(0);

        playingParam.setMusicAudioTrackIndex(songInfo.getMusicTrackNo());
        playingParam.setMusicAudioChannel(songInfo.getMusicChannel());

        playingParam.setVocalAudioTrackIndex(songInfo.getVocalTrackNo());
        playingParam.setCurrentAudioTrackIndexPlayed(playingParam.getVocalAudioTrackIndex());
        playingParam.setVocalAudioChannel(songInfo.getVocalChannel());
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

        // activity is not being destroyed then check
        // if there are still songs to be play
        boolean hasSongs = false;   // no more songs to play
        if (hasSongs) {
            playingParam.setPlayingPublic(false);   // playing next ordered song
        } else {
            playingParam.setPlayingPublic(true);   // playing next public song on list
        }

        SongInfo songInfo = null;
        if (playingParam.isPlayingPublic())  {
            int publicSongListSize = 0;
            if (publicSongList != null) {
                publicSongListSize = publicSongList.size();
            }
            if (publicSongListSize <= 0) {
                // no public songs
                ScreenUtil.showToast(callingContext, callingContext.getString(R.string.noPlaylistString), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                playingParam.setAutoPlay(false);    // cancel auto play
            } else {
                // There are public songs to be played
                boolean stillPlayNext = true;
                int repeatStatus = playingParam.getRepeatStatus();
                int publicNextSongIndex = playingParam.getPublicNextSongIndex();
                switch (repeatStatus) {
                    case PlayerConstants.NoRepeatPlaying:
                        // no repeat
                        if ( (publicNextSongIndex >= publicSongListSize) || (publicNextSongIndex<0) ) {
                            // stop playing
                            playingParam.setAutoPlay(false);
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
                    songInfo = publicSongList.get(publicNextSongIndex);
                    playSingleSong(songInfo);
                    publicNextSongIndex++;  // set next index of playlist that will be played
                    playingParam.setPublicNextSongIndex(publicNextSongIndex);
                }
                Log.d(TAG, "Repeat status = " + repeatStatus);
                Log.d(TAG, "startAutoPlay() finished --> " + publicNextSongIndex--);
                // }
            }
        } else {
            // play next song that user has ordered
            playingParam.setMusicOrVocalOrNoSetting(PlayerConstants.PlayingMusic);  // presume music
            if (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) {
                if (playingParam.getRepeatStatus() != PlayerConstants.NoRepeatPlaying) {
                    // repeat playing this mediaUri
                } else {
                    // next song that user ordered
                }
            }
            Log.d(TAG, "startAutoPlay() finished --> ordered song.");
        }

        presentView.setImageButtonStatus();
    }

    public void setAutoPlayStatusAndAction() {
        boolean isAutoPlay = !playingParam.isAutoPlay();
        canShowNotSupportedFormat = true;
        if (isAutoPlay) {
            publicSongList = DataOrContentAccessUtil.readPublicSongList(callingContext);
            if ( (publicSongList != null) && (publicSongList.size() > 0) ) {
                playingParam.setAutoPlay(true);
                playingParam.setPublicNextSongIndex(0);
                // start playing video from list
                // if (exoPlayer.getPlaybackState() != Player.STATE_IDLE) {
                if (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_NONE) {
                    // media is playing or prepared
                    // exoPlayer.stop();// no need   // will go to onPlayerStateChanged()
                    Log.d(TAG, "isAutoPlay is true and exoPlayer.stop().");

                }
                startAutoPlay();
            } else {
                String msg = callingContext.getString(R.string.noPlaylistString);
                ScreenUtil.showToast(callingContext, msg, toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
            }
        } else {
            playingParam.setAutoPlay(isAutoPlay);
        }
        presentView.setImageButtonStatus();
    }

    public void playPreviousSong() {
        if ( publicSongList==null || !playingParam.isAutoPlay() || playingParam.isPlaySingleSong()) {
            return;
        }
        int publicSongListSize = publicSongList.size();
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
            default:
                break;
        }
        playingParam.setPublicNextSongIndex(nextIndex);

        startAutoPlay();
    }

    public void playNextSong() {
        if ( publicSongList==null || !playingParam.isAutoPlay() || playingParam.isPlaySingleSong()) {
            return;
        }
        int publicSongListSize = publicSongList.size();
        int nextIndex = playingParam.getPublicNextSongIndex();
        int repeatStatus = playingParam.getRepeatStatus();
        if (repeatStatus == PlayerConstants.RepeatOneSong) {
            nextIndex++;
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

    public void playSelectedSongFromStorage(Uri tempUri) {
        mediaUri = getValidatedUri(tempUri);
        Log.i(TAG, "mediaUri = " + mediaUri);
        if ((mediaUri == null) || (Uri.EMPTY.equals(mediaUri))) {
            return;
        }

        playingParam.setCurrentVideoTrackIndexPlayed(0);
        int currentAudioRederer = 0;
        playingParam.setMusicAudioTrackIndex(currentAudioRederer);
        playingParam.setVocalAudioTrackIndex(currentAudioRederer);
        playingParam.setCurrentAudioTrackIndexPlayed(currentAudioRederer);
        playingParam.setMusicAudioChannel(CommonConstants.LeftChannel);
        playingParam.setVocalAudioChannel(CommonConstants.StereoChannel);
        playingParam.setCurrentChannelPlayed(CommonConstants.StereoChannel);
        playingParam.setCurrentAudioPosition(0);
        playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);
        playingParam.setMediaSourcePrepared(false);
        // music or vocal is unknown
        playingParam.setMusicOrVocalOrNoSetting(PlayerConstants.MusicOrVocalUnknown);

        playMediaFromUri(mediaUri);
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
        int playbackState = playingParam.getCurrentPlaybackState();
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
        Log.d(TAG, "startPlay() is called.");
    }

    public void pausePlay() {
        if ( (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) ) {
            // no media file opened or playing has been stopped
            if (mediaTransportControls != null) {
                mediaTransportControls.pause();
                presentView.showNativeAd();
            }
        }
    }

    public void stopPlay() {
        if (playingParam.isAutoPlay()) {
            // auto play
            startAutoPlay();
        } else {
            if (playingParam.getRepeatStatus() == PlayerConstants.NoRepeatPlaying) {
                // no repeat playing
                if ((mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_NONE)) {
                    // media file opened or playing has been stopped
                    if (mediaTransportControls != null) {
                        mediaTransportControls.stop();
                    }
                    Log.d(TAG, "stopPlay() ---> mediaTransportControls.stop() is called.");
                }
            } else {
                replayMedia();
            }
        }
        Log.d(TAG, "stopPlay() is called.");
    }

    protected void specificPlayerReplayMedia(long currentAudioPosition) {
    }

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
