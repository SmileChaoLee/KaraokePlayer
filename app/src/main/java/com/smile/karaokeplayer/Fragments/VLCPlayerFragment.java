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
import android.os.Handler;
import android.os.Looper;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatSeekBar;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.smile.karaokeplayer.BuildConfig;
import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
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

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VLCPlayerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VLCPlayerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VLCPlayerFragment extends Fragment {

    private static final String TAG = new String(".VLCPlayerFragment");
    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = true;

    private float textFontSize;
    private float fontScale;
    private float toastTextSize;

    private Context callingContext;
    private View fragmentView;

    private Toolbar supportToolbar;  // use customized ToolBar
    private ImageButton repeatImageButton;
    private ImageButton switchToMusicImageButton;
    private ImageButton switchToVocalImageButton;
    private ImageButton actionMenuImageButton;
    private ActionMenuView actionMenuView;
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

    private LibVLC mLibVLC = null;
    private MediaPlayer vlcPlayer = null;
    private VLCVideoLayout videoVLCPlayerView;

    private VerticalSeekBar volumeSeekBar;
    private ImageButton volumeImageButton;
    private LinearLayout audioControllerView;
    private ImageButton previousMediaImageButton;
    private ImageButton playMediaImageButton;
    private ImageButton replayMediaImageButton;
    private ImageButton pauseMediaImageButton;
    private ImageButton stopMediaImageButton;
    private ImageButton nextMediaImageButton;

    private TextView playingTimeTextView;
    private AppCompatSeekBar player_duration_seekbar;
    private TextView durationTimeTextView;

    private LinearLayout linearLayout_for_ads;
    private LinearLayout messageLinearLayout;
    private TextView bufferingStringTextView;
    private Animation animationText;
    private LinearLayout nativeAdsLinearLayout;
    private TextView nativeAdsStringTextView;

    // instances of the following members have to be saved when configuration changed
    private Uri mediaUri;
    // private MediaSource mediaSource;
    private int numberOfVideoTracks;
    private int numberOfAudioTracks;
    private ArrayList<Integer> videoTrackIndicesList;
    private ArrayList<Integer> audioTrackIndicesList;
    private ArrayList<SongInfo> publicSongList;
    private PlayingParameters playingParam;
    private boolean canShowNotSupportedFormat;
    private SongInfo songInfo;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            timerHandler.removeCallbacksAndMessages(null);
            if (playingParam.isMediaSourcePrepared()) {
                if (supportToolbar.getVisibility() == View.VISIBLE) {
                    // hide supportToolbar
                    hideSupportToolbarAndAudioController();
                }
            } else {
                showSupportToolbarAndAudioController();
            }
        }
    };

    // temporary settings
    private boolean useFilePicker = true;
    //

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
    public static VLCPlayerFragment newInstance(boolean isPlaySingleSong, SongInfo songInfo) {
        VLCPlayerFragment fragment = new VLCPlayerFragment();
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
        // LibVLCPlayer library has bugs when fragment retains instance (etRetainInstance(true);)
        // so retains instance has to be set to false
        setRetainInstance(false);
        setHasOptionsMenu(true);

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(callingContext, ScreenUtil.FontSize_Pixel_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(callingContext, defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(callingContext, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        toastTextSize = 0.7f * textFontSize;
        useFilePicker = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView() is called");

        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_vlcplayer, container, false);
        if (fragmentView == null) {
            // exit
            mListener.onExitFragment();
        }

        initializeVariables(savedInstanceState);

        // Video player view
        videoVLCPlayerView = fragmentView.findViewById(R.id.videoVLCPlayerView);
        videoVLCPlayerView.setVisibility(View.VISIBLE);

        initVLCPlayer();
        initMediaSessionCompat();

        // use custom toolbar
        supportToolbar = fragmentView.findViewById(R.id.custom_toolbar);
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
                float currentVolume = (float)i / (float) PlayerConstants.MaxProgress;
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
        currentProgress = (int)(currentVolume * PlayerConstants.MaxProgress);
        volumeSeekBar.setProgressAndThumb(currentProgress);
        volumeImageButton = fragmentView.findViewById(R.id.volumeImageButton);

        //
        audioControllerView = fragmentView.findViewById(R.id.audioControllerView);
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

        setToolbarImageButtonStatus();

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
        messageLinearLayout.setVisibility(View.INVISIBLE);
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
        playingTimeTextView = fragmentView.findViewById(R.id.playingTimeTextView);
        playingTimeTextView.setText("000:00");
        ScreenUtil.resizeTextSize(playingTimeTextView, durationTextSize, ScreenUtil.FontSize_Pixel_Type);

        player_duration_seekbar = fragmentView.findViewById(R.id.player_duration_seekbar);

        durationTimeTextView = fragmentView.findViewById(R.id.durationTimeTextView);
        durationTimeTextView.setText("000:00");
        ScreenUtil.resizeTextSize(durationTimeTextView, durationTextSize, ScreenUtil.FontSize_Pixel_Type);

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
            Log.d(TAG, "onCreateView() --> playingParam.getCurrentPlaybackState() = " + playbackState);
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
                setToolbarImageButtonStatus();
                break;
            case R.id.songList:
                Intent songListIntent = new Intent(callingContext, SongListActivity.class);
                startActivityForResult(songListIntent, PlayerConstants.SONG_LIST_ACTIVITY_CODE);
                break;
            case R.id.open:
                if (!playingParam.isAutoPlay()) {
                    // isMediaSourcePrepared = false;
                    selectFileToOpen();
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
    public void onStart() {
        super.onStart();
        Log.d(TAG,"VLCPlayerFragment-->onStart() is called.");
        videoVLCPlayerView.requestFocus();
        vlcPlayer.attachViews(videoVLCPlayerView, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"VLCPlayerFragment-->onResume() is called.");
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"VLCPlayerFragment-->onPause() is called.");
    }

    @Override
    public void onStop() {
        super.onStop();
        vlcPlayer.detachViews();
        Log.d(TAG,"VLCPlayerFragment-->onStop() is called.");
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Log.d(TAG,"VLCPlayerFragment-->onConfigurationChanged() is called.");
        super.onConfigurationChanged(newConfig);
        // mainMenu.close();
        closeMenu(mainMenu);
        setButtonsPositionAndSize(newConfig);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG,"ExoVLCPlayerFragment-->onSaveInstanceState() is called.");

        outState.putInt("NumberOfVideoTracks", numberOfVideoTracks);
        outState.putInt("NumberOfAudioTracks", numberOfAudioTracks);
        outState.putIntegerArrayList("VideoTrackIndexList", videoTrackIndicesList);
        outState.putIntegerArrayList("AudioTrackIndexList", audioTrackIndicesList);
        outState.putParcelableArrayList("PublicSongList", publicSongList);

        outState.putParcelable("MediaUri", mediaUri);
        Log.d(TAG, "onSaveInstanceState() --> playingParam.getCurrentPlaybackState() = " + playingParam.getCurrentPlaybackState());
        playingParam.setCurrentAudioPosition(vlcPlayer.getTime());

        outState.putParcelable("PlayingParameters", playingParam);
        outState.putBoolean("CanShowNotSupportedFormat", canShowNotSupportedFormat);
        outState.putParcelable(SongInfoState, songInfo);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
                Bundle playingParamOriginExtras = new Bundle();
                playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);

                if (useFilePicker) {
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"VLCPlayerFragment-->onDestroy() is called.");
        releaseMediaSessionCompat();
        releaseVLCPlayer();

        timerHandler.removeCallbacksAndMessages(null);
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
        actionMenuView.setOverflowIcon(iconDrawable);   // set icon of three dots
        // supportToolbar.setOverflowIcon(iconDrawable);   // set icon of three dots

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
                if (nextIndex < 0) {
                    // is going to play the last one
                    nextIndex = publicSongListSize - 1; // the last one
                }
                if (repeatStatus == PlayerConstants.RepeatOneSong) {
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
                if (repeatStatus == PlayerConstants.RepeatOneSong) {
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

        actionMenuImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // getActivity().openOptionsMenu();
                actionMenuView.showOverflowMenu();
            }
        });

        player_duration_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // update the duration on controller UI
                float positionTime = progress / 1000.0f;   // seconds
                int minutes = (int)(positionTime / 60.0f);    // minutes
                int seconds = (int)positionTime - (minutes * 60);
                String durationString = String.format("%3d:%02d", minutes, seconds);
                playingTimeTextView.setText(durationString);
                if (fromUser) {
                    vlcPlayer.setTime(progress);
                }
                playingParam.setCurrentAudioPosition(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        supportToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int visibility = v.getVisibility();
                if (visibility == View.VISIBLE) {
                    // use custom toolbar
                    hideSupportToolbarAndAudioController();
                    Log.d(TAG, "supportToolbar.onClick() is called --> View.VISIBLE.");
                } else {
                    // use custom toolbar
                    showSupportToolbarAndAudioController();
                    setTimerToHideSupportAndAudioController();
                    Log.d(TAG, "supportToolbar.onClick() is called --> View.INVISIBLE.");
                }
                volumeSeekBar.setVisibility(View.INVISIBLE);

                Log.d(TAG, "supportToolbar.onClick() is called.");
            }
        });

        videoVLCPlayerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "videoVLCPlayerView.onClick() is called.");
                supportToolbar.performClick();
            }
        });
    }

    private void setTimerToHideSupportAndAudioController() {
        timerHandler.removeCallbacksAndMessages(null);
        timerHandler.postDelayed(timerRunnable, PlayerConstants.PlayerView_Timeout); // 10 seconds
    }

    private void showSupportToolbarAndAudioController() {
        supportToolbar.setVisibility(View.VISIBLE);
        audioControllerView.setVisibility(View.VISIBLE);
    }

    private void hideSupportToolbarAndAudioController() {
        supportToolbar.setVisibility(View.GONE);
        audioControllerView.setVisibility(View.GONE);
        closeMenu(mainMenu);
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
        Log.d(TAG, "showNativeAds() is called.");
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
        } else {
            // needed to be set
            numberOfVideoTracks = savedInstanceState.getInt("NumberOfVideoTracks",0);
            numberOfAudioTracks = savedInstanceState.getInt("NumberOfAudioTracks");
            videoTrackIndicesList = savedInstanceState.getIntegerArrayList("VideoTrackIndexList");
            audioTrackIndicesList = savedInstanceState.getIntegerArrayList("AudioTrackIndexList");
            publicSongList = savedInstanceState.getParcelableArrayList("PublicSongList");

            mediaUri = savedInstanceState.getParcelable("MediaUri");
            playingParam = savedInstanceState.getParcelable("PlayingParameters");
            canShowNotSupportedFormat = savedInstanceState.getBoolean("CanShowNotSupportedFormat");
            if (playingParam == null) {
                initializePlayingParam();
            }
            songInfo = savedInstanceState.getParcelable(SongInfoState);
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
        startActivityForResult(intent, PlayerConstants.FILE_READ_REQUEST_CODE);
    }

    private void showBufferingMessage() {
        messageLinearLayout.setVisibility(View.VISIBLE);
        bufferingStringTextView.startAnimation(animationText);
    }
    private void dismissBufferingMessage() {
        messageLinearLayout.setVisibility(View.INVISIBLE);
        animationText.cancel();
    }

    private void initVLCPlayer() {
        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        mLibVLC = new LibVLC(callingContext, args);
        mLibVLC = new LibVLC(callingContext);
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
        // playbackStateBuilder.setState(state, vlcPlayer.getTime(), 1f);
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
    }

    private void initMediaSessionCompat() {
        // Create a MediaSessionCompat
        mediaSessionCompat = new MediaSessionCompat(callingContext, PlayerConstants.LOG_TAG);
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
        // if there are still songs to be startPlay
        boolean hasSongs = false;   // no more songs to startPlay
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
                playingParam.setAutoPlay(false);    // cancel auto startPlay
            } else {
                // There are public songs to be played
                boolean stillPlayNext = true;
                int repeatStatus = playingParam.getRepeatStatus();
                int publicNextSongIndex = playingParam.getPublicNextSongIndex();
                switch (repeatStatus) {
                    case PlayerConstants.NoRepeatPlaying:
                        // no repeat
                        if (publicNextSongIndex >= publicSongListSize) {
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

                if (stillPlayNext) {    // still startPlay the next song
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
            // startPlay next song that user has ordered
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
        setToolbarImageButtonStatus();
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

        if (useFilePicker) {
            filePath = ExternalStorageUtil.getUriRealPath(callingContext, Uri.parse(filePath));
            if (filePath != null) {
                if (!filePath.isEmpty()) {
                    File songFile = new File(filePath);
                    mediaUri = Uri.fromFile(songFile);   // ACTION_GET_CONTENT
                }
            }
        } else {
            mediaUri = Uri.parse(filePath);
        }

        if (mediaUri==null || Uri.EMPTY.equals(mediaUri)) {
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
        Bundle playingParamOriginExtras = new Bundle();
        playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
        mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
    }

    private void startPlay() {
        Log.d(TAG, "startPlay() is called.");
        int playbackState = playingParam.getCurrentPlaybackState();
        if ( (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playbackState != PlaybackStateCompat.STATE_PLAYING) ) {
            // no media file opened or playing has been stopped
            if ( (playbackState == PlaybackStateCompat.STATE_PAUSED)
                    || (playbackState == PlaybackStateCompat.STATE_REWINDING)
                    || (playbackState == PlaybackStateCompat.STATE_FAST_FORWARDING) ) {
                mediaTransportControls.play();
                Log.d(TAG, "startPlay() --> mediaTransportControls.startPlay() is called.");
            } else {
                // (playbackState == PlaybackStateCompat.STATE_STOPPED) or
                // (playbackState == PlaybackStateCompat.STATE_NONE)
                Log.d(TAG, "startPlay() --> replayMedia() is called.");
                replayMedia();
            }
        }
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
            // auto startPlay
            startAutoPlay();
        } else {
            if (playingParam.getRepeatStatus() == PlayerConstants.NoRepeatPlaying) {
                // no repeat playing
                if ((mediaUri != null && !Uri.EMPTY.equals(mediaUri)) && (playingParam.getCurrentPlaybackState() != PlaybackStateCompat.STATE_NONE)) {
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
        Log.d(TAG, "replayMedia() is called.");
        if ( (mediaUri == null) || (Uri.EMPTY.equals(mediaUri)) || (numberOfAudioTracks<=0) ) {
            return;
        }

        long currentAudioPosition = 0;
        playingParam.setCurrentAudioPosition(currentAudioPosition);
        if (playingParam.isMediaSourcePrepared()) {
            vlcPlayer.setTime(currentAudioPosition); // use time to set position
            setProperAudioTrackAndChannel();
            if (!vlcPlayer.isPlaying()) {
                vlcPlayer.play();
            }
            Log.d(TAG, "replayMedia()--> vlcPlayer.seekTo(currentAudioPosition).");
        } else {
            Bundle playingParamOriginExtras = new Bundle();
            playingParamOriginExtras.putParcelable(PlayerConstants.PlayingParamOrigin, playingParam);
            mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);   // prepare and startPlay
            Log.d(TAG, "replayMedia()--> mediaTransportControls.prepareFromUri().");
        }
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

            int audioTrackId = audioTrackIndicesList.get(indexInArrayList);
            vlcPlayer.setAudioTrack(audioTrackId);
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
        int audioChannel = playingParam.getCurrentChannelPlayed();
        float leftVolume = volume;
        float rightVolume = volume;
        switch (audioChannel) {
            case CommonConstants.LeftChannel:
                rightVolume = 0;
                break;
            case CommonConstants.RightChannel:
                leftVolume = 0;
                break;
            case CommonConstants.StereoChannel:
                leftVolume = rightVolume;
                break;
        }
        int vlcMaxVolume = 100;
        vlcPlayer.setVolume((int) (volume * vlcMaxVolume));
        playingParam.setCurrentVolume(volume);
    }

    private void setAudioVolume(float leftVolume, float rightVolume) {

        int vlcMaxVolume = 100;
        vlcPlayer.setVolume((int) (leftVolume * vlcMaxVolume));
        Log.d(TAG, "setAudioVolume(float, float) is called --> leftVolume = " + leftVolume);
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

    private void getPlayingMediaInfoAndSetAudioActionSubMenu() {
        numberOfVideoTracks = vlcPlayer.getVideoTracksCount();
        MediaPlayer.TrackDescription videoDis[] = vlcPlayer.getVideoTracks();
        int videoTrackId;
        String videoTrackName;
        videoTrackIndicesList.clear();
        if (videoDis != null) {
            // because it is null sometimes
            for (int i = 0; i < videoDis.length; i++) {
                videoTrackId = videoDis[i].id;
                videoTrackName = videoDis[i].name;
                Log.d(TAG, "videoDis[i].id = " + videoTrackId);
                Log.d(TAG, "videoDis[i].name = " + videoTrackName);
                // exclude disabled
                if (videoTrackId >=0 ) {
                    // enabled audio track
                    videoTrackIndicesList.add(videoTrackId);
                }
            }
        }
        numberOfVideoTracks = videoTrackIndicesList.size();
        Log.d(TAG, "numberOfVideoTracks = " + numberOfVideoTracks);
        if (numberOfVideoTracks == 0) {
            playingParam.setCurrentVideoTrackIndexPlayed(PlayerConstants.NoVideoTrack);
        } else {
            // set which video track to be played
            int videoTrackIdPlayed = vlcPlayer.getVideoTrack();
            playingParam.setCurrentVideoTrackIndexPlayed(videoTrackIdPlayed);
        }

        //
        MediaPlayer.TrackDescription audioDis[] = vlcPlayer.getAudioTracks();
        int audioTrackId;
        String audioTrackName;
        audioTrackIndicesList.clear();
        if (audioDis != null) {
            // because it is null sometimes
            for (int i = 0; i < audioDis.length; i++) {
                audioTrackId = audioDis[i].id;
                audioTrackName = audioDis[i].name;
                Log.d(TAG, "audioDis[i].id = " + audioTrackId);
                Log.d(TAG, "audioDis[i].name = " + audioTrackName);
                // exclude disabled
                if (audioTrackId >=0 ) {
                    // enabled audio track
                    audioTrackIndicesList.add(audioTrackId);
                }
            }
        }
        numberOfAudioTracks = audioTrackIndicesList.size();
        Log.d(TAG, "numberOfAudioTracks = " + numberOfAudioTracks);
        if (numberOfAudioTracks == 0) {
            playingParam.setCurrentAudioTrackIndexPlayed(PlayerConstants.NoAudioTrack);
            playingParam.setCurrentChannelPlayed(PlayerConstants.NoAudioChannel);
        } else {
            int audioTrackIdPlayed = vlcPlayer.getAudioTrack();
            Log.d(TAG, "vlcPlayer.getAudioTrack() = " + audioTrackIdPlayed);
            Log.d(TAG, "audioTrackIdPlayed = " + audioTrackIdPlayed);
            int audioTrackIndex = 1;    // default audio track index
            int audioChannel = CommonConstants.StereoChannel;
            if (playingParam.isAutoPlay() || playingParam.isPlaySingleSong()) {
                audioTrackIndex = playingParam.getCurrentAudioTrackIndexPlayed();
                audioChannel = playingParam.getCurrentChannelPlayed();
            } else {
                for (int index = 0; index< audioTrackIndicesList.size(); index++) {
                    int audioId = audioTrackIndicesList.get(index);
                    if (audioId == audioTrackIdPlayed) {
                        audioTrackIndex = index + 1;
                        break;
                    }
                }
                // for open media. do not know the music track and vocal track
                playingParam.setMusicAudioTrackIndex(audioTrackIndex);
                playingParam.setMusicAudioChannel(audioChannel);
                playingParam.setVocalAudioTrackIndex(audioTrackIndex);
                playingParam.setVocalAudioChannel(audioChannel);
            }
            setAudioTrackAndChannel(audioTrackIndex, audioChannel);

            // for testing
            Media media = vlcPlayer.getMedia();
            int trackCount = media.getTrackCount();
            for (int i=0; i<trackCount; i++) {
                Media.Track track = media.getTrack(i);
                Log.d(TAG, "track.id = " + track.id);
                Log.d(TAG, "track.type = " + track.type);
                Log.d(TAG, "track.codec = " + track.codec);
                Log.d(TAG, "track.description = " + track.description);
                Log.d(TAG, "track.language = " + track.language);
                Log.d(TAG, "track.originalCodec = " + track.originalCodec);
                Log.d(TAG, "track.level = " + track.level);
                Log.d(TAG, "track.bitrate = " + track.bitrate);
                Log.d(TAG, "track.profile = " + track.profile);
                if (track.type == Media.Track.Type.Audio) {
                    // audio
                    Media.AudioTrack audioTrack = (Media.AudioTrack)track;
                    Log.d(TAG, "audioTrack.channels = " + audioTrack.channels);
                    Log.d(TAG, "audioTrack.rate = " + audioTrack.rate);
                }
            }
            //

            // build R.id.audioTrack submenu
            if (audioTrackMenuItem != null) {
                if (!audioTrackMenuItem.hasSubMenu()) {
                    // no sub menu
                    ((Menu) audioTrackMenuItem).addSubMenu("Text title");
                }
                SubMenu subMenu = audioTrackMenuItem.getSubMenu();
                subMenu.clear();
                for (int index = 0; index< audioTrackIndicesList.size(); index++) {
                    // audio track index start from 1 for user interface
                    audioTrackName = getString(R.string.audioTrackString) + " " + (index+1);
                    subMenu.add(audioTrackName);
                }
            }
        }

        // update the duration on controller UI
        float duration = vlcPlayer.getLength();
        player_duration_seekbar.setProgress(0);
        player_duration_seekbar.setMax((int)duration);
        duration /= 1000.0f;   // seconds
        int minutes = (int)(duration / 60.0f);    // minutes
        int seconds = (int)duration - (minutes * 60);
        String durationString = String.format("%3d:%02d", minutes, seconds);
        durationTimeTextView.setText(durationString);
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
                        Log.d(TAG, "MediaControllerCallback--> Song was finished.");
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
                    }
                    playMediaImageButton.setVisibility(View.VISIBLE);
                    pauseMediaImageButton.setVisibility(View.GONE);
                    break;
                case PlaybackStateCompat.STATE_PLAYING:
                    // when playing
                    Log.d(TAG, "PlaybackStateCompat.STATE_PLAYING");
                    if (!playingParam.isMediaSourcePrepared()) {
                        // first playing
                        playingParam.setMediaSourcePrepared(true);  // has been prepared
                        final Handler handler = new Handler(Looper.getMainLooper());
                        final Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                handler.removeCallbacksAndMessages(null);
                                getPlayingMediaInfoAndSetAudioActionSubMenu();
                            }
                        };
                        handler.postDelayed(runnable, 500); // delay 0.5 seconds
                    }
                    playMediaImageButton.setVisibility(View.GONE);
                    pauseMediaImageButton.setVisibility(View.VISIBLE);
                    // set up a timer for supportToolbar's visibility
                    setTimerToHideSupportAndAudioController();
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    Log.d(TAG, "PlaybackStateCompat.STATE_PAUSED");
                    playMediaImageButton.setVisibility(View.VISIBLE);
                    pauseMediaImageButton.setVisibility(View.GONE);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    Log.d(TAG, "PlaybackStateCompat.STATE_STOPPED");
                    if (mediaUri != null && !Uri.EMPTY.equals(mediaUri)) {
                        Log.d(TAG, "MediaControllerCallback--> Song was stopped by user.");
                    }
                    playMediaImageButton.setVisibility(View.VISIBLE);
                    pauseMediaImageButton.setVisibility(View.GONE);
                    break;
            }
            playingParam.setCurrentPlaybackState(currentState);
            Log.d(TAG, "MediaControllerCallback.onPlaybackStateChanged() is called. " + currentState);
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
            Log.d(TAG, "MediaSessionCallback.onPrepareFromUri() is called.");
            super.onPrepareFromUri(uri, extras);
            playingParam.setMediaSourcePrepared(false);
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
            setAudioVolume(currentVolume);
            vlcPlayer.setTime(currentAudioPosition); // use time to set position
            try {
                switch (playbackState) {
                    case PlaybackStateCompat.STATE_PAUSED:
                        vlcPlayer.pause();
                        // moved to VLCPlayerEventListener
                        // setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                        Log.d(TAG, "onPrepareFromUri() --> PlaybackStateCompat.STATE_PAUSED");
                        break;
                    case PlaybackStateCompat.STATE_STOPPED:
                        vlcPlayer.stop();
                        // moved to VLCPlayerEventListener
                        // setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                        Log.d(TAG, "onPrepareFromUri() --> PlaybackStateCompat.STATE_STOPPED");
                        break;
                    case PlaybackStateCompat.STATE_PLAYING:
                    case PlaybackStateCompat.STATE_NONE:
                        // start playing when ready or just start new playing
                        final Media mediaSource = new Media(mLibVLC, uri);
                        vlcPlayer.setMedia(mediaSource);
                        vlcPlayer.play();
                        mediaSource.release();
                        // moved to VLCPlayerEventListener
                        // setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                        Log.d(TAG, "onPrepareFromUri() --> PlaybackStateCompat.STATE_PLAYING");
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Invalid mediaId");
            }
        }

        @Override
        public void onPlay() {
            super.onPlay();
            Log.d(TAG, "MediaSessionCallback.onPlay() is called.");
            MediaControllerCompat controller = mediaSessionCompat.getController();
            PlaybackStateCompat stateCompat = controller.getPlaybackState();
            int state = stateCompat.getState();
            if (state != PlaybackStateCompat.STATE_PLAYING) {
                int playerState = vlcPlayer.getPlayerState();
                if (!vlcPlayer.isPlaying()) {
                    vlcPlayer.play();
                    // moved to VLCPlayerEventListener
                    // setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }
            }
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
            Log.d(TAG, "MediaSessionCallback.onPause() is called.");
            MediaControllerCompat controller = mediaSessionCompat.getController();
            PlaybackStateCompat stateCompat = controller.getPlaybackState();
            int state = stateCompat.getState();
            if (state != PlaybackStateCompat.STATE_PAUSED) {
                vlcPlayer.pause();
                // moved to VLCPlayerEventListener
                // setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            }
        }

        @Override
        public void onStop() {
            super.onStop();
            Log.d(TAG, "MediaSessionCallback.onStop() is called.");
            MediaControllerCompat controller = mediaSessionCompat.getController();
            PlaybackStateCompat stateCompat = controller.getPlaybackState();
            int state = stateCompat.getState();
            if (state != PlaybackStateCompat.STATE_STOPPED) {
                vlcPlayer.stop();
                // moved to VLCPlayerEventListener
                // setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            }
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            Log.d(TAG, "MediaSessionCallback.onFastForward() is called.");
            setMediaPlaybackState(PlaybackStateCompat.STATE_FAST_FORWARDING);
        }

        @Override
        public void onRewind() {
            super.onRewind();
            Log.d(TAG, "MediaSessionCallback.onRewind() is called.");
            setMediaPlaybackState(PlaybackStateCompat.STATE_REWINDING);
        }
    }

    private class VLCPlayerEventListener implements MediaPlayer.EventListener {

        @Override
        public synchronized void onEvent(MediaPlayer.Event event) {

            switch(event.type) {
                case MediaPlayer.Event.Buffering:
                    Log.d(TAG, "vlcPlayer is buffering.");
                    if (!vlcPlayer.isPlaying()) {
                        hideNativeAds();
                        showBufferingMessage();
                        setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
                    }
                    break;
                case MediaPlayer.Event.Playing:
                    Log.d(TAG, "vlcPlayer is being played.");
                    // moved to MediaControllerCallback
                    // playingParam.setMediaSourcePrepared(true);  // has been prepared
                    // getPlayingMediaInfoAndSetAudioActionSubMenu();
                    dismissBufferingMessage();
                    hideNativeAds();
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    break;
                case MediaPlayer.Event.Paused:
                    Log.d(TAG, "vlcPlayer is paused");
                    dismissBufferingMessage();
                    hideNativeAds();
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                    break;
                case MediaPlayer.Event.Stopped:
                    Log.d(TAG, "vlcPlayer is stopped");
                    dismissBufferingMessage();
                    showNativeAds();
                    // Because vlcPlayer will send a MediaPlayer.Event.Stopped
                    // after sending a MediaPlayer.Event.EndReached when finished playing
                    // Avoid sending a MediaPlayer.Event.Stopped after finished playing
                    if (playingParam.isMediaSourcePrepared()) {
                        // playing has been finished yet
                        Log.d(TAG, "Sending a event, PlaybackStateCompat.STATE_STOPPED");
                        setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                    }
                    break;
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "vlcPlayer is Reached end.");
                    playingParam.setMediaSourcePrepared(false);
                    setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);
                    // to fix bugs that vlcPlayer.attachViews() does works in onResume()
                    // after finishing playing and reopen the same media file
                    // vlcPlayer.stop() can make status become stop status for vlcPlayer
                    // but will not affect the playingParam.getCurrentPlaybackState()
                    vlcPlayer.stop();   // using to fix bug of vlcPlayer
                    break;
                case MediaPlayer.Event.Opening:
                    Log.d(TAG, "vlcPlayer is Opening media.");
                    break;
                case MediaPlayer.Event.PositionChanged:
                    break;
                case MediaPlayer.Event.TimeChanged:
                    player_duration_seekbar.setProgress((int)vlcPlayer.getTime());
                    break;
                case MediaPlayer.Event.EncounteredError:
                    Log.d(TAG, "vlcPlayer is EncounteredError event");
                    showNativeAds();
                    setMediaPlaybackState(PlaybackStateCompat.STATE_ERROR);
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
                    break;
                default:
                    // showNativeAds();
                    break;
            }
        }
    }
}
