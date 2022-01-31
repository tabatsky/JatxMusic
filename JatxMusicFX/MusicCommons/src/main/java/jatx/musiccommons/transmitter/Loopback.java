package jatx.musiccommons.transmitter;

import jatx.musiccommons.util.Frame;
import xt.audio.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class Loopback {
    public static final int FRAME_RATE = 44100;

    private static volatile XtPlatform platform = null;
    private static volatile XtService service = null;
    private static volatile XtDevice device = null;
    private static volatile XtStream stream = null;
    private static volatile ArrayBlockingQueue<FrameData> frameDataQueue = new ArrayBlockingQueue<>(200);
    private static List<String> deviceIdList = new ArrayList<>();

    public static int getDeviceCount() {
        return deviceIdList.size();
    }

    public synchronized static void start(int loopbackIndex) throws LoopbackStartException {
        try {
            String deviceId = deviceIdList.get(loopbackIndex);
            device = service.openDevice(deviceId);
            Structs.XtMix mix = new Structs.XtMix(FRAME_RATE, Enums.XtSample.INT16);
            Structs.XtChannels channels = new Structs.XtChannels(2, 0, 0, 0);
            Structs.XtFormat format = new Structs.XtFormat(mix, channels);
            Structs.XtBufferSize bufferSize = device.getBufferSize(format);
            Structs.XtStreamParams streamParams = new Structs.XtStreamParams(true, Loopback::onBuffer, null, null);
            Structs.XtDeviceStreamParams deviceParams = new Structs.XtDeviceStreamParams(streamParams, format, bufferSize.current);

            stream = device.openStream(deviceParams, null);
            XtSafeBuffer safeBuffer = XtSafeBuffer.register(stream, true);
            stream.start();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new LoopbackStartException(t);
        }
    }

    public synchronized static void stop() {
        if (stream != null && stream.isRunning()) stream.stop();
        if (stream != null) stream.close();
        if (device != null) device.close();
    }

    public static void initLoopbackList() {
        try {
            deviceIdList.clear();
            if (platform == null) platform = XtAudio.init(null, null);
            if (service == null) service = platform.getService(Enums.XtSystem.WASAPI);
            XtDeviceList list = service.openDeviceList(EnumSet.of(Enums.XtEnumFlags.INPUT));
            for (int i=0; i<list.getCount(); i++) {
                String deviceId = list.getId(i);
                String deviceName = list.getName(deviceId);
                System.out.println(deviceId + ": " + deviceName);
                EnumSet<Enums.XtDeviceCaps> caps = list.getCapabilities(deviceId);
                if (caps.contains(Enums.XtDeviceCaps.LOOPBACK)) {
                    deviceIdList.add(deviceId);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static int onBuffer(XtStream stream, Structs.XtBuffer buffer, Object user) throws Exception {
        XtSafeBuffer safe = XtSafeBuffer.get(stream);
        if (safe == null) return 0;
        safe.lock(buffer);
        short[] audio = (short[])safe.getInput();
        processAudio(audio, buffer.frames);
        safe.unlock(buffer);
        return 0;
    }

    private static void processAudio(short[] audio, int frames) throws Exception {
        int numBytesRead = frames * 2 * 2;
        FrameData frameData = new FrameData(numBytesRead);

        for(int frame = 0; frame < frames; frame++) {
            for(int channel = 0; channel < 2; channel++) {
                int sampleIndex = frame * 2 + channel;
                short shrt = audio[sampleIndex];
                int byteIndex0 = sampleIndex * 2;
                int byteIndex1 = sampleIndex * 2 + 1;
                frameData.data[byteIndex0] = (byte)(shrt & 0xff);
                frameData.data[byteIndex1] = (byte)((shrt >> 8 ) & 0xff);
        }
        }

        frameDataQueue.offer(frameData);
        System.out.println("numBytesRead: " + numBytesRead);
    }

    public static Frame readFrame(int position) throws LoopbackReadException {
        try {
            FrameData frameData = frameDataQueue.poll();
            return fromLoopbackRawData(frameData.data, frameData.numBytesRead, position);
        } catch (Throwable e) {
            throw new LoopbackReadException(e);
        }
    }

    private static Frame fromLoopbackRawData(byte[] rawData, int dataSize, int position) {
        Frame f = new Frame();

        f.position = position;

        f.freq = Loopback.FRAME_RATE;
        f.channels = 2;

        f.data = Arrays.copyOf(rawData, dataSize);
        f.size = dataSize;

        return f;
    }

    public static class FrameData {
        byte[] data;
        int numBytesRead;

        public FrameData(int numBytesRead) {
            this.numBytesRead = numBytesRead;
            data = new byte[numBytesRead];
        }
    }

    public static class LoopbackStartException extends Exception {
        public LoopbackStartException(Throwable e) {
            super(e);
        }
    }

    public static class LoopbackReadException extends Exception {
        public LoopbackReadException(Throwable e) {
            super(e);
        }
    }
}
