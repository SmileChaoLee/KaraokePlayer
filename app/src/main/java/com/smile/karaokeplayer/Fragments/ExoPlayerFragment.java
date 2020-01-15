package com.smile.karaokeplayer.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import androidx.appcompat.widget.ActionMenuView;

import android.widget.FrameLayout;
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
import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.ExoRenderersFactory.MyRenderersFactory;
import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.Models.SongListSQLite;
import com.smile.karaokeplayer.Models.VerticalSeekBar;
import com.smile.karaokeplayer.R;
import com.smile.karaokeplayer.SmileApplication;
import com.smile.karaokeplayer.SongListActivity;
import com.smile.karaokeplayer.Utilities.AccessContentUtil;
import com.smile.karaokeplayer.Utilities.ExternalStorageUtil;
import com.smile.smilelibraries.privacy_policy.PrivacyPolicyUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.io.File;
import java.util.ArrayList;

import static com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES;
import static com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS;

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

    private float textFontSize;
    private float fontScale;
    private float toastTextSize;

    private Context callingContext;
    private View fragmentView;

    private Toolbar supportToolbar;  // use customized ToolBar
    private ActionMenuView actionMenuView;
    private VerticalSeekBar volumeSeekBar;
    private ImageButton volumeImageButton;
    private ImageButton previousMediaImageButton;
    private ImageButton playMediaImageButton;
    private ImageButton replayMediaImageButton;
    private ImageButton pauseMediaImageButton;
    private ImageButton stopMediaImageButton;
    private ImageButton nextMediaImageButton;
    private ImageButton repeatImageButton;
    private ImageButton switchToMusicImageButton;
    private ImageButton switchToVocalImageButton;
    private ImageButton actionMenuImageButton;
    private int volumeSeekBarHeightForLandscape;

    private Menu mainMenu;
    // submenu of file
    private MenuItem autoPlayMenuItem;
    private MenuItem openMenuItem;
    // submenu of audio
    private MenuItem audioTrackMenuItem;
    // submenu of channel
    private MenuItem leftChannelMenuItem;
    private MenuItem rightChannelMenuItem;
    private MenuItem stereoChannelMenuItem;

    private MediaSessionCompat mediaSessionCompat;
    private MediaControllerCompat mediaControllerCompat;
    private MediaControllerCallback mediaControllerCallback;
    private MediaControllerCompat.TransportControls mediaTransportControls;
    private MediaSessionConnector mediaSessionConnector;

    private StereoVolumeAudioProcessor stereoVolumeAudioProcessor;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private SimpleExoPlayer exoPlayer;
    private ExoPlayerEventListener mExoPlayerEventListener;
    private PlayerView videoExoPlayerView;

    private LinearLayout linearLayout_for_ads;
    private LinearLayout messageLinearLayout;
    private TextView bufferingStringTextView;
    private Animation animationText;
    private LinearLayout nativeAdsLinearLayout;
    private TextView nativeAdsStringTextView;

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

    private OnFragmentInteractionListener mListener;

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
        args.putBoolean(PlayerConstants.IsPlaySingleSongState, isPlaySingleSong);
        args.putParcelable(PlayerConstants.SongInfoState, songInfo);
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

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(callingContext, ScreenUtil.FontSize_Pixel_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(callingContext, defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(callingContext, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        toastTextSize = 0.7f * textFontSize;
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
        supportToolbar.setVisibility(View.VISIBLE);
        mListener.setSupportActionBarForFragment(supportToolbar);
        ActionBar actionBar = mListener.getSupportActionBarForFragment();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        volumeSeekBar = fragmentView.findViewById(R.id.volumeSeekBar);
        // get default height of volumeBar from dimen.xml
        volumeSeekBarHeightForLandscape = volumeSeekBar.getLayoutParams().height;

        // uses dimens.xml for different devices' sizes
        volumeSeekBar.setVisibility(View.INVISIBLE); // default is not showing
        volumeSeekBar.setMax(PlayerConstants.MaxProgress);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                volumeSeekBar.setProgressAndThumb(i);
                // float currentVolume = (float)i / (float)MaxProgress;
                float currentVolume = 1.0f;
                if (i < PlayerConstants.MaxProgress) {
                    currentVolume = (float)(1.0f - (Math.log(PlayerConstants.MaxProgress - i) / Math.log(PlayerConstants.MaxProgress)));
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

        int currentProgress;
        float currentVolume = playingParam.getCurrentVolume();
        if ( currentVolume >= 1.0f) {
            currentProgress = PlayerConstants.MaxProgress;
        } else {
            currentProgress = PlayerConstants.MaxProgress - (int)Math.pow(PlayerConstants.MaxProgress, (1-currentVolume));
            currentProgress = Math.max(0, currentProgress);
        }
        volumeSeekBar.setProgressAndThumb(currentProgress);
        volumeImageButton = fragmentView.findViewById(R.id.volumeImageButton);

        //
        previousMediaImageButton = fragmentView.findViewById(R.id.previousMediaImageButton);
        playMediaImageButton = fragmentView.findViewById(R.id.playMediaImageButton);
        pauseMediaImageButton = fragmentView.findViewById(R.id.pauseMediaImageButton);
        if (playingParam.getCurrentPlaybackState()==PlaybackStateCompat.STATE_PLAYING) {
            playMediaImageButton.setVisibility(View.GONE);
            pauseMediaImageButton.setVisibility(View.VISIBLE);
        } else {
            playMediaImageButton.setVisibility(View.VISIBLE);
            pauseMediaImageButton.setVisibility(View.GONE);
        }
        replayMediaImageButton = fragmentView.findViewById(R.id.replayMediaImageButton);
        stopMediaImageButton = fragmentView.findViewById(R.id.stopMediaImageButton);
        nextMediaImageButton = fragmentView.findViewById(R.id.nextMediaImageButton);

        //
        repeatImageButton = fragmentView.findViewById(R.id.repeatImageButton);
        switchToMusicImageButton = fragmentView.findViewById(R.id.switchToMusicImageButton);
        switchToVocalImageButton = fragmentView.findViewById(R.id.switchToVocalImageButton);
        actionMenuImageButton = fragmentView.findViewById(R.id.actionMenuImageButton);

        setImageButtonStatus();

        // added on 2019-12-26
        actionMenuView = supportToolbar.findViewById(R.id.actionMenuViewLayout); // main menu
        actionMenuView.setOnMenuItemClickListener(new ActionMenuView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });
        //

        setButtonsPositionAndSize(getResources().getConfiguration());

        linearLayout_for_ads = fragmentView.findViewById(R.id.linearLayout_for_ads);
        if (!SmileApplication.googleAdMobBannerID.isEmpty()) {
            try {
                AdView bannerAdView = new AdView(callingContext);
                bannerAdView.setAdSize(AdSize.BANNER);
                bannerAdView.setAdUnitId(SmileApplication.googleAdMobBannerID);
                linearLayout_for_ads.addView(bannerAdView);
                AdRequest adRequest = new AdRequest.Builder().build();
                bannerAdView.loadAd(adRequest);
                linearLayout_for_ads.setGravity(Gravity.TOP);
                linearLayout_for_ads.setVisibility(View.VISIBLE);
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
        ScreenUtil.resizeTextSize(bufferingStringTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        animationText = new AlphaAnimation(0.0f,1.0f);
        animationText.setDuration(500);
        animationText.setStartOffset(0);
        animationText.setRepeatMode(Animation.REVERSE);
        animationText.setRepeatCount(Animation.INFINITE);

        nativeAdsLinearLayout = fragmentView.findViewById(R.id.nativeAdsLinearLayout);
        nativeAdsStringTextView = fragmentView.findViewById(R.id.nativeAdsStringTextView);
        ScreenUtil.resizeTextSize(nativeAdsStringTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);

        float durationTextSize = textFontSize * 0.6f;
        TextView exo_position_TextView = fragmentView.findViewById(R.id.exo_position);
        ScreenUtil.resizeTextSize(exo_position_TextView, durationTextSize, ScreenUtil.FontSize_Pixel_Type);

        TextView exo_duration_TextView = fragmentView.findViewById(R.id.exo_duration);
        ScreenUtil.resizeTextSize(exo_duration_TextView, durationTextSize, ScreenUtil.FontSize_Pixel_Type);

        setOnClickEvents();

        showNativeAds();

        if (songInfo == null) {
            Log.d(TAG, "songInfo is null");
        } else {
            Log.d(TAG, "songInfo is not null");
        }

        if (mediaUri==null || Uri.EMPTY.equals(mediaUri)) {
            if (playingParam.isPlaySingleSong()) {
                if (songInfo != null) {
                    playingParam.setAutoPlay(false);
                    playSingleSong(songInfo);
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
                mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
            }
        }

        return fragmentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated() is called");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        Log.d(TAG, "onCreateOptionsMenu() is called");
        // Inflate the menu; this adds items to the action bar if it is present.

        // mainMenu = menu;
        mainMenu = actionMenuView.getMenu();
        menuInflater.inflate(R.menu.menu_main, mainMenu);

        // according to the above explanations, the following statement will fit every situation
        ScreenUtil.resizeMenuTextSize(mainMenu, fontScale);

        // submenu of file
        autoPlayMenuItem = mainMenu.findItem(R.id.autoPlay);
        openMenuItem = mainMenu.findItem(R.id.open);

        // submenu of audio
        audioTrackMenuItem = mainMenu.findItem(R.id.audioTrack);
        // submenu of channel
        leftChannelMenuItem = mainMenu.findItem(R.id.leftChannel);
        rightChannelMenuItem = mainMenu.findItem(R.id.rightChannel);
        stereoChannelMenuItem = mainMenu.findItem(R.id.stereoChannel);

        if (playingParam.isPlaySingleSong()) {
            MenuItem fileMenuItem = mainMenu.findItem(R.id.file);
            fileMenuItem.setVisible(false);
            audioTrackMenuItem.setVisible(false);
            MenuItem channelMenuItem = mainMenu.findItem(R.id.channel);
            channelMenuItem.setVisible(false);
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu() is called.");
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        boolean isAutoPlay;
        int currentChannelPlayed = playingParam.getCurrentChannelPlayed();

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
                } else {
                    autoPlayMenuItem.setChecked(false);
                    openMenuItem.setEnabled(true);
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
                        ScreenUtil.showToast(callingContext, msg, toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                    }
                } else {
                    playingParam.setAutoPlay(isAutoPlay);
                }
                setImageButtonStatus();
                break;
            case R.id.songList:
                Intent songListIntent = new Intent(callingContext, SongListActivity.class);
                startActivityForResult(songListIntent, PlayerConstants.SONG_LIST_ACTIVITY_CODE);
                break;
            case R.id.open:
                if (!playingParam.isAutoPlay()) {
                    // isMediaSourcePrepared = false;
                    AccessContentUtil.selectFileToOpen(this, PlayerConstants.FILE_READ_REQUEST_CODE);
                }
                break;
            case R.id.privacyPolicy:
                PrivacyPolicyUtil.startPrivacyPolicyActivity(getActivity(), CommonConstants.PrivacyPolicyUrl, PlayerConstants.PrivacyPolicyActivityRequestCode);
                break;
            case R.id.exit:
                if (mListener != null) {
                    mListener.onExitFragment();
                }
                break;
            case R.id.audioTrack:
                // if there are audio tracks
                SubMenu subMenu = item.getSubMenu();
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
            case R.id.audioTrack1:
                setAudioTrackAndChannel(1, currentChannelPlayed);
                break;
            case R.id.audioTrack2:
                setAudioTrackAndChannel(2, currentChannelPlayed);
                break;
            case R.id.audioTrack3:
                setAudioTrackAndChannel(3, currentChannelPlayed);
                break;
            case R.id.audioTrack4:
                setAudioTrackAndChannel(4, currentChannelPlayed);
                break;
            case R.id.audioTrack5:
                setAudioTrackAndChannel(5, currentChannelPlayed);
                break;
            case R.id.audioTrack6:
                setAudioTrackAndChannel(6, currentChannelPlayed);
                break;
            case R.id.audioTrack7:
                setAudioTrackAndChannel(7, currentChannelPlayed);
                break;
            case R.id.audioTrack8:
                setAudioTrackAndChannel(8, currentChannelPlayed);
                break;
            case R.id.channel:
                if (mediaUri != null && !Uri.EMPTY.equals(mediaUri) && numberOfAudioTracks>0) {
                    leftChannelMenuItem.setEnabled(true);
                    rightChannelMenuItem.setEnabled(true);
                    stereoChannelMenuItem.setEnabled(true);
                    if (playingParam.isMediaSourcePrepared()) {
                        if (currentChannelPlayed == CommonConstants.LeftChannel) {
                            leftChannelMenuItem.setCheckable(true);
                            leftChannelMenuItem.setChecked(true);
                        } else {
                            leftChannelMenuItem.setCheckable(false);
                            leftChannelMenuItem.setChecked(false);
                        }
                        if (currentChannelPlayed == CommonConstants.RightChannel) {
                            rightChannelMenuItem.setCheckable(true);
                            rightChannelMenuItem.setChecked(true);
                        } else {
                            rightChannelMenuItem.setCheckable(false);
                            rightChannelMenuItem.setChecked(false);
                        }
                        if (currentChannelPlayed == CommonConstants.StereoChannel) {
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
                playingParam.setCurrentChannelPlayed(CommonConstants.LeftChannel);
                setAudioVolume(playingParam.getCurrentVolume());
                break;
            case R.id.rightChannel:
                playingParam.setCurrentChannelPlayed(CommonConstants.RightChannel);
                setAudioVolume(playingParam.getCurrentVolume());
                break;
            case R.id.stereoChannel:
                playingParam.setCurrentChannelPlayed(CommonConstants.StereoChannel);
                setAudioVolume(playingParam.getCurrentVolume());
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Log.d(TAG,"ExoPlayerFragment-->onConfigurationChanged() is called.");
        super.onConfigurationChanged(newConfig);
        // mainMenu.close();
        closeMenu(mainMenu);
        setButtonsPositionAndSize(newConfig);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG,"ExoPlayerFragment-->onSaveInstanceState() is called.");

        outState.putInt(PlayerConstants.NumberOfVideoTracksState, numberOfVideoTracks);
        outState.putInt(PlayerConstants.NumberOfAudioTracksState, numberOfAudioTracks);
        outState.putSerializable(PlayerConstants.VideoTrackIndicesListState, videoTrackIndicesList);
        outState.putSerializable(PlayerConstants.AudioTrackIndicesListState, audioTrackIndicesList);
        outState.putParcelableArrayList(PlayerConstants.PublicSongListState, publicSongList);

        outState.putParcelable(PlayerConstants.MediaUriState, mediaUri);
        if (exoPlayer != null) {
            playingParam.setCurrentAudioPosition(exoPlayer.getContentPosition());
        } else {
            playingParam.setCurrentAudioPosition(0);
        }

        outState.putParcelable(PlayerConstants.PlayingParamState, playingParam);
        outState.putBoolean(PlayerConstants.CanShowNotSupportedFormatState, canShowNotSupportedFormat);
        outState.putParcelable(PlayerConstants.SongInfoState, songInfo);

        trackSelectorParameters = trackSelector.getParameters();
        outState.putParcelable(PlayerConstants.TrackSelectorParametersState, trackSelectorParameters);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "onActivityResult() is called.");
        super.onActivityResult(requestCode, resultCode, data);
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == PlayerConstants.FILE_READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
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

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getActivity().getContentResolver().takePersistableUriPermission(mediaUri, takeFlags);
                    }
                } catch (Exception ex) {
                    Log.d(TAG, "Failed to add persistable permission of mediaUri");
                    ex.printStackTrace();
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
                // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
                // pass the saved instance of playingParam to
                // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)
                Bundle playingParamOriginExtras = new Bundle();
                playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
                mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
            }
            return;
        }
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

    private void setButtonsPositionAndSize(Configuration config) {

        int buttonMarginLeft = (int)(60.0f * fontScale);    // 60 pixels = 20dp on Nexus 5
        Log.d(TAG, "buttonMarginLeft = " + buttonMarginLeft);
        Point screenSize = ScreenUtil.getScreenSize(callingContext);
        Log.d(TAG, "screenSize.x = " + screenSize.x);
        Log.d(TAG, "screenSize.y = " + screenSize.y);
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            buttonMarginLeft = (int)((float)buttonMarginLeft * ((float)screenSize.x / (float)screenSize.y));
            Log.d(TAG, "buttonMarginLeft = " + buttonMarginLeft);
        }

        ViewGroup.MarginLayoutParams layoutParams;
        int imageButtonHeight = (int)(textFontSize * 1.5f);
        int buttonNum = 6;  // 6 buttons
        int maxWidth = buttonNum * imageButtonHeight + (buttonNum-1) * buttonMarginLeft;
        if (maxWidth > screenSize.x) {
            // greater than the width of screen
            buttonMarginLeft = (screenSize.x-10-(buttonNum*imageButtonHeight)) / (buttonNum-1);
        }

        layoutParams = (ViewGroup.MarginLayoutParams) volumeSeekBar.getLayoutParams();
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(0, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) volumeImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(0, 0, 0, 0);

        //
        layoutParams = (ViewGroup.MarginLayoutParams) previousMediaImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        FrameLayout playPauseButtonFrameLayout = fragmentView.findViewById(R.id.playPauseButtonFrameLayout);
        layoutParams = (ViewGroup.MarginLayoutParams) playPauseButtonFrameLayout.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) replayMediaImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) stopMediaImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) nextMediaImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        //
        layoutParams = (ViewGroup.MarginLayoutParams) repeatImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(0, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) switchToMusicImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) switchToVocalImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) actionMenuImageButton.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        layoutParams = (ViewGroup.MarginLayoutParams) actionMenuView.getLayoutParams();
        layoutParams.height = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);

        Bitmap tempBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.circle_and_three_dots);
        Drawable iconDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(tempBitmap, imageButtonHeight, imageButtonHeight, true));
        actionMenuView.setOverflowIcon(iconDrawable);   // set icon of three dots for ActionMenuView
        // supportToolbar.setOverflowIcon(iconDrawable);   // set icon of three dots for toolbar

        // reset the heights of volumeBar and supportToolbar
        final float timesOfVolumeBarForPortrait = 1.5f;
        if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // if orientation is portrait, then double the height of volumeBar
            volumeSeekBar.getLayoutParams().height = (int) ((float)volumeSeekBarHeightForLandscape * timesOfVolumeBarForPortrait);
        } else {
            volumeSeekBar.getLayoutParams().height = volumeSeekBarHeightForLandscape;
        }

        supportToolbar.getLayoutParams().height = volumeImageButton.getLayoutParams().height;
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
        });

        repeatImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                setImageButtonStatus();
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

        actionMenuImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // getActivity().openOptionsMenu();
                actionMenuView.showOverflowMenu();
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

        videoExoPlayerView.setControllerShowTimeoutMs(PlayerConstants.PlayerView_Timeout);
        videoExoPlayerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int visibility) {
                if (visibility == View.VISIBLE) {
                    // use custom toolbar
                    supportToolbar.setVisibility(View.VISIBLE);
                } else {
                    // use custom toolbar
                    supportToolbar.setVisibility(View.GONE);
                    closeMenu(mainMenu);
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

    private void setImageButtonStatus() {
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
            case PlayerConstants.NoRepeatPlaying:
                // no repeat but show symbol of repeat all song with transparent background
                repeatImageButton.setImageResource(R.drawable.repeat_all_white);
                break;
            case PlayerConstants.RepeatOneSong:
                // repeat one song
                repeatImageButton.setImageResource(R.drawable.repeat_one_white);
                break;
            case PlayerConstants.RepeatAllSongs:
                // repeat all song list
                repeatImageButton.setImageResource(R.drawable.repeat_all_white);
                break;
        }
        if (repeatStatus == PlayerConstants.NoRepeatPlaying) {
            repeatImageButton.setBackgroundColor(ContextCompat.getColor(callingContext, R.color.transparentDark));
        } else {
            repeatImageButton.setBackgroundColor(ContextCompat.getColor(callingContext, R.color.red));
        }
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
        playingParam.setMusicAudioChannel(CommonConstants.LeftChannel);     // default
        playingParam.setVocalAudioChannel(CommonConstants.StereoChannel);   // default
        playingParam.setCurrentChannelPlayed(playingParam.getMusicAudioChannel());
        playingParam.setCurrentAudioPosition(0);
        playingParam.setCurrentVolume(1.0f);

        playingParam.setPublicNextSongIndex(0);
        playingParam.setPlayingPublic(true);

        playingParam.setMusicOrVocalOrNoSetting(0); // no music and vocal setting
        playingParam.setRepeatStatus(PlayerConstants.NoRepeatPlaying);    // no repeat playing songs
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
                playingParam.setPlaySingleSong(arguments.getBoolean(PlayerConstants.IsPlaySingleSongState));
                songInfo = arguments.getParcelable(PlayerConstants.SongInfoState);
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
            songInfo = savedInstanceState.getParcelable(PlayerConstants.SongInfoState);

            trackSelectorParameters = savedInstanceState.getParcelable(PlayerConstants.TrackSelectorParametersState);
        }
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

        trackSelector = new DefaultTrackSelector(callingContext, new AdaptiveTrackSelection.Factory());
        trackSelector.setParameters(trackSelectorParameters);

        MyRenderersFactory myRenderersFactory = new MyRenderersFactory(callingContext);
        stereoVolumeAudioProcessor = myRenderersFactory.getStereoVolumeAudioProcessor();


        SimpleExoPlayer.Builder exoPlayerBuilder = new SimpleExoPlayer.Builder(callingContext, myRenderersFactory);
        exoPlayerBuilder.setTrackSelector(trackSelector);
        exoPlayer = exoPlayerBuilder.build();
        // exoPlayer = ExoPlayerFactory.newSimpleInstance(callingContext, myRenderersFactory, trackSelector);

        // no need. It will ve overridden by MediaSessionConnector
        mExoPlayerEventListener = new ExoPlayerEventListener();
        exoPlayer.addListener(mExoPlayerEventListener);

        videoExoPlayerView.setPlayer(exoPlayer);
        videoExoPlayerView.requestFocus();

        Log.d(TAG, "FfmpegLibrary.isAvailable() = " + FfmpegLibrary.isAvailable());
        Log.d(TAG, "VpxLibrary.isAvailable() = " + VpxLibrary.isAvailable());
        Log.d(TAG, "FlacLibrary.isAvailable() = " + FlacLibrary.isAvailable());
        Log.d(TAG, "OpusLibrary.isAvailable() = " + OpusLibrary.isAvailable());
        Log.d(TAG, "Gav1Library.isAvailable() = " + Gav1Library.isAvailable());
    }

    private void releaseExoPlayer() {
        if (exoPlayer != null) {
            exoPlayer.removeListener(mExoPlayerEventListener);
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private void initMediaSessionCompat() {
        // Create a MediaSessionCompat
        Activity mActivity = getActivity();

        mediaSessionCompat = new MediaSessionCompat(callingContext, PlayerConstants.LOG_TAG);

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
                ScreenUtil.showToast(callingContext, getString(R.string.noPlaylistString), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
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
        setImageButtonStatus();
    }

    private void playSingleSong(SongInfo songInfo) {

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

        if (mediaUri==null || Uri.EMPTY.equals(mediaUri)) {
            Log.d(TAG, "Media Uri is empty.");
            ScreenUtil.showToast(callingContext, "Media Uri is empty.", toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
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

        // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
        // pass the saved instance of playingParam to
        // MediaSessionConnector.PlaybackPreparer.onPrepareFromUri(Uri uri, Bundle extras)
        Bundle playingParamOriginExtras = new Bundle();
        playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
        mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
    }

    private void startPlay() {
        // if ( (mediaSource != null) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PLAYING) ) {
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

    private void pausePlay() {
        // if ( (mediaSource != null) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) ) {
        if ( (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_PAUSED) ) {
            // no media file opened or playing has been stopped
            mediaTransportControls.pause();
        }
    }

    private void stopPlay() {
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

    private void replayMedia() {
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

    private void setAudioTrackAndChannel(int audioTrackIndex, int audioChannel) {
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

    private void switchAudioToVocal() {
        int vocalAudioTrackIndex = playingParam.getVocalAudioTrackIndex();
        int vocalAudioChannel = playingParam.getVocalAudioChannel();
        playingParam.setMusicOrVocalOrNoSetting(PlayerConstants.PlayingVocal);
        setAudioTrackAndChannel(vocalAudioTrackIndex, vocalAudioChannel);
    }

    private void switchAudioToMusic() {
        int musicAudioTrackIndex = playingParam.getMusicAudioTrackIndex();
        int musicAudioChannel = playingParam.getMusicAudioChannel();
        playingParam.setMusicOrVocalOrNoSetting(PlayerConstants.PlayingMusic);
        setAudioTrackAndChannel(musicAudioTrackIndex, musicAudioChannel);
    }

    private void setAudioVolume(float volume) {
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
                    if (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) {
                        Log.d(TAG, "MediaControllerCallback--> Song was stopped by user.");
                    }
                    playMediaImageButton.setVisibility(View.VISIBLE);
                    pauseMediaImageButton.setVisibility(View.GONE);
                    break;
                case PlaybackStateCompat.STATE_PLAYING:
                    // when playing
                    Log.d(TAG, "PlaybackStateCompat.STATE_PLAYING");
                    playMediaImageButton.setVisibility(View.GONE);
                    pauseMediaImageButton.setVisibility(View.VISIBLE);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    Log.d(TAG, "PlaybackStateCompat.STATE_PAUSED");
                    playMediaImageButton.setVisibility(View.VISIBLE);
                    pauseMediaImageButton.setVisibility(View.GONE);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    // when finished playing
                    Log.d(TAG, "PlaybackStateCompat.STATE_STOPPED");
                    playMediaImageButton.setVisibility(View.VISIBLE);
                    pauseMediaImageButton.setVisibility(View.GONE);
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
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory()
                    .setTsExtractorFlags(FLAG_DETECT_ACCESS_UNITS)
                    .setTsExtractorFlags(FLAG_ALLOW_NON_IDR_KEYFRAMES);
            playingParam.setMediaSourcePrepared(false);
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(callingContext, Util.getUserAgent(callingContext, callingContext.getPackageName()));
            // MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            // mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(uri);
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(uri);
            exoPlayer.prepare(mediaSource);
            long currentAudioPosition = playingParam.getCurrentAudioPosition();
            float currentVolume = playingParam.getCurrentVolume();
            int playbackState = playbackState = playingParam.getCurrentPlaybackState();
            if (extras != null) {
                Log.d(TAG, "extras is not null.");
                PlayingParameters playingParamOrigin = extras.getParcelable(PlayerConstants.PlayingParamOrigin);
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

            Log.d(TAG, "Player.EventListener.onPlayerStateChanged is called.");
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
                            SubMenu subMenu = audioTrackMenuItem.getSubMenu();
                            int index=0;
                            for (index = 0; index < audioTrackIndicesList.size(); index++) {
                                // audio track index start from 1 for user interface
                                subMenu.getItem(index).setVisible(true);
                            }
                            for (int j=index; j<subMenu.size(); j++) {
                                subMenu.getItem(j).setVisible(false);
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
                        if (playingParam.getRepeatStatus() != PlayerConstants.NoRepeatPlaying) {
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
                    if (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) {
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
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            Log.d(TAG,"Player.EventListener.onTracksChanged() is called.");
        }
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            Log.d(TAG,"Player.EventListener.onIsPlayingChanged() is called.");
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.d(TAG,"Player.EventListener.onPlayerError() is called.");
            switch (error.type) {
                case ExoPlaybackException.TYPE_SOURCE:
                    Log.d(TAG, "TYPE_SOURCE: " + error.getSourceException().getMessage());
                    break;

                case ExoPlaybackException.TYPE_RENDERER:
                    Log.d(TAG, "TYPE_RENDERER: " + error.getRendererException().getMessage());
                    break;

                case ExoPlaybackException.TYPE_UNEXPECTED:
                    Log.d(TAG, "TYPE_UNEXPECTED: " + error.getUnexpectedException().getMessage());
                    break;
            }

            String formatNotSupportedString = getString(R.string.formatNotSupportedString);
            if (playingParam.isAutoPlay()) {
                // go to next one in the list
                if (canShowNotSupportedFormat) {
                    // only show once
                    ScreenUtil.showToast(callingContext, formatNotSupportedString, toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                    canShowNotSupportedFormat = false;
                }
                startAutoPlay();
            } else {
                ScreenUtil.showToast(callingContext, formatNotSupportedString, toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
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
