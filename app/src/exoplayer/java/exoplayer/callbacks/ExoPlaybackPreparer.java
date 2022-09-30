package exoplayer.callbacks;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.smile.karaokeplayer.constants.PlayerConstants;
import com.smile.karaokeplayer.models.PlayingParameters;

import exoplayer.presenters.ExoPlayerPresenter;

public class ExoPlaybackPreparer implements MediaSessionConnector.PlaybackPreparer {

    private static final String TAG = "ExoPlaybackPreparer";
    private final Context callingContext;
    private final ExoPlayerPresenter mPresenter;

    public ExoPlaybackPreparer(Context context, ExoPlayerPresenter presenter) {
        callingContext = context;
        mPresenter = presenter;
        Log.d(TAG, "ExoPlaybackPreparer is created.");
    }

    @Override
    public synchronized long getSupportedPrepareActions() {
        Log.d(TAG, "getSupportedPrepareActions() is called --> MediaSessionConnector.PlaybackPreparer.ACTIONS = " + MediaSessionConnector.PlaybackPreparer.ACTIONS);
        return MediaSessionConnector.PlaybackPreparer.ACTIONS;
    }

    @Override
    public synchronized void onPrepare(boolean playWhenReady) {
        Log.d(TAG, "onPrepare() is called.");
    }

    @Override
    public synchronized void onPrepareFromMediaId(String mediaId, boolean playWhenReady, Bundle extras) {
        Log.d(TAG, "onPrepareFromMediaId() is called.");
    }

    @Override
    public synchronized void onPrepareFromSearch(String query, boolean playWhenReady, Bundle extras) {
        Log.d(TAG, "onPrepareFromSearch() is called.");
    }

    @Override
    public synchronized void onPrepareFromUri(Uri uri, boolean playWhenReady, Bundle extras) {
        Log.d(TAG, "onPrepareFromUri.Uri = " + uri);
        if (uri == null) {
            return;
        }

        PlayingParameters playingParam = mPresenter.getPlayingParam();
        ExoPlayer exoPlayer = mPresenter.getExoPlayer();

        playingParam.setMediaPrepared(false);

        /*
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(new MediaMetadata.Builder().setTitle("Opened Media").build())
                .setMimeType(MimeTypes.BASE_TYPE_VIDEO)
                // .setDrmConfiguration(null)
                .build();
        */
        MediaItem mediaItem = MediaItem.fromUri(uri);

        // removed on 2021-05-23 for testing
        /*
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory()
            .setTsExtractorFlags(FLAG_DETECT_ACCESS_UNITS)
            .setTsExtractorFlags(FLAG_ALLOW_NON_IDR_KEYFRAMES);
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(callingContext, Util.getUserAgent(callingContext, callingContext.getPackageName()));
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(mediaItem);
        Log.d(TAG, "onPrepareFromUri() --> mediaSource = " + mediaSource);
        if (mediaSource == null) {
            return;
        }
        Log.d(TAG, "onPrepareFromUri() --> preparing mediaSource");
        exoPlayer.setMediaSource(mediaSource);
        */

        Log.d(TAG, "onPrepareFromUri.exoPlayer.getMediaItemCount() = " + exoPlayer.getMediaItemCount());
        TrackSelectionParameters trackParameters = new TrackSelectionParameters.Builder(callingContext).build();
        exoPlayer.setTrackSelectionParameters(trackParameters);
        exoPlayer.setMediaItem(mediaItem);      // added on 2021-05-23
        exoPlayer.prepare();

        float currentVolume = playingParam.getCurrentVolume();
        long currentAudioPosition = playingParam.getCurrentAudioPosition();
        int currentPlaybackState = playingParam.getCurrentPlaybackState();
        Log.d(TAG, "onPrepareFromUri.currentVolume = " + currentVolume +
                ", currentAudioPosition= " + currentAudioPosition + ", currentPlaybackState = " +
                currentPlaybackState);
        if (extras != null) {
            Log.d(TAG, "onPrepareFromUri.extras is not null.");
            PlayingParameters playingParamOrigin;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                playingParamOrigin = extras.getParcelable(PlayerConstants.PlayingParamOrigin, PlayingParameters.class);
            } else playingParamOrigin = extras.getParcelable(PlayerConstants.PlayingParamOrigin);
            if (playingParamOrigin != null) {
                currentPlaybackState = playingParamOrigin.getCurrentPlaybackState();
                currentAudioPosition = playingParamOrigin.getCurrentAudioPosition();
                Log.d(TAG, "onPrepareFromUri.not null.currentVolume = " + currentVolume +
                        ", currentAudioPosition= " + currentAudioPosition + ", currentPlaybackState = " +
                        currentPlaybackState);
            }
        }
        exoPlayer.seekTo(currentAudioPosition);
        // exoPlayer.setVolume(currentVolume);
        switch (currentPlaybackState) {
            case PlaybackStateCompat.STATE_PAUSED:
                exoPlayer.setPlayWhenReady(false);
                Log.d(TAG, "onPrepareFromUri.PlaybackStateCompat.STATE_PAUSED");
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                exoPlayer.setPlayWhenReady(false);
                Log.d(TAG, "onPrepareFromUri.PlaybackStateCompat.STATE_STOPPED");
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                exoPlayer.setPlayWhenReady(true);  // start playing when ready
                Log.d(TAG, "onPrepareFromUri.PlaybackStateCompat.STATE_PLAYING");
                break;
            default:
                // PlaybackStateCompat.STATE_NONE:
                exoPlayer.setPlayWhenReady(true);  // start playing when ready
                Log.d(TAG, "onPrepareFromUr.iPlaybackStateCompat.STATE_NONE or default");
                break;
        }
    }

    @Override
    public boolean onCommand(Player player, String command, @Nullable Bundle extras, @Nullable ResultReceiver cb) {
        return false;
    }
}
