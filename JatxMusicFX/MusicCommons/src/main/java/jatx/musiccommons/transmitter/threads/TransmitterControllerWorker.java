package jatx.musiccommons.transmitter.threads;

import jatx.musiccommons.transmitter.Globals;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TransmitterControllerWorker extends Thread {
    public static final byte COMMAND_EMPTY = (byte)255;
    public static final byte COMMAND_STOP = (byte)127;
    public static final byte COMMAND_PAUSE = (byte)126;
    public static final byte COMMAND_PLAY = (byte)125;

    private final Socket s;
    private final String host;

    private volatile long threadId = 0L;

    private volatile boolean finishWorkerFlag = false;
    volatile BlockingQueue<Byte> fifo;

    public Runnable onWorkerStopped = () -> {};

    private OutputStream os;

    public TransmitterControllerWorker(Socket s, String host) {
        this.s = s;
        this.host = host;

        fifo = new ArrayBlockingQueue<>(2048);
    }

    public void setFinishWorkerFlag() {
        finishWorkerFlag = true;
    }

    public void setVolume(int volume) {
        final String msg = "(controller " + threadId + ") set volume: " + volume;
        System.out.println(msg);
        if (volume >= 0 && volume <= 100) {
            fifo.offer((byte)volume);
        }
    }

    public void play() {
        final String msg = "(controller " + threadId + ") play";
        System.out.println(msg);
        fifo.offer(COMMAND_PLAY);
    }

    public void pause() {
        final String msg = "(controller " + threadId + ") pause";
        System.out.println(msg);
        fifo.offer(COMMAND_PAUSE);
    }

    public void sendStop() {
        final String msg = "(controller " + threadId + ") stop";
        System.out.println(msg);
        fifo.offer(COMMAND_STOP);
    }

    @Override
    public void run() {
        threadId = Thread.currentThread().getId();
        try {
            os = s.getOutputStream();
            final String msg = "(controller " + threadId + ") socket connect";
            System.out.println(msg);
            while (!finishWorkerFlag) {
                byte cmd;

                Byte objCmd = fifo.poll();

                if (objCmd!=null) {
                    cmd = objCmd;
                } else {
                    cmd = COMMAND_EMPTY;
                }

                byte[] data = new byte[]{cmd};

                os.write(data);
                os.flush();

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            byte[] data = new byte[]{COMMAND_STOP};

            os.write(data);
            os.flush();
        } catch (IOException e) {
            final String msg = "(controller " + threadId + ") socket disconnect";
            System.out.println(msg);
        } finally {
            Globals.tpck.getWorkerByHost(host).setFinishWorkerFlag();
            try {
                Thread.sleep(250);
                os.close();
                final String msg = "(controller " + threadId + ") output stream closed";
                System.out.println(msg);
            } catch (InterruptedException | IOException | NullPointerException e) {
                e.printStackTrace();
            } finally {
                onWorkerStopped.run();
                final String msg = "(controller " + threadId + ") finished";
                System.out.println(msg);
            }
        }
    }
}
