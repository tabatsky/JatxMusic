package jatx.musiccommons.transmitter.threads;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class TransmitterPlayerWorker extends Thread {
    private final Socket s;

    private volatile long threadId = 0L;

    private volatile boolean finishWorkerFlag = false;

    public Runnable onWorkerStopped = () -> {};

    private OutputStream os;

    public TransmitterPlayerWorker(Socket s) {
        this.s = s;
    }

    public void setFinishWorkerFlag() {
        finishWorkerFlag = true;
    }

    public void writeData(byte[] data) {
        try {
            os.write(data);
            os.flush();
        } catch (IOException e) {
            final String msg = "(player " + threadId + ") write data error";
            System.out.println(msg);
        }
    }

    @Override
    public void run() {
        threadId = Thread.currentThread().getId();
        try {
            os = s.getOutputStream();
            final String msg = "(player " + threadId + ") socket connect";
            System.out.println(msg);
            try {
                while (!finishWorkerFlag) {
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                final String msg2 = "(player " + threadId + ") interrupted";
                System.out.println(msg2);
            }
        } catch (IOException e) {
            final String msg = "(player " + threadId + ") socket disconnect";
            System.out.println(msg);
        } finally {
            try {
                Thread.sleep(250);
                os.close();
                final String msg = "(player " + threadId + ") output stream closed";
                System.out.println(msg);
            } catch (InterruptedException | IOException | NullPointerException e) {
                e.printStackTrace();
            } finally {
                onWorkerStopped.run();
                final String msg = "(player " + threadId + ") finished";
                System.out.println(msg);
            }
        }
    }
}
