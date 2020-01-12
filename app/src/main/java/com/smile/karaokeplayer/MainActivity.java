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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Fragments.ExoPlayerFragment;
// import com.smile.karaokeplayer.Fragments.VLCPlayerFragment;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.smilelibraries.Models.ExitAppTimer;
import com.smile.smilelibraries.showing_instertitial_ads_utility.ShowingInterstitialAdsUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

public class MainActivity extends AppCompatActivity implements ExoPlayerFragment.OnFragmentInteractionListener {
// public class MainActivity extends AppCompatActivity implements VLCPlayerFragment.OnFragmentInteractionListener {

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
        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(SmileApplication.AppContext, ScreenUtil.FontSize_Pixel_Type, null);
        Log.d(TAG, "defaultTextFontSize = " + defaultTextFontSize);
        textFontSize = ScreenUtil.suitableFontSize(SmileApplication.AppContext, defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        Log.d(TAG, "textFontSize = " + textFontSize);
        fontScale = ScreenUtil.suitableFontScale(SmileApplication.AppContext, ScreenUtil.FontSize_Pixel_Type, 0.0f);
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
                    isPlayingSingleSong = extras.getBoolean(ExoPlayerFragment.IsPlaySingleSongState, false);
                    songInfo = extras.getParcelable(ExoPlayerFragment.SongInfoState);
                }
            }
        } else {
            Log.d(TAG, "savedInstanceState is not null.");
            isPlayingSingleSong = savedInstanceState.getBoolean(ExoPlayerFragment.IsPlaySingleSongState, false);
            songInfo = savedInstanceState.getParcelable(ExoPlayerFragment.SongInfoState);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int playerFragmentLayoutId = R.id.playerFragmentLayout;
        LinearLayout playerFragmentLayout = findViewById(playerFragmentLayoutId);

        FragmentManager fmManager = getSupportFragmentManager();
        FragmentTransaction ft = fmManager.beginTransaction();

        String fragmentTag = CommonConstants.PlayerFragmentTag;
        playerFragment = fmManager.findFragmentByTag(fragmentTag);
        if (playerFragment == null) {
            playerFragment = ExoPlayerFragment.newInstance(isPlayingSingleSong, songInfo);
            // playerFragment = VLCPlayerFragment.newInstance(isPlayingSingleSong, songInfo);
            ft.add(playerFragmentLayoutId, playerFragment, fragmentTag);
        } else {
            ft.replace(playerFragmentLayoutId, playerFragment, fragmentTag);
        }
        ft.addToBackStack(fragmentTag);
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
                ScreenUtil.showToast(this, accessExternalStoragePermissionDeniedString, toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_LONG);
            } else {
                hasPermissionForExternalStorage = true;
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG,"MainActivity-->onSaveInstanceState() is called.");
        outState.putBoolean(ExoPlayerFragment.IsPlaySingleSongState, isPlayingSingleSong);
        outState.putParcelable(ExoPlayerFragment.SongInfoState, songInfo);
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
            showAdAndExitActivity();
        } else {
            exitAppTimer.start();
            ScreenUtil.showToast(this, getString(R.string.backKeyToExitApp), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
        }

        Log.d(TAG, "onBackPressed() is called");
    }

    private void returnToPrevious() {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);    // can bundle some data to previous activity
        // setResult(Activity.RESULT_OK);   // no bundle data
        finish();
    }

    private void showAdAndExitActivity() {
        returnToPrevious();
        if (SmileApplication.InterstitialAd != null) {
            // free version
            int entryPoint = 0; //  no used
            ShowingInterstitialAdsUtil.ShowAdAsyncTask showAdAsyncTask =
                    SmileApplication.InterstitialAd.new ShowAdAsyncTask(entryPoint
                            , new ShowingInterstitialAdsUtil.AfterDismissFunctionOfShowAd() {
                        @Override
                        public void executeAfterDismissAds(int endPoint) {
                            // returnToPrevious();
                        }
                    });
            showAdAsyncTask.execute();
        } else {
            // returnToPrevious();
        }
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
        showAdAndExitActivity();
    }
}
