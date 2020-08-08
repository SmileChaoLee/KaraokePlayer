package com.smile.karaokeplayer.AudioProcessors;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.audio.BaseAudioProcessor;

import java.nio.ByteBuffer;

public class StereoVolumeAudioProcessor extends BaseAudioProcessor {

    private static final String TAG = "StereoVolumeAudioProcessor";
    private static final int maxChannels = 16;
    private float[] volume;
    public static final int LEFT_SPEAKER = 0;
    public static final int RIGHT_SPEAKER = 1;

    public StereoVolumeAudioProcessor() {
        super();
        // volume has to be set at first place
        volume = new float[maxChannels]; // max is 16 channels
        for (int i=0; i<volume.length; i++) {
            volume[i] = 1.0f;
        }
    }

    /**
     * Sets the volume of right and left channels/speakers
     * The values are between 0.0 and 1.0
     *
     */
    public void setVolume(float[] volumeInput) {
        if ( (volume == null) || (volumeInput == null) )  {
            return;
        }
        for (int i=0; (i<volumeInput.length) && (i<maxChannels); i++) {
            volume[i] = volumeInput[i];
        }
    }

    public float[] getVolume() {
        return volume;
    }

    @Override
    public AudioFormat onConfigure(AudioFormat inputAudioFormat)
            throws UnhandledAudioFormatException {

        return new AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, inputAudioFormat.encoding);
    }

    /*
    @Override
    public boolean configure(int sampleRateHz, int channelCount, @C.Encoding int encoding)
            throws UnhandledFormatException {

        Log.d(TAG, "StereoVolumeAudioProcessor.configure() --> sampleRateHz = " + sampleRateHz);
        Log.d(TAG, "StereoVolumeAudioProcessor.configure() --> channelCount = " + channelCount);
        Log.d(TAG, "StereoVolumeAudioProcessor.configure() --> encoding = " + encoding);

        boolean isSetConfig = setInputFormat(sampleRateHz, channelCount, encoding);

        return isSetConfig;
    }
    */

    @Override
    public void queueInput(ByteBuffer inputBuffer) {

        if (!isActive()) {
            Log.d(TAG, "queueInput() --> Exception because of isActive() = " + isActive());
            throw new IllegalStateException();
        }

        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        int outputSize = limit - position;
        int reSampledSize;
        switch (inputAudioFormat.encoding) {
            case C.ENCODING_PCM_8BIT:
                reSampledSize = outputSize * 2;
                break;
            case C.ENCODING_PCM_24BIT:
                reSampledSize = (outputSize / 3) * 2;
                break;
            case C.ENCODING_PCM_32BIT:
                reSampledSize = outputSize / 2;
                break;
            case C.ENCODING_PCM_16BIT:
            default:
                reSampledSize = outputSize;
                break;
        }

        ByteBuffer buffer = replaceOutputBuffer(reSampledSize);

        switch (inputAudioFormat.encoding) {
            case C.ENCODING_PCM_8BIT:
                // 8->16 bit resampling. Shift each byte from [0, 256) to [-128, 128) and scale up.
                for (int i = position; i < limit; i++) {
                    buffer.put((byte) 0);
                    // buffer.put((byte) ((inputBuffer.get(i) & 0xFF) - 128));
                    int b = (int)(((inputBuffer.get(i) & 0xFF) - 128) * volume[0]);
                    buffer.put((byte)b);
                }
                break;
            case C.ENCODING_PCM_24BIT:
                // 24->16 bit resampling. Drop the least significant byte.
                for (int i = position; i < limit; i += 3) {
                    buffer.putShort((short)(inputBuffer.get(i + 1) * volume[0]));
                    buffer.putShort((short)(inputBuffer.get(i + 2) * volume[1]));
                }
                break;
            case C.ENCODING_PCM_32BIT:
                // 32->16 bit resampling. Drop the two least significant bytes.
                for (int i = position; i < limit; i += 4) {
                    buffer.putShort((short)(inputBuffer.get(i + 2) * volume[0]));
                    buffer.putShort((short)(inputBuffer.get(i + 3) * volume[1]));
                }
                break;
            case C.ENCODING_PCM_16BIT:
            default:
                if (inputAudioFormat.channelCount != 2) {
                    // not stereo
                    while (position < limit) {
                        for (int channelIndex=0; channelIndex<inputAudioFormat.channelCount; channelIndex++) {
                            buffer.putShort((short) (inputBuffer.getShort(position + 2 * channelIndex) * volume[channelIndex]));
                        }
                        position += inputAudioFormat.channelCount * 2;
                    }
                } else {
                    // channelCount = 2 (Stereo)
                    try {
                        if ((volume[LEFT_SPEAKER] != 0) && (volume[RIGHT_SPEAKER] == 0)) {
                            // only left speaker has sound
                            for (int i = position; i < limit; i += 4) {
                                short sampleLeft = (short) (inputBuffer.getShort(i) * volume[LEFT_SPEAKER]);
                                int j = i + 2;
                                if (j < limit) {
                                    short sampleRight = (short) (inputBuffer.getShort(j) * volume[RIGHT_SPEAKER]);
                                    buffer.putShort(sampleLeft);    // left speaker
                                    buffer.putShort(sampleLeft);    // use left sound for right speaker
                                }
                            }
                        } else if ((volume[LEFT_SPEAKER] == 0) && (volume[RIGHT_SPEAKER] != 0)) {
                            // only right speaker has sound
                            for (int i = position; i < limit; i += 4) {
                                short sampleLeft = (short) (inputBuffer.getShort(i) * volume[LEFT_SPEAKER]);
                                int j = i + 2;
                                if (j < limit) {
                                    short sampleRight = (short) (inputBuffer.getShort(j) * volume[RIGHT_SPEAKER]);
                                    buffer.putShort(sampleRight);    // use right sound for left speaker
                                    buffer.putShort(sampleRight);    // right speaker
                                }
                            }
                        } else {
                            for (int i = position; i < limit; i += 4) {
                                short sampleLeft = (short) (inputBuffer.getShort(i) * volume[LEFT_SPEAKER]);
                                int j = i + 2;
                                if (j < limit) {
                                    short sampleRight = (short) (inputBuffer.getShort(j) * volume[RIGHT_SPEAKER]);
                                    buffer.putShort(sampleLeft);    // left speaker
                                    buffer.putShort(sampleRight);   // right speaker
                                }
                            }
                        }
                    } catch (Exception ex) {
                        Log.d(TAG, "queueInputStereo() --> Exception: ");
                        ex.printStackTrace();
                        reset();
                    }
                }
                break;
        }

        inputBuffer.position(limit);
        buffer.flip();
    }

    public int getOutputChannelCount() {
        return inputAudioFormat.channelCount;
    }
}
