package com.smile.karaokeplayer.Presenters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
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

import java.util.ArrayList;

public class ExoPlayerPresenter extends PlayerBasePresenter{

    private static final String TAG = new String("ExoPlayerPresenter");

    private final Context callingContext;
    private final PresentView presentView;
    private final Activity mActivity;

    private MediaControllerCompat mediaControllerCompat;
    private ExoMediaControllerCallback mediaControllerCallback;
    private MediaSessionConnector mediaSessionConnector;

    private StereoVolumeAudioProcessor stereoVolumeAudioProcessor;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private SimpleExoPlayer exoPlayer;
    private ExoPlayerEventListener mExoPlayerEventListener;

    // instances of the following members have to be saved when configuration changed
    private ArrayList<Integer[]> videoTrackIndicesList = new ArrayList<>();
    private ArrayList<Integer[]> audioTrackIndicesList = new ArrayList<>();

    public interface PresentView extends PlayerBasePresenter.PresentView {
    }

    private final Handler durationSeekBarHandler = new Handler(Looper.getMainLooper());
    private final Runnable durationSeekBarRunnable = new Runnable() {
        @Override
        public synchronized void run() {
            durationSeekBarHandler.removeCallbacksAndMessages(null);
            if (exoPlayer != null) {
                int playbackState = exoPlayer.getPlaybackState();
                if (exoPlayer.getPlayWhenReady() && playbackState!=Player.STATE_IDLE && playbackState!=Player.STATE_ENDED) {
                    presentView.update_Player_duration_seekbar_progress((int)exoPlayer.getCurrentPosition());
                }
            }
            durationSeekBarHandler.postDelayed(durationSeekBarRunnable, 1000);
        }
    };

    public ExoPlayerPresenter(Context context, PresentView presentView) {
        super(context, presentView);
        this.callingContext = context;
        this.presentView = presentView;
        this.mActivity = (Activity)(this.presentView);
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

    public PresentView getPresentView() {
        return presentView;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initializeVariables(Bundle savedInstanceState, Intent callingIntent) {
        super.initializeVariables(savedInstanceState, callingIntent);
        if (savedInstanceState == null) {
            videoTrackIndicesList = new ArrayList<>();
            audioTrackIndicesList = new ArrayList<>();
            trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder(callingContext).build();
        } else {
            videoTrackIndicesList = (ArrayList<Integer[]>)savedInstanceState.getSerializable(PlayerConstants.VideoTrackIndicesListState);
            audioTrackIndicesList = (ArrayList<Integer[]>)savedInstanceState.getSerializable(PlayerConstants.AudioTrackIndicesListState);
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

    public void getPlayingMediaInfoAndSetAudioActionSubMenu() {
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

        // update the duration on controller UI
        float duration = exoPlayer.getDuration();
        presentView.update_Player_duration_seekbar(duration);
    }

    public synchronized void startDurationSeekBarHandler() {
        // start monitor player_duration_seekbar
        durationSeekBarHandler.removeCallbacksAndMessages(null);
        final Handler tempHandler = new Handler(Looper.getMainLooper());
        final Runnable tempRunnable = new Runnable() {
            @Override
            public void run() {
                tempHandler.removeCallbacksAndMessages(null);
                // start durationSeekBarHandler immediately
                durationSeekBarHandler.postDelayed(durationSeekBarRunnable, 0);
            }
        };
        tempHandler.postDelayed(tempRunnable, 200); // delay 200ms
        //
    }

    @Override
    public void setPlayerTime(int progress) {
        super.setPlayerTime(progress);
        exoPlayer.seekTo(progress);
    }

    @Override
    public void setAudioVolume(float volume) {
        super.setAudioVolume(volume);

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

    @Override
    public void setAudioVolumeInsideVolumeSeekBar(int i) {
        super.setAudioVolumeInsideVolumeSeekBar(i);
        // needed to put inside the presenter
        float currentVolume = 1.0f;
        if (i < PlayerConstants.MaxProgress) {
            currentVolume = (float)(1.0f - (Math.log(PlayerConstants.MaxProgress - i) / Math.log(PlayerConstants.MaxProgress)));
        }
        playingParam.setCurrentVolume(currentVolume);
        setAudioVolume(currentVolume);
        //
    }

    @Override
    public int setCurrentProgressForVolumeSeekBar() {
        int currentProgress = super.setCurrentProgressForVolumeSeekBar();
        float currentVolume = playingParam.getCurrentVolume();
        if ( currentVolume >= 1.0f) {
            currentProgress = PlayerConstants.MaxProgress;
        } else {
            currentProgress = PlayerConstants.MaxProgress - (int)Math.pow(PlayerConstants.MaxProgress, (1-currentVolume));
            currentProgress = Math.max(0, currentProgress);
        }

        return currentProgress;
    }

    @Override
    public void setAudioTrackAndChannel(int audioTrackIndex, int audioChannel) {
        super.setAudioTrackAndChannel(audioTrackIndex, audioChannel);

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

    @Override
    public void replayMedia() {
        super.replayMedia();

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

    @Override
    public void initMediaSessionCompat() {
        // Create a MediaSessionCompat
        super.initMediaSessionCompat();
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

    @Override
    public void releaseMediaSessionCompat() {
        super.releaseMediaSessionCompat();

        if (mediaControllerCallback != null) {
            mediaControllerCompat.unregisterCallback(mediaControllerCallback);
            mediaControllerCallback = null;
        }
        mediaControllerCompat = null;
        mediaSessionConnector = null;
    }

    @Override
    public void saveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG,"saveInstanceState() is called.");

        if (exoPlayer != null) {
            playingParam.setCurrentAudioPosition(exoPlayer.getContentPosition());
        } else {
            playingParam.setCurrentAudioPosition(0);
        }
        outState.putSerializable(PlayerConstants.VideoTrackIndicesListState, videoTrackIndicesList);
        outState.putSerializable(PlayerConstants.AudioTrackIndicesListState, audioTrackIndicesList);
        trackSelectorParameters = trackSelector.getParameters();
        outState.putParcelable(PlayerConstants.TrackSelectorParametersState, trackSelectorParameters);

        super.saveInstanceState(outState);
    }
}
