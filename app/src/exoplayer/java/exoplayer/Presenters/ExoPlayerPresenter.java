package exoplayer.Presenters;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.mediarouter.media.MediaRouter;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.av1.Gav1Library;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener;
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
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.dynamite.DynamiteModule;
import exoplayer.AudioProcessors.StereoVolumeAudioProcessor;
import exoplayer.Callbacks.ExoMediaControllerCallback;
import exoplayer.Callbacks.ExoPlaybackPreparer;
import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import exoplayer.ExoRenderersFactory.MyRenderersFactory;
import exoplayer.Listeners.ExoPlayerCastStateListener;
import exoplayer.Listeners.ExoPlayerEventListener;
import exoplayer.Utilities.UriUtil;

import com.smile.karaokeplayer.Presenters.BasePlayerPresenter;
import com.smile.smilelibraries.utilities.ContentUriAccessUtil;

import java.util.ArrayList;
import java.util.List;

public class ExoPlayerPresenter extends BasePlayerPresenter {

    private static final String TAG = "ExoPlayerPresenter";

    private final Activity mActivity;
    private final ExoPlayerPresentView presentView;
    private CastContext castContext;
    private ExoPlayerCastStateListener exoPlayerCastStateListener;

    private MediaControllerCompat mediaControllerCompat;
    private ExoMediaControllerCallback mediaControllerCallback;
    private MediaSessionConnector mediaSessionConnector;

    private StereoVolumeAudioProcessor stereoVolumeAudioProcessor;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private SimpleExoPlayer exoPlayer;

    private CastPlayer castPlayer;
    private int currentCastState;
    private boolean isOnInternet = false;
    private SessionAvailabilityListener mSessionAvailabilityListener;

    private ExoPlayerEventListener mExoPlayerEventListener;
    private Player currentPlayer;
    private int currentItemIndex;

    // instances of the following members have to be saved when configuration changed
    private ArrayList<Integer[]> videoTrackIndicesList = new ArrayList<>();
    private ArrayList<Integer[]> audioTrackIndicesList = new ArrayList<>();

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
            durationSeekBarHandler.postDelayed(durationSeekBarRunnable, 500);
        }
    };

    public interface ExoPlayerPresentView extends BasePlayerPresenter.BasePresentView {
        void setCurrentPlayerToPlayerView();
    }

    public ExoPlayerPresenter(Activity activity, ExoPlayerPresentView presentView) {
        super(activity, presentView);
        mActivity = activity;
        this.presentView = presentView;
        // this.mActivity = (Activity)(this.presentView);

        castContext = null;
        currentCastState = CastState.NO_DEVICES_AVAILABLE;
        if (com.smile.karaokeplayer.BuildConfig.DEBUG) {
            Log.d(TAG, "com.smile.karaokeplayer.BuildConfig.DEBUG");
            try {
                castContext = CastContext.getSharedInstance(mActivity);
            } catch (RuntimeException e) {
                castContext = null;
                Throwable cause = e.getCause();
                while (cause != null) {
                    if (cause instanceof DynamiteModule.LoadingException) {
                        Log.d(TAG, "Failed to get CastContext. Try updating Google Play Services and restart the app.");
                    }
                    cause = cause.getCause();
                }
                // Unknown error. We propagate it.
                Log.d(TAG, "Failed to get CastContext. Unknown error.");
            }
            if (castContext != null) {
                Log.d(TAG, "castContext is " + castContext);
                exoPlayerCastStateListener = new ExoPlayerCastStateListener(mActivity, this);
                currentCastState = castContext.getCastState();
            }
        }
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

    public ExoPlayerPresentView getPresentView() {
        return presentView;
    }

    public void initExoPlayerAndCastPlayer() {
        trackSelector = new DefaultTrackSelector(mActivity, new AdaptiveTrackSelection.Factory());
        trackSelector.setParameters(trackSelectorParameters);

        MyRenderersFactory myRenderersFactory = new MyRenderersFactory(mActivity);
        stereoVolumeAudioProcessor = myRenderersFactory.getStereoVolumeAudioProcessor();

        SimpleExoPlayer.Builder exoPlayerBuilder = new SimpleExoPlayer.Builder(mActivity, myRenderersFactory);
        exoPlayerBuilder.setTrackSelector(trackSelector);
        exoPlayer = exoPlayerBuilder.build();

        // no need. It will ve overridden by MediaSessionConnector
        mExoPlayerEventListener = new ExoPlayerEventListener(mActivity, this);
        exoPlayer.addListener(mExoPlayerEventListener);

        if (castContext != null) {
            castPlayer = new CastPlayer(castContext);
            // castPlayer.addListener(mExoPlayerEventListener); // add different listener later
            mSessionAvailabilityListener = new SessionAvailabilityListener() {
                @Override
                public synchronized void onCastSessionAvailable() {
                    Log.d(TAG, "onCastSessionAvailable() is called.");
                    Log.d(TAG, "onCastSessionAvailable() --> mediaUri = " + mediaUri);
                    if (mediaUri==null || !isOnInternet) {
                        Log.d(TAG, "Stopped casting because mediaUri is null or not online");
                        Log.d(TAG, "Set current player back to exoPlayer");
                        MediaRouter mRouter = MediaRouter.getInstance(mActivity);  // singleton
                        mRouter.unselect(MediaRouter.UNSELECT_REASON_STOPPED);  // stop casting
                        return;
                    }
                    setCurrentPlayer(castPlayer);
                    Log.d(TAG, "Set current player to castPlayer");
                }

                @Override
                public void onCastSessionUnavailable() {
                    Log.d(TAG, "onCastSessionUnavailable() is called.");
                    setCurrentPlayer(exoPlayer);
                    Log.d(TAG, "Set current player to exoPlayer");
                }
            };

            // moved to setSessionAvailabilityListener() method
            // castPlayer.setSessionAvailabilityListener(mSessionAvailabilityListener);
        }

        currentPlayer = exoPlayer; // default is playing video on Android device

        Log.d(TAG, "FfmpegLibrary.isAvailable() = " + FfmpegLibrary.isAvailable());
        Log.d(TAG, "VpxLibrary.isAvailable() = " + VpxLibrary.isAvailable());
        Log.d(TAG, "FlacLibrary.isAvailable() = " + FlacLibrary.isAvailable());
        Log.d(TAG, "OpusLibrary.isAvailable() = " + OpusLibrary.isAvailable());
        Log.d(TAG, "Gav1Library.isAvailable() = " + Gav1Library.isAvailable());
    }

    public void releaseExoPlayerAndCastPlayer() {
        if (exoPlayer != null) {
            exoPlayer.removeListener(mExoPlayerEventListener);
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
        if (castPlayer != null) {
            castPlayer.setSessionAvailabilityListener(null);
            castPlayer.release();
        }

    }

    public SimpleExoPlayer getExoPlayer() {
        return exoPlayer;
    }

    public CastPlayer getCastPlayer() {
        return castPlayer;
    }

    public int getCurrentCastState() {
        return currentCastState;
    }
    public void setCurrentCastState(int currentCastState) {
        this.currentCastState = currentCastState;
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

        // if (mappedTrackInfo.getTrackSupport(audioRendererIndex, audioTrackGroupIndex, audioTrackIndex)
        //         != RendererCapabilities.FORMAT_HANDLED) {
        if (mappedTrackInfo.getTrackSupport(audioRendererIndex, audioTrackGroupIndex, audioTrackIndex)
                 != C.FORMAT_HANDLED) {
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

    @Override
    public void getPlayingMediaInfoAndSetAudioActionSubMenu() {
        Log.d(TAG, "getPlayingMediaInfoAndSetAudioActionSubMenu()");
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
            Log.d(TAG, "videoTrackIdPlayed = " + videoTrackIdPlayed);
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
            if (playingParam.isAutoPlay() || playingParam.isPlaySingleSong() || playingParam.isInSongList()) {
                audioTrackIdPlayed = playingParam.getCurrentAudioTrackIndexPlayed();
                audioChannel = playingParam.getCurrentChannelPlayed();
                Log.d(TAG, "Auto play or playing single song.");
            } else {
                // for open media. do not know the music track and vocal track
                Log.d(TAG, "Do not know the music track and vocal track.");
                // guess
                audioTrackIdPlayed = 1;
                if (numberOfAudioTracks >= 2) {
                    // more than 2 audio tracks
                    audioChannel = CommonConstants.StereoChannel;
                    playingParam.setVocalAudioTrackIndex(audioTrackIdPlayed);
                    playingParam.setVocalAudioChannel(audioChannel);
                    playingParam.setMusicAudioTrackIndex(2);
                    playingParam.setMusicAudioChannel(audioChannel);
                } else {
                    // only one track
                    audioChannel = CommonConstants.LeftChannel;
                    playingParam.setVocalAudioTrackIndex(audioTrackIdPlayed);
                    playingParam.setVocalAudioChannel(audioChannel);
                    playingParam.setMusicAudioTrackIndex(audioTrackIdPlayed);
                    playingParam.setMusicAudioChannel(CommonConstants.RightChannel);
                }
            }

            Log.d(TAG, "audioTrackIdPlayed = " + audioTrackIdPlayed);
            Log.d(TAG, "audioChannel = " + audioChannel);

            if (audioTrackIdPlayed < 0) {
                audioTrackIdPlayed = 1;
            }
            setAudioTrackAndChannel(audioTrackIdPlayed, audioChannel);
        }

        // build R.id.audioTrack submenu
        presentView.buildAudioTrackMenuItem(audioTrackIndicesList.size());

        // update the duration on controller UI
        presentView.update_Player_duration_seekbar(exoPlayer.getDuration());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initializeVariables(Bundle savedInstanceState, Intent callingIntent) {
        Log.d(TAG, "ExoPlayerPresenter --> initializeVariables()");
        super.initializeVariables(savedInstanceState, callingIntent);
        if (savedInstanceState == null) {
            videoTrackIndicesList = new ArrayList<>();
            audioTrackIndicesList = new ArrayList<>();
            trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder(mActivity).build();
        } else {
            videoTrackIndicesList = (ArrayList<Integer[]>)savedInstanceState.getSerializable(PlayerConstants.VideoTrackIndicesListState);
            audioTrackIndicesList = (ArrayList<Integer[]>)savedInstanceState.getSerializable(PlayerConstants.AudioTrackIndicesListState);
            trackSelectorParameters = savedInstanceState.getParcelable(PlayerConstants.TrackSelectorParametersState);
        }
    }

    @Override
    public boolean isSeekable() {
        super.isSeekable();
        boolean result = exoPlayer.isCurrentWindowSeekable();
        return result;
    }

    @Override
    public void setPlayerTime(int progress) {
        exoPlayer.seekTo(progress);
    }

    @Override
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

    @Override
    public void setAudioVolumeInsideVolumeSeekBar(int i) {
        // needed to put inside the presenter
        float currentVolume = 1.0f;
        if (i < PlayerConstants.MaxProgress) {
            currentVolume = (float)(1.0f - (Math.log(PlayerConstants.MaxProgress - i) / Math.log(PlayerConstants.MaxProgress)));
        }
        setAudioVolume(currentVolume);
        //
    }

    @Override
    public int getCurrentProgressForVolumeSeekBar() {
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

    @Override
    public void setAudioTrackAndChannel(int audioTrackIndex, int audioChannel) {
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

            // set audio track
            playingParam.setCurrentAudioTrackIndexPlayed(audioTrackIndex);
            // set audio channel
            playingParam.setCurrentChannelPlayed(audioChannel);
            setAudioVolume(playingParam.getCurrentVolume());
        }
    }

    @Override
    public Uri getValidatedUri(Uri tempUri) {
        Log.d(TAG, "ExoPlayerPresenter.java --> getValidatedUri() is called.");
        return super.getValidatedUri(tempUri);
    }

    @Override
    public void specificPlayerReplayMedia(long currentAudioPosition) {
        // song is playing, paused, or finished playing
        // cannot do the following statement (exoPlayer.setPlayWhenReady(false); )
        // because it will send Play.STATE_ENDED event after the playing has finished
        // but the playing was stopped in the middle of playing then wo'nt send
        // Play.STATE_ENDED event
        // exoPlayer.setPlayWhenReady(false);
        exoPlayer.seekTo(currentAudioPosition);
        switchAudioToVocal();
        // exoPlayer.retry();   // deprecated and removed on 2021-03-27
        exoPlayer.prepare();    // replace exoPlayer.retry();
        //
        exoPlayer.setPlayWhenReady(true);
        Log.d(TAG, "replayMedia()--> exoPlayer.seekTo(currentAudioPosition).");
    }

    @Override
    public void initMediaSessionCompat() {
        // Create a MediaSessionCompat
        super.initMediaSessionCompat();

        // Create a MediaControllerCompat
        mediaControllerCompat = new MediaControllerCompat(mActivity, mediaSessionCompat);
        MediaControllerCompat.setMediaController(mActivity, mediaControllerCompat);
        mediaControllerCallback = new ExoMediaControllerCallback(this);
        mediaControllerCompat.registerCallback(mediaControllerCallback);
        mediaTransportControls = mediaControllerCompat.getTransportControls();

        mediaSessionConnector = new MediaSessionConnector(mediaSessionCompat);
        mediaSessionConnector.setPlayer(exoPlayer);
        mediaSessionConnector.setPlaybackPreparer(new ExoPlaybackPreparer(mActivity, this));
    }

    @Override
    public void releaseMediaSessionCompat() {
        Log.d(TAG, "ExoPlayerPresenter.releaseMediaSessionCompat() is called.");
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

    @Override
    public Intent createSelectFilesToOpenIntent() {
        return ContentUriAccessUtil.createIntentForSelectingFile(false);
    }

    @Override
    public ArrayList<Uri> getUrisListFromIntentPresenter(Intent data) {
        return UriUtil.getUrisListFromIntent(mActivity, data);
    }

    @Override
    public void switchAudioToMusic() {
        if (!playingParam.isInSongList()) {
            // not in the database and show message
            presentView.showMusicAndVocalIsNotSet();
        }
        int audioTrack = playingParam.getMusicAudioTrackIndex();
        int audioChannel = playingParam.getMusicAudioChannel();
        setAudioTrackAndChannel(audioTrack, audioChannel);
    }

    @Override
    public void switchAudioToVocal() {
        if (!playingParam.isInSongList()) {
            // not in the database and show message
            presentView.showMusicAndVocalIsNotSet();
        }
        setAudioTrackAndChannel(playingParam.getVocalAudioTrackIndex(), playingParam.getVocalAudioChannel());
    }

    @Override
    public synchronized void startDurationSeekBarHandler() {
        // start monitor player_duration_seekbar
        durationSeekBarHandler.postDelayed(durationSeekBarRunnable, 200); // delay 200ms
    }

    @Override
    public long getMediaDuration() {
        return exoPlayer.getDuration();
    }

    @Override
    public void removeCallbacksAndMessages() {
        durationSeekBarHandler.removeCallbacksAndMessages(null);
    }

    // methods related to ChromeCast
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public synchronized void setCurrentPlayer(Player currentPlayer) {

        if (currentPlayer == null) {
            return;
        }

        if (this.currentPlayer == currentPlayer) {
            return;
        }

        if (mediaUri == null) {
            return;
        }

        // Player View management.
        presentView.setCurrentPlayerToPlayerView();

        // Player state management.
        long playbackPositionMs = C.TIME_UNSET;
        int windowIndex = C.INDEX_UNSET;
        boolean playWhenReady = false;

        Player previousPlayer = this.currentPlayer;
        if (previousPlayer != null) {
            // Save state from the previous player.
            int playbackState = previousPlayer.getPlaybackState();
            if (playbackState != Player.STATE_ENDED) {
                playbackPositionMs = previousPlayer.getCurrentPosition();
                playWhenReady = previousPlayer.getPlayWhenReady();
                windowIndex = previousPlayer.getCurrentWindowIndex();
                if (windowIndex != currentItemIndex) {
                    playbackPositionMs = C.TIME_UNSET;
                    windowIndex = currentItemIndex;
                }
            }
            // previousPlayer.stop(true);
            stopPlay(); // or pausePlay();
        }

        this.currentPlayer = currentPlayer;

        if (this.currentPlayer == exoPlayer) {
            Log.d(TAG, "exoPlayer startPlay()");
            startPlay();
        } else {
            // Playback transition.
            if (castPlayer.getCurrentTimeline().isEmpty()) {
                // has not play yet
                Log.d(TAG, "getCurrentTimeline() is Empty()");

                // removed on 2021-03-27
                // MediaItemConverter mediaItemConverter = new DefaultMediaItemConverter();
                // MediaQueueItem mediaQueueItem;
                //
                MediaItem mediaItem = new MediaItem.Builder()
                        .setUri(mediaUri)
                        .setMediaMetadata(new MediaMetadata.Builder().setTitle("Video Casted").build())
                        .setMimeType(MimeTypes.BASE_TYPE_VIDEO)
                        // .setDrmConfiguration(null)
                        .build();
                // removed on 2021-03-27
                // mediaQueueItem = mediaItemConverter.toMediaQueueItem(mediaItem);
                /*
                // added for testing
                mediaItem = new MediaItem.Builder()
                    .setUri("https://html5demos.com/assets/dizzy.mp4")
                    .setMediaMetadata(new MediaMetadata.Builder().setTitle("Clear MP4: Dizzy").build())
                    .setMimeType(MimeTypes.VIDEO_MP4)
                    .build();
                // removed on 2021-03-27
                // mediaQueueItem = mediaItemConverter.toMediaQueueItem(mediaItem);
                */
                // Log.d(TAG, "mediaQueueItem = " + mediaQueueItem);    // removed on 2021-03-27
                Log.d(TAG, "windowIndex = " + windowIndex);
                // deprecated // removed on 2021-03-27
                // castPlayer.loadItems(new MediaQueueItem[] {mediaQueueItem}, windowIndex, C.TIME_UNSET, playingParam.getRepeatStatus());
                //
                List<MediaItem> mediaItems = new ArrayList<>();
                mediaItems.add(mediaItem);
                castPlayer.setMediaItems(mediaItems, windowIndex, C.TIME_UNSET);
                castPlayer.setRepeatMode(playingParam.getRepeatStatus());
                //
                castPlayer.setPlayWhenReady(playWhenReady);
            } else {
                // already played before
                Log.d(TAG, "getCurrentTimeline() is not Empty()");
            }
        }
        // Playback transition.
        if (windowIndex != C.INDEX_UNSET) {
            Log.d(TAG, "windowIndex != C.INDEX_UNSET");
            currentPlayer.seekTo(playbackPositionMs);
            currentPlayer.setPlayWhenReady(playWhenReady);
        }
    }

    // ChromeCast methods
    public void setSessionAvailabilityListener() {
        if (castPlayer!=null && mSessionAvailabilityListener!=null) {
            castPlayer.setSessionAvailabilityListener(mSessionAvailabilityListener);
        }
    }
    public void releaseSessionAvailabilityListener() {
        if (castPlayer!=null) {
            castPlayer.setSessionAvailabilityListener(null);
        }
    }
    public void addBaseCastStateListener() {
        Log.d(TAG, "addBaseCastStateListener() is called.");
        Log.d(TAG, "castContext = " + castContext);
        if (castContext!=null && exoPlayerCastStateListener!=null) {
            castContext.addCastStateListener(exoPlayerCastStateListener);
            Log.d(TAG, "castContext.addCastStateListener(baseCastStateListener)");
        }
    }
    public void removeBaseCastStateListener() {
        Log.d(TAG, "removeBaseCastStateListener() is called.");
        if (castContext!=null && exoPlayerCastStateListener!=null) {
            castContext.removeCastStateListener(exoPlayerCastStateListener);
            Log.d(TAG, "castContext.removeCastStateListener(baseCastStateListener)");
        }
    }
}
