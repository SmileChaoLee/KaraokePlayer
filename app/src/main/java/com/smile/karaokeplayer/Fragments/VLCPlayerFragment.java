package com.smile.karaokeplayer.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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

import com.aditya.filebrowser.FileChooser;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.smile.karaokeplayer.BuildConfig;
import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.Models.SongListSQLite;
import com.smile.karaokeplayer.Models.VerticalSeekBar;
import com.smile.karaokeplayer.R;
import com.smile.karaokeplayer.SmileApplication;
import com.smile.karaokeplayer.SongListActivity;
import com.smile.karaokeplayer.Utilities.ExternalStorageUtil;
import com.smile.smilelibraries.privacy_policy.PrivacyPolicyUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VLCPlayerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VLCPlayerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VLCPlayerFragment extends Fragment {

    public static final String VLCPlayerFragmentTag = "VLCPlayerFragmentTag";

    private static final String TAG = new String(".VLCPlayerFragment");
    private static final String LOG_TAG = new String("MediaSessionCompatTag");
    private static final String PlayingParamOrigin = "PlayingParamOrigin";
    private static final int PrivacyPolicyActivityRequestCode = 10;
    private static final int FILE_READ_REQUEST_CODE = 1;
    private static final int SONG_LIST_ACTIVITY_CODE = 2;
    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = true;
    private static final int VLCPlayerView_Timeout = 10000;  //  10 seconds
    private static final int noVideoRenderer = -1;
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
    private View vlcPlayerFragmentView;

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
    // submenu of channel
    private MenuItem leftChannelMenuItem;
    private MenuItem rightChannelMenuItem;
    private MenuItem stereoChannelMenuItem;

    private LibVLC mLibVLC = null;
    private MediaPlayer vlcPlayer = null;
    private VLCVideoLayout videoVLCPlayerView;

    private MediaSessionCompat mediaSessionCompat;
    private MediaControllerCompat mediaControllerCompat;
    private MediaControllerCallback mediaControllerCallback;
    private MediaControllerCompat.TransportControls mediaTransportControls;

    private LinearLayout messageLinearLayout;
    private TextView bufferingStringTextView;
    private Animation animationText;
    private int colorRed;
    private int colorTransparent;
    private LinearLayout linearLayout_for_ads;
    private AdView bannerAdView;

    // instances of the following members have to be saved when configuration changed
    private Uri mediaUri;
    // private MediaSource mediaSource;
    private int numberOfVideoRenderers;
    private int numberOfAudioRenderers;
    // private SortedMap<String, Integer> videoRendererIndexMap;
    // private SortedMap<String, Integer> audioRendererIndexMap;
    private SortedMap<String, Integer> videoRendererIndexMap;
    private ArrayList<Integer> audioTrackIndexList;
    private ArrayList<SongInfo> publicSongList;
    private PlayingParameters playingParam;
    private boolean canShowNotSupportedFormat;
    private SongInfo songInfo;
    //

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String IsPlaySingleSongPara = "IsPlaySingleSong";
    public static final String SongInfoPara = "SongInfo";

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
        // TODO: Update argument type and name
        void setSupportActionBarForFragment(Toolbar toolbar);
        ActionBar getSupportActionBarForFragment();
        void onExitFragment();
    }

    public VLCPlayerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param isPlaySingleSong Parameter 1.
     * @param songInfo Parameter 2.
     * @return A new instance of fragment VLCPlayerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static VLCPlayerFragment newInstance(boolean isPlaySingleSong, SongInfo songInfo) {
        VLCPlayerFragment fragment = new VLCPlayerFragment();
        Bundle args = new Bundle();
        args.putBoolean(IsPlaySingleSongPara, isPlaySingleSong);
        args.putParcelable(SongInfoPara, songInfo);
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
        // LibVLCPlayer library has bugs when fragment retains instance (etRetainInstance(true);)
        // so retains instance has to be set to false
        setRetainInstance(false);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView() is called");

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(callingContext, SmileApplication.FontSize_Scale_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(callingContext, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(callingContext, SmileApplication.FontSize_Scale_Type, 0.0f);
        toastTextSize = 0.7f * textFontSize;

        // Inflate the layout for this fragment
        vlcPlayerFragmentView = inflater.inflate(R.layout.fragment_vlcplayer, container, false);

        return vlcPlayerFragmentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated() is called");

        colorRed = ContextCompat.getColor(callingContext, R.color.red);
        colorTransparent = ContextCompat.getColor(callingContext, android.R.color.transparent);

        initializeVariables(savedInstanceState);
        // Video player view
        videoVLCPlayerView = vlcPlayerFragmentView.findViewById(R.id.videoVLCPlayerView);
        videoVLCPlayerView.setVisibility(View.VISIBLE);
        videoVLCPlayerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "videoVLCPlayerView.onClick() is called.");
                supportToolbar.performClick();
            }
        });
        //
        initVLCPlayer();
        initMediaSessionCompat();
        // use custom toolbar
        supportToolbar = vlcPlayerFragmentView.findViewById(R.id.custom_toolbar);
        mListener.setSupportActionBarForFragment(supportToolbar);
        ActionBar actionBar = mListener.getSupportActionBarForFragment();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        supportToolbar.setVisibility(View.VISIBLE);
        supportToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int visibility = v.getVisibility();
                if (visibility == View.VISIBLE) {
                    // use custom toolbar
                    supportToolbar.setVisibility(View.GONE);
                    closeMenu(mainMenu);
                    showBannerAds();
                    Log.d(TAG, "supportToolbar.onClick() is called --> View.VISIBLE.");
                } else {
                    // use custom toolbar
                    supportToolbar.setVisibility(View.VISIBLE);
                    hideBannerAds();
                    Log.d(TAG, "supportToolbar.onClick() is called --> View.INVISIBLE.");
                }
                volumeSeekBar.setVisibility(View.INVISIBLE);

                Log.d(TAG, "supportToolbar.onClick() is called.");
            }
        });

        volumeSeekBar = vlcPlayerFragmentView.findViewById(R.id.volumeSeekBar);
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

        repeatImageButton = vlcPlayerFragmentView.findViewById(R.id.repeatImageButton);
        repeatImageButton.getLayoutParams().height = imageButtonHeight;
        repeatImageButton.getLayoutParams().width = imageButtonHeight;
        repeatImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int repeatStatus = playingParam.getRepeatStatus();
                switch (repeatStatus) {
                    case NoRepeatPlaying:
                        // switch to repeat all
                        playingParam.setRepeatStatus(RepeatAllSongs);
                        break;
                    case RepeatAllSongs:
                        // switch to repeat on song
                        playingParam.setRepeatStatus(RepeatOneSong);
                        break;
                    case RepeatOneSong:
                        // switch to no repeat but show symbol of repeat all song with transparent background
                        playingParam.setRepeatStatus(NoRepeatPlaying);
                        break;
                }
                setImageButtonStatus();
            }
        });

        switchToMusicImageButton = vlcPlayerFragmentView.findViewById(R.id.switchToMusicImageButton);
        switchToMusicImageButton.getLayoutParams().height = imageButtonHeight;
        switchToMusicImageButton.getLayoutParams().width = imageButtonHeight;
        switchToMusicImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchAudioToMusic();
            }
        });

        switchToVocalImageButton = vlcPlayerFragmentView.findViewById(R.id.switchToVocalImageButton);
        switchToVocalImageButton.getLayoutParams().height = imageButtonHeight;
        switchToVocalImageButton.getLayoutParams().width = imageButtonHeight;
        switchToVocalImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchAudioToVocal();
            }
        });

        setImageButtonStatus();

        int volumeSeekBarHeight = (int)(textFontSize * 2.0f);
        volumeSeekBar.getLayoutParams().width = volumeSeekBarHeight;
        supportToolbar.getLayoutParams().height = volumeImageButton.getLayoutParams().height + volumeSeekBar.getLayoutParams().height;
        Log.d(TAG, "supportToolbar = " + supportToolbar.getLayoutParams().height);

        // message area
        messageLinearLayout = vlcPlayerFragmentView.findViewById(R.id.messageLinearLayout);
        messageLinearLayout.setVisibility(View.GONE);
        bufferingStringTextView = vlcPlayerFragmentView.findViewById(R.id.bufferingStringTextView);
        ScreenUtil.resizeTextSize(bufferingStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        animationText = new AlphaAnimation(0.0f,1.0f);
        animationText.setDuration(500);
        animationText.setStartOffset(0);
        animationText.setRepeatMode(Animation.REVERSE);
        animationText.setRepeatCount(Animation.INFINITE);
        //

        // the original places for the two following statements
        // initVLCPlayer();
        // initMediaSessionCompat();

        linearLayout_for_ads = vlcPlayerFragmentView.findViewById(R.id.linearLayout_for_ads);
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
    }

    // TODO: Rename method, update argument and hook method into UI event
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
    public boolean onOptionsItemSelected(MenuItem item) {

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
                        playingParam.setPublicSongIndex(0);
                        // start playing video from list
                        // if (vlcPlayer.getPlaybackState() != Player.STATE_IDLE) {
                        if (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_NONE) {
                            // media is playing or prepared
                            // vlcPlayer.stop();// no need   // will go to onPlayerStateChanged()
                            Log.d(TAG, "isAutoPlay is true and vlcPlayer.stop().");

                        }
                        startAutoPlay();
                    } else {
                        String msg = getString(R.string.noPlaylistString);
                        ScreenUtil.showToast(callingContext, msg, toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
                    }
                } else {
                    playingParam.setAutoPlay(isAutoPlay);
                }
                setImageButtonStatus();
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
                Log.d(TAG, "R.id.action --> numberOfAudioRenderers = " + numberOfAudioRenderers);
                // if ( (mediaSource != null) && (numberOfAudioRenderers>0) ) {
                if ( (mediaUri != null) && (numberOfAudioRenderers>0) ) {
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
                    // if ((mediaSource != null) && (mCurrentState == PlaybackStateCompat.STATE_NONE)) {
                    if ((mediaUri != null) && (mCurrentState == PlaybackStateCompat.STATE_NONE)) {
                        stopMenuItem.setCheckable(true);
                        stopMenuItem.setChecked(true);
                    } else {
                        stopMenuItem.setCheckable(false);
                    }
                    // toTvMenuItem
                } else {
                    Log.d(TAG, "R.id.action --> mediaUri is null or numberOfAudioRenderers = 0.");
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
                // if there are audio tracks
                SubMenu subMenu = item.getSubMenu();
                ScreenUtil.resizeMenuTextSize(subMenu, fontScale);
                // check if MenuItems of audioTrack need CheckBox
                for (int i=0; i<subMenu.size(); i++) {
                    MenuItem mItem = subMenu.getItem(i);
                    // audio track index start from 1 for user interface
                    if ( (i+1) == playingParam.getCurrentAudioRendererPlayed() ) {
                        mItem.setCheckable(true);
                        mItem.setChecked(true);
                    } else {
                        mItem.setCheckable(false);
                    }
                }
                //
                break;
            case R.id.channel:
                // if ( (mediaSource != null) && (numberOfAudioRenderers>0) ) {
                if ( (mediaUri != null) && (numberOfAudioRenderers>0) ) {
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
                setAudioTrackAndChannel(playingParam.getCurrentAudioRendererPlayed(), SmileApplication.leftChannel);
                playingParam.setCurrentChannelPlayed(SmileApplication.leftChannel);
                break;
            case R.id.rightChannel:
                setAudioTrackAndChannel(playingParam.getCurrentAudioRendererPlayed(), SmileApplication.rightChannel);
                playingParam.setCurrentChannelPlayed(SmileApplication.rightChannel);
                break;
            case R.id.stereoChannel:
                setAudioTrackAndChannel(playingParam.getCurrentAudioRendererPlayed(), SmileApplication.stereoChannel);
                playingParam.setCurrentChannelPlayed(SmileApplication.stereoChannel);
                break;
        }

        // deal with switching audio track
        if ( (mediaUri != null) && (numberOfAudioRenderers>0) ) {
            if (item.getTitle() != null) {
                String itemTitle = item.getTitle().toString();
                Log.d(TAG, "itemTitle = " + itemTitle);
                if (audioTrackMenuItem.hasSubMenu()) {
                    SubMenu subMenu = audioTrackMenuItem.getSubMenu();
                    int menuSize = subMenu.size();
                    for (int index=0; index<menuSize; index++) {
                        if (itemTitle.equals(subMenu.getItem(index).getTitle().toString())) {
                            setAudioTrackAndChannel(index + 1, currentChannelPlayed);
                            break;
                        }
                    }
                }
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        Log.d(TAG,"VLCPlayerFragment-->onResume() is called.");
        videoVLCPlayerView.requestFocus();
        vlcPlayer.attachViews(videoVLCPlayerView, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);
        super.onResume();
    }
    @Override
    public void onPause() {
        Log.d(TAG,"VLCPlayerFragment-->onPause() is called.");
        vlcPlayer.detachViews();
        super.onPause();
    }
    @Override
    public void onStop() {
        Log.d(TAG,"VLCPlayerFragment-->onStop() is called.");
        super.onStop();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Log.d(TAG,"VLCPlayerFragment-->onConfigurationChanged() is called.");
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
        Log.d(TAG,"ExoVLCPlayerFragment-->onSaveInstanceState() is called.");

        outState.putInt("NumberOfVideoRenderers",numberOfVideoRenderers);
        outState.putInt("NumberOfAudioRenderers", numberOfAudioRenderers);
        outState.putSerializable("VideoRendererIndexMap", new HashMap<String, Integer>(videoRendererIndexMap));
        outState.putIntegerArrayList("AudioTrackIndexList", audioTrackIndexList);
        outState.putParcelableArrayList("PublicSongList", publicSongList);

        outState.putParcelable("MediaUri", mediaUri);
        Log.d(TAG, "onSaveInstanceState() --> playingParam.getCurrentPlaybackState() = " + playingParam.getCurrentPlaybackState());
        // ?????????
        playingParam.setCurrentAudioPosition((long)vlcPlayer.getPosition());

        outState.putParcelable("PlayingParameters", playingParam);
        outState.putBoolean("CanShowNotSupportedFormat", canShowNotSupportedFormat);
        outState.putParcelable(SongInfoPara, songInfo);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"VLCPlayerFragment-->onDestroy() is called.");
        releaseMediaSessionCompat();
        releaseVLCPlayer();
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

                playingParam.setCurrentVideoRendererPlayed(0);

                int currentAudioRederer = 0;
                playingParam.setMusicAudioRenderer(currentAudioRederer);
                playingParam.setVocalAudioRenderer(currentAudioRederer);
                playingParam.setCurrentAudioRendererPlayed(currentAudioRederer);

                playingParam.setMusicAudioChannel(SmileApplication.leftChannel);
                playingParam.setVocalAudioChannel(SmileApplication.stereoChannel);
                playingParam.setCurrentChannelPlayed(SmileApplication.stereoChannel);

                playingParam.setCurrentAudioPosition(0);
                playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);
                playingParam.setMediaSourcePrepared(false);

                // music or vocal is unknown
                playingParam.setMusicOrVocalOrNoSetting(MusicOrVocalUnknown);
                Bundle playingParamOriginExtras = new Bundle();
                playingParamOriginExtras.putParcelable(PlayingParamOrigin, playingParam);

                if (!SmileApplication.UseAdityaFileBrowser) {
                    // not useFileChooser.class in com.adityak:browsemyfiles:1.9
                    String filePath = ExternalStorageUtil.getUriRealPath(callingContext, mediaUri);
                    if (filePath != null) {
                        if (!filePath.isEmpty()) {
                            File songFile = new File(filePath);
                            mediaUri = Uri.fromFile(songFile);   // ACTION_GET_CONTENT
                        }
                    }
                }

                Log.i(TAG, "mediaUri = " + mediaUri.toString());
                if ((mediaUri != null) && (!Uri.EMPTY.equals(mediaUri))) {
                    // has Uri
                    mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
                }
            }
            return;
        }
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
            case NoRepeatPlaying:
                // switch to repeat all
                repeatImageButton.setImageResource(R.drawable.repeat_all_white);
                break;
            case RepeatAllSongs:
                // switch to repeat on song
                repeatImageButton.setImageResource(R.drawable.repeat_one_white);
                break;
            case RepeatOneSong:
                // switch to no repeat but show symbol of repeat all song with transparent background
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
            messageLinearLayout.setVisibility(View.VISIBLE);
        }
    }
    private void hideNativeAds() {
        // simulate hide native ad
        if (BuildConfig.DEBUG) {
            messageLinearLayout.setVisibility(View.GONE);
        }
    }

    private void initializePlayingParam() {
        playingParam = new PlayingParameters();
        playingParam.setAutoPlay(false);
        playingParam.setMediaSourcePrepared(false);
        playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);

        playingParam.setCurrentVideoRendererPlayed(0);

        playingParam.setMusicAudioRenderer(1);
        playingParam.setVocalAudioRenderer(1);
        playingParam.setCurrentAudioRendererPlayed(playingParam.getMusicAudioRenderer());
        playingParam.setMusicAudioChannel(SmileApplication.leftChannel);     // default
        playingParam.setVocalAudioChannel(SmileApplication.stereoChannel);   // default
        playingParam.setCurrentChannelPlayed(playingParam.getMusicAudioChannel());
        playingParam.setCurrentAudioPosition(0);
        playingParam.setCurrentVolume(1.0f);

        playingParam.setPublicSongIndex(0);
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

            numberOfVideoRenderers = 0;
            numberOfAudioRenderers = 0;
            videoRendererIndexMap = new TreeMap<>();
            audioTrackIndexList = new ArrayList<>();

            mediaUri = null;
            initializePlayingParam();
            canShowNotSupportedFormat = false;
            songInfo = null;    // default
            Bundle arguments = getArguments();
            if (arguments != null) {
                playingParam.setPlaySingleSong(arguments.getBoolean(IsPlaySingleSongPara));
                songInfo = arguments.getParcelable(SongInfoPara);
            }
        } else {
            // needed to be set
            numberOfVideoRenderers = savedInstanceState.getInt("NumberOfVideoRenderers",0);
            numberOfAudioRenderers = savedInstanceState.getInt("NumberOfAudioRenderers");
            HashMap<String, Integer> videoHashMap = (HashMap)savedInstanceState.get("VideoRendererIndexMap");
            videoRendererIndexMap = new TreeMap<>();
            videoRendererIndexMap.putAll(videoHashMap);
            HashMap<String, Integer> audioHashMap = (HashMap)savedInstanceState.get("AudioRendererIndexMap");
            audioTrackIndexList = savedInstanceState.getIntegerArrayList("AudioTrackIndexList");
            publicSongList = savedInstanceState.getParcelableArrayList("PublicSongList");

            mediaUri = savedInstanceState.getParcelable("MediaUri");
            playingParam = savedInstanceState.getParcelable("PlayingParameters");
            canShowNotSupportedFormat = savedInstanceState.getBoolean("CanShowNotSupportedFormat");
            if (playingParam == null) {
                initializePlayingParam();
            }
            songInfo = savedInstanceState.getParcelable(SongInfoPara);
        }
    }

    private void selectFileToOpen() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent;
        if (!SmileApplication.UseAdityaFileBrowser) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            } else {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
            }
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
        } else {
            intent = new Intent(callingContext, FileChooser.class);
            intent.putExtra(com.aditya.filebrowser.Constants.SELECTION_MODE, com.aditya.filebrowser.Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
        }
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

    private void initVLCPlayer() {
        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        mLibVLC = new LibVLC(callingContext, args);
        vlcPlayer = new MediaPlayer(mLibVLC);
        vlcPlayer.setEventListener(new VLCPlayerEventListener());
    }

    private void releaseVLCPlayer() {
        if (vlcPlayer != null) {
            vlcPlayer.stop();
            vlcPlayer.detachViews();
            vlcPlayer.release();
            vlcPlayer = null;
        }
        if (mLibVLC != null) {
            mLibVLC.release();
            mLibVLC = null;
        }
    }

    private void setMediaPlaybackState(int state) {
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        } else {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
        }
        playbackStateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        // playbackStateBuilder.setState(state, exoPlayer.getContentPosition(), 1f);
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
    }

    private void initMediaSessionCompat() {
        // Create a MediaSessionCompat
        mediaSessionCompat = new MediaSessionCompat(callingContext, LOG_TAG);
        MediaSessionCallback mediaSessionCallback = new MediaSessionCallback();
        mediaSessionCompat.setCallback(mediaSessionCallback);
        // Do not let MediaButtons restart the player when the app is not visible
        mediaSessionCompat.setMediaButtonReceiver(null);
        mediaSessionCompat.setActive(true); // might need to find better place to put
        setMediaPlaybackState(playingParam.getCurrentPlaybackState());

        // Create a MediaControllerCompat
        mediaControllerCompat = new MediaControllerCompat(callingContext, mediaSessionCompat);
        MediaControllerCompat.setMediaController(getActivity(), mediaControllerCompat);
        mediaControllerCallback = new MediaControllerCallback();
        mediaControllerCompat.registerCallback(mediaControllerCallback);
        mediaTransportControls = mediaControllerCompat.getTransportControls();
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
                int publicSongIndex = playingParam.getPublicSongIndex();
                switch (repeatStatus) {
                    case NoRepeatPlaying:
                        // no repeat
                        if (publicSongIndex >= publicSongListSize) {
                            // stop playing
                            playingParam.setAutoPlay(false);
                            stopPlay();
                            stillPlayNext = false;  // stop here and do not go to next
                        }
                        break;
                    case RepeatOneSong:
                        // repeat one song
                        if ( (publicSongIndex > 0) && (publicSongIndex <= publicSongListSize) ) {
                            publicSongIndex--;
                        }
                        break;
                    case RepeatAllSongs:
                        // repeat all songs
                        if (publicSongIndex >= publicSongListSize) {
                            publicSongIndex = 0;
                        }
                        break;
                }

                if (stillPlayNext) {    // still play the next song
                    songInfo = publicSongList.get(publicSongIndex);
                    playSingleSong(songInfo);
                    publicSongIndex++;  // set next index of playlist that will be played
                    playingParam.setPublicSongIndex(publicSongIndex);
                }
                Log.d(TAG, "startAutoPlay() finished --> " + publicSongIndex--);
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
        setImageButtonStatus();
    }

    private void playSingleSong(SongInfo songInfo) {
        playingParam.setMusicOrVocalOrNoSetting(PlayingVocal);  // presume vocal
        playingParam.setCurrentVideoRendererPlayed(0);

        playingParam.setMusicAudioRenderer(songInfo.getMusicTrackNo());
        playingParam.setMusicAudioChannel(songInfo.getMusicChannel());

        playingParam.setVocalAudioRenderer(songInfo.getVocalTrackNo());
        playingParam.setCurrentAudioRendererPlayed(playingParam.getVocalAudioRenderer());
        playingParam.setVocalAudioChannel(songInfo.getVocalChannel());
        playingParam.setCurrentChannelPlayed(playingParam.getVocalAudioChannel());

        playingParam.setCurrentAudioPosition(0);
        playingParam.setCurrentPlaybackState(PlaybackStateCompat.STATE_NONE);
        playingParam.setMediaSourcePrepared(false);

        String filePath = songInfo.getFilePath();
        Log.i(TAG, "filePath : " + filePath);
        // File songFile = new File(filePath);
        // if (songFile.exists()) {
        // mediaUri = Uri.fromFile(new File(filePath));
        // mediaUri = Uri.parse("file://" + filePath);
        mediaUri = Uri.parse(filePath);
        Log.i(TAG, "mediaUri from filePath : " + mediaUri);
        // to avoid the bugs from MediaSessionConnector or MediaControllerCallback
        // pass the saved instance of playingParam to
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
        // if ( (mediaSource != null) && (numberOfAudioRenderers>0) ) {
        if ( (mediaUri != null) && (numberOfAudioRenderers>0) ) {
            long currentAudioPosition = 0;
            playingParam.setCurrentAudioPosition(currentAudioPosition);
            if (playingParam.isMediaSourcePrepared()) {
                vlcPlayer.setPosition(currentAudioPosition);
                setProperAudioTrackAndChannel();
                if (!vlcPlayer.isPlaying()) {
                    vlcPlayer.play();
                }
                Log.d(TAG, "replayMedia()--> vlcPlayer.seekTo(currentAudioPosition).");
            } else {
                Bundle playingParamOriginExtras = new Bundle();
                playingParamOriginExtras.putParcelable(PlayingParamOrigin, playingParam);
                mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);   // prepare and play
                Log.d(TAG, "replayMedia()--> mediaTransportControls.prepareFromUri().");
            }
        }

        Log.d(TAG, "replayMedia() is called.");
    }

    private void setAudioTrackAndChannel(int audioTrackIndex, int audioChannel) {
        if (numberOfAudioRenderers > 0) {
            // select audio track
            int audioTrackId = audioTrackIndexList.get(audioTrackIndex - 1);
            vlcPlayer.setAudioTrack(audioTrackId);
            playingParam.setCurrentAudioRendererPlayed(audioTrackIndex);

            // select audio channel
            vlcPlayer.setChapter(audioChannel);
            playingParam.setCurrentChannelPlayed(audioChannel);

            setAudioVolume(playingParam.getCurrentVolume());
        }
    }

    private void switchAudioToVocal() {
        int vocalAudioRenderer = playingParam.getVocalAudioRenderer();
        int vocalAudioChannel = playingParam.getVocalAudioChannel();
        playingParam.setMusicOrVocalOrNoSetting(PlayingVocal);
        setAudioTrackAndChannel(vocalAudioRenderer, vocalAudioChannel);
    }

    private void switchAudioToMusic() {
        int musicAudioRenderer = playingParam.getMusicAudioRenderer();
        int musicAudioChannel = playingParam.getMusicAudioChannel();
        playingParam.setMusicOrVocalOrNoSetting(PlayingMusic);
        setAudioTrackAndChannel(musicAudioRenderer, musicAudioChannel);
    }

    private void setAudioVolume(float volume) {
        //
        // vlcPlayer.setVolume((int)volume);
        //
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

    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
        }

        @Override
        public void onPrepare() {
            super.onPrepare();
            Log.d(TAG, "MediaSessionCallback.onPrepare() is called.");
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            super.onPrepareFromMediaId(mediaId, extras);
            Log.d(TAG, "MediaSessionCallback.onPrepareFromMediaId() is called.");
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            super.onPrepareFromUri(uri, extras);
            playingParam.setMediaSourcePrepared(false);
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
            setAudioVolume(currentVolume);
            vlcPlayer.setPosition((float) currentAudioPosition);
            try {
                switch (playbackState) {
                    case PlaybackStateCompat.STATE_PAUSED:
                        vlcPlayer.pause();
                        setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                        Log.d(TAG, "setMediaSourcePrepared() --> PlaybackStateCompat.STATE_PAUSED");
                        break;
                    case PlaybackStateCompat.STATE_STOPPED:
                        vlcPlayer.stop();
                        setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                        Log.d(TAG, "setMediaSourcePrepared() --> PlaybackStateCompat.STATE_STOPPED");
                        break;
                    case PlaybackStateCompat.STATE_PLAYING:
                    case PlaybackStateCompat.STATE_NONE:
                        // start playing when ready or just start new playing
                        Media mediaSource = new Media(mLibVLC, uri);
                        vlcPlayer.setMedia(mediaSource);
                        vlcPlayer.play();
                        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                        Log.d(TAG, "setMediaSourcePrepared() --> PlaybackStateCompat.STATE_PLAYING");
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Invalid mediaId");
            }
            Log.d(TAG, "MediaSessionCallback.onPrepareFromUri() is called.");
        }

        @Override
        public void onPlay() {
            super.onPlay();
            MediaControllerCompat controller = mediaSessionCompat.getController();
            PlaybackStateCompat stateCompat = controller.getPlaybackState();
            int state = stateCompat.getState();
            if (state != PlaybackStateCompat.STATE_PLAYING) {
                int playerState = vlcPlayer.getPlayerState();
                if (!vlcPlayer.isPlaying()) {
                    vlcPlayer.play();
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }
            }
            Log.d(TAG, "MediaSessionCallback.onPlay() is called.");
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            Log.d(TAG, "MediaSessionCallback.onPlayFromMediaId() is called.");
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            super.onPlayFromUri(uri, extras);
            Log.d(TAG, "MediaSessionCallback.onPlayFromUri() is called.");
        }

        @Override
        public void onPause() {
            super.onPause();
            MediaControllerCompat controller = mediaSessionCompat.getController();
            PlaybackStateCompat stateCompat = controller.getPlaybackState();
            int state = stateCompat.getState();
            if (state != PlaybackStateCompat.STATE_PAUSED) {
                vlcPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            }
            Log.d(TAG, "MediaSessionCallback.onPause() is called.");
        }

        @Override
        public void onStop() {
            super.onStop();
            MediaControllerCompat controller = mediaSessionCompat.getController();
            PlaybackStateCompat stateCompat = controller.getPlaybackState();
            int state = stateCompat.getState();
            if (state != PlaybackStateCompat.STATE_STOPPED) {
                vlcPlayer.stop();
                setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            }
            Log.d(TAG, "MediaSessionCallback.onStop() is called.");
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            setMediaPlaybackState(PlaybackStateCompat.STATE_FAST_FORWARDING);
            Log.d(TAG, "MediaSessionCallback.onFastForward() is called.");
        }

        @Override
        public void onRewind() {
            super.onRewind();
            setMediaPlaybackState(PlaybackStateCompat.STATE_REWINDING);
            Log.d(TAG, "MediaSessionCallback.onRewind() is called.");
        }
    }

    private class MediaControllerCallback extends MediaControllerCompat.Callback {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
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

    private class VLCPlayerEventListener implements MediaPlayer.EventListener {

        @Override
        public void onEvent(MediaPlayer.Event event) {

            switch(event.type) {
                case MediaPlayer.Event.Buffering:
                    Log.d(TAG, "vlcPlayer is buffering.");
                    if (!vlcPlayer.isPlaying()) {
                        showBufferingMessage();
                    }
                    break;
                case MediaPlayer.Event.Playing:
                    Log.d(TAG, "vlcPlayer is being played.");
                    playingParam.setMediaSourcePrepared(true);  // has been prepared
                    numberOfVideoRenderers = vlcPlayer.getVideoTracksCount();
                    Log.d(TAG, "numberOfVideoRenderers = " + numberOfVideoRenderers);
                    if (numberOfVideoRenderers == 0) {
                        playingParam.setCurrentVideoRendererPlayed(noVideoRenderer);
                    } else {
                        // set which video track to be played
                        int videoTrack = playingParam.getCurrentVideoRendererPlayed();
                        playingParam.setCurrentVideoRendererPlayed(videoTrack);
                    }

                    // for testing
                    numberOfAudioRenderers = vlcPlayer.getAudioTracksCount();
                    Log.d(TAG, "numberOfAudioRenderers = " + numberOfAudioRenderers);

                    numberOfAudioRenderers = 0;
                    MediaPlayer.TrackDescription audioDis[] = vlcPlayer.getAudioTracks();
                    int audioTrackId;
                    String audioTrackName;
                    if (audioDis != null) {
                        // because it is null sometimes
                        for (int i = 0; i < audioDis.length; i++) {
                            audioTrackId = audioDis[i].id;
                            audioTrackName = audioDis[i].name;
                            Log.d(TAG, "audioDis[i].id = " + audioTrackId);
                            Log.d(TAG, "audioDis[i].name = " + audioDis[i].name);
                            if (audioTrackId >= 0) {
                                // enabled audio track
                                numberOfAudioRenderers++;
                                audioTrackIndexList.add(audioTrackId);
                            }
                        }
                    }
                    if (numberOfAudioRenderers == 0) {
                        playingParam.setCurrentAudioRendererPlayed(noAudioTrack);
                        playingParam.setCurrentChannelPlayed(noAudioChannel);
                    } else {
                        audioTrackId = vlcPlayer.getAudioTrack();
                        int audioTrackIndex = 1;    // default audio track index
                        int audioChannel = SmileApplication.stereoChannel;
                        Log.d(TAG, "vlcPlayer.getAudioTrack() = " + audioTrackId);
                        if (playingParam.isAutoPlay() || playingParam.isPlaySingleSong()) {
                            audioTrackIndex = playingParam.getCurrentAudioRendererPlayed();
                            audioChannel = playingParam.getCurrentChannelPlayed();
                        } else {
                            for (int index = 0; index< audioTrackIndexList.size(); index++) {
                                int audioId = audioTrackIndexList.get(index);
                                if (audioId == audioTrackId) {
                                    audioTrackIndex = index + 1;
                                    break;
                                }
                            }
                        }
                        setAudioTrackAndChannel(audioTrackIndex, audioChannel);

                        // build R.id.audioTrack submenu
                        if (audioTrackMenuItem != null) {
                            if (!audioTrackMenuItem.hasSubMenu()) {
                                // no sub menu
                                ((Menu) audioTrackMenuItem).addSubMenu("Text title");
                            }
                            SubMenu subMenu = audioTrackMenuItem.getSubMenu();
                            subMenu.clear();
                            for (int index=0; index<audioTrackIndexList.size(); index++) {
                                // audio track index start from 1 for user interface
                                audioTrackName = getString(R.string.audioTrackString) + " " + (index+1);
                                subMenu.add(audioTrackName);
                            }
                        }
                    }
                    dismissBufferingMessage();
                    hideNativeAds();
                    break;
                case MediaPlayer.Event.Paused:
                    Log.d(TAG, "vlcPlayer is paused");
                    dismissBufferingMessage();
                    hideNativeAds();
                    break;
                case MediaPlayer.Event.Stopped:
                    Log.d(TAG, "vlcPlayer is stopped");
                    dismissBufferingMessage();
                    showNativeAds();
                    break;
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "vlcPlayer is Reached end.");
                    mediaTransportControls.stop();
                    break;
                case MediaPlayer.Event.Opening:
                    Log.d(TAG, "vlcPlayer is Opening media.");
                    break;
                case MediaPlayer.Event.PositionChanged:
                    break;
                case MediaPlayer.Event.EncounteredError:
                    Log.d(TAG, "vlcPlayer is EncounteredError event");
                    showNativeAds();
                    break;
                default:
                    // showNativeAds();
                    break;
            }
        }
    }

}
