package com.smile.karaokeplayer.Presenters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.av1.Gav1Library;
import com.google.android.exoplayer2.ext.ffmpeg.FfmpegLibrary;
import com.google.android.exoplayer2.ext.flac.FlacLibrary;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ext.opus.OpusLibrary;
import com.google.android.exoplayer2.ext.vp9.VpxLibrary;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.smile.karaokeplayer.AudioProcessor_implement.StereoVolumeAudioProcessor;
import com.smile.karaokeplayer.Callbacks.ExoMediaControllerCallback;
import com.smile.karaokeplayer.Callbacks.ExoPlaybackPreparer;
import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.ExoRenderersFactory.MyRenderersFactory;
import com.smile.karaokeplayer.Listeners.ExoPlayerEventListener;
import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.R;
import com.smile.karaokeplayer.Utilities.DataOrContentAccessUtil;
import com.smile.karaokeplayer.Utilities.ExternalStorageUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.io.File;
import java.util.ArrayList;

public class ExoPlayerPresenter {

    private static final String TAG = new String("ExoPlayerPresenter");

    private final PresentView presentView;
    private final Context callingContext;
    private final Activity mActivity;
    private final float toastTextSize;

    private MediaSessionCompat mediaSessionCompat;
    private MediaControllerCompat mediaControllerCompat;
    private ExoMediaControllerCallback mediaControllerCallback;
    private MediaControllerCompat.TransportControls mediaTransportControls;
    private MediaSessionConnector mediaSessionConnector;

    private StereoVolumeAudioProcessor stereoVolumeAudioProcessor;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private SimpleExoPlayer exoPlayer;
    private ExoPlayerEventListener mExoPlayerEventListener;

    // instances of the following members have to be saved when configuration changed
    private Uri mediaUri;
    private int numberOfVideoTracks;
    private int numberOfAudioTracks;
    private ArrayList<Integer[]> videoTrackIndicesList = new ArrayList<>();
    private ArrayList<Integer[]> audioTrackIndicesList = new ArrayList<>();
    private ArrayList<SongInfo> publicSongList;
    private PlayingParameters playingParam;
    private boolean canShowNotSupportedFormat;
    private SongInfo singleSongInfo;    // when playing single song in songs list

    public interface PresentView {
        void setImageButtonStatus();
        void playButtonOnPauseButtonOff();
        void playButtonOffPauseButtonOn();
        void showBufferingMessage();
        void dismissBufferingMessage();
        void hideNativeAds();
        void showNativeAds();
        void buildAudioTrackMenuItem(int audioTrackNumber);
    }

    public ExoPlayerPresenter(PresentView presentView, Context context) {
        this.presentView = presentView;
        this.callingContext = context;
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

    public ArrayList<Integer[]> getAudioTrackIndicesList() {
        return audioTrackIndicesList;
    }
    public void setAudioTrackIndicesList(ArrayList<Integer[]> audioTrackIndicesList) {
        this.audioTrackIndicesList = audioTrackIndicesList;
    }

    public ArrayList<Integer[]> getVideoTrackIndicesList() {
        return videoTrackIndicesList;
    }
    public void setVideoTrackIndicesList(ArrayList<Integer[]> videoTrackIndicesList) {
        this.videoTrackIndicesList = videoTrackIndicesList;
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
            videoTrackIndicesList = new ArrayList<>();
            audioTrackIndicesList = new ArrayList<>();
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
            trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder(callingContext).build();
        } else {
            // needed to be set
            numberOfVideoTracks = savedInstanceState.getInt(PlayerConstants.NumberOfVideoTracksState,0);
            numberOfAudioTracks = savedInstanceState.getInt(PlayerConstants.NumberOfAudioTracksState);
            videoTrackIndicesList = (ArrayList<Integer[]>)savedInstanceState.getSerializable(PlayerConstants.VideoTrackIndicesListState);
            audioTrackIndicesList = (ArrayList<Integer[]>)savedInstanceState.getSerializable(PlayerConstants.AudioTrackIndicesListState);
            publicSongList = savedInstanceState.getParcelableArrayList(PlayerConstants.PublicSongListState);
            mediaUri = savedInstanceState.getParcelable(PlayerConstants.MediaUriState);
            playingParam = savedInstanceState.getParcelable(PlayerConstants.PlayingParamState);
            canShowNotSupportedFormat = savedInstanceState.getBoolean(PlayerConstants.CanShowNotSupportedFormatState);
            if (playingParam == null) {
                initializePlayingParam();
            }
            singleSongInfo = savedInstanceState.getParcelable(PlayerConstants.SongInfoState);
            trackSelectorParameters = savedInstanceState.getParcelable(PlayerConstants.TrackSelectorParametersState);
        }
    }

    public void initExoPlayer() {

        trackSelector = new DefaultTrackSelector(callingContext, new AdaptiveTrackSelection.Factory());
        trackSelector.setParameters(trackSelectorParameters);

        MyRenderersFactory myRenderersFactory = new MyRenderersFactory(callingContext);
        stereoVolumeAudioProcessor = myRenderersFactory.getStereoVolumeAudioProcessor();


        SimpleExoPlayer.Builder exoPlayerBuilder = new SimpleExoPlayer.Builder(callingContext, myRenderersFactory);
        exoPlayerBuilder.setTrackSelector(trackSelector);
        exoPlayer = exoPlayerBuilder.build();

        // no need. It will ve overridden by MediaSessionConnector
        mExoPlayerEventListener = new ExoPlayerEventListener(callingContext, this);
        exoPlayer.addListener(mExoPlayerEventListener);

        Log.d(TAG, "FfmpegLibrary.isAvailable() = " + FfmpegLibrary.isAvailable());
        Log.d(TAG, "VpxLibrary.isAvailable() = " + VpxLibrary.isAvailable());
        Log.d(TAG, "FlacLibrary.isAvailable() = " + FlacLibrary.isAvailable());
        Log.d(TAG, "OpusLibrary.isAvailable() = " + OpusLibrary.isAvailable());
        Log.d(TAG, "Gav1Library.isAvailable() = " + Gav1Library.isAvailable());
    }

    public void releaseExoPlayer() {
        if (exoPlayer != null) {
            exoPlayer.removeListener(mExoPlayerEventListener);
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    public SimpleExoPlayer getExoPlayer() {
        return exoPlayer;
    }

    public void setAudioVolume(float volume) {
        // get current channel
        int currentChannelPlayed = playingParam.getCurrentChannelPlayed();
        //
        boolean useAudioProcessor = false;
        if (stereoVolumeAudioProcessor != null) {
            int channelCount = stereoVolumeAudioProcessor.getOutputChannelCount();
            if (channelCount >= 0) {
                useAudioProcessor = true;
                float[] volumeInput = new float[stereoVolumeAudioProcessor.getOutputChannelCount()];
                switch (channelCount) {
                    case 2:
                        if (currentChannelPlayed == CommonConstants.LeftChannel) {
                            volumeInput[StereoVolumeAudioProcessor.LEFT_SPEAKER] = volume;
                            volumeInput[StereoVolumeAudioProcessor.RIGHT_SPEAKER] = 0.0f;
                        } else if (currentChannelPlayed == CommonConstants.RightChannel) {
                            volumeInput[StereoVolumeAudioProcessor.LEFT_SPEAKER] = 0.0f;
                            volumeInput[StereoVolumeAudioProcessor.RIGHT_SPEAKER] = volume;
                        } else {
                            volumeInput[StereoVolumeAudioProcessor.LEFT_SPEAKER] = volume;
                            volumeInput[StereoVolumeAudioProcessor.RIGHT_SPEAKER] = volume;
                        }
                        break;
                    default:
                        for (int i = 0; i < volumeInput.length; i++) {
                            volumeInput[i] = volume;
                        }
                        break;
                }
                stereoVolumeAudioProcessor.setVolume(volumeInput);
            }
        }
        if (!useAudioProcessor) {
            exoPlayer.setVolume(volume);
        }
        playingParam.setCurrentVolume(volume);
    }

    public void setAudioVolumeInsideVolumeSeekBar(int i) {
        // needed to put inside the presenter
        float currentVolume = 1.0f;
        if (i < PlayerConstants.MaxProgress) {
            currentVolume = (float)(1.0f - (Math.log(PlayerConstants.MaxProgress - i) / Math.log(PlayerConstants.MaxProgress)));
        }
        playingParam.setCurrentVolume(currentVolume);
        setAudioVolume(currentVolume);
        //
    }

    public int setCurrentProgressForVolumeSeekBar() {
        int currentProgress;
        float currentVolume = playingParam.getCurrentVolume();
        if ( currentVolume >= 1.0f) {
            currentProgress = PlayerConstants.MaxProgress;
        } else {
            currentProgress = PlayerConstants.MaxProgress - (int)Math.pow(PlayerConstants.MaxProgress, (1-currentVolume));
            currentProgress = Math.max(0, currentProgress);
        }

        return currentProgress;
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
        // numberOfAudioTracks = audioTrackIndicesList.size();
        if (numberOfAudioTracks > 0) {
            // select audio track
            if (audioTrackIndex<=0) {
                Log.d(TAG, "No such audio Track Index = " + audioTrackIndex);
                return;
            }
            if (audioTrackIndex>numberOfAudioTracks) {
                Log.d(TAG, "No such audio Track Index = " + audioTrackIndex);
                // set to first track
                audioTrackIndex = 1;
            }
            int indexInArrayList = audioTrackIndex - 1;

            Integer[] trackIndicesCombination = audioTrackIndicesList.get(indexInArrayList);
            selectAudioTrack(trackIndicesCombination);
            playingParam.setCurrentAudioTrackIndexPlayed(audioTrackIndex);

            // select audio channel
            playingParam.setCurrentChannelPlayed(audioChannel);
            setAudioVolume(playingParam.getCurrentVolume());
        }
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

    private void setProperAudioTrackAndChannel() {
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

    private boolean selectAudioTrack(Integer[] trackIndicesCombination) {
        boolean result = false;
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if ( (trackIndicesCombination == null) || (mappedTrackInfo == null) ) {
            return result;
        }

        int audioRendererIndex = trackIndicesCombination[0];
        Log.d(TAG, "selectAudioTrack() --> audioRendererIndex = " + audioRendererIndex);
        int audioTrackGroupIndex = trackIndicesCombination[1];
        Log.d(TAG, "selectAudioTrack() --> audioTrackGroupIndex = " + audioTrackGroupIndex);
        int audioTrackIndex = trackIndicesCombination[2];
        Log.d(TAG, "selectAudioTrack() --> audioTrackIndex = " + audioTrackIndex);

        if (mappedTrackInfo.getTrackSupport(audioRendererIndex, audioTrackGroupIndex, audioTrackIndex)
                != RendererCapabilities.FORMAT_HANDLED) {
            return result;
        }

        DefaultTrackSelector.Parameters trackParameters = trackSelector.getParameters();
        DefaultTrackSelector.ParametersBuilder parametersBuilder = trackParameters.buildUpon();

        DefaultTrackSelector.SelectionOverride initialOverride = trackParameters.getSelectionOverride(audioRendererIndex, mappedTrackInfo.getTrackGroups(audioRendererIndex));

        initialOverride = new DefaultTrackSelector.SelectionOverride(audioTrackGroupIndex, audioTrackIndex);
        // trackSelector.setParameters(parametersBuilder.build());
        // or
        parametersBuilder.clearSelectionOverrides(audioRendererIndex)
                .setRendererDisabled(audioRendererIndex, false)
                .setSelectionOverride(audioRendererIndex, mappedTrackInfo.getTrackGroups(audioRendererIndex), initialOverride);
        trackSelector.setParameters(parametersBuilder);

        trackSelectorParameters = trackSelector.getParameters();

        return result;
    }

    public void findTracksForVideoAudio() {
        int numVideoRenderers = 0;
        int numAudioRenderers = 0;
        int numVideoTrackGroups = 0;
        int numAudioTrackGroups = 0;
        int totalVideoTracks = 0;
        int totalAudioTracks = 0;

        videoTrackIndicesList.clear();
        audioTrackIndicesList.clear();

        Integer[] trackIndicesCombination = new Integer[3];
        int videoTrackIdPlayed = -1;
        int audioTrackIdPlayed = -1;

        Format videoPlayedFormat = exoPlayer.getVideoFormat();
        if (videoPlayedFormat != null) {
            Log.d(TAG, "videoPlayedFormat.id = " + videoPlayedFormat.id);
        } else {
            Log.d(TAG, "videoPlayedFormat is null.");
        }
        Format audioPlayedFormat = exoPlayer.getAudioFormat();
        if (audioPlayedFormat != null) {
            Log.d(TAG, "audioPlayedFormat.id = " + audioPlayedFormat.id);
            int channelsNum = audioPlayedFormat.channelCount;
            Log.d(TAG, "audioPlayedFormat.channelCount = " + channelsNum);
            Log.d(TAG, "audioPlayedFormat.sampleRate = " + audioPlayedFormat.sampleRate);
            Log.d(TAG, "audioPlayedFormat.pcmEncoding = " + audioPlayedFormat.pcmEncoding);
        } else {
            Log.d(TAG, "audioPlayedFormat is null.");
        }

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo != null) {
            int rendererCount = mappedTrackInfo.getRendererCount();
            Log.d(TAG, "mappedTrackInfo.getRendererCount() = " + rendererCount);
            //
            for (int rendererIndex = 0; rendererIndex < rendererCount; rendererIndex++) {
                Log.d(TAG, "rendererIndex = " + rendererIndex);
                int rendererType = mappedTrackInfo.getRendererType(rendererIndex);
                switch (rendererType) {
                    case C.TRACK_TYPE_VIDEO:
                        numVideoRenderers++;
                        break;
                    case C.TRACK_TYPE_AUDIO:
                        numAudioRenderers++;
                        break;
                }
                //
                TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);
                if (trackGroupArray != null) {
                    int arraySize = trackGroupArray.length;
                    Log.d(TAG, "trackGroupArray.length of renderer no ( " + rendererIndex + " ) = " + arraySize);
                    for (int groupIndex = 0; groupIndex < arraySize; groupIndex++) {
                        Log.d(TAG, "trackGroupArray.index = " + groupIndex);
                        switch (rendererType) {
                            case C.TRACK_TYPE_VIDEO:
                                numVideoTrackGroups++;
                                break;
                            case C.TRACK_TYPE_AUDIO:
                                numAudioTrackGroups++;
                                break;
                        }
                        TrackGroup trackGroup = trackGroupArray.get(groupIndex);
                        if (trackGroup != null) {
                            int groupSize = trackGroup.length;
                            Log.d(TAG, "trackGroup.length of trackGroup [ " + groupIndex + " ] = " + groupSize);
                            for (int trackIndex = 0; trackIndex < groupSize; trackIndex++) {
                                Format tempFormat = trackGroup.getFormat(trackIndex);
                                switch (rendererType) {
                                    case C.TRACK_TYPE_VIDEO:
                                        trackIndicesCombination = new Integer[3];
                                        trackIndicesCombination[0] = rendererIndex;
                                        trackIndicesCombination[1] = groupIndex;
                                        trackIndicesCombination[2] = trackIndex;
                                        videoTrackIndicesList.add(trackIndicesCombination);
                                        totalVideoTracks++;
                                        if (tempFormat.equals(videoPlayedFormat)) {
                                            videoTrackIdPlayed = totalVideoTracks;
                                        }
                                        break;
                                    case C.TRACK_TYPE_AUDIO:
                                        trackIndicesCombination = new Integer[3];
                                        trackIndicesCombination[0] = rendererIndex;
                                        trackIndicesCombination[1] = groupIndex;
                                        trackIndicesCombination[2] = trackIndex;
                                        audioTrackIndicesList.add(trackIndicesCombination);
                                        totalAudioTracks++;
                                        if (tempFormat.equals(audioPlayedFormat)) {
                                            audioTrackIdPlayed = totalAudioTracks;
                                        }
                                        break;
                                }
                                //
                                Log.d(TAG, "tempFormat = " + tempFormat);
                            }
                        }
                    }
                }
            }
        } else {
            Log.d(TAG, "mappedTrackInfo is null.");
        }

        Log.d(TAG, "numVideoRenderer = " + numVideoRenderers);
        Log.d(TAG, "numAudioRenderer = " + numAudioRenderers);
        Log.d(TAG, "numVideoTrackGroups = " + numVideoTrackGroups);
        Log.d(TAG, "numAudioTrackGroups = " + numAudioTrackGroups);
        Log.d(TAG, "totalVideoTracks = " + totalVideoTracks);
        Log.d(TAG, "totalAudioTracks = " + totalAudioTracks);

        numberOfVideoTracks = videoTrackIndicesList.size();
        Log.d(TAG, "numberOfVideoTracks = " + numberOfVideoTracks);
        if (numberOfVideoTracks == 0) {
            playingParam.setCurrentVideoTrackIndexPlayed(PlayerConstants.NoVideoTrack);
        } else {
            Log.d(TAG, "audioTrackIdPlayed = " + videoTrackIdPlayed);
            if (videoTrackIdPlayed < 0) {
                videoTrackIdPlayed = 1;
            }
            playingParam.setCurrentVideoTrackIndexPlayed(videoTrackIdPlayed);
        }

        numberOfAudioTracks = audioTrackIndicesList.size();
        Log.d(TAG, "numberOfAudioTracks = " + numberOfAudioTracks);
        if (numberOfAudioTracks == 0) {
            playingParam.setCurrentAudioTrackIndexPlayed(PlayerConstants.NoAudioTrack);
            playingParam.setCurrentChannelPlayed(PlayerConstants.NoAudioChannel);
        } else {
            int audioChannel = CommonConstants.StereoChannel;  // default channel
            Log.d(TAG, "audioTrackIdPlayed = " + audioTrackIdPlayed);
            if (playingParam.isAutoPlay() || playingParam.isPlaySingleSong()) {
                audioTrackIdPlayed = playingParam.getCurrentAudioTrackIndexPlayed();
                audioChannel = playingParam.getCurrentChannelPlayed();
            } else {
                // for open media. do not know the music track and vocal track
                playingParam.setMusicAudioTrackIndex(audioTrackIdPlayed);
                playingParam.setMusicAudioChannel(audioChannel);
                playingParam.setVocalAudioTrackIndex(audioTrackIdPlayed);
                playingParam.setVocalAudioChannel(audioChannel);
                playingParam.setCurrentAudioTrackIndexPlayed(audioTrackIdPlayed);
            }

            if (audioTrackIdPlayed < 0) {
                audioTrackIdPlayed = 1;
            }
            setAudioTrackAndChannel(audioTrackIdPlayed, audioChannel);
        }

        // build R.id.audioTrack submenu
        presentView.buildAudioTrackMenuItem(audioTrackIndicesList.size());
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
        mediaUri = null;
        filePath = ExternalStorageUtil.getUriRealPath(callingContext, Uri.parse(filePath));
        if (filePath != null) {
            if (!filePath.isEmpty()) {
                File songFile = new File(filePath);
                mediaUri = Uri.fromFile(songFile);
            }
        }
        // } else {
        //     mediaUri = Uri.parse(filePath);
        // }

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

        // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
        // pass the saved instance of playingParam to
        // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)
        Bundle playingParamOriginExtras = new Bundle();
        playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
        mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
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

    public void playSelectedSongFromStorage(Uri tempUri) {
        mediaUri = tempUri;
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
        // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
        // pass the saved instance of playingParam to
        // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)

        Bundle playingParamOriginExtras = new Bundle();
        playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
        mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
    }

    public void playTheSongThatWasPlayedBeforeActivityRecreated() {
        // Uri mediaUri = mPresenter.getMediaUri();
        if (mediaUri==null || Uri.EMPTY.equals(mediaUri)) {
            if (playingParam.isPlaySingleSong()) {
                // SongInfo singleSongInfo = mPresenter.getSingleSongInfo();
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
                // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
                // pass the saved instance of playingParam to
                // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)
                Bundle playingParamOriginExtras = new Bundle();
                playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
                // MediaControllerCompat.TransportControls mediaTransportControls = mPresenter.getMediaTransportControls();
                mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
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
                mediaTransportControls.play();
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
        // if ( (mediaSource != null) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) ) {
        if ( (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) ) {
            // no media file opened or playing has been stopped
            mediaTransportControls.pause();
        }
    }

    public void stopPlay() {
        if (playingParam.isAutoPlay()) {
            // auto play
            startAutoPlay();
        } else {
            if (playingParam.getRepeatStatus() == PlayerConstants.NoRepeatPlaying) {
                // no repeat playing
                // if ((mediaSource != null) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_NONE)) {
                if ((mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_NONE)) {
                    // media file opened or playing has been stopped
                    mediaTransportControls.stop();
                    Log.d(TAG, "stopPlay() ---> mediaTransportControls.stop() is called.");
                }
            } else {
                replayMedia();
            }
        }
        Log.d(TAG, "stopPlay() is called.");
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
            exoPlayer.seekTo(currentAudioPosition);
            setProperAudioTrackAndChannel();
            exoPlayer.retry();
            exoPlayer.setPlayWhenReady(true);
            Log.d(TAG, "replayMedia()--> exoPlayer.seekTo(currentAudioPosition).");
        } else {
            // song was stopped by user
            // mediaTransportControls.prepare();   // prepare and play
            // Log.d(TAG, "replayMedia()--> mediaTransportControls.prepare().");
            // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
            // pass the saved instance of playingParam to
            // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)
            Bundle playingParamOriginExtras = new Bundle();
            playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
            mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);   // prepare and play
            Log.d(TAG, "replayMedia()--> mediaTransportControls.prepareFromUri().");
        }

        Log.d(TAG, "replayMedia() is called.");
    }

    public void initMediaSessionCompat() {
        // Create a MediaSessionCompat
        mediaSessionCompat = new MediaSessionCompat(callingContext, PlayerConstants.LOG_TAG);

        // Do not let MediaButtons restart the player when the app is not visible
        mediaSessionCompat.setMediaButtonReceiver(null);
        mediaSessionCompat.setActive(true); // might need to find better place to put

        // Create a MediaControllerCompat
        mediaControllerCompat = new MediaControllerCompat(callingContext, mediaSessionCompat);
        MediaControllerCompat.setMediaController(mActivity, mediaControllerCompat);
        mediaControllerCallback = new ExoMediaControllerCallback(this);
        mediaControllerCompat.registerCallback(mediaControllerCallback);
        mediaTransportControls = mediaControllerCompat.getTransportControls();

        mediaSessionConnector = new MediaSessionConnector(mediaSessionCompat);
        mediaSessionConnector.setPlayer(exoPlayer);
        mediaSessionConnector.setPlaybackPreparer(new ExoPlaybackPreparer(callingContext, this));
    }

    public void releaseMediaSessionCompat() {
        if (mediaSessionCompat != null) {
            mediaSessionCompat.setActive(false);
            mediaSessionCompat.release();
            mediaSessionCompat = null;
        }
        mediaTransportControls = null;
        if (mediaControllerCallback != null) {
            mediaControllerCompat.unregisterCallback(mediaControllerCallback);
            mediaControllerCallback = null;
        }
        mediaControllerCompat = null;
        mediaSessionConnector = null;
    }

    public MediaControllerCompat.TransportControls getMediaTransportControls() {
        return mediaTransportControls;
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG,"onSaveInstanceState() is called.");

        if (exoPlayer != null) {
            playingParam.setCurrentAudioPosition(exoPlayer.getContentPosition());
        } else {
            playingParam.setCurrentAudioPosition(0);
        }

        outState.putInt(PlayerConstants.NumberOfVideoTracksState, numberOfVideoTracks);
        outState.putInt(PlayerConstants.NumberOfAudioTracksState, numberOfAudioTracks);
        outState.putSerializable(PlayerConstants.VideoTrackIndicesListState, videoTrackIndicesList);
        outState.putSerializable(PlayerConstants.AudioTrackIndicesListState, audioTrackIndicesList);
        outState.putParcelableArrayList(PlayerConstants.PublicSongListState, publicSongList);

        outState.putParcelable(PlayerConstants.MediaUriState, mediaUri);

        outState.putParcelable(PlayerConstants.PlayingParamState, playingParam);
        outState.putBoolean(PlayerConstants.CanShowNotSupportedFormatState, canShowNotSupportedFormat);
        outState.putParcelable(PlayerConstants.SongInfoState, singleSongInfo);

        trackSelectorParameters = trackSelector.getParameters();
        outState.putParcelable(PlayerConstants.TrackSelectorParametersState, trackSelectorParameters);
    }
}
