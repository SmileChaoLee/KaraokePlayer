package com.smile.karaokeplayer.ExoRenderersFactory;

import android.content.Context;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.smile.karaokeplayer.AudioProcessor_implement.StereoVolumeAudioProcessor;

public class MyRenderersFactory extends DefaultRenderersFactory {

    private static final String TAG = new String(".ExoRenderersFactory.MyRenderersFactory");

    // Customized AudioProcessor
    private final StereoVolumeAudioProcessor stereoVolumeAudioProcessor = new StereoVolumeAudioProcessor();

    public MyRenderersFactory(Context context) {
        super(context);
        setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON);   // default is using extension
        // setExtensionRendererMode(EXTENSION_RENDERER_MODE_OFF);
        // setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER);
    }

    @Override
    protected AudioProcessor[] buildAudioProcessors() {
        Log.d(TAG,"DefaultRenderersFactory.buildAudioProcessors() is called.");
        AudioProcessor[] audioProcessors;

        int arrayLength = 0;
        if (super.buildAudioProcessors() != null) {
            arrayLength = super.buildAudioProcessors().length;
        }

        audioProcessors = new AudioProcessor[arrayLength + 1];
        for (int i=0; i<arrayLength; i++) {
            audioProcessors[i] = (super.buildAudioProcessors())[i];
        }
        audioProcessors[arrayLength] = stereoVolumeAudioProcessor;

        return audioProcessors;
    }

    public StereoVolumeAudioProcessor getStereoVolumeAudioProcessor() {
        return stereoVolumeAudioProcessor;
    }
}
