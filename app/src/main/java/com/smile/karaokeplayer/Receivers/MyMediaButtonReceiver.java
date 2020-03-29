package com.smile.karaokeplayer.Receivers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.media.session.MediaButtonReceiver;

public class MyMediaButtonReceiver extends MediaButtonReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            super.onReceive(context, intent);
        // } catch (IllegalStateException e) {
        } catch (Exception e) {
            Log.d(this.getClass().getName(), e.getMessage());
        }
    }
}
