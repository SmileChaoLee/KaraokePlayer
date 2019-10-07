package com.smile.karaokeplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.smilelibraries.utilities.ScreenUtil;

public class PlayOneSongActivity extends AppCompatActivity implements PlayerFragment.OnFragmentInteractionListener{

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
                    isPlayingSingleSong = extras.getBoolean(PlayerFragment.IsPlaySingleSongPara, true);
                    songInfo = extras.getParcelable(PlayerFragment.SongInfoPara);
                }
            }
        } else {
            Log.d(TAG, "savedInstanceState is not null.");
            isPlayingSingleSong = savedInstanceState.getBoolean(PlayerFragment.IsPlaySingleSongPara, true);
            songInfo = savedInstanceState.getParcelable(PlayerFragment.SongInfoPara);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_one_song);

        int oneSongPlayerFragmentLayoutId = R.id.oneSongPlayerFragmentLayout;

        FragmentManager fmManager = getSupportFragmentManager();
        FragmentTransaction ft = fmManager.beginTransaction();

        playerFragment = fmManager.findFragmentByTag(PlayerFragment.PlayerFragmentTag);
        if (playerFragment == null) {
            playerFragment = PlayerFragment.newInstance(isPlayingSingleSong, songInfo);
            ft.add(oneSongPlayerFragmentLayoutId, playerFragment, PlayerFragment.PlayerFragmentTag);
        } else {
            ft.replace(oneSongPlayerFragmentLayoutId, playerFragment, PlayerFragment.PlayerFragmentTag);
        }
        ft.addToBackStack(PlayerFragment.PlayerFragmentTag);
        if (playerFragment.isStateSaved()) {
            ft.commitAllowingStateLoss();
        } else {
            ft.commit();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG,"PlayOneSongActivity-->onSaveInstanceState() is called.");
        outState.putBoolean(PlayerFragment.IsPlaySingleSongPara, isPlayingSingleSong);
        outState.putParcelable(PlayerFragment.SongInfoPara, songInfo);
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
