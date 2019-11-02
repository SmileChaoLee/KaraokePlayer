package com.smile.karaokeplayer.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.audio.AudioListener;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.smile.karaokeplayer.AudioProcessor_implement.StereoVolumeAudioProcessor;
import com.smile.karaokeplayer.BuildConfig;
import com.smile.karaokeplayer.ExoRenderersFactory.MyRenderersFactory;
import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.Models.SongListSQLite;
import com.smile.karaokeplayer.Models.VerticalSeekBar;
import com.smile.karaokeplayer.R;
import com.smile.karaokeplayer.SmileApplication;
import com.smile.karaokeplayer.SongListActivity;
import com.smile.smilelibraries.privacy_policy.PrivacyPolicyUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ExoPlayerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ExoPlayerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExoPlayerFragment extends Fragment {

    private static final String TAG = new String(".ExoPlayerFragment");
    private static final String LOG_TAG = new String("MediaSessionCompatTag");
    private static final String PlayingParamOrigin = "PlayingParamOrigin";

    private static final String PlayingParamState = "PlayingParam";
    private static final String TrackSelectorParametersState = "TrackSelectorParameters";
    private static final String NumberOfVideoTracksState = "NumberOfVideoTracks";
    private static final String NumberOfAudioTracksState = "NumberOfAudioTracks";
    private static final String PublicSongListState = "PublicSongList";
    private static final String MediaUriState = "MediaUri";
    private static final String CanShowNotSupportedFormatState = "CanShowNotSupportedFormat";
    private static final String VideoTrackIndicesListState = "VideoTrackIndicesList";
    private static final String AudioTrackIndicesListState = "AudioTrackIndicesList";

    private static final int PrivacyPolicyActivityRequestCode = 10;
    private static final int FILE_READ_REQUEST_CODE = 1;
    private static final int SONG_LIST_ACTIVITY_CODE = 2;
    private static final int ExoPlayerView_Timeout = 10000;  //  10 seconds
    private static final int noVideoTrack = -1;
    private static final int noAudioTrack = -1;
    private static final int noAudioChannel = -1;
    private static final int maxProgress = 100;
    private static final float timesOfVolumeBarForPortrait = 1.5f;
    private static final int MusicOrVocalUnknown = 0;
    private static final int PlayingMusic = 1;
    private static final int PlayingVocal = 2;
    private static final int NoRepeatPlaying = 0;
    private static final int RepeatOneSong = 1;
    private static final int RepeatAllSongs = 2;

    private float textFontSize;
    private float fontScale;
    private float toastTextSize;

    private Context callingContext;
    private View fragmentView;

    private Toolbar supportToolbar;  // use customized ToolBar
    private ImageButton volumeImageButton;
    private VerticalSeekBar volumeSeekBar;
    private ImageButton switchToMusicImageButton;
    private ImageButton switchToVocalImageButton;
    private ImageButton repeatImageButton;
    private int volumeSeekBarHeightForLandscape;

    private Menu mainMenu;
    // submenu of file
    private MenuItem autoPlayMenuItem;
    private MenuItem openMenuItem;
    private MenuItem closeMenuItem;
    // submenu of action
    private MenuItem playMenuItem;
    private MenuItem pauseMenuItem;
    private MenuItem stopMenuItem;
    private MenuItem replayMenuItem;
    private MenuItem toTvMenuItem;
    // submenu of audio
    private MenuItem audioTrackMenuItem;
    private boolean isAudioTrackMenuItemPressed;
    // submenu of channel
    private MenuItem leftChannelMenuItem;
    private MenuItem rightChannelMenuItem;
    private MenuItem stereoChannelMenuItem;

    private MediaSessionCompat mediaSessionCompat;
    private MediaControllerCompat mediaControllerCompat;
    private MediaControllerCallback mediaControllerCallback;
    private MediaControllerCompat.TransportControls mediaTransportControls;
    private MediaSessionConnector mediaSessionConnector;

    private PlayerView videoExoPlayerView;
    private DataSource.Factory dataSourceFactory;
    private StereoVolumeAudioProcessor stereoVolumeAudioProcessor;
    private MyRenderersFactory myRenderersFactory;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private MediaSource mediaSource;
    private SimpleExoPlayer exoPlayer;

    private LinearLayout linearLayout_for_ads;
    private AdView bannerAdView;
    private LinearLayout messageLinearLayout;
    private TextView bufferingStringTextView;
    private Animation animationText;
    private LinearLayout nativeAdsLinearLayout;
    private TextView nativeAdsStringTextView;

    private LinearLayout audioControllerView;
    private ImageButton previousMediaImageButton;
    private ImageButton playMediaImageButton;
    private ImageButton replayMediaImageButton;
    private ImageButton pauseMediaImageButton;
    private ImageButton stopMediaImageButton;
    private ImageButton nextMediaImageButton;
    private TextView exo_position_TextView;
    private TextView exo_duration_TextView;

    private int colorRed;
    private int colorTransparent;

    // instances of the following members have to be saved when configuration changed
    private Uri mediaUri;
    private int numberOfVideoTracks;
    private int numberOfAudioTracks;
    private ArrayList<Integer[]> videoTrackIndicesList = new ArrayList<>();
    private ArrayList<Integer[]> audioTrackIndicesList = new ArrayList<>();
    private ArrayList<SongInfo> publicSongList;
    private PlayingParameters playingParam;
    private boolean canShowNotSupportedFormat;
    private SongInfo songInfo;

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String IsPlaySingleSongState = "IsPlaySingleSong";
    public static final String SongInfoState = "SongInfo";

    private OnFragmentInteractionListener mListener;

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void setSupportActionBarForFragment(Toolbar toolbar);
        ActionBar getSupportActionBarForFragment();
        void onExitFragment();
    }

    public ExoPlayerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param isPlaySingleSong Parameter 1.
     * @param songInfo Parameter 2.
     * @return A new instance of fragment ExoPlayerFragment.
     */
    public static ExoPlayerFragment newInstance(boolean isPlaySingleSong, SongInfo songInfo) {
        ExoPlayerFragment fragment = new ExoPlayerFragment();
        Bundle args = new Bundle();
        args.putBoolean(IsPlaySingleSongState, isPlaySingleSong);
        args.putParcelable(SongInfoState, songInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        callingContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() is called");

        super.onCreate(savedInstanceState);
        // retain install of fragment when recreate
        // onCreate() will not be called when configuration changed because of setRetainInstance(true);
        // setRetainInstance(true);
        // ExoPlayer library has bugs when fragment retains instance (etRetainInstance(true);)
        // so retains instance has to be set to false
        setRetainInstance(false);
        setHasOptionsMenu(true);

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(callingContext, SmileApplication.FontSize_Scale_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(callingContext, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(callingContext, SmileApplication.FontSize_Scale_Type, 0.0f);
        toastTextSize = 0.7f * textFontSize;

        colorRed = ContextCompat.getColor(callingContext, R.color.red);
        colorTransparent = ContextCompat.getColor(callingContext, android.R.color.transparent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView() is called");

        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_exoplayer, container, false);
        if (fragmentView == null) {
            // exit
            mListener.onExitFragment();
        }

        initializeVariables(savedInstanceState);

        // Video player view
        videoExoPlayerView = fragmentView.findViewById(R.id.videoExoPlayerView);
        videoExoPlayerView.setVisibility(View.VISIBLE);

        initExoPlayer();
        initMediaSessionCompat();

        // use custom toolbar
        supportToolbar = fragmentView.findViewById(R.id.custom_toolbar);
        mListener.setSupportActionBarForFragment(supportToolbar);
        ActionBar actionBar = mListener.getSupportActionBarForFragment();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        supportToolbar.setVisibility(View.VISIBLE);

        volumeSeekBar = fragmentView.findViewById(R.id.volumeSeekBar);
        // get default height of volumeBar from dimen.xml
        volumeSeekBarHeightForLandscape = volumeSeekBar.getLayoutParams().height;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // if orientation is portrait, then double the height of volumeBar
            volumeSeekBar.getLayoutParams().height = (int) ((float)volumeSeekBarHeightForLandscape * timesOfVolumeBarForPortrait);
        }
        // uses dimens.xml for different devices' sizes
        volumeSeekBar.setVisibility(View.INVISIBLE); // default is not showing
        volumeSeekBar.setMax(maxProgress);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                volumeSeekBar.setProgressAndThumb(i);
                // float currentVolume = (float)i / (float)maxProgress;
                float currentVolume = 1.0f;
                if (i < maxProgress) {
                    currentVolume = (float)(1.0f - (Math.log(maxProgress - i) / Math.log(maxProgress)));
                }
                playingParam.setCurrentVolume(currentVolume);
                setAudioVolume(currentVolume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        // int currentProgress = (int)(playingParam.getCurrentVolume() * maxProgress);
        int currentProgress;
        float currentVolume = playingParam.getCurrentVolume();
        if ( currentVolume >= 1.0f) {
            currentProgress = maxProgress;
        } else {
            currentProgress = maxProgress - (int)Math.pow(maxProgress, (1-currentVolume));
            currentProgress = Math.max(0, currentProgress);
        }
        volumeSeekBar.setProgressAndThumb(currentProgress);

        int imageButtonHeight = (int)(textFontSize * 1.5f);
        volumeImageButton = supportToolbar.findViewById(R.id.volumeImageButton);
        volumeImageButton.getLayoutParams().height = imageButtonHeight;
        volumeImageButton.getLayoutParams().width = imageButtonHeight;

        repeatImageButton = fragmentView.findViewById(R.id.repeatImageButton);
        repeatImageButton.getLayoutParams().height = imageButtonHeight;
        repeatImageButton.getLayoutParams().width = imageButtonHeight;

        switchToMusicImageButton = fragmentView.findViewById(R.id.switchToMusicImageButton);
        switchToMusicImageButton.getLayoutParams().height = imageButtonHeight;
        switchToMusicImageButton.getLayoutParams().width = imageButtonHeight;

        switchToVocalImageButton = fragmentView.findViewById(R.id.switchToVocalImageButton);
        switchToVocalImageButton.getLayoutParams().height = imageButtonHeight;
        switchToVocalImageButton.getLayoutParams().width = imageButtonHeight;

        setToolbarImageButtonStatus();

        int volumeSeekBarWidth = (int)(textFontSize * 2.0f);
        volumeSeekBar.getLayoutParams().width = volumeSeekBarWidth;
        supportToolbar.getLayoutParams().height = volumeImageButton.getLayoutParams().height + volumeSeekBar.getLayoutParams().height;
        Log.d(TAG, "supportToolbar = " + supportToolbar.getLayoutParams().height);

        linearLayout_for_ads = fragmentView.findViewById(R.id.linearLayout_for_ads);
        if (!SmileApplication.googleAdMobBannerID.isEmpty()) {
            try {
                bannerAdView = new AdView(callingContext);
                bannerAdView.setAdSize(AdSize.BANNER);
                bannerAdView.setAdUnitId(SmileApplication.googleAdMobBannerID);
                linearLayout_for_ads.addView(bannerAdView);
                AdRequest adRequest = new AdRequest.Builder().build();
                bannerAdView.loadAd(adRequest);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            linearLayout_for_ads.setVisibility(View.GONE);
        }

        // message area
        messageLinearLayout = fragmentView.findViewById(R.id.messageLinearLayout);
        messageLinearLayout.setVisibility(View.GONE);
        bufferingStringTextView = fragmentView.findViewById(R.id.bufferingStringTextView);
        ScreenUtil.resizeTextSize(bufferingStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        animationText = new AlphaAnimation(0.0f,1.0f);
        animationText.setDuration(500);
        animationText.setStartOffset(0);
        animationText.setRepeatMode(Animation.REVERSE);
        animationText.setRepeatCount(Animation.INFINITE);

        nativeAdsLinearLayout = fragmentView.findViewById(R.id.nativeAdsLinearLayout);
        nativeAdsStringTextView = fragmentView.findViewById(R.id.nativeAdsStringTextView);
        ScreenUtil.resizeTextSize(nativeAdsStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);

        audioControllerView = fragmentView.findViewById(R.id.audioControllerView);
        previousMediaImageButton = fragmentView.findViewById(R.id.previousMediaImageButton);
        previousMediaImageButton.getLayoutParams().height = imageButtonHeight;
        previousMediaImageButton.getLayoutParams().width = imageButtonHeight;

        playMediaImageButton = fragmentView.findViewById(R.id.playMediaImageButton);
        playMediaImageButton.getLayoutParams().height = imageButtonHeight;
        playMediaImageButton.getLayoutParams().width = imageButtonHeight;
        playMediaImageButton.setVisibility(View.VISIBLE);

        pauseMediaImageButton = fragmentView.findViewById(R.id.pauseMediaImageButton);
        pauseMediaImageButton.getLayoutParams().height = imageButtonHeight;
        pauseMediaImageButton.getLayoutParams().width = imageButtonHeight;
        pauseMediaImageButton.setVisibility(View.GONE);

        replayMediaImageButton = fragmentView.findViewById(R.id.replayMediaImageButton);
        replayMediaImageButton.getLayoutParams().height = imageButtonHeight;
        replayMediaImageButton.getLayoutParams().width = imageButtonHeight;

        stopMediaImageButton = fragmentView.findViewById(R.id.stopMediaImageButton);
        stopMediaImageButton.getLayoutParams().height = imageButtonHeight;
        stopMediaImageButton.getLayoutParams().width = imageButtonHeight;

        nextMediaImageButton = fragmentView.findViewById(R.id.nextMediaImageButton);
        nextMediaImageButton.getLayoutParams().height = imageButtonHeight;
        nextMediaImageButton.getLayoutParams().width = imageButtonHeight;

        float durationTextSize = textFontSize * 0.6f;
        exo_position_TextView = fragmentView.findViewById(R.id.exo_position);
        ScreenUtil.resizeTextSize(exo_position_TextView, durationTextSize, SmileApplication.FontSize_Scale_Type);

        exo_duration_TextView = fragmentView.findViewById(R.id.exo_duration);
        ScreenUtil.resizeTextSize(exo_duration_TextView, durationTextSize, SmileApplication.FontSize_Scale_Type);


        setOnClickEvents();

        hideBannerAds();
        showNativeAds();

        if (songInfo == null) {
            Log.d(TAG, "songInfo is null");
        } else {
            Log.d(TAG, "songInfo is not null");
        }
        if (mediaUri != null) {
            int playbackState = playingParam.getCurrentPlaybackState();
            Log.d(TAG, "onActivityCreated() --> playingParam.getCurrentPlaybackState() = " + playbackState);
            if (playbackState != PlaybackStateCompat.STATE_NONE) {
                // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
                // pass the saved instance of playingParam to
                // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)
                Bundle playingParamOriginExtras = new Bundle();
                playingParamOriginExtras.putParcelable(PlayingParamOrigin, playingParam);
                mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
            }
        } else {
            if (playingParam.isPlaySingleSong()) {
                if (songInfo != null) {
                    playingParam.setAutoPlay(false);
                    playSingleSong(songInfo);
                }
            }
        }

        return fragmentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated() is called");
    }

    public void onInteractionProcessed(String msgString) {
        if (mListener != null) {
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        Log.d(TAG, "onCreateOptionsMenu() is called");
        // Inflate the menu; this adds items to the action bar if it is present.

        menuInflater.inflate(R.menu.menu_main, menu);
        mainMenu = menu;

        // according to the above explanations, the following statement will fit every situation
        ScreenUtil.resizeMenuTextSize(menu, fontScale);

        // submenu of file
        autoPlayMenuItem = menu.findItem(R.id.autoPlay);
        openMenuItem = menu.findItem(R.id.open);
        closeMenuItem = menu.findItem(R.id.close);
        // submenu of action
        playMenuItem = menu.findItem(R.id.play);
        pauseMenuItem = menu.findItem(R.id.pause);
        stopMenuItem = menu.findItem(R.id.stop);
        replayMenuItem = menu.findItem(R.id.replay);
        toTvMenuItem = menu.findItem(R.id.toTV);

        // submenu of audio
        audioTrackMenuItem = menu.findItem(R.id.audioTrack);
        // submenu of channel
        leftChannelMenuItem = menu.findItem(R.id.leftChannel);
        rightChannelMenuItem = menu.findItem(R.id.rightChannel);
        stereoChannelMenuItem = menu.findItem(R.id.stereoChannel);

        if (playingParam.isPlaySingleSong()) {
            MenuItem fileMenuItem = menu.findItem(R.id.file);
            fileMenuItem.setVisible(false);
            MenuItem actionMenuItem = menu.findItem(R.id.action);
            actionMenuItem.setVisible(true);
            actionMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            audioTrackMenuItem.setVisible(false);
            MenuItem channelMenuItem = menu.findItem(R.id.channel);
            channelMenuItem.setVisible(false);
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        isAudioTrackMenuItemPressed = false;
        Log.d(TAG, "onPrepareOptionsMenu() is called.");
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        videoExoPlayerView.setControllerShowTimeoutMs(-1); // cancel the time out

        boolean isAutoPlay;
        int currentChannelPlayed = playingParam.getCurrentChannelPlayed();
        int mCurrentState;

        if (item.hasSubMenu()) {
            SubMenu subMenu = item.getSubMenu();
            subMenu.clearHeader();
        }
        int id = item.getItemId();
        switch (id) {
            case R.id.file:
                autoPlayMenuItem.setCheckable(true);
                if (playingParam.isAutoPlay()) {
                    autoPlayMenuItem.setChecked(true);
                    openMenuItem.setEnabled(false);
                    closeMenuItem.setEnabled(false);
                } else {
                    autoPlayMenuItem.setChecked(false);
                    openMenuItem.setEnabled(true);
                    closeMenuItem.setEnabled(true);
                }
                break;
            case R.id.autoPlay:
                // item.isChecked() return the previous value
                isAutoPlay = !playingParam.isAutoPlay();
                canShowNotSupportedFormat = true;
                if (isAutoPlay) {
                    publicSongList = readPublicSongList();
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
                        String msg = getString(R.string.noPlaylistString);
                        ScreenUtil.showToast(callingContext, msg, toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
                    }
                } else {
                    playingParam.setAutoPlay(isAutoPlay);
                }
                setToolbarImageButtonStatus();
                break;
            case R.id.songList:
                Intent songListIntent = new Intent(callingContext, SongListActivity.class);
                startActivityForResult(songListIntent, SONG_LIST_ACTIVITY_CODE);
                break;
            case R.id.open:
                if (!playingParam.isAutoPlay()) {
                    // isMediaSourcePrepared = false;
                    selectFileToOpen();
                }
                break;
            case R.id.close:
                stopPlay();
                break;
            case R.id.privacyPolicy:
                PrivacyPolicyUtil.startPrivacyPolicyActivity(getActivity(), SmileApplication.PrivacyPolicyUrl, PrivacyPolicyActivityRequestCode);
                break;
            case R.id.exit:
                if (mListener != null) {
                    mListener.onExitFragment();
                }
                break;
            case R.id.action:
                Log.d(TAG, "R.id.action --> mediaUri = " + mediaUri);
                Log.d(TAG, "R.id.action --> numberOfAudioTracks = " + numberOfAudioTracks);
                if ( (mediaUri != null) && (numberOfAudioTracks >0) ) {
                    Log.d(TAG, "R.id.action --> mediaUri is not null.");
                    playMenuItem.setEnabled(true);
                    pauseMenuItem.setEnabled(true);
                    stopMenuItem.setEnabled(true);
                    replayMenuItem.setEnabled(true);
                    toTvMenuItem.setEnabled(true);
                    mCurrentState = playingParam.getCurrentPlaybackState();
                    if (mCurrentState == PlaybackStateCompat.STATE_PLAYING) {
                        playMenuItem.setCheckable(true);
                        playMenuItem.setChecked(true);
                    } else {
                        playMenuItem.setCheckable(false);
                    }
                    if (mCurrentState == PlaybackStateCompat.STATE_PAUSED) {
                        pauseMenuItem.setCheckable(true);
                        pauseMenuItem.setChecked(true);
                    } else {
                        pauseMenuItem.setCheckable(false);
                    }
                    if ((mediaUri != null) && (mCurrentState == PlaybackStateCompat.STATE_NONE)) {
                        stopMenuItem.setCheckable(true);
                        stopMenuItem.setChecked(true);
                    } else {
                        stopMenuItem.setCheckable(false);
                    }
                    // toTvMenuItem
                } else {
                    Log.d(TAG, "R.id.action --> mediaUri is null or numberOfAudioTracks = 0.");
                    playMenuItem.setEnabled(false);
                    pauseMenuItem.setEnabled(false);
                    stopMenuItem.setEnabled(false);
                    replayMenuItem.setEnabled(false);
                    toTvMenuItem.setEnabled(false);
                }
                break;
            case R.id.play:
                startPlay();
                break;
            case R.id.pause:
                pausePlay();
                break;
            case R.id.stop:
                // if (mCurrentState != PlaybackStateCompat.STATE_STOPPED) {
                // mCurrentState = PlaybackStateCompat.STATE_STOPPED when finished playing
                stopPlay();
                break;
            case R.id.replay:
                replayMedia();
                break;
            case R.id.toTV:
                break;
            case R.id.audioTrack:
                isAudioTrackMenuItemPressed = true;
                // if there are audio tracks
                SubMenu subMenu = item.getSubMenu();
                ScreenUtil.resizeMenuTextSize(subMenu, fontScale);
                // check if MenuItems of audioTrack need CheckBox
                for (int i=0; i<subMenu.size(); i++) {
                    MenuItem mItem = subMenu.getItem(i);
                    // audio track index start from 1 for user interface
                    if ( (i+1) == playingParam.getCurrentAudioTrackIndexPlayed() ) {
                        mItem.setCheckable(true);
                        mItem.setChecked(true);
                    } else {
                        mItem.setCheckable(false);
                    }
                }
                //
                break;
            case R.id.channel:
                // if ( (mediaSource != null) && (numberOfAudioTracks>0) ) {
                if ( (mediaUri != null) && (numberOfAudioTracks >0) ) {
                    leftChannelMenuItem.setEnabled(true);
                    rightChannelMenuItem.setEnabled(true);
                    stereoChannelMenuItem.setEnabled(true);
                    if (playingParam.isMediaSourcePrepared()) {
                        if (currentChannelPlayed == SmileApplication.leftChannel) {
                            leftChannelMenuItem.setCheckable(true);
                            leftChannelMenuItem.setChecked(true);
                        } else {
                            leftChannelMenuItem.setCheckable(false);
                            leftChannelMenuItem.setChecked(false);
                        }
                        if (currentChannelPlayed == SmileApplication.rightChannel) {
                            rightChannelMenuItem.setCheckable(true);
                            rightChannelMenuItem.setChecked(true);
                        } else {
                            rightChannelMenuItem.setCheckable(false);
                            rightChannelMenuItem.setChecked(false);
                        }
                        if (currentChannelPlayed == SmileApplication.stereoChannel) {
                            stereoChannelMenuItem.setCheckable(true);
                            stereoChannelMenuItem.setChecked(true);
                        } else {
                            stereoChannelMenuItem.setCheckable(false);
                            stereoChannelMenuItem.setChecked(false);
                        }
                    } else {
                        leftChannelMenuItem.setCheckable(false);
                        leftChannelMenuItem.setChecked(false);
                        rightChannelMenuItem.setCheckable(false);
                        rightChannelMenuItem.setChecked(false);
                        stereoChannelMenuItem.setCheckable(false);
                        stereoChannelMenuItem.setChecked(false);
                    }
                } else {
                    leftChannelMenuItem.setEnabled(false);
                    rightChannelMenuItem.setEnabled(false);
                    stereoChannelMenuItem.setEnabled(false);
                }

                break;
            case R.id.leftChannel:
                playingParam.setCurrentChannelPlayed(SmileApplication.leftChannel);
                setAudioVolume(playingParam.getCurrentVolume());
                break;
            case R.id.rightChannel:
                playingParam.setCurrentChannelPlayed(SmileApplication.rightChannel);
                setAudioVolume(playingParam.getCurrentVolume());
                break;
            case R.id.stereoChannel:
                playingParam.setCurrentChannelPlayed(SmileApplication.stereoChannel);
                setAudioVolume(playingParam.getCurrentVolume());
                break;
        }

        // deal with switching audio track
        if ( (isAudioTrackMenuItemPressed) && (id != audioTrackMenuItem.getItemId()) ) {
            Log.d(TAG, "isAudioTrackMenuItemPressed is true");
            if ((mediaUri != null) && (numberOfAudioTracks > 0)) {
                if (item.getTitle() != null) {
                    String itemTitle = item.getTitle().toString();
                    Log.d(TAG, "itemTitle = " + itemTitle);
                    if (audioTrackMenuItem.hasSubMenu()) {
                        SubMenu subMenu = audioTrackMenuItem.getSubMenu();
                        int menuSize = subMenu.size();
                        for (int index = 0; index < menuSize; index++) {
                            if (itemTitle.equals(subMenu.getItem(index).getTitle().toString())) {
                                setAudioTrackAndChannel(index + 1, currentChannelPlayed);
                                break;
                            }
                        }
                    }
                }
            }
            isAudioTrackMenuItemPressed = false;
        }

        videoExoPlayerView.setControllerShowTimeoutMs(ExoPlayerView_Timeout); // cancel the time out

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Log.d(TAG,"ExoPlayerFragment-->onConfigurationChanged() is called.");
        super.onConfigurationChanged(newConfig);
        // mainMenu.close();
        closeMenu(mainMenu);

        // reset the heights of volumeBar and supportToolbar
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // if orientation is portrait, then double the height of volumeBar
            volumeSeekBar.getLayoutParams().height = (int) ((float)volumeSeekBarHeightForLandscape * timesOfVolumeBarForPortrait);
        } else {
            volumeSeekBar.getLayoutParams().height = volumeSeekBarHeightForLandscape;
        }
        supportToolbar.getLayoutParams().height = volumeImageButton.getLayoutParams().height + volumeSeekBar.getLayoutParams().height;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG,"ExoPlayerFragment-->onSaveInstanceState() is called.");

        outState.putInt(NumberOfVideoTracksState, numberOfVideoTracks);
        outState.putInt(NumberOfAudioTracksState, numberOfAudioTracks);
        outState.putSerializable(VideoTrackIndicesListState, videoTrackIndicesList);
        outState.putSerializable(AudioTrackIndicesListState, audioTrackIndicesList);
        outState.putParcelableArrayList(PublicSongListState, publicSongList);

        outState.putParcelable(MediaUriState, mediaUri);
        playingParam.setCurrentAudioPosition(exoPlayer.getContentPosition());
        outState.putParcelable(PlayingParamState, playingParam);
        outState.putBoolean(CanShowNotSupportedFormatState, canShowNotSupportedFormat);
        outState.putParcelable(SongInfoState, songInfo);

        trackSelectorParameters = trackSelector.getParameters();
        outState.putParcelable(TrackSelectorParametersState, trackSelectorParameters);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"ExoPlayerFragment-->onDestroy() is called.");
        releaseMediaSessionCompat();
        releaseExoPlayer();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == FILE_READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData()
            if (data != null) {
                mediaUri = data.getData();
                Log.i(TAG, "Uri: " + mediaUri.toString());

                if ( (mediaUri == null) || (Uri.EMPTY.equals(mediaUri)) ) {
                    return;
                }
                playingParam.setCurrentVideoTrackIndexPlayed(0);

                int currentAudioRederer = 0;
                playingParam.setMusicAudioTrackIndex(currentAudioRederer);
                playingParam.setVocalAudioTrackIndex(currentAudioRederer);
                playingParam.setCurrentAudioTrackIndexPlayed(currentAudioRederer);

                playingParam.setMusicAudioChannel(SmileApplication.leftChannel);
                playingParam.setVocalAudioChannel(SmileApplication.stereoChannel);
                playingParam.setCurrentChannelPlayed(SmileApplication.stereoChannel);

                playingParam.setCurrentAudioPosition(0);
                playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);
                playingParam.setMediaSourcePrepared(false);

                // music or vocal is unknown
                playingParam.setMusicOrVocalOrNoSetting(MusicOrVocalUnknown);
                // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
                // pass the saved instance of playingParam to
                // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)
                Bundle playingParamOriginExtras = new Bundle();
                playingParamOriginExtras.putParcelable(PlayingParamOrigin, playingParam);
                mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
                /*
                String BEAR_URI = "asset:///roadtrip-vp92-10bit.webm";
                mediaUri = Uri.parse(BEAR_URI);
                mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
                */
            }
            return;
        }
    }

    private void setOnClickEvents() {

        volumeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int visibility = volumeSeekBar.getVisibility();
                if ( (visibility == View.GONE) || (visibility == View.INVISIBLE) ) {
                    volumeSeekBar.setVisibility(View.VISIBLE);
                } else {
                    volumeSeekBar.setVisibility(View.INVISIBLE);
                }
            }
        });

        repeatImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int repeatStatus = playingParam.getRepeatStatus();
                switch (repeatStatus) {
                    case NoRepeatPlaying:
                        // switch to repeat one song
                        playingParam.setRepeatStatus(RepeatOneSong);
                        break;
                    case RepeatOneSong:
                        // switch to repeat song list
                        playingParam.setRepeatStatus(RepeatAllSongs);
                        break;
                    case RepeatAllSongs:
                        // switch to no repeat
                        playingParam.setRepeatStatus(NoRepeatPlaying);
                        break;
                }
                setToolbarImageButtonStatus();
            }
        });

        switchToMusicImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchAudioToMusic();
            }
        });

        switchToVocalImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchAudioToVocal();
            }
        });

        previousMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( (publicSongList == null) || (!playingParam.isAutoPlay())) {
                    return;
                }
                int publicSongListSize = publicSongList.size();
                int nextIndex = playingParam.getPublicNextSongIndex();
                int repeatStatus = playingParam.getRepeatStatus();
                nextIndex = nextIndex - 2;
                if (nextIndex < 0) {
                    // is going to play the last one
                    nextIndex = publicSongListSize - 1; // the last one
                }
                if (repeatStatus == RepeatOneSong) {
                    // because in startAutoPlay() will subtract 1 from next index
                    nextIndex++;
                }
                playingParam.setPublicNextSongIndex(nextIndex);

                startAutoPlay();
            }
        });

        playMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlay();
            }
        });

        pauseMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pausePlay();
            }
        });

        replayMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replayMedia();
            }
        });

        stopMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlay();
            }
        });

        nextMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( (publicSongList == null) || (!playingParam.isAutoPlay())) {
                    return;
                }
                int publicSongListSize = publicSongList.size();
                int nextIndex = playingParam.getPublicNextSongIndex();
                int repeatStatus = playingParam.getRepeatStatus();
                if (repeatStatus == RepeatOneSong) {
                    nextIndex++;
                }
                if (nextIndex > publicSongListSize) {
                    // it is playing the last one right now
                    // so it is going to play the first one
                    playingParam.setPublicNextSongIndex(0);
                } else {
                    playingParam.setPublicNextSongIndex(nextIndex);
                }
                startAutoPlay();
            }
        });

        supportToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int visibility = v.getVisibility();
                if (visibility == View.VISIBLE) {
                    videoExoPlayerView.hideController();
                }
            }
        });

        videoExoPlayerView.setControllerShowTimeoutMs(ExoPlayerView_Timeout);
        videoExoPlayerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int visibility) {
                if (visibility == View.VISIBLE) {
                    // use custom toolbar
                    supportToolbar.setVisibility(View.VISIBLE);
                    hideBannerAds();
                } else {
                    // use custom toolbar
                    supportToolbar.setVisibility(View.GONE);
                    // mainMenu.close();
                    closeMenu(mainMenu);
                    showBannerAds();
                }
                volumeSeekBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void closeMenu(Menu menu) {
        if (menu == null) {
            return;
        }
        MenuItem menuItem;
        Menu subMenu;
        int mSize = menu.size();
        for (int i=0; i<mSize; i++) {
            menuItem = menu.getItem(i);
            subMenu = menuItem.getSubMenu();
            closeMenu(subMenu);
        }
        menu.close();
    }

    private void setToolbarImageButtonStatus() {
        boolean isAutoPlay = playingParam.isAutoPlay();
        boolean isPlayingSingleSong = playingParam.isPlaySingleSong();
        if ( isAutoPlay || isPlayingSingleSong ) {
            switchToMusicImageButton.setEnabled(true);
            switchToMusicImageButton.setVisibility(View.VISIBLE);
            switchToVocalImageButton.setEnabled(true);
            switchToVocalImageButton.setVisibility(View.VISIBLE);
        } else {
            switchToMusicImageButton.setEnabled(false);
            switchToMusicImageButton.setVisibility(View.GONE);
            switchToVocalImageButton.setEnabled(false);
            switchToVocalImageButton.setVisibility(View.GONE);
        }

        // repeatImageButton
        int repeatStatus = playingParam.getRepeatStatus();
        switch (repeatStatus) {
            case NoRepeatPlaying:
                // no repeat but show symbol of repeat all song with transparent background
                repeatImageButton.setImageResource(R.drawable.repeat_all_white);
                break;
            case RepeatOneSong:
                // repeat one song
                repeatImageButton.setImageResource(R.drawable.repeat_one_white);
                break;
            case RepeatAllSongs:
                // repeat all song list
                repeatImageButton.setImageResource(R.drawable.repeat_all_white);
                break;
        }
        if (repeatStatus == NoRepeatPlaying) {
            repeatImageButton.setBackgroundColor(colorTransparent);
        } else {
            repeatImageButton.setBackgroundColor(colorRed);
        }
    }

    private void showBannerAds() {
        linearLayout_for_ads.setGravity(Gravity.TOP);
        linearLayout_for_ads.setVisibility(View.VISIBLE);
    }
    private void hideBannerAds() {
        linearLayout_for_ads.setVisibility(View.GONE);
    }
    private void showNativeAds() {
        // simulate showing native ad
        if (BuildConfig.DEBUG) {
            nativeAdsLinearLayout.setVisibility(View.VISIBLE);
        }
    }
    private void hideNativeAds() {
        // simulate hide native ad
        Log.d(TAG, "hideNativeAds() is called.");
        if (BuildConfig.DEBUG) {
            nativeAdsLinearLayout.setVisibility(View.GONE);
        }
    }

    private void initializePlayingParam() {
        playingParam = new PlayingParameters();
        playingParam.setAutoPlay(false);
        playingParam.setMediaSourcePrepared(false);
        playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);

        playingParam.setCurrentVideoTrackIndexPlayed(0);

        playingParam.setMusicAudioTrackIndex(1);
        playingParam.setVocalAudioTrackIndex(1);
        playingParam.setCurrentAudioTrackIndexPlayed(playingParam.getMusicAudioTrackIndex());
        playingParam.setMusicAudioChannel(SmileApplication.leftChannel);     // default
        playingParam.setVocalAudioChannel(SmileApplication.stereoChannel);   // default
        playingParam.setCurrentChannelPlayed(playingParam.getMusicAudioChannel());
        playingParam.setCurrentAudioPosition(0);
        playingParam.setCurrentVolume(1.0f);

        playingParam.setPublicNextSongIndex(0);
        playingParam.setPlayingPublic(true);

        playingParam.setMusicOrVocalOrNoSetting(0); // no music and vocal setting
        playingParam.setRepeatStatus(NoRepeatPlaying);    // no repeat playing songs
        playingParam.setPlaySingleSong(true);     // default
    }

    @SuppressWarnings("unchecked")
    private void initializeVariables(Bundle savedInstanceState) {

        // private MediaSource mediaSource;

        if (savedInstanceState == null) {
            // new fragment is being created

            numberOfVideoTracks = 0;
            numberOfAudioTracks = 0;
            videoTrackIndicesList = new ArrayList<>();
            audioTrackIndicesList = new ArrayList<>();

            mediaUri = null;
            initializePlayingParam();
            canShowNotSupportedFormat = false;
            songInfo = null;    // default
            Bundle arguments = getArguments();
            if (arguments != null) {
                playingParam.setPlaySingleSong(arguments.getBoolean(IsPlaySingleSongState));
                songInfo = arguments.getParcelable(SongInfoState);
            }

            trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();

        } else {
            // needed to be set
            numberOfVideoTracks = savedInstanceState.getInt(NumberOfVideoTracksState,0);
            numberOfAudioTracks = savedInstanceState.getInt(NumberOfAudioTracksState);
            videoTrackIndicesList = (ArrayList<Integer[]>)savedInstanceState.getSerializable(VideoTrackIndicesListState);
            audioTrackIndicesList = (ArrayList<Integer[]>)savedInstanceState.getSerializable(AudioTrackIndicesListState);
            publicSongList = savedInstanceState.getParcelableArrayList(PublicSongListState);

            mediaUri = savedInstanceState.getParcelable(MediaUriState);
            playingParam = savedInstanceState.getParcelable(PlayingParamState);
            canShowNotSupportedFormat = savedInstanceState.getBoolean(CanShowNotSupportedFormatState);
            if (playingParam == null) {
                initializePlayingParam();
            }
            songInfo = savedInstanceState.getParcelable(SongInfoState);

            trackSelectorParameters = savedInstanceState.getParcelable(TrackSelectorParametersState);
        }
    }

    private void selectFileToOpen() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        startActivityForResult(intent, FILE_READ_REQUEST_CODE);
    }

    private void showBufferingMessage() {
        messageLinearLayout.setVisibility(View.VISIBLE);
        bufferingStringTextView.startAnimation(animationText);
    }
    private void dismissBufferingMessage() {
        messageLinearLayout.setVisibility(View.GONE);
        animationText.cancel();
    }

    private void initExoPlayer() {

        // trackSelector = new DefaultTrackSelector();
        // trackSelector = new DefaultTrackSelector(new RandomTrackSelection.Factory());
        trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory());
        trackSelector.setParameters(trackSelectorParameters);

        myRenderersFactory = new MyRenderersFactory(callingContext);
        stereoVolumeAudioProcessor = myRenderersFactory.getStereoVolumeAudioProcessor();
        // exoPlayer = ExoPlayerFactory.newSimpleInstance(callingContext, trackSelector);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(callingContext, myRenderersFactory, trackSelector);

        // no need. It will ve overridden by MediaSessionConnector
        exoPlayer.addListener(new ExoPlayerEventListener());
        exoPlayer.addAudioListener(new AudioListener() {
            @Override
            public void onAudioSessionId(int audioSessionId) {
                Log.d(TAG, "addAudioListener.onAudioSessionId() is called.");
                Log.d(TAG, "addAudioListener.audioSessionId = " + audioSessionId);
            }

            @Override
            public void onAudioAttributesChanged(AudioAttributes audioAttributes) {
                Log.d(TAG, "addAudioListener.onAudioAttributesChanged() is called.");
            }

            @Override
            public void onVolumeChanged(float volume) {
                Log.d(TAG, "addAudioListener.onVolumeChanged() is called.");
            }
        });

        // removed on 2019-10-14 for testing
        /*
        audioAttributesBuilder = new AudioAttributes.Builder();
        audioAttributesBuilder.setUsage(C.USAGE_MEDIA).setContentType(C.CONTENT_TYPE_MOVIE);
        exoPlayer.setAudioAttributes(audioAttributesBuilder.build(), true);
        */

        videoExoPlayerView.setPlayer(exoPlayer);
        videoExoPlayerView.requestFocus();

        // Log.d(TAG, "FfmpegLibrary.isAvailable() = " + FfmpegLibrary.isAvailable());
        // Log.d(TAG, "VpxLibrary.isAvailable() = " + VpxLibrary.isAvailable());
        // Log.d(TAG, "FlacLibrary.isAvailable() = " + FlacLibrary.isAvailable());
        // Log.d(TAG, "OpusLibrary.isAvailable() = " + OpusLibrary.isAvailable());
    }

    private void releaseExoPlayer() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private void initMediaSessionCompat() {
        // Create a MediaSessionCompat
        Activity mActivity = getActivity();

        mediaSessionCompat = new MediaSessionCompat(callingContext, LOG_TAG);

        // Do not let MediaButtons restart the player when the app is not visible
        mediaSessionCompat.setMediaButtonReceiver(null);
        mediaSessionCompat.setActive(true); // might need to find better place to put

        // Create a MediaControllerCompat
        mediaControllerCompat = new MediaControllerCompat(callingContext, mediaSessionCompat);
        MediaControllerCompat.setMediaController(mActivity, mediaControllerCompat);
        mediaControllerCallback = new MediaControllerCallback();
        mediaControllerCompat.registerCallback(mediaControllerCallback);
        mediaTransportControls = mediaControllerCompat.getTransportControls();

        mediaSessionConnector = new MediaSessionConnector(mediaSessionCompat);
        mediaSessionConnector.setPlayer(exoPlayer);
        mediaSessionConnector.setPlaybackPreparer(new PlaybackPreparer());
    }

    private void releaseMediaSessionCompat() {
        mediaSessionCompat.setActive(false);
        mediaSessionCompat.release();
        mediaSessionCompat = null;
        mediaTransportControls = null;
        if (mediaControllerCallback != null) {
            mediaControllerCompat.unregisterCallback(mediaControllerCallback);
            mediaControllerCallback = null;
        }
        mediaControllerCompat = null;
        mediaSessionConnector = null;
    }

    private ArrayList<SongInfo> readPublicSongList() {
        ArrayList<SongInfo> playlist;
        SongListSQLite songListSQLite = new SongListSQLite(callingContext);
        if (songListSQLite != null) {
            playlist = songListSQLite.readPlaylist();
            songListSQLite.closeDatabase();
            songListSQLite = null;
        } else {
            playlist = new ArrayList<>();
            Log.d(TAG, "Read database unsuccessfully --> " + playlist.size());
        }

        return playlist;
    }

    private void startAutoPlay() {

        if (getActivity().isFinishing()) {
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
                ScreenUtil.showToast(callingContext, getString(R.string.noPlaylistString), toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
                playingParam.setAutoPlay(false);    // cancel auto play
            } else {
                // There are public songs to be played
                boolean stillPlayNext = true;
                int repeatStatus = playingParam.getRepeatStatus();
                int publicNextSongIndex = playingParam.getPublicNextSongIndex();
                switch (repeatStatus) {
                    case NoRepeatPlaying:
                        // no repeat
                        if (publicNextSongIndex >= publicSongListSize) {
                            // stop playing
                            playingParam.setAutoPlay(false);
                            stopPlay();
                            stillPlayNext = false;  // stop here and do not go to next
                        }
                        break;
                    case RepeatOneSong:
                        // repeat one song
                        Log.d(TAG, "startAutoPlay() --> RepeatOneSong");
                        if ( (publicNextSongIndex > 0) && (publicNextSongIndex <= publicSongListSize) ) {
                            publicNextSongIndex--;
                            Log.d(TAG, "startAutoPlay() --> RepeatOneSong --> publicSongIndex = " + publicNextSongIndex);
                        }
                        break;
                    case RepeatAllSongs:
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
            playingParam.setMusicOrVocalOrNoSetting(PlayingMusic);  // presume music
            // if (mediaSource != null) {
            if (mediaUri != null) {
                if (playingParam.getRepeatStatus() != NoRepeatPlaying) {
                    // repeat playing this mediaUri
                } else {
                    // next song that user ordered
                }
            }
            Log.d(TAG, "startAutoPlay() finished --> ordered song.");
        }
        setToolbarImageButtonStatus();
    }

    private void playSingleSong(SongInfo songInfo) {
        playingParam.setMusicOrVocalOrNoSetting(PlayingVocal);  // presume vocal
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

        String filePath = songInfo.getFilePath();
        Log.i(TAG, "filePath : " + filePath);
        mediaUri = Uri.parse(filePath);
        Log.i(TAG, "mediaUri from filePath : " + mediaUri);
        // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
        // pass the saved instance of playingParam to
        // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)
        Bundle playingParamOriginExtras = new Bundle();
        playingParamOriginExtras.putParcelable(PlayingParamOrigin, playingParam);
        mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
    }

    private void startPlay() {
        // if ( (mediaSource != null) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PLAYING) ) {
        int playbackState = playingParam.getCurrentPlaybackState();
        if ( (mediaUri != null) && (playbackState != PlaybackStateCompat.STATE_PLAYING) ) {
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

    private void pausePlay() {
        // if ( (mediaSource != null) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) ) {
        if ( (mediaUri != null) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) ) {
            // no media file opened or playing has been stopped
            mediaTransportControls.pause();
        }
    }

    private void stopPlay() {
        if (playingParam.isAutoPlay()) {
            // auto play
            startAutoPlay();
        } else {
            if (playingParam.getRepeatStatus() == NoRepeatPlaying) {
                // no repeat playing
                // if ((mediaSource != null) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_NONE)) {
                if ((mediaUri != null) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_NONE)) {
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

    private void replayMedia() {
        // if ( (mediaSource != null) && (numberOfAudioTracks>0) ) {
        if ( (mediaUri != null) && (numberOfAudioTracks >0) ) {
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
                playingParamOriginExtras.putParcelable(PlayingParamOrigin, playingParam);
                mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);   // prepare and play
                Log.d(TAG, "replayMedia()--> mediaTransportControls.prepareFromUri().");
            }
        }

        Log.d(TAG, "replayMedia() is called.");
    }

    private void setAudioTrackAndChannel(int audioTrackIndex, int audioChannel) {
        Log.d(TAG, "setAudioTrackAndChannel() -- > audioTrackIndex = " + audioTrackIndex);
        if (numberOfAudioTracks > 0) {
            // select audio track
            int indexInArrayList = audioTrackIndex - 1;
            if (indexInArrayList < 0) {
                //
                return;
            }

            Integer[] trackIndicesCombination = audioTrackIndicesList.get(indexInArrayList);
            selectAudioTrack(trackIndicesCombination);
            playingParam.setCurrentAudioTrackIndexPlayed(audioTrackIndex);

            // select audio channel
            Log.d(TAG, "setAudioTrackAndChannel() -- > audioChannel = " + audioChannel);
            playingParam.setCurrentChannelPlayed(audioChannel);
            setAudioVolume(playingParam.getCurrentVolume());
        }
    }

    private void switchAudioToVocal() {
        int vocalAudioTrackIndex = playingParam.getVocalAudioTrackIndex();
        int vocalAudioChannel = playingParam.getVocalAudioChannel();
        playingParam.setMusicOrVocalOrNoSetting(PlayingVocal);
        setAudioTrackAndChannel(vocalAudioTrackIndex, vocalAudioChannel);
    }

    private void switchAudioToMusic() {
        int musicAudioTrackIndex = playingParam.getMusicAudioTrackIndex();
        int musicAudioChannel = playingParam.getMusicAudioChannel();
        playingParam.setMusicOrVocalOrNoSetting(PlayingMusic);
        setAudioTrackAndChannel(musicAudioTrackIndex, musicAudioChannel);
    }

    private void setAudioVolume(float volume) {
        // get current channel
        int currentChannelPlayed = playingParam.getCurrentChannelPlayed();
        Log.d(TAG, "setAudioVolume() -- > currentChannelPlayed = " + currentChannelPlayed);
        //
        boolean useAudioProcessor = false;
        if (stereoVolumeAudioProcessor != null) {
            if (stereoVolumeAudioProcessor.getVolume() != null) {
                useAudioProcessor = true;
                float[] volumeInput = new float[stereoVolumeAudioProcessor.getVolume().length];
                switch (volumeInput.length) {
                    case 2:
                        if (currentChannelPlayed == SmileApplication.leftChannel) {
                            volumeInput[StereoVolumeAudioProcessor.LEFT_SPEAKER] = volume;
                            volumeInput[StereoVolumeAudioProcessor.RIGHT_SPEAKER] = 0.0f;
                        } else if (currentChannelPlayed == SmileApplication.rightChannel) {
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

    private class MediaControllerCallback extends MediaControllerCompat.Callback {

        @Override
        public synchronized void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if( state == null ) {
                return;
            }

            int currentState = state.getState();
            switch (currentState) {
                case PlaybackStateCompat.STATE_NONE:
                    // initial state and when playing is stopped by user
                    Log.d(TAG, "PlaybackStateCompat.STATE_NONE");
                    // if (mediaSource != null) {
                    if (mediaUri != null) {
                        Log.d(TAG, "MediaControllerCallback--> Song was stopped by user.");
                    }
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    // when finished playing
                    Log.d(TAG, "PlaybackStateCompat.STATE_STOPPED");
                    break;
            }
            playingParam.setCurrentPlaybackState(currentState);
            Log.d(TAG, "MediaControllerCallback.onPlaybackStateChanged() is called. " + currentState);
        }
    }

    private class PlaybackPreparer implements MediaSessionConnector.PlaybackPreparer {

        @Override
        public long getSupportedPrepareActions() {
            long supportedPrepareActions = PlaybackPreparer.ACTIONS;
            Log.d(TAG, "MediaSessionConnector.PlaybackPreparer.getSupportedPrepareActions() is called.");
            return supportedPrepareActions;
        }

        @Override
        public void onPrepare(boolean playWhenReady) {
            Log.d(TAG, "MediaSessionConnector.PlaybackPreparer.onPrepare() is called.");
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, boolean playWhenReady, Bundle extras) {
            Log.d(TAG, "MediaSessionConnector.PlaybackPreparer.onPrepareFromMediaId() is called.");
        }

        @Override
        public void onPrepareFromSearch(String query, boolean playWhenReady, Bundle extras) {
            Log.d(TAG, "MediaSessionConnector.PlaybackPreparer.onPrepareFromSearch() is called.");
        }

        @Override
        public void onPrepareFromUri(Uri uri, boolean playWhenReady, Bundle extras) {
            Log.d(TAG, "Uri = " + uri);
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
            playingParam.setMediaSourcePrepared(false);
            dataSourceFactory = new DefaultDataSourceFactory(callingContext, Util.getUserAgent(callingContext, callingContext.getPackageName()));
            // MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(uri);
            exoPlayer.prepare(mediaSource);
            long currentAudioPosition = playingParam.getCurrentAudioPosition();
            float currentVolume = playingParam.getCurrentVolume();
            int playbackState = playbackState = playingParam.getCurrentPlaybackState();
            if (extras != null) {
                Log.d(TAG, "extras is not null.");
                PlayingParameters playingParamOrigin = extras.getParcelable(PlayingParamOrigin);
                if (playingParamOrigin != null) {
                    Log.d(TAG, "playingParamOrigin is not null.");
                    playbackState = playingParamOrigin.getCurrentPlaybackState();
                    currentAudioPosition = playingParamOrigin.getCurrentAudioPosition();
                    currentVolume = playingParamOrigin.getCurrentVolume();
                }
            }
            // setAudioVolume(currentVolume);
            exoPlayer.seekTo(currentAudioPosition);
            switch (playbackState) {
                case PlaybackStateCompat.STATE_PAUSED:
                    exoPlayer.setPlayWhenReady(false);
                    Log.d(TAG, "setMediaSourcePrepared() --> PlaybackStateCompat.STATE_PAUSED");
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    exoPlayer.setPlayWhenReady(false);
                    Log.d(TAG, "setMediaSourcePrepared() --> PlaybackStateCompat.STATE_STOPPED");
                    break;
                case PlaybackStateCompat.STATE_PLAYING:
                    exoPlayer.setPlayWhenReady(true);  // start playing when ready
                    Log.d(TAG, "setMediaSourcePrepared() --> PlaybackStateCompat.STATE_PLAYING");
                    break;
                default:
                    // PlaybackStateCompat.STATE_NONE:
                    exoPlayer.setPlayWhenReady(true);  // start playing when ready
                    Log.d(TAG, "setMediaSourcePrepared() --> PlaybackStateCompat.STATE_NONE");
                    break;
            }

            Log.d(TAG, "MediaSessionConnector.PlaybackPreparer.onPrepareFromUri() is called--> " + playbackState);
        }

        @Override
        public boolean onCommand(Player player, ControlDispatcher controlDispatcher, String command, Bundle extras, ResultReceiver cb) {
            return false;
        }
    }


    private class ExoPlayerEventListener implements Player.EventListener {

        @Override
        public synchronized void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

            Log.d(TAG,"Player.EventListener.onPlayerStateChanged is called.");
            Log.d(TAG, "Playback state = " + playbackState);

            switch (playbackState) {
                case Player.STATE_BUFFERING:
                    hideNativeAds();
                    showBufferingMessage();
                    return;
                case Player.STATE_READY:
                    if (!playingParam.isMediaSourcePrepared()) {
                        // the first time of Player.STATE_READY means prepared

                        findTracksForVideoAudio();

                        // build R.id.audioTrack submenu
                        if (audioTrackMenuItem != null) {
                            if (!audioTrackMenuItem.hasSubMenu()) {
                                // no sub menu
                                ((Menu) audioTrackMenuItem).addSubMenu("Text title");
                            }
                            SubMenu subMenu = audioTrackMenuItem.getSubMenu();
                            subMenu.clear();
                            String audioTrackName;
                            for (int index=0; index<audioTrackIndicesList.size(); index++) {
                                // audio track index start from 1 for user interface
                                audioTrackName = getString(R.string.audioTrackString) + " " + (index+1);
                                subMenu.add(audioTrackName);
                            }
                        }
                    }

                    playingParam.setMediaSourcePrepared(true);

                    if (numberOfVideoTracks == 0) {
                        // no video is being played, show native ads
                        showNativeAds();
                    } else {
                        // video is being played, hide native ads
                        hideNativeAds();
                    }
                    break;
                case Player.STATE_ENDED:
                    // playing is finished
                    if (playingParam.isAutoPlay()) {
                        // start playing next video from list
                        startAutoPlay();
                    } else {
                        // end of playing
                        if (playingParam.getRepeatStatus() != NoRepeatPlaying) {
                            replayMedia();
                        } else {
                            showNativeAds();
                        }
                    }
                    Log.d(TAG, "Playback state = Player.STATE_ENDED after startAutoPlay()");
                    break;
                case Player.STATE_IDLE:
                    // There is bug here
                    // The listener will get twice of (Player.STATE_IDLE)
                    // when user stop playing using ExoPlayer.stop()
                    // so do not put startAutoPlay() inside this event
                    // if (mediaSource != null) {
                    if (mediaUri != null) {
                        playingParam.setMediaSourcePrepared(false);
                        Log.d(TAG, "Song was stopped by user (by stopPlay()).");
                    }
                    if (!playingParam.isAutoPlay()) {
                        // not auto play
                        showNativeAds();
                    }
                    break;
            }

            dismissBufferingMessage();
        }

        @Override
        public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {
            Log.d(TAG,"Player.EventListener.onTimelineChanged() is called.");
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            Log.d(TAG,"Player.EventListener.onTracksChanged() is called.");
        }
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            Log.d(TAG,"Player.EventListener.onIsPlayingChanged() is called.");
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            switch (error.type) {
                case ExoPlaybackException.TYPE_SOURCE:
                    Log.e(TAG, "TYPE_SOURCE: " + error.getSourceException().getMessage());
                    break;

                case ExoPlaybackException.TYPE_RENDERER:
                    Log.e(TAG, "TYPE_RENDERER: " + error.getRendererException().getMessage());
                    break;

                case ExoPlaybackException.TYPE_UNEXPECTED:
                    Log.e(TAG, "TYPE_UNEXPECTED: " + error.getUnexpectedException().getMessage());
                    break;
            }
            Log.d(TAG,"Player.EventListener.onPlayerError() is called.");

            String formatNotSupportedString = getString(R.string.formatNotSupportedString);
            if (playingParam.isAutoPlay()) {
                // go to next one in the list
                if (canShowNotSupportedFormat) {
                    // only show once
                    ScreenUtil.showToast(callingContext, formatNotSupportedString, toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
                    canShowNotSupportedFormat = false;
                }
                startAutoPlay();
            } else {
                ScreenUtil.showToast(callingContext, formatNotSupportedString, toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
            }

            mediaUri = null;
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            Log.d(TAG,"Player.EventListener.onPositionDiscontinuity() is called.");
        }

        @Override
        public void onSeekProcessed() {
            Log.d(TAG,"Player.EventListener.onSeekProcessed() is called.");
        }
    }

    private void findTracksForVideoAudio() {

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

        MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
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
            playingParam.setCurrentVideoTrackIndexPlayed(noVideoTrack);
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
            playingParam.setCurrentAudioTrackIndexPlayed(noAudioTrack);
            playingParam.setCurrentChannelPlayed(noAudioChannel);
        } else {
            int audioChannel = SmileApplication.stereoChannel;  // default channel
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
    }

    private boolean selectAudioTrack(Integer[] trackIndicesCombination) {

        boolean result = false;

        MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
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

        SelectionOverride initialOverride = trackParameters.getSelectionOverride(audioRendererIndex, mappedTrackInfo.getTrackGroups(audioRendererIndex));

        initialOverride = new SelectionOverride(audioTrackGroupIndex, audioTrackIndex);
        // trackSelector.setParameters(parametersBuilder.build());
        // or
        parametersBuilder.clearSelectionOverrides(audioRendererIndex)
                .setRendererDisabled(audioRendererIndex, false)
                .setSelectionOverride(audioRendererIndex, mappedTrackInfo.getTrackGroups(audioRendererIndex), initialOverride);
        trackSelector.setParameters(parametersBuilder);

        trackSelectorParameters = trackSelector.getParameters();

        return result;
    }
}
