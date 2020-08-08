package com.smile.karaokeplayer.ExoRenderersFactory;

import android.content.Context;
import android.util.Log;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.smile.karaokeplayer.AudioProcessors.StereoVolumeAudioProcessor;

public class MyRenderersFactory extends DefaultRenderersFactory {

    private static final String TAG = "MyRenderersFactory";

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

        Log.d(TAG,"buildAudioProcessors --> arrayLength() = " + arrayLength);

        audioProcessors = new AudioProcessor[arrayLength + 1];

        audioProcessors[0] = stereoVolumeAudioProcessor;

        for (int i=1; i<=arrayLength; i++) {
            audioProcessors[i] = (super.buildAudioProcessors())[i-1];
        }

        return audioProcessors;
    }

    public StereoVolumeAudioProcessor getStereoVolumeAudioProcessor() {
        return stereoVolumeAudioProcessor;
    }
}
