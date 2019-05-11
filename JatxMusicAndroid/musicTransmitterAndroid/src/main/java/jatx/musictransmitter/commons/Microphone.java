package jatx.musictransmitter.commons;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import jatx.debug.Debug;
import jatx.musiccommons.mp3.Frame;

/**
 * Created by jatx on 23.09.17.
 */

public class Microphone {
    private static int SAMPLE_RATE; // Hz
    private static int ENCODING;
    private static int CHANNEL_MASK;

    private static int BUFFER_SIZE;

    private static volatile byte[] data;
    private static volatile AudioRecord audioRecord;

    public static void start() throws MicrophoneInitException {
        audioRecord = findAudioRecord();
        if (audioRecord==null) {
            throw new MicrophoneInitException();
        }
        audioRecord.startRecording();
    }

    public static void stop() {
        //audioRecord.stop();
        audioRecord.release();
    }

    public static Frame readFrame(int position) throws MicrophoneReadException {
        try {
            int numBytesRead = audioRecord.read(data, 0, BUFFER_SIZE);
            return Frame.fromMicRawData(data, numBytesRead, position, SAMPLE_RATE);
        } catch (Throwable e) {
            throw new MicrophoneReadException(e);
        }
    }

    public static class MicrophoneReadException extends Exception {
        public MicrophoneReadException(Throwable e) {
            super(e);
        }
    }

    public static class MicrophoneInitException extends Exception {}

    private static AudioRecord findAudioRecord() {
        final int[] sampleRates = new int[] { 48000, 44100, 32000, 22050, 16000, 11025, 8000 };
        for (int rate : sampleRates) {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_16BIT/*, AudioFormat.ENCODING_PCM_8BIT*/ }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_STEREO/*, AudioFormat.CHANNEL_IN_MONO*/ }) {
                    try {
                        Log.e("Trying mic config", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        BUFFER_SIZE = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (BUFFER_SIZE != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            data = new byte[BUFFER_SIZE];
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, BUFFER_SIZE);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                Log.e("Attempt", "Success");
                                SAMPLE_RATE = rate;
                                ENCODING = audioFormat;
                                CHANNEL_MASK = channelConfig;
                                return recorder;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Attempt fault", rate + "Exception, keep trying.", e);
                    }
                }
            }
        }
        return null;
    }

}
