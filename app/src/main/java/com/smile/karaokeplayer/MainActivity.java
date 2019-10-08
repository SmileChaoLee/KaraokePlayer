package com.smile.karaokeplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.smilelibraries.Models.ExitAppTimer;
import com.smile.smilelibraries.showing_instertitial_ads_utility.ShowingInterstitialAdsUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

public class MainActivity extends AppCompatActivity implements PlayerFragment.OnFragmentInteractionListener {

    private static final String TAG = new String(".MainActivity");
    private static final int PERMISSION_REQUEST_CODE = 0x11;

    private float textFontSize;
    private float fontScale;
    private float toastTextSize;
    private String accessExternalStoragePermissionDeniedString;
    private boolean hasPermissionForExternalStorage;
    private boolean isPlayingSingleSong;

    private SongInfo songInfo;
    private Fragment playerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate() is called");
        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(SmileApplication.AppContext, SmileApplication.FontSize_Scale_Type, null);
        Log.d(TAG, "defaultTextFontSize = " + defaultTextFontSize);
        textFontSize = ScreenUtil.suitableFontSize(SmileApplication.AppContext, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        Log.d(TAG, "textFontSize = " + textFontSize);
        fontScale = ScreenUtil.suitableFontScale(SmileApplication.AppContext, SmileApplication.FontSize_Scale_Type, 0.0f);
        Log.d(TAG, "fontScale = " + fontScale);
        toastTextSize = 0.7f * textFontSize;

        isPlayingSingleSong = false;    // first entry of main activity
        songInfo = null;
        if (savedInstanceState == null) {
            Intent callingIntent = getIntent();
            if (callingIntent != null) {
                Bundle extras = callingIntent.getExtras();
                if (extras != null) {
                    Log.d(TAG, "extras is not null.");
                    isPlayingSingleSong = extras.getBoolean(PlayerFragment.IsPlaySingleSongPara, false);
                    songInfo = extras.getParcelable(PlayerFragment.SongInfoPara);
                }
            }
        } else {
            Log.d(TAG, "savedInstanceState is not null.");
            isPlayingSingleSong = savedInstanceState.getBoolean(PlayerFragment.IsPlaySingleSongPara, false);
            songInfo = savedInstanceState.getParcelable(PlayerFragment.SongInfoPara);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int playerFragmentLayoutId = R.id.playerFragmentLayout;
        LinearLayout playerFragmentLayout = findViewById(playerFragmentLayoutId);

        FragmentManager fmManager = getSupportFragmentManager();
        FragmentTransaction ft = fmManager.beginTransaction();

        playerFragment = fmManager.findFragmentByTag(PlayerFragment.PlayerFragmentTag);
        if (playerFragment == null) {
            playerFragment = PlayerFragment.newInstance(isPlayingSingleSong, songInfo);
            ft.add(playerFragmentLayoutId, playerFragment, PlayerFragment.PlayerFragmentTag);
        } else {
            ft.replace(playerFragmentLayoutId, playerFragment, PlayerFragment.PlayerFragmentTag);
        }
        ft.addToBackStack(PlayerFragment.PlayerFragmentTag);
        if (playerFragment.isStateSaved()) {
            ft.commitAllowingStateLoss();
        } else {
            ft.commit();
        }

        hasPermissionForExternalStorage = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                String permissions[] = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        Log.d(TAG,"MainActivity-->onStart() is called.");
        super.onStart();
    }
    @Override
    protected void onResume() {
        Log.d(TAG,"MainActivity-->onResume() is called.");
        super.onResume();
    }
    @Override
    protected void onPause() {
        Log.d(TAG,"MainActivity-->onPause() is called.");
        super.onPause();
    }
    @Override
    protected void onStop() {
        Log.d(TAG,"MainActivity-->onStop() is called.");
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                hasPermissionForExternalStorage = false;
                ScreenUtil.showToast(this, accessExternalStoragePermissionDeniedString, toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_LONG);
            } else {
                hasPermissionForExternalStorage = true;
            }
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
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"MainActivity-->onDestroy() is called.");
    }

    @Override
    public void onBackPressed() {
        ExitAppTimer exitAppTimer = ExitAppTimer.getInstance(1000); // singleton class
        if (exitAppTimer.canExit()) {
            showAdAndExitApplication();
        } else {
            exitAppTimer.start();
            ScreenUtil.showToast(this, getString(R.string.backKeyToExitApp), toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
        }

        Log.d(TAG, "onBackPressed() is called");
    }

    private void returnToPrevious() {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);    // can bundle some data to previous activity
        // setResult(Activity.RESULT_OK);   // no bundle data
        finish();
    }

    private void showAdAndExitApplication() {
        if (SmileApplication.InterstitialAd != null) {
            // free version
            int entryPoint = 0; //  no used
            ShowingInterstitialAdsUtil.ShowAdAsyncTask showAdAsyncTask =
                    SmileApplication.InterstitialAd.new ShowAdAsyncTask(this
                            , entryPoint
                            , new ShowingInterstitialAdsUtil.AfterDismissFunctionOfShowAd() {
                        @Override
                        public void executeAfterDismissAds(int endPoint) {
                            exitApplication();
                        }
                    });
            showAdAsyncTask.execute();
        } else {
            exitApplication();
        }
    }

    private void exitApplication() {
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
        if (!isPlayingSingleSong) {
            // not only play one single song
            // so not android.intent.category.LAUNCHER
            showAdAndExitApplication();
        } else {
            returnToPrevious();
        }
    }
}
