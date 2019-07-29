package com.smile.karaokeplayer.NotUsed;

public class NotUsedClassesOrMethods {
    private NotUsedClassesOrMethods() {
    }

    // use default ActionBar inside onCreate() in MainActivity.class
        /*
        setTitle(String.format(Locale.getDefault(), ""));
        supportToolbar = getSupportActionBar();
        supportToolbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        supportToolbar.setCustomView(titleView);
        toolbarTitleView = new TextView(this);
        toolbarTitleView.setText(supportToolbar.getTitle());
        toolbarTitleView.setTextColor(Color.WHITE);
        ScreenUtil.resizeTextSize(toolbarTitleView, textFontSize, SmileApplication.FontSize_Scale_Type);
        */
    //

    /*
    // open a media file for playing in external storage
    String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    Log.d(TAG, "Public root directory: " + externalPath);
    String filePath = externalPath + "/Song/perfume_h264.mp4";
    Log.d(TAG, "File path: " + filePath);
    File songFile = new File(filePath);
    if (songFile.exists()) {
        Log.d(TAG, "File exists");
        Uri mediaUri = Uri.parse("file://" + filePath);
        mediaTransportControls.prepareFromUri(mediaUri, null);
    } else {
        Log.d(TAG, "File does not exist");
    }
    */

    /*
    // No need if use MediaSessionConnector
    private void setMediaPlaybackState(int state) {
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        } else {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
        }
        // playbackStateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        playbackStateBuilder.setState(state, exoPlayer.getContentPosition(), 1f);
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build());
    }
    //
    */

    // Enable callbacks from MediaButtons and TransportControls
    // MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS is deprecated
    // MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS is deprecated
    // mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

    // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
    // PlaybackStateCompat is already defined and mapped in MediaSessionConnector
    // setMediaPlaybackState(mCurrentState);

    // MySessionCallback has methods that handle callbacks from a media controller
    // No need because it will be overridden by MediaSessionConnector
    /*
    MediaSessionCallback mediaSessionCallback = new MediaSessionCallback();
    mediaSessionCompat.setCallback(mediaSessionCallback);
    */

    /*
    // Already defined in MediaSessionConnector if use MediaSessionConnector
    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
        }

        @Override
        public void onPrepare() {
            super.onPrepare();

            exoPlayer.seekTo(0);
            exoPlayer.setPlayWhenReady(false);
            setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);

            Log.d(TAG, "MediaSessionCallback.onPrepare() is called.");
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            super.onPrepareFromUri(uri, extras);
            try {
                mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                exoPlayer.prepare(mediaSource);
                exoPlayer.setPlayWhenReady(false);  // do not start playing
                setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);
            } catch (Exception ex) {
                Log.d(TAG, "Exception happened to onPrepareFromUri() of MediaSessionCallback.");
                ex.printStackTrace();
            }

            Log.d(TAG, "MediaSessionCallback.onPrepareFromUri() is called.");
        }

        @Override
        public void onPlay() {
            super.onPlay();
            MediaControllerCompat controller = mediaSessionCompat.getController();
            PlaybackStateCompat stateCompat = controller.getPlaybackState();
            int state = stateCompat.getState();
            if (state != PlaybackStateCompat.STATE_PLAYING) {
                int exoPlayerState = exoPlayer.getPlaybackState();
                if (exoPlayerState == Player.STATE_READY) {
                    exoPlayer.setPlayWhenReady(true);
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }
            }
            Log.d(TAG, "MediaSessionCallback.onPlay() is called.");
        }

        @Override
        public void onPause() {
            super.onPause();
            MediaControllerCompat controller = mediaSessionCompat.getController();
            PlaybackStateCompat stateCompat = controller.getPlaybackState();
            int state = stateCompat.getState();
            if (state != PlaybackStateCompat.STATE_PAUSED) {
                exoPlayer.setPlayWhenReady(false);
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            }
            Log.d(TAG, "MediaSessionCallback.onPause() is called.");
        }

        @Override
        public void onStop() {
            super.onStop();
            MediaControllerCompat controller = mediaSessionCompat.getController();
            PlaybackStateCompat stateCompat = controller.getPlaybackState();
            int state = stateCompat.getState();
            if (state != PlaybackStateCompat.STATE_STOPPED) {
                // exoPlayer.stop();
                exoPlayer.setPlayWhenReady(false);
                exoPlayer.seekTo(0);
                exoPlayer.retry();
                setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            }
            Log.d(TAG, "MediaSessionCallback.onStop() is called.");
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            setMediaPlaybackState(PlaybackStateCompat.STATE_FAST_FORWARDING);
            Log.d(TAG, "MediaSessionCallback.onFastForward() is called.");
        }

        @Override
        public void onRewind() {
            super.onRewind();
            setMediaPlaybackState(PlaybackStateCompat.STATE_REWINDING);
            Log.d(TAG, "MediaSessionCallback.onRewind() is called.");
        }
    }
    //
    */
}
