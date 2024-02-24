package jatx.musiccommons.transmitter.threads;

import jatx.musiccommons.frame.Frame;
import jatx.musiccommons.receiver.DesktopSoundOut;
import jatx.musiccommons.receiver.SoundOut;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

public class LocalPlayer extends TransmitterPlayerDataAcceptor {

    private final SoundOut mSoundOut = new DesktopSoundOut();

    private volatile int volume = 100;

    private final ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(20);

    void play() {
        mSoundOut.play();
    }

    void pause() {
        mSoundOut.pause();
    }

    void setVolume(int volume) {
        this.volume = volume;
        mSoundOut.setVolume(volume);
    }

    @Override
    void writeData(byte[] data) {
        try {
            queue.put(data);
        } catch (InterruptedException e) {
            System.out.println("(local player) queue interrupted");
        }
    }

    @Override
    public void run() {
        System.out.println("(local player) starting");
        int frameRate = 44100;
        int channels = 2;
        int position = 0;
        restartPlayer(frameRate, channels);

        try {
            while (true) {
                final byte[] data = queue.poll();
                if (data == null) {
                    Thread.sleep(5);
                } else {
                    final Frame f = Frame.fromRawData(data);
                    if (frameRate != f.freq || channels != f.channels || position != f.position) {
                        frameRate = f.freq;
                        channels = f.channels;
                        position = f.position;
                        restartPlayer(frameRate, channels);
                    }
                    mSoundOut.write(f.data, 0, f.size);
                }
            }
        } catch (InterruptedException e) {
            System.out.println("(local player) thread interrupted");
            mSoundOut.destroy();
        } catch (IOException e) {
            System.out.println("(local player) IO exception");
            mSoundOut.destroy();
        }
    }

    private void restartPlayer(int frameRate, int channels) {
        mSoundOut.renew(frameRate, channels);
        mSoundOut.setVolume(volume);
        System.out.println("(player) player restarted");
        System.out.println("(player) frame rate: $frameRate");
        System.out.println("(player) channels: $channels");
    }
}
