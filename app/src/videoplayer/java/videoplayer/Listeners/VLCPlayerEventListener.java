package videoplayer.Listeners;

import android.content.Context;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.smile.karaokeplayer.Models.PlayingParameters;
import com.smile.karaokeplayer.R;
import com.smile.smilelibraries.utilities.ScreenUtil;

import org.videolan.libvlc.MediaPlayer;

import videoplayer.Presenters.VLCPlayerPresenter;

public class VLCPlayerEventListener implements MediaPlayer.EventListener {

    private static final String TAG = "VLCPlayerEventListener";
    private final Context callingContext;
    private final VLCPlayerPresenter mPresenter;
    private final MediaPlayer vlcPlayer;
    private final float toastTextSize;

    public VLCPlayerEventListener(Context context, VLCPlayerPresenter presenter) {
        callingContext = context;
        mPresenter = presenter;
        vlcPlayer = mPresenter.getVlcPlayer();

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(callingContext, ScreenUtil.FontSize_Pixel_Type, null);
        float textFontSize = ScreenUtil.suitableFontSize(callingContext, defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        toastTextSize = 0.7f * textFontSize;
    }

    @Override
    public synchronized void onEvent(MediaPlayer.Event event) {
        Log.d(TAG, "onEvent() is called and MediaPlayer.Event event = " + event);
        PlayingParameters playingParam = mPresenter.getPlayingParam();
        switch(event.type) {
            case MediaPlayer.Event.Buffering:
                Log.d(TAG, "vlcPlayer is buffering.");
                if (!vlcPlayer.isPlaying()) {
                    mPresenter.getPresentView().hideNativeAndBannerAd();
                    mPresenter.getPresentView().showBufferingMessage();
                    mPresenter.setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
                }
                break;
            case MediaPlayer.Event.Playing:
                Log.d(TAG, "vlcPlayer is being played.");
                mPresenter.getPresentView().dismissBufferingMessage();
                mPresenter.getPresentView().hideNativeAndBannerAd();
                mPresenter.setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                break;
            case MediaPlayer.Event.Paused:
                Log.d(TAG, "vlcPlayer is paused");
                mPresenter.getPresentView().dismissBufferingMessage();
                mPresenter.setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                break;
            case MediaPlayer.Event.Stopped:
                Log.d(TAG, "vlcPlayer is stopped");
                mPresenter.getPresentView().dismissBufferingMessage();
                mPresenter.getPresentView().showNativeAndBannerAd();
                // Because vlcPlayer will send a MediaPlayer.Event.Stopped
                // after sending a MediaPlayer.Event.EndReached when finished playing
                // Avoid sending a MediaPlayer.Event.Stopped after finished playing
                if (playingParam.isMediaSourcePrepared()) {
                    // playing has not been finished yet
                    Log.d(TAG, "Sending a event, PlaybackStateCompat.STATE_STOPPED");
                    mPresenter.setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                } // else {
                    // playing has finished
                // }
                break;
            case MediaPlayer.Event.EndReached:
                Log.d(TAG, "vlcPlayer is Reached end.");
                playingParam.setMediaSourcePrepared(false);
                mPresenter.setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);
                // to fix bugs that vlcPlayer.attachViews() does works in onResume()
                // after finishing playing and reopen the same media file
                // vlcPlayer.stop() can make status become stop status for vlcPlayer
                // but will not affect the playingParam.getCurrentPlaybackState()
                vlcPlayer.stop();   // using to fix bug of vlcPlayer
                break;
            case MediaPlayer.Event.Opening:
                Log.d(TAG, "vlcPlayer is Opening media.");
                break;
            case MediaPlayer.Event.PositionChanged:
                break;
            case MediaPlayer.Event.TimeChanged:
                mPresenter.getPresentView().update_Player_duration_seekbar_progress((int)vlcPlayer.getTime());
                break;
            case MediaPlayer.Event.EncounteredError:
                Log.d(TAG, "vlcPlayer is EncounteredError event");
                mPresenter.getPresentView().showNativeAndBannerAd();
                mPresenter.setMediaPlaybackState(PlaybackStateCompat.STATE_ERROR);
                String formatNotSupportedString = callingContext.getString(R.string.formatNotSupportedString);
                Log.d(TAG, "mPresenter.isCanShowNotSupportedFormat() = " + mPresenter.isCanShowNotSupportedFormat());
                if (mPresenter.isCanShowNotSupportedFormat()) {
                    // only show once
                    mPresenter.setCanShowNotSupportedFormat(false);
                    ScreenUtil.showToast(callingContext, formatNotSupportedString, toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
                }
                mPresenter.startAutoPlay();
                break;
            default:
                break;
        }
    }
}
