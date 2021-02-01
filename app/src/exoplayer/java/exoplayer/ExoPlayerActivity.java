package exoplayer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;
import androidx.mediarouter.app.MediaRouteButton;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import exoplayer.Presenters.ExoPlayerPresenter;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastState;
import com.smile.karaokeplayer.BasePlayerActivity;
import com.smile.karaokeplayer.Presenters.BasePlayerPresenter;
import com.smile.karaokeplayer.R;

public class ExoPlayerActivity extends BasePlayerActivity implements ExoPlayerPresenter.ExoPlayerPresentView {
    private static final String TAG = "ExoPlayerActivity";

    private ExoPlayerPresenter mPresenter;
    private SimpleExoPlayer exoPlayer;
    private PlayerView videoExoPlayerView;
    private MediaRouteButton mMediaRouteButton;
    private CastPlayer castPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate() is called.");

        mPresenter = new ExoPlayerPresenter(this, this);

        // removed on 2020-12-08
        // Intent callingIntent = getIntent();
        // mPresenter.initializeVariables(savedInstanceState, callingIntent);
        //

        super.onCreate(savedInstanceState);

        mPresenter.initExoPlayerAndCastPlayer();   // must be before volumeSeekBar settings
        mPresenter.initMediaSessionCompat();

        exoPlayer = mPresenter.getExoPlayer();
        castPlayer = mPresenter.getCastPlayer();

        // Video player view
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        videoExoPlayerView = new PlayerView(this);
        videoExoPlayerView.setLayoutParams(layoutParams);
        videoExoPlayerView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        playerViewLinearLayout.addView(videoExoPlayerView);

        videoExoPlayerView.setVisibility(View.VISIBLE);
        videoExoPlayerView.setPlayer(exoPlayer);
        videoExoPlayerView.setUseArtwork(true);
        videoExoPlayerView.setUseController(false);
        videoExoPlayerView.requestFocus();

        int currentProgress = mPresenter.getCurrentProgressForVolumeSeekBar();
        volumeSeekBar.setProgressAndThumb(currentProgress);

        mPresenter.playTheSongThatWasPlayedBeforeActivityCreated();

        mPresenter.addBaseCastStateListener();
        if (castPlayer != null && exoPlayer != null) {
            Log.d(TAG, "castPlayer != null && exoPlayer != null");
            mPresenter.setCurrentPlayer(castPlayer.isCastSessionAvailable() ? castPlayer : exoPlayer);
        } else {
            Log.d(TAG, "castPlayer == null || exoPlayer == null");
        }

        Log.d(TAG,"onCreate() is finished.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy() is called.");
        if (mPresenter != null) {
            mPresenter.releaseMediaSessionCompat();
            mPresenter.releaseExoPlayerAndCastPlayer();
            mPresenter.removeBaseCastStateListener();
        }
        videoExoPlayerView.setPlayer(null);
    }

    // implementing methods of ExoPlayerPresenter.ExoPlayerPresentView

    @Override
    public void setCurrentPlayerToPlayerView() {
        Player currentPlayer = mPresenter.getCurrentPlayer();
        if (currentPlayer == null) {
            return;
        }

        if (currentPlayer == exoPlayer) {
            // videoExoPlayerView.setVisibility(View.VISIBLE);
            // videoExoPlayerView.setPlayer(exoPlayer);
            // castControlView.hide();
            Log.d(TAG, "Current player is exoPlayer." );
        } else /* currentPlayer == castPlayer */ {
            // videoExoPlayerView.setVisibility(View.INVISIBLE);
            // castControlView.show();
            Log.d(TAG, "Current player is castPlayer." );
        }
    }
    // end of implementing methods of PlayerBasePresenter.BasePresentView

    // implement abstract methods of super class
    @Override
    public BasePlayerPresenter getPlayerBasePresenter() {
        return mPresenter;
    }

    @Override
    public void setMediaRouteButtonView(int buttonMarginLeft, int imageButtonHeight) {
        // MediaRouteButton View
        mMediaRouteButton = findViewById(R.id.media_route_button);
        if (mPresenter.getCurrentCastState() == CastState.NO_DEVICES_AVAILABLE) {
            setMediaRouteButtonVisible(false);
        } else {
            setMediaRouteButtonVisible(true);
        }
        CastButtonFactory.setUpMediaRouteButton(this, mMediaRouteButton);

        ViewGroup.MarginLayoutParams layoutParams;
        layoutParams = (ViewGroup.MarginLayoutParams) mMediaRouteButton.getLayoutParams();
        // layoutParams.height = imageButtonHeight;
        // layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);
        // the following drawable is to customize the image of MediaRouteButton
        // setRemoteIndicatorDrawable(Drawable d)
        Bitmap mediaRouteButtonBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cast);
        Drawable mediaRouteButtonDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(mediaRouteButtonBitmap, imageButtonHeight, imageButtonHeight, true));
        mMediaRouteButton.setRemoteIndicatorDrawable(mediaRouteButtonDrawable);
        //
    }

    @Override
    public void setMediaRouteButtonVisible(boolean isVisible) {
        if (isVisible) {
            mMediaRouteButton.setVisibility(View.VISIBLE);
        } else {
            mMediaRouteButton.setVisibility(View.GONE);
        }
    }

    @Override
    public Intent createIntentForSongListActivity() {
        return new Intent(getApplicationContext(), SongListActivity.class);
    }

    @Override
    public void setMenuItemsVisibility() {
        // do nothing
    }

    @Override
    public void setSwitchToVocalImageButtonVisibility() {
        // do nothing
    }

    // end of implementing methods of super class
}
