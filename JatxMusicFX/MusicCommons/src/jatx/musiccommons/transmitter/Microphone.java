package jatx.musiccommons.transmitter;

import jatx.musiccommons.util.Frame;

import javax.sound.sampled.*;

/**
 * Created by jatx on 15.09.17.
 */
public class Microphone {
    public static final AudioFormat format =
            new AudioFormat(48000.0f, 16, 2, true, false);
    private static volatile TargetDataLine microphone;
    private static final int CHUNK_SIZE = 10240;
    private static volatile byte[] data = new byte[CHUNK_SIZE];

    public static void start() throws LineUnavailableException {
        microphone = AudioSystem.getTargetDataLine(format);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);

        microphone.start();
    }

    public static void stop() {
        microphone.stop();
    }

    public static Frame readFrame(int position) throws MicrophoneReadException {
        try {
            int numBytesRead = microphone.read(data, 0, CHUNK_SIZE);
            return Frame.fromMicRawData(data, numBytesRead, position);
        } catch (Throwable e) {
            throw new MicrophoneReadException(e);
        }
    }

    public static class MicrophoneReadException extends Exception {
        public MicrophoneReadException(Throwable e) {
            super(e);
        }
    }
}
