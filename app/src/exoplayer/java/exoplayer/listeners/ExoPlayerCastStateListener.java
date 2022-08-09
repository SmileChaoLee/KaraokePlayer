package exoplayer.listeners;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.cast.framework.CastState;
import com.smile.karaokeplayer.R;
import com.smile.smilelibraries.utilities.ScreenUtil;

import exoplayer.ExoPlayerActivity;
import exoplayer.ExoPlayerFragment;
import exoplayer.presenters.ExoPlayerPresenter;

public class ExoPlayerCastStateListener implements com.google.android.gms.cast.framework.CastStateListener {
    private static final String TAG = "ExoPlayerCastStateListener";
    private final ExoPlayerFragment exoPlayerFragment;
    private final FragmentActivity fragmentActivity;
    private final ExoPlayerPresenter presenter;
    private final float toastTextSize;

    public ExoPlayerCastStateListener(Fragment fragment, ExoPlayerPresenter presenter) {
        exoPlayerFragment = (ExoPlayerFragment)fragment;
        fragmentActivity = exoPlayerFragment.getActivity();
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
                ScreenUtil.showToast(fragmentActivity, fragmentActivity.getString(R.string.no_chromecast_devices_avaiable), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                exoPlayerFragment.setMediaRouteButtonVisible(false);
                break;
            case CastState.NOT_CONNECTED:
                Log.d(TAG, "CastState is NOT_CONNECTED.");
                ScreenUtil.showToast(fragmentActivity, fragmentActivity.getString(R.string.chromecast_not_connected), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                exoPlayerFragment.setMediaRouteButtonVisible(true);
                break;
            case CastState.CONNECTING:
                Log.d(TAG, "CastState is CONNECTING.");
                ScreenUtil.showToast(fragmentActivity, fragmentActivity.getString(R.string.chromecast_is_connecting), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                exoPlayerFragment.setMediaRouteButtonVisible(true);
                break;
            case CastState.CONNECTED:
                Log.d(TAG, "CastState is CONNECTED.");
                ScreenUtil.showToast(fragmentActivity, fragmentActivity.getString(R.string.chromecast_is_connected), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                exoPlayerFragment.setMediaRouteButtonVisible(true);
                break;
            default:
                Log.d(TAG, "CastState is unknown.");
                exoPlayerFragment.setMediaRouteButtonVisible(true);
                break;
        }
    }
}
