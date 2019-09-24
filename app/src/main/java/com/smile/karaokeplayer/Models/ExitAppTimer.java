package com.smile.karaokeplayer.Models;

import android.os.Handler;
import android.os.Looper;

public class ExitAppTimer {

    private final int timePeriod;
    private final Handler timerHandler;
    private final Runnable timerRunnable;
    private int numOfBackPressTouched = 0;

    public ExitAppTimer(int timePeriod) {
        this.timePeriod = timePeriod;
        numOfBackPressTouched = 0;
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                numOfBackPressTouched = 0;
                timerHandler.removeCallbacksAndMessages(null);
            }
        };
    }

    public void start() {
        numOfBackPressTouched++;
        timerHandler.removeCallbacksAndMessages(null);
        timerHandler.postDelayed(timerRunnable, timePeriod);
    }

    public boolean canExit() {
        return (numOfBackPressTouched > 0) ? true : false;
    }
}
