package exoplayer.ExoRenderersFactory;

import android.content.Context;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioSink;
import com.google.android.exoplayer2.audio.DefaultAudioSink;

import exoplayer.AudioProcessors.StereoVolumeAudioProcessor;

public class MyRenderersFactory extends DefaultRenderersFactory {

    private static final String TAG = "MyRenderersFactory";

    // Customized AudioProcessor
    private final StereoVolumeAudioProcessor stereoVolumeAudioProcessor = new StereoVolumeAudioProcessor();
    private final AudioProcessor[] audioProcessors = {stereoVolumeAudioProcessor};

    public MyRenderersFactory(Context context, int extension_renderer_mode) {
        super(context);
        setExtensionRendererMode(extension_renderer_mode);
        // setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON);   // default is using extension
        // setExtensionRendererMode(EXTENSION_RENDERER_MODE_OFF);     // do not use extension
        // setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER);
    }

    @Nullable
    @Override
    protected AudioSink buildAudioSink(Context context, boolean enableFloatOutput, boolean enableAudioTrackPlaybackParams, boolean enableOffload) {
        /*
        DefaultAudioSink.DefaultAudioProcessorChain audioProcessorChain = new DefaultAudioSink.DefaultAudioProcessorChain(audioProcessors);
        AudioSink audioSink = new DefaultAudioSink(AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES,
                        audioProcessorChain,
                        enableFloatOutput,
                        enableAudioTrackPlaybackParams,
                        enableOffload);
        */
        // or
        AudioSink audioSink = new DefaultAudioSink(AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES,
                        audioProcessors,
                        enableFloatOutput);
        return audioSink;
    }

    public StereoVolumeAudioProcessor getStereoVolumeAudioProcessor() {
        return stereoVolumeAudioProcessor;
    }
}
