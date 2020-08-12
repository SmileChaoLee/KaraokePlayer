package exoplayer.Listeners;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.cast.framework.CastState;
import com.smile.karaokeplayer.R;
import com.smile.karaokeplayer.SmileApplication;
import com.smile.smilelibraries.utilities.ScreenUtil;

import exoplayer.ExoPlayerActivity;
import exoplayer.Presenters.ExoPlayerPresenter;

public class ExoPlayerCastStateListener implements com.google.android.gms.cast.framework.CastStateListener {
    private static final String TAG = "BaseCastStateListener";
    private final Context callinContext;
    private final ExoPlayerActivity exoPlayerActivity;
    private final ExoPlayerPresenter mPresenter;
    private final float toastTextSize;

    public ExoPlayerCastStateListener(Activity activity, ExoPlayerPresenter presenter) {
        exoPlayerActivity = (ExoPlayerActivity)activity;
        callinContext = exoPlayerActivity.getApplicationContext();
        mPresenter = presenter;
        toastTextSize = mPresenter.getToastTextSize();
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onCastStateChanged(int i) {
        mPresenter.setCurrentCastState(i);
        switch (i) {
            case CastState.NO_DEVICES_AVAILABLE:
                Log.d(TAG, "CastState is NO_DEVICES_AVAILABLE.");
                ScreenUtil.showToast(callinContext, callinContext.getString(R.string.no_chromecast_devices_avaiable), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                exoPlayerActivity.setMediaRouteButtonVisible(false);
                break;
            case CastState.NOT_CONNECTED:
                Log.d(TAG, "CastState is NOT_CONNECTED.");
                ScreenUtil.showToast(callinContext, callinContext.getString(R.string.chromecast_not_connected), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                exoPlayerActivity.setMediaRouteButtonVisible(true);
                break;
            case CastState.CONNECTING:
                Log.d(TAG, "CastState is CONNECTING.");
                ScreenUtil.showToast(callinContext, callinContext.getString(R.string.chromecast_is_connecting), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                exoPlayerActivity.setMediaRouteButtonVisible(true);
                break;
            case CastState.CONNECTED:
                Log.d(TAG, "CastState is CONNECTED.");
                ScreenUtil.showToast(callinContext, callinContext.getString(R.string.chromecast_is_connected), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                exoPlayerActivity.setMediaRouteButtonVisible(true);
                break;
            default:
                Log.d(TAG, "CastState is unknown.");
                exoPlayerActivity.setMediaRouteButtonVisible(true);
                break;
        }
        Log.d(TAG, "onCastStateChanged() is called.");
    }
}
