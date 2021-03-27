package exoplayer.Callbacks;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Models.PlayingParameters;
import exoplayer.Presenters.ExoPlayerPresenter;

import static com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES;
import static com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS;

public class ExoPlaybackPreparer implements MediaSessionConnector.PlaybackPreparer {

    private static final String TAG = "ExoPlaybackPreparer";
    private final Context callingContext;
    private final ExoPlayerPresenter mPresenter;

    public ExoPlaybackPreparer(Context context, ExoPlayerPresenter presenter) {
        callingContext = context;
        mPresenter = presenter;
    }

    @Override
    public synchronized long getSupportedPrepareActions() {
        long supportedPrepareActions = MediaSessionConnector.PlaybackPreparer.ACTIONS;
        Log.d(TAG, "getSupportedPrepareActions() is called.");
        return supportedPrepareActions;
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
        Log.d(TAG, "ExoPlaybackPreparer.onPrepareFromUri()->Uri = " + uri);
        if (uri == null) {
            return;
        }

        PlayingParameters playingParam = mPresenter.getPlayingParam();
        SimpleExoPlayer exoPlayer = mPresenter.getExoPlayer();

        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory()
                .setTsExtractorFlags(FLAG_DETECT_ACCESS_UNITS)
                .setTsExtractorFlags(FLAG_ALLOW_NON_IDR_KEYFRAMES);

        playingParam.setMediaSourcePrepared(false);

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(callingContext, Util.getUserAgent(callingContext, callingContext.getPackageName()));

        // removed on 2021-03-27
        // MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(uri);
        //
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(new MediaMetadata.Builder().setTitle("Opened Media").build())
                .setMimeType(MimeTypes.BASE_TYPE_VIDEO)
                // .setDrmConfiguration(null)
                .build();
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(mediaItem);
        Log.d(TAG, "ExoPlaybackPreparer.onPrepareFromUri()->mediaSource = " + mediaSource);
        if (mediaSource == null) {
            return;
        }
        
        // exoPlayer.prepare(mediaSource);  // deprecated, removed on 2021-03-26
        exoPlayer.setMediaSource(mediaSource);
        exoPlayer.prepare();

        long currentAudioPosition = playingParam.getCurrentAudioPosition();
        int playbackState = playbackState = playingParam.getCurrentPlaybackState();
        if (extras != null) {
            Log.d(TAG, "extras is not null.");
            PlayingParameters playingParamOrigin = extras.getParcelable(PlayerConstants.PlayingParamOrigin);
            if (playingParamOrigin != null) {
                Log.d(TAG, "playingParamOrigin is not null.");
                playbackState = playingParamOrigin.getCurrentPlaybackState();
                currentAudioPosition = playingParamOrigin.getCurrentAudioPosition();
            }
        }
        exoPlayer.seekTo(currentAudioPosition);
        switch (playbackState) {
            case PlaybackStateCompat.STATE_PAUSED:
                exoPlayer.setPlayWhenReady(false);
                Log.d(TAG, "setMediaSourcePrepared() --> PlaybackStateCompat.STATE_PAUSED");
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                exoPlayer.setPlayWhenReady(false);
                Log.d(TAG, "setMediaSourcePrepared() --> PlaybackStateCompat.STATE_STOPPED");
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                exoPlayer.setPlayWhenReady(true);  // start playing when ready
                Log.d(TAG, "setMediaSourcePrepared() --> PlaybackStateCompat.STATE_PLAYING");
                break;
            default:
                // PlaybackStateCompat.STATE_NONE:
                exoPlayer.setPlayWhenReady(true);  // start playing when ready
                Log.d(TAG, "setMediaSourcePrepared() --> PlaybackStateCompat.STATE_NONE");
                break;
        }

        Log.d(TAG, "MediaSessionConnector.PlaybackPreparer.onPrepareFromUri() is called--> " + playbackState);
    }

    @Override
    public synchronized boolean onCommand(Player player, ControlDispatcher controlDispatcher, String command, Bundle extras, ResultReceiver cb) {
        return false;
    }
}
