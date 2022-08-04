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

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastState;
import com.smile.karaokeplayer.BasePlayerActivity;
import com.smile.karaokeplayer.presenters.BasePlayerPresenter;
import com.smile.karaokeplayer.R;

import exoplayer.presenters.ExoPlayerPresenter;

public class ExoPlayerActivity extends BasePlayerActivity implements ExoPlayerPresenter.ExoPlayerPresentView {
    private static final String TAG = ExoPlayerActivity.class.getName();

    private ExoPlayerPresenter presenter;
    private ExoPlayer exoPlayer;
    private StyledPlayerView playerView;
    private MediaRouteButton mediaRouteButton;
    private CastPlayer castPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate() is called.");

        presenter = new ExoPlayerPresenter(this, this);

        super.onCreate(savedInstanceState);

        presenter.initExoPlayerAndCastPlayer();   // must be before volumeSeekBar settings
        presenter.initMediaSessionCompat();

        exoPlayer = presenter.getExoPlayer();
        castPlayer = presenter.getCastPlayer();

        // Video player view
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        playerView = new StyledPlayerView(this);
        playerView.setLayoutParams(layoutParams);
        playerView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        playerViewLinearLayout.addView(playerView);

        playerView.setVisibility(View.VISIBLE);
        playerView.setPlayer(exoPlayer);
        playerView.setUseArtwork(true);
        playerView.setUseController(false);
        playerView.requestFocus();

        int currentProgress = presenter.getCurrentProgressForVolumeSeekBar();
        volumeSeekBar.setProgressAndThumb(currentProgress);

        presenter.playTheSongThatWasPlayedBeforeActivityCreated();

        // mPresenter.addBaseCastStateListener();   // moved to onResume() on 2021-03-26
        if (castPlayer != null && exoPlayer != null) {
            Log.d(TAG, "castPlayer != null && exoPlayer != null");
            presenter.setCurrentPlayer(castPlayer.isCastSessionAvailable() ? castPlayer : exoPlayer);
        } else {
            Log.d(TAG, "castPlayer == null || exoPlayer == null");
        }

        Log.d(TAG,"onCreate() is finished.");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"onStart() is finished.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume() is finished.");
        presenter.setSessionAvailabilityListener();
        presenter.addBaseCastStateListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause() is finished.");
        presenter.releaseSessionAvailabilityListener();
        presenter.removeBaseCastStateListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop() is finished.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy() is called.");
        if (presenter != null) {
            presenter.releaseMediaSessionCompat();
            presenter.releaseExoPlayerAndCastPlayer();
        }
        playerView.setPlayer(null);
    }

    // implementing methods of ExoPlayerPresenter.ExoPlayerPresentView

    @Override
    public void setCurrentPlayerToPlayerView() {
        Player currentPlayer = presenter.getCurrentPlayer();
        if (currentPlayer == null) {
            return;
        }

        if (currentPlayer == exoPlayer) {
            // playerView.setVisibility(View.VISIBLE);
            // playerView.setPlayer(exoPlayer);
            // castControlView.hide();
            Log.d(TAG, "Current player is exoPlayer." );
        } else /* currentPlayer == castPlayer */ {
            // playerView.setVisibility(View.INVISIBLE);
            // castControlView.show();
            Log.d(TAG, "Current player is castPlayer." );
        }
    }
    // end of implementing methods of PlayerBasePresenter.BasePresentView

    // implement abstract methods of super class
    @Override
    public BasePlayerPresenter getPlayerBasePresenter() {
        return presenter;
    }

    @Override
    public void setMediaRouteButtonView(int buttonMarginLeft, int imageButtonHeight) {
        // MediaRouteButton View
        mediaRouteButton = findViewById(R.id.media_route_button);
        /*
        if (presenter.getCurrentCastState() == CastState.NO_DEVICES_AVAILABLE) {
            setMediaRouteButtonVisible(false);
        } else {
            setMediaRouteButtonVisible(true);
        }
        */
        setMediaRouteButtonVisible(presenter.getCurrentCastState() != CastState.NO_DEVICES_AVAILABLE);
        CastButtonFactory.setUpMediaRouteButton(this, mediaRouteButton);

        ViewGroup.MarginLayoutParams layoutParams;
        layoutParams = (ViewGroup.MarginLayoutParams) mediaRouteButton.getLayoutParams();
        // layoutParams.height = imageButtonHeight;
        // layoutParams.width = imageButtonHeight;
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0);
        // the following drawable is to customize the image of MediaRouteButton
        // setRemoteIndicatorDrawable(Drawable d)
        Bitmap mediaRouteButtonBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cast);
        Drawable mediaRouteButtonDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(mediaRouteButtonBitmap, imageButtonHeight, imageButtonHeight, true));
        mediaRouteButton.setRemoteIndicatorDrawable(mediaRouteButtonDrawable);
        //
    }

    @Override
    public void setMediaRouteButtonVisible(boolean isVisible) {
        if (isVisible) {
            mediaRouteButton.setVisibility(View.VISIBLE);
        } else {
            mediaRouteButton.setVisibility(View.GONE);
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
