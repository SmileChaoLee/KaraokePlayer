package com.smile.karaokeplayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.smile.smilelibraries.utilities.ScreenUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = new String(".MainActivity");
    private static final int PERMISSION_REQUEST_CODE = 0x11;

    private float textFontSize;
    private float fontScale;
    private float toastTextSize;
    private String accessExternalStoragePermissionDeniedString;
    private boolean hasPermissionForExternalStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        hasPermissionForExternalStorage = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                String permissions[] = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            } else {
                hasPermissionForExternalStorage = true;
            }
        } else {
            hasPermissionForExternalStorage = true;
        }

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(SmileApplication.AppContext, ScreenUtil.FontSize_Pixel_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(SmileApplication.AppContext, defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(SmileApplication.AppContext, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        toastTextSize = 0.7f * textFontSize;

        super.onCreate(savedInstanceState);

        if (hasPermissionForExternalStorage) {
            startPlayerActivity();
            returnToPrevious();   // exit the MainActivity immediately
        }
    }

    private void startPlayerActivity() {
        // Intent callingIntent = getIntent();
        Intent startPlayerActivityIntent = new Intent(this, ExoPlayerActivity.class);
        // Intent startPlayerActivityIntent = new Intent(this, VLCPlayerActivity.class);
        // startPlayerActivityIntent.putExtra("CallingIntent", callingIntent);
        startActivity(startPlayerActivityIntent);
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

        if (hasPermissionForExternalStorage) {
            startPlayerActivity();
        }
        returnToPrevious();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"MainActivity-->onDestroy() is called.");
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed() is called");
        returnToPrevious();
    }

    private void returnToPrevious() {
        Log.d(TAG, "returnToPrevious() is called");
        finish();
    }
}
