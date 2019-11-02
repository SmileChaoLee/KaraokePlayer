package com.smile.karaokeplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.smile.karaokeplayer.Fragments.ExoPlayerFragment;
// import com.smile.karaokeplayer.Fragments.VLCPlayerFragment;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.smilelibraries.utilities.ScreenUtil;

public class PlaySingleSongActivity extends AppCompatActivity implements ExoPlayerFragment.OnFragmentInteractionListener{
// public class PlaySingleSongActivity extends AppCompatActivity implements VLCPlayerFragment.OnFragmentInteractionListener{

    private static final String TAG = new String(".PlayOneSongActivity");

    private float textFontSize;
    private float fontScale;
    private float toastTextSize;

    private boolean isPlayingSingleSong;
    private SongInfo songInfo;
    private Fragment playerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(SmileApplication.AppContext, SmileApplication.FontSize_Scale_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(SmileApplication.AppContext, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(SmileApplication.AppContext, SmileApplication.FontSize_Scale_Type, 0.0f);
        toastTextSize = 0.7f * textFontSize;

        isPlayingSingleSong = true;
        songInfo = null;
        if (savedInstanceState == null) {
            Intent callingIntent = getIntent();
            if (callingIntent != null) {
                Bundle extras = callingIntent.getExtras();
                if (extras != null) {
                    isPlayingSingleSong = extras.getBoolean(ExoPlayerFragment.IsPlaySingleSongState, true);
                    songInfo = extras.getParcelable(ExoPlayerFragment.SongInfoState);
                }
            }
        } else {
            Log.d(TAG, "savedInstanceState is not null.");
            isPlayingSingleSong = savedInstanceState.getBoolean(ExoPlayerFragment.IsPlaySingleSongState, true);
            songInfo = savedInstanceState.getParcelable(ExoPlayerFragment.SongInfoState);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_single_song);

        int oneSongPlayerFragmentLayoutId = R.id.oneSongPlayerFragmentLayout;

        FragmentManager fmManager = getSupportFragmentManager();
        FragmentTransaction ft = fmManager.beginTransaction();

        String fragmentTag = SmileApplication.PlayerFragmentTag;
        playerFragment = fmManager.findFragmentByTag(fragmentTag);
        if (playerFragment == null) {
            playerFragment = ExoPlayerFragment.newInstance(isPlayingSingleSong, songInfo);
            /// playerFragment = VLCPlayerFragment.newInstance(isPlayingSingleSong, songInfo);
            ft.add(oneSongPlayerFragmentLayoutId, playerFragment, fragmentTag);
        } else {
            ft.replace(oneSongPlayerFragmentLayoutId, playerFragment, fragmentTag);
        }
        ft.addToBackStack(fragmentTag);
        if (playerFragment.isStateSaved()) {
            ft.commitAllowingStateLoss();
        } else {
            ft.commit();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG,"PlayOneSongActivity-->onSaveInstanceState() is called.");
        outState.putBoolean(ExoPlayerFragment.IsPlaySingleSongState, isPlayingSingleSong);
        outState.putParcelable(ExoPlayerFragment.SongInfoState, songInfo);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        returnToPrevious();
    }

    private void returnToPrevious() {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);    // can bundle some data to previous activity
        // setResult(Activity.RESULT_OK);   // no bundle data
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void returnToPrevious(int exitCode) {
        Intent returnIntent = new Intent();
        // can bundle some data to previous activity
        Bundle extras = new Bundle();
        extras.putInt("ExitCode", exitCode);
        returnIntent.putExtras(extras);
        setResult(Activity.RESULT_OK, returnIntent);
        // setResult(Activity.RESULT_OK);   // no bundle data
        finish();
    }

    @Override
    public void setSupportActionBarForFragment(Toolbar toolbar) {
        setSupportActionBar(toolbar);
    }
    @Override
    public ActionBar getSupportActionBarForFragment() {
        return getSupportActionBar();
    }
    @Override
    public void onExitFragment() {
        returnToPrevious();
    }
}
