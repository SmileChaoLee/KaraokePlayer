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
import android.support.v4.media.session.MediaControllerCompat;
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
import com.smile.karaokeplayer.Models.VerticalSeekBar;
import com.smile.karaokeplayer.Presenters.VLCPlayerPresenter;
import com.smile.karaokeplayer.R;
import com.smile.karaokeplayer.SmileApplication;
import com.smile.karaokeplayer.SongListActivity;
import com.smile.karaokeplayer.Utilities.DataOrContentAccessUtil;
import com.smile.karaokeplayer.Utilities.ExternalStorageUtil;
import com.smile.smilelibraries.privacy_policy.PrivacyPolicyUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

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
public class VLCPlayerFragment extends Fragment implements VLCPlayerPresenter.PresentView{

    private static final String TAG = new String(".VLCPlayerFragment");
    private static final boolean ENABLE_SUBTITLES = true;
    private static final boolean USE_TEXTURE_VIEW = false;

    private VLCPlayerPresenter mPresenter;
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

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            timerHandler.removeCallbacksAndMessages(null);
            if (mPresenter != null) {
                PlayingParameters playingParam = mPresenter.getPlayingParam();
                if (playingParam != null) {
                    if (playingParam.isMediaSourcePrepared()) {
                        if (supportToolbar.getVisibility() == View.VISIBLE) {
                            // hide supportToolbar
                            hideSupportToolbarAndAudioController();
                        }
                    } else {
                        showSupportToolbarAndAudioController();
                    }
                }
            }
        }
    };

    private OnFragmentInteractionListener mListener;

    // temporary settings
    public static final boolean useFilePicker = true;
    //

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
     * @param callingActivityIntent Parameter 1.
     * @return A new instance of fragment VLCPlayerFragment.
     */
    public static VLCPlayerFragment newInstance(Intent callingActivityIntent) {
        boolean isPlaySingleSong = false;
        SongInfo singleSongInfo = null;
        Bundle extras = null;
        if (callingActivityIntent != null) {
            extras = callingActivityIntent.getExtras();
            if (extras != null) {
                isPlaySingleSong = extras.getBoolean(PlayerConstants.IsPlaySingleSongState, false);
                singleSongInfo = extras.getParcelable(PlayerConstants.SongInfoState);
            }
        }
        VLCPlayerFragment fragment = new VLCPlayerFragment();
        Bundle args = new Bundle();
        args.putBoolean(PlayerConstants.IsPlaySingleSongState, isPlaySingleSong);
        args.putParcelable(PlayerConstants.SongInfoState, singleSongInfo);
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
        // useFilePicker = true;
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

        mPresenter = new VLCPlayerPresenter(this, callingContext);
        mPresenter.initializeVariables(savedInstanceState, getArguments());

        final PlayingParameters playingParam = mPresenter.getPlayingParam();

        // Video player view
        videoVLCPlayerView = fragmentView.findViewById(R.id.videoVLCPlayerView);
        videoVLCPlayerView.setVisibility(View.VISIBLE);

        // use custom toolbar
        supportToolbar = fragmentView.findViewById(R.id.custom_toolbar);
        mListener.setSupportActionBarForFragment(supportToolbar);
        ActionBar actionBar = mListener.getSupportActionBarForFragment();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        volumeImageButton = fragmentView.findViewById(R.id.volumeImageButton);
        //
        audioControllerView = fragmentView.findViewById(R.id.audioControllerView);
        previousMediaImageButton = fragmentView.findViewById(R.id.previousMediaImageButton);
        playMediaImageButton = fragmentView.findViewById(R.id.playMediaImageButton);
        pauseMediaImageButton = fragmentView.findViewById(R.id.pauseMediaImageButton);
        if (playingParam.getCurrentPlaybackState()==PlaybackStateCompat.STATE_PLAYING) {
            playButtonOffPauseButtonOn();
        } else {
            playButtonOnPauseButtonOff();
        }
        replayMediaImageButton = fragmentView.findViewById(R.id.replayMediaImageButton);
        stopMediaImageButton = fragmentView.findViewById(R.id.stopMediaImageButton);
        nextMediaImageButton = fragmentView.findViewById(R.id.nextMediaImageButton);

        //
        repeatImageButton = fragmentView.findViewById(R.id.repeatImageButton);
        switchToMusicImageButton = fragmentView.findViewById(R.id.switchToMusicImageButton);
        switchToVocalImageButton = fragmentView.findViewById(R.id.switchToVocalImageButton);
        actionMenuImageButton = fragmentView.findViewById(R.id.actionMenuImageButton);

        // added on 2019-12-26
        actionMenuView = supportToolbar.findViewById(R.id.actionMenuViewLayout); // main menu
        actionMenuView.setOnMenuItemClickListener(new ActionMenuView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });
        //

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

        mPresenter.initVLCPlayer();   // must be before volumeSeekBar settings
        mPresenter.initMediaSessionCompat();

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
                if (playingParam != null) {
                    playingParam.setCurrentVolume(currentVolume);
                    mPresenter.setAudioVolume(currentVolume);
                }
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

        setImageButtonStatus();
        setButtonsPositionAndSize(getResources().getConfiguration());

        setOnClickEvents();

        showNativeAds();

        Uri mediaUri = mPresenter.getMediaUri();
        if (mediaUri==null || Uri.EMPTY.equals(mediaUri)) {
            if (playingParam.isPlaySingleSong()) {
                if (playingParam.isPlaySingleSong()) {
                    SongInfo singleSongInfo = mPresenter.getSingleSongInfo();
                    if (singleSongInfo == null) {
                        Log.d(TAG, "singleSongInfo is null");
                    } else {
                        Log.d(TAG, "singleSongInfo is not null");
                        playingParam.setAutoPlay(false);
                        mPresenter.playSingleSong(singleSongInfo);
                    }
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
                // mediaTransportControls.prepareFromUri(mediaUri, playingParamOriginExtras);
                MediaControllerCompat.TransportControls mediaTransportControls = mPresenter.getMediaTransportControls();
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

        final PlayingParameters playingParam = mPresenter.getPlayingParam();
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

        PlayingParameters playingParam = mPresenter.getPlayingParam();

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
                // canShowNotSupportedFormat = true;
                mPresenter.setCanShowNotSupportedFormat(true);
                if (isAutoPlay) {
                    ArrayList<SongInfo> publicSongList = DataOrContentAccessUtil.readPublicSongList(callingContext);
                    mPresenter.setPublicSongList(publicSongList);
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
                        mPresenter.startAutoPlay();
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
                    DataOrContentAccessUtil.selectFileToOpen(this, PlayerConstants.FILE_READ_REQUEST_CODE);
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
                mPresenter.setAudioTrackAndChannel(1, currentChannelPlayed);
                break;
            case R.id.audioTrack2:
                mPresenter.setAudioTrackAndChannel(2, currentChannelPlayed);
                break;
            case R.id.audioTrack3:
                mPresenter.setAudioTrackAndChannel(3, currentChannelPlayed);
                break;
            case R.id.audioTrack4:
                mPresenter.setAudioTrackAndChannel(4, currentChannelPlayed);
                break;
            case R.id.audioTrack5:
                mPresenter.setAudioTrackAndChannel(5, currentChannelPlayed);
                break;
            case R.id.audioTrack6:
                mPresenter.setAudioTrackAndChannel(6, currentChannelPlayed);
                break;
            case R.id.audioTrack7:
                mPresenter.setAudioTrackAndChannel(7, currentChannelPlayed);
                break;
            case R.id.audioTrack8:
                mPresenter.setAudioTrackAndChannel(8, currentChannelPlayed);
                break;
            case R.id.channel:
                Uri mediaUri = mPresenter.getMediaUri();
                int numberOfAudioTracks = mPresenter.getNumberOfAudioTracks();
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
                mPresenter.setAudioVolume(playingParam.getCurrentVolume());
                break;
            case R.id.rightChannel:
                playingParam.setCurrentChannelPlayed(CommonConstants.RightChannel);
                mPresenter.setAudioVolume(playingParam.getCurrentVolume());
                break;
            case R.id.stereoChannel:
                playingParam.setCurrentChannelPlayed(CommonConstants.StereoChannel);
                mPresenter.setAudioVolume(playingParam.getCurrentVolume());
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG,"VLCPlayerFragment-->onStart() is called.");
        videoVLCPlayerView.requestFocus();
        // vlcPlayer.attachViews(videoVLCPlayerView, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);
        mPresenter.attachPlayerViews(videoVLCPlayerView, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);
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
        // vlcPlayer.detachViews();
        mPresenter.detachPlayerViews();
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
        mPresenter.onSaveInstanceState(outState);
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
                Uri orgMediaUri = data.getData();
                Log.i(TAG, "Uri: " + orgMediaUri.toString());

                if ( (orgMediaUri == null) || (Uri.EMPTY.equals(orgMediaUri)) ) {
                    return;
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getActivity().getContentResolver().takePersistableUriPermission(orgMediaUri, takeFlags);
                    }
                } catch (Exception ex) {
                    Log.d(TAG, "Failed to add persistable permission of orgMediaUri");
                    ex.printStackTrace();
                }

                PlayingParameters playingParam = mPresenter.getPlayingParam();
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

                Uri mediaUri = orgMediaUri;
                String filePath = ExternalStorageUtil.getUriRealPath(callingContext, orgMediaUri);
                if (filePath != null) {
                    if (!filePath.isEmpty()) {
                        File songFile = new File(filePath);
                        mediaUri = Uri.fromFile(songFile);
                    }
                }

                Log.i(TAG, "mediaUri = " + mediaUri.toString());
                if ((mediaUri != null) && (!Uri.EMPTY.equals(mediaUri))) {
                    // has Uri
                    mPresenter.setMediaUri(mediaUri);
                    MediaControllerCompat.TransportControls mediaTransportControls = mPresenter.getMediaTransportControls();
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
        mPresenter.releaseMediaSessionCompat();
        mPresenter.releaseVLCPlayer();

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
                ArrayList<SongInfo> publicSongList = mPresenter.getPublicSongList();
                PlayingParameters playingParam = mPresenter.getPlayingParam();
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

                mPresenter.startAutoPlay();
            }
        });

        playMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.startPlay();
            }
        });

        pauseMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.pausePlay();
            }
        });

        replayMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.replayMedia();
            }
        });

        stopMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.stopPlay();
            }
        });

        nextMediaImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<SongInfo> publicSongList = mPresenter.getPublicSongList();
                PlayingParameters playingParam = mPresenter.getPlayingParam();
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

                mPresenter.startAutoPlay();
            }
        });

        repeatImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayingParameters playingParam = mPresenter.getPlayingParam();
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
                mPresenter.switchAudioToMusic();
            }
        });

        switchToVocalImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.switchAudioToVocal();
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
                    mPresenter.setPlayerTime(progress);
                    // vlcPlayer.setTime(progress);
                }
                PlayingParameters playingParam = mPresenter.getPlayingParam();
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

    // Implement PlayerPresenter.PresentView
    @Override
    public void setImageButtonStatus() {
        PlayingParameters playingParam = mPresenter.getPlayingParam();
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

    @Override
    public void playButtonOnPauseButtonOff() {
        playMediaImageButton.setVisibility(View.VISIBLE);
        pauseMediaImageButton.setVisibility(View.GONE);
    }

    @Override
    public void playButtonOffPauseButtonOn() {
        playMediaImageButton.setVisibility(View.GONE);
        pauseMediaImageButton.setVisibility(View.VISIBLE);
    }

    @Override
    public AppCompatSeekBar getPlayer_duration_seekbar() {
        return player_duration_seekbar;
    }

    @Override
    public void showNativeAds() {
        // simulate showing native ad
        Log.d(TAG, "showNativeAds() is called.");
        if (BuildConfig.DEBUG) {
            nativeAdsLinearLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideNativeAds() {
        // simulate hide native ad
        Log.d(TAG, "hideNativeAds() is called.");
        if (BuildConfig.DEBUG) {
            nativeAdsLinearLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void showBufferingMessage() {
        messageLinearLayout.setVisibility(View.VISIBLE);
        bufferingStringTextView.startAnimation(animationText);
    }

    @Override
    public void dismissBufferingMessage() {
        messageLinearLayout.setVisibility(View.INVISIBLE);
        animationText.cancel();
    }

    @Override
    public void setTimerToHideSupportAndAudioController() {
        timerHandler.removeCallbacksAndMessages(null);
        timerHandler.postDelayed(timerRunnable, PlayerConstants.PlayerView_Timeout); // 10 seconds
    }

    @Override
    public void buildAudioTrackMenuItem(int audioTrackNumber) {
        // build R.id.audioTrack submenu
        if (audioTrackMenuItem != null) {
            SubMenu subMenu = audioTrackMenuItem.getSubMenu();
            int index=0;
            for (index = 0; index < audioTrackNumber; index++) {
                // audio track index start from 1 for user interface
                subMenu.getItem(index).setVisible(true);
            }
            for (int j=index; j<subMenu.size(); j++) {
                subMenu.getItem(j).setVisible(false);
            }
        }
    }

    @Override
    public void update_Player_duration_seekbar(float duration) {
        player_duration_seekbar.setProgress(0);
        player_duration_seekbar.setMax((int)duration);
        duration /= 1000.0f;   // seconds
        int minutes = (int)(duration / 60.0f);    // minutes
        int seconds = (int)duration - (minutes * 60);
        String durationString = String.format("%3d:%02d", minutes, seconds);
        durationTimeTextView.setText(durationString);
    }
    //
}
