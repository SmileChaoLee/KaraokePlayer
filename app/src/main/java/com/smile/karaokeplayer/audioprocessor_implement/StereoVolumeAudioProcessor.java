package com.smile.karaokeplayer.audioprocessor_implement;

import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.C;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class StereoVolumeAudioProcessor implements AudioProcessor {
    private int channelCount;
    private int sampleRateHz;
    private int[] pendingOutputChannels;

    private boolean active;
    private int[] outputChannels;
    private ByteBuffer buffer;
    private ByteBuffer outputBuffer;
    private boolean inputEnded;

    private float[] volume;

    private static final int LEFT_SPEAKER = 0;
    private static final int RIGHT_SPEAKER = 1;

    public StereoVolumeAudioProcessor() {
        buffer = EMPTY_BUFFER;
        outputBuffer = EMPTY_BUFFER;
        channelCount = Format.NO_VALUE;
        sampleRateHz = Format.NO_VALUE;
    }

    public void setChannelMap(int[] outputChannels) {
        pendingOutputChannels = outputChannels;
    }

    @Override
    public boolean configure(int sampleRateHz, int channelCount, @C.Encoding int encoding)
            throws UnhandledFormatException {
        if (volume == null) {
            throw new IllegalStateException("volume has not been set! Call setVolume(float left,float right)");
        }

        boolean outputChannelsChanged = !Arrays.equals(pendingOutputChannels, outputChannels);
        outputChannels = pendingOutputChannels;
        if (outputChannels == null) {
            active = false;
            return outputChannelsChanged;
        }
        if (encoding != C.ENCODING_PCM_16BIT) {
            throw new UnhandledFormatException(sampleRateHz, channelCount, encoding);
        }
        if (!outputChannelsChanged && this.sampleRateHz == sampleRateHz
                && this.channelCount == channelCount) {
            return false;
        }
        this.sampleRateHz = sampleRateHz;
        this.channelCount = channelCount;

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
        int size = limit - position;

        if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        } else {
            buffer.clear();
        }

        if (isActive()) {
            int ch = 0;
            for (int i = position; i < limit; i += 2) {
                short sample = (short) (inputBuffer.getShort(i) * volume[ch++]);
                buffer.putShort(sample);
                ch %= channelCount;
            }
        } else {
            throw new IllegalStateException();
        }

        inputBuffer.position(limit);
        buffer.flip();
        outputBuffer = buffer;
    }

    @Override
    public void queueEndOfStream() {
        inputEnded = true;
    }

    /**
     * Sets the volume of right and left channels/speakers
     * The values are between 0.0 and 1.0
     *
     * @param left
     * @param right
     */
    public void setVolume(float left, float right) {
        volume = new float[]{left, right};
    }

    public float getLeftVolume() {
        return volume[LEFT_SPEAKER];
    }

    public float getRightVolume() {
        return volume[RIGHT_SPEAKER];
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
