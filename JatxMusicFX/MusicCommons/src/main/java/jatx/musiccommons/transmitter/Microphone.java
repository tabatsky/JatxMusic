package jatx.musiccommons.transmitter;

import jatx.musiccommons.util.Frame;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jatx on 15.09.17.
 */
public class Microphone {
    public static final AudioFormat format =
            new AudioFormat(48000.0f, 16, 2, true, false);
    private static volatile TargetDataLine microphone;
    private static List<Line> lineList = new ArrayList<>();
    private static final int CHUNK_SIZE = 10240;
    private static volatile byte[] data = new byte[CHUNK_SIZE];

    public static void start() throws LineUnavailableException {
        microphone = AudioSystem.getTargetDataLine(format);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);

        microphone.start();
    }

    public static int getLineCount() {
        return lineList.size();
    }

    public static void start(int micIndex) throws LineUnavailableException {
        microphone = (TargetDataLine) lineList.get(micIndex);

        float[] sampleRates = {48000f, 44100f, 32000f};
        boolean success = false;
        for (float sampleRate: sampleRates) {
            AudioFormat fmt =
                    new AudioFormat(sampleRate, 16, 2, true, false);
            try {
                System.err.println("trying " + sampleRate);
                microphone.open(fmt);
                success = true;
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }

            if (success) break;
        }

        if (success) {
            System.err.println("success");
            microphone.start();
        } else {
            throw new LineUnavailableException();
        }
    }

    public static void initMicList() throws LineUnavailableException {
        lineList.clear();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info: mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(info);
            Line.Info[] lineInfos = mixer.getTargetLineInfo();
            if (lineInfos.length >= 1 && lineInfos[0].getLineClass().equals(TargetDataLine.class)) {
                for (Line.Info lineInfo: lineInfos) {
                    Line line = mixer.getLine(lineInfo);
                    lineList.add(line);
                }
            }
        }
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
