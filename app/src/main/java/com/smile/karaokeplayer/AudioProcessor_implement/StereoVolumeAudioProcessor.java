package com.smile.karaokeplayer.AudioProcessor_implement;

/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.audio.AudioSink;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class StereoVolumeAudioProcessor implements AudioProcessor {
    private static final String TAG = new String(".StereoVolumeAudioProcessor");
    private int channelCount;
    private int sampleRateHz;
    @Nullable private int[] pendingOutputChannels;
    @Nullable private int[] outputChannels;
    private boolean active;

    private ByteBuffer buffer;
    private ByteBuffer outputBuffer;
    private boolean inputEnded;

    private float[] volume;

    public static final int LEFT_SPEAKER = 0;
    public static final int RIGHT_SPEAKER = 1;

    public StereoVolumeAudioProcessor() {
        buffer = EMPTY_BUFFER;
        outputBuffer = EMPTY_BUFFER;
        channelCount = Format.NO_VALUE;
        sampleRateHz = Format.NO_VALUE;

        active = false;
    }

    /**
     * Resets the channel mapping. After calling this method, call {@link #configure(int, int, int)}
     * to start using the new channel map.
     *
     * @param outputChannels The mapping from input to output channel indices, or {@code null} to
     *     leave the input unchanged.
     * @see AudioSink#configure(int, int, int, int, int[], int, int)
     */
    public void setChannelMap(@Nullable int[] outputChannels) {
        pendingOutputChannels = outputChannels;
        Log.d(TAG, "StereoVolumeAudioProcessor.setChannelMap() was executed.");
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
        for (int i=0; i<volumeInput.length; i++) {
            volume[i] = volumeInput[i];
        }
    }

    public float[] getVolume() {
        return volume;
    }

    @Override
    public boolean configure(int sampleRateHz, int channelCount, @C.Encoding int encoding)
            throws UnhandledFormatException {

        Log.d(TAG, "StereoVolumeAudioProcessor.configure() was called.");
        Log.d(TAG, "StereoVolumeAudioProcessor.configure() --> sampleRateHz = " + sampleRateHz);
        Log.d(TAG, "StereoVolumeAudioProcessor.configure() --> channelCount = " + channelCount);
        Log.d(TAG, "StereoVolumeAudioProcessor.configure() --> encoding = " + encoding);

        this.sampleRateHz = sampleRateHz;
        this.channelCount = channelCount;

        outputChannels = new int[this.channelCount];
        volume = new float[channelCount];
        for (int i=0; i<channelCount; i++) {
            outputChannels[i] = i;
            volume[i] = 1.0f;
        }

        // the followings are useless in this case
        // setChannelMap(outputChannels);
        // or
        // pendingOutputChannels = outputChannels;

        if (encoding != C.ENCODING_PCM_16BIT) {
            Log.d(TAG, "configure() --> it is not C.ENCODING_PCM_16BIT : ");
            throw new UnhandledFormatException(sampleRateHz, channelCount, encoding);
        }

        active = true;

        return true;
    }

    public boolean configure_OLD(int sampleRateHz, int channelCount, @C.Encoding int encoding)
            throws UnhandledFormatException {

        Log.d(TAG, "StereoVolumeAudioProcessor.configure() was called.");
        Log.d(TAG, "StereoVolumeAudioProcessor.configure() --> channelCount = " + channelCount);
        Log.d(TAG, "StereoVolumeAudioProcessor.configure() --> encoding = " + encoding);

        this.sampleRateHz = sampleRateHz;
        this.channelCount = channelCount;

        outputChannels = new int[this.channelCount];
        volume = new float[channelCount];
        for (int i=0; i<channelCount; i++) {
            outputChannels[i] = i;
            volume[i] = 1.0f;
        }

        setChannelMap(outputChannels);

        if (volume == null) {
            throw new IllegalStateException("volume has not been set! Call setVolume(float left,float right)");
        }

        boolean outputChannelsChanged = !Arrays.equals(pendingOutputChannels, outputChannels);
        Log.d(TAG, "StereoVolumeAudioProcessor.outputChannelsChanged() = " + outputChannelsChanged);
        outputChannels = pendingOutputChannels;
        if (outputChannels == null) {
            active = false;
            Log.d(TAG, "StereoVolumeAudioProcessor.outputChannels is null.");
            Log.d(TAG, "StereoVolumeAudioProcessor.configure() = " + outputChannelsChanged);
            return outputChannelsChanged;
        }
        if (encoding != C.ENCODING_PCM_16BIT) {
            throw new UnhandledFormatException(sampleRateHz, channelCount, encoding);
        }
        if (!outputChannelsChanged && this.sampleRateHz == sampleRateHz
                && this.channelCount == channelCount) {
            Log.d(TAG, "StereoVolumeAudioProcessor.outputChannelsChanged is not true.");
            Log.d(TAG, "StereoVolumeAudioProcessor.configure() = " + false);
            return false;
        }

        // this.sampleRateHz = sampleRateHz;
        // this.channelCount = channelCount;

        Log.d(TAG, "StereoVolumeAudioProcessor.configure() = " + true);

        active = true;

        return true;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public int getOutputChannelCount() {
        return outputChannels == null ? channelCount : outputChannels.length;
    }

    @Override
    public int getOutputEncoding() {
        return C.ENCODING_PCM_16BIT;
    }

    /**
     * Returns the sample rate of audio output by the processor, in hertz. The value may change as a
     * result of calling {@link #configure(int, int, int)} and is undefined if the instance is not
     * active.
     */
    @Override
    public int getOutputSampleRateHz() {
        return sampleRateHz;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();

        /*
        int frameCount = (limit - position) / (2 * channelCount);
        int outputSize = frameCount * outputChannels.length * 2;
        */

        int outputSize = limit - position;
        if (buffer.capacity() < outputSize) {
            buffer = ByteBuffer.allocateDirect(outputSize).order(ByteOrder.nativeOrder());
        } else {
            buffer.clear();
        }

        if (!isActive()) {
            Log.d(TAG, "queueInputStereo() --> Exception: ");
            throw new IllegalStateException();
        }

        if (channelCount != 2) {
            // not stereo
            while (position < limit) {
                for (int channelIndex : outputChannels) {
                    buffer.putShort((short) (inputBuffer.getShort(position + 2 * channelIndex) * volume[channelIndex]));
                }
                position += channelCount * 2;
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

        inputBuffer.position(limit);
        buffer.flip();
        outputBuffer = buffer;
    }

    @Override
    public void queueEndOfStream() {
        inputEnded = true;
    }

    @Override
    public ByteBuffer getOutput() {
        ByteBuffer outputBuffer = this.outputBuffer;
        this.outputBuffer = EMPTY_BUFFER;
        return outputBuffer;
    }

    @SuppressWarnings("ReferenceEquality")
    @Override
    public boolean isEnded() {
        return inputEnded && outputBuffer == EMPTY_BUFFER;
    }

    @Override
    public void flush() {
        outputBuffer = EMPTY_BUFFER;
        inputEnded = false;
    }

    @Override
    public void reset() {
        flush();
        buffer = EMPTY_BUFFER;
        channelCount = Format.NO_VALUE;
        sampleRateHz = Format.NO_VALUE;
        outputChannels = null;
        active = false;
    }
}
