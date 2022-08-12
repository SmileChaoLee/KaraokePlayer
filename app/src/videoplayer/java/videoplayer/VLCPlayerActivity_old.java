package videoplayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.smile.karaokeplayer.BasePlayerActivity;
import com.smile.karaokeplayer.presenters.BasePlayerPresenter;
import com.smile.karaokeplayer.R;
import com.smile.smilelibraries.utilities.ScreenUtil;

import org.videolan.libvlc.util.VLCVideoLayout;

import videoplayer.Presenters.VLCPlayerPresenter;

public class VLCPlayerActivity_old extends BasePlayerActivity { // implements VLCPlayerPresenter.VLCPlayerPresentView {

    private static final String TAG = "VLCPlayerActivity";
    private static final boolean ENABLE_SUBTITLES = true;
    private static final boolean USE_TEXTURE_VIEW = false;

    private static final int PERMISSION_REQUEST_CODE = 0x11;
    private boolean hasPermissionForExternalStorage;

    private VLCPlayerPresenter presenter;
    private VLCVideoLayout videoVLCPlayerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate() is called.");

        // presenter = new VLCPlayerPresenter(this, this);

        super.onCreate(savedInstanceState);

        presenter.initVLCPlayer();   // must be before volumeSeekBar settings
        presenter.initMediaSessionCompat();

        // Video player view
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        videoVLCPlayerView = new VLCVideoLayout(this);
        videoVLCPlayerView.setLayoutParams(layoutParams);
        videoVLCPlayerView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        playerViewLinearLayout.addView(videoVLCPlayerView);

        videoVLCPlayerView.setVisibility(View.VISIBLE);

        int currentProgress = presenter.getCurrentProgressForVolumeSeekBar();
        volumeSeekBar.setProgressAndThumb(currentProgress);

        presenter.playTheSongThatWasPlayedBeforeActivityCreated();

        hasPermissionForExternalStorage = (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissionForExternalStorage) {
                String[] permissions = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            }
        } else {
            if (!hasPermissionForExternalStorage) {
                ScreenUtil.showToast(this, "Permission Denied", 60, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_LONG);
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            int rLen = grantResults.length;
            hasPermissionForExternalStorage = (rLen>0) &&
                    (grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }
        if (!hasPermissionForExternalStorage) {
            ScreenUtil.showToast(this, "Permission Denied", 60, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_LONG);
            finish();   // exit the activity immediately
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart() is called.");
        super.onStart();
        if (videoVLCPlayerView != null) {
            videoVLCPlayerView.requestFocus();
        }
        if (presenter != null) {
            presenter.attachPlayerViews(videoVLCPlayerView, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG,"onStop() is called.");
        super.onStop();
        if (presenter != null) {
            presenter.detachPlayerViews();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy() is called.");
        if (presenter != null) {
            presenter.releaseMediaSessionCompat();
            presenter.releaseVLCPlayer();
        }
    }

    // implement abstract methods of super class
    @Override
    public BasePlayerPresenter getPlayerBasePresenter() {
        return presenter;
    }

    @Override
    public void setMediaRouteButtonView(int buttonMarginLeft, int imageButtonHeight) {

    }

    @Override
    public void setMediaRouteButtonVisible(boolean isVisible) {

    }

    @Override
    public Intent createIntentForSongListActivity() {
        return new Intent(getApplicationContext(), SongListActivity.class);
    }

    @Override
    public void setMenuItemsVisibility() {
        MenuItem channelMenuItem = mainMenu.findItem(R.id.channel);
        channelMenuItem.setVisible(false);
    }

    @Override
    public void setSwitchToVocalImageButtonVisibility() {
        switchToVocalImageButton.setVisibility(View.GONE);
    }
    // end of implementing methods of super class
}
