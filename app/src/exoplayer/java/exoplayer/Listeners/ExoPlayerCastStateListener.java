package exoplayer.Listeners;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.cast.framework.CastState;
import com.smile.karaokeplayer.R;
import com.smile.smilelibraries.utilities.ScreenUtil;

import exoplayer.ExoPlayerActivity;
import exoplayer.Presenters.ExoPlayerPresenter;

public class ExoPlayerCastStateListener implements com.google.android.gms.cast.framework.CastStateListener {
    private static final String TAG = "ExoPlayerCastStateListener";
    private final ExoPlayerActivity exoPlayerActivity;
    private final ExoPlayerPresenter presenter;
    private final float toastTextSize;

    public ExoPlayerCastStateListener(Activity activity, ExoPlayerPresenter presenter) {
        exoPlayerActivity = (ExoPlayerActivity)activity;
        this.presenter = presenter;
        toastTextSize = this.presenter.getToastTextSize();
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onCastStateChanged(int i) {
        Log.d(TAG, "onCastStateChanged() is called");
        presenter.setCurrentCastState(i);
        switch (i) {
            case CastState.NO_DEVICES_AVAILABLE:
                Log.d(TAG, "CastState is NO_DEVICES_AVAILABLE.");
                ScreenUtil.showToast(exoPlayerActivity, exoPlayerActivity.getString(R.string.no_chromecast_devices_avaiable), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                exoPlayerActivity.setMediaRouteButtonVisible(false);
                break;
            case CastState.NOT_CONNECTED:
                Log.d(TAG, "CastState is NOT_CONNECTED.");
                ScreenUtil.showToast(exoPlayerActivity, exoPlayerActivity.getString(R.string.chromecast_not_connected), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                exoPlayerActivity.setMediaRouteButtonVisible(true);
                break;
            case CastState.CONNECTING:
                Log.d(TAG, "CastState is CONNECTING.");
                ScreenUtil.showToast(exoPlayerActivity, exoPlayerActivity.getString(R.string.chromecast_is_connecting), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                exoPlayerActivity.setMediaRouteButtonVisible(true);
                break;
            case CastState.CONNECTED:
                Log.d(TAG, "CastState is CONNECTED.");
                ScreenUtil.showToast(exoPlayerActivity, exoPlayerActivity.getString(R.string.chromecast_is_connected), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                exoPlayerActivity.setMediaRouteButtonVisible(true);
                break;
            default:
                Log.d(TAG, "CastState is unknown.");
                exoPlayerActivity.setMediaRouteButtonVisible(true);
                break;
        }
    }
}
