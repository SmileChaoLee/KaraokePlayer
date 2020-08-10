package exoplayer.Listeners;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.cast.framework.CastState;
import com.smile.karaokeplayer.Presenters.PlayerBasePresenter;
import com.smile.karaokeplayer.R;
import com.smile.karaokeplayer.SmileApplication;
import com.smile.smilelibraries.utilities.ScreenUtil;

public class BaseCastStateListener implements com.google.android.gms.cast.framework.CastStateListener {
    private static final String TAG = "BaseCastStateListener";
    private final Context callingContext;
    private final PlayerBasePresenter mPresenter;
    private final float toastTextSize;

    public BaseCastStateListener(Context context, PlayerBasePresenter presenter) {
        callingContext = context;
        mPresenter = presenter;
        toastTextSize = mPresenter.getToastTextSize();
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onCastStateChanged(int i) {
        SmileApplication.currentCastState = i;
        switch (i) {
            case CastState.NO_DEVICES_AVAILABLE:
                Log.d(TAG, "CastState is NO_DEVICES_AVAILABLE.");
                ScreenUtil.showToast(callingContext, callingContext.getString(R.string.no_chromecast_devices_avaiable), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                mPresenter.getPresentView().setMediaRouteButtonVisible(false);
                break;
            case CastState.NOT_CONNECTED:
                Log.d(TAG, "CastState is NOT_CONNECTED.");
                ScreenUtil.showToast(callingContext, callingContext.getString(R.string.chromecast_not_connected), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                mPresenter.getPresentView().setMediaRouteButtonVisible(true);
                break;
            case CastState.CONNECTING:
                Log.d(TAG, "CastState is CONNECTING.");
                ScreenUtil.showToast(callingContext, callingContext.getString(R.string.chromecast_is_connecting), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                mPresenter.getPresentView().setMediaRouteButtonVisible(true);
                break;
            case CastState.CONNECTED:
                Log.d(TAG, "CastState is CONNECTED.");
                ScreenUtil.showToast(callingContext, callingContext.getString(R.string.chromecast_is_connected), toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                mPresenter.getPresentView().setMediaRouteButtonVisible(true);
                break;
            default:
                Log.d(TAG, "CastState is unknown.");
                mPresenter.getPresentView().setMediaRouteButtonVisible(true);
                break;
        }
        Log.d(TAG, "onCastStateChanged() is called.");
    }
}
