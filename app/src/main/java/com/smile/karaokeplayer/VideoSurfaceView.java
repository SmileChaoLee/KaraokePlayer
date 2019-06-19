package com.smile.karaokeplayer;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VideoSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = new String(".VideoSurfaceView");

    private final Context mContext;
    private final MainActivity mainActivity;
    private final SurfaceHolder surfaceHolder;

    private int viewWidth;
    private int viewHeight;

    public VideoSurfaceView(Context context) {
        super(context);

        mContext = context;
        mainActivity = (MainActivity)mContext;

        setWillNotDraw(true);   // disable the onDraw()

        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this); // register the interface

        setZOrderOnTop(true);
        // surfaceHolder.setFormat(PixelFormat.TRANSPARENT);    // same effect as the following
        // surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        viewWidth = getWidth();
        viewHeight = getHeight();
        Log.d(TAG, "surfaceCreated() is called.");
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d(TAG, "surfaceChanged() is called.");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceDestroyed() is called.");
    }

    public SurfaceHolder getSurfaceHolder() {
        return surfaceHolder;
    }
}
