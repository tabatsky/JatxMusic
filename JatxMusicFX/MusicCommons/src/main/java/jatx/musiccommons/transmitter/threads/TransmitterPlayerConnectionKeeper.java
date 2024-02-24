package jatx.musiccommons.transmitter.threads;

import jatx.musiccommons.transmitter.Globals;
import jatx.musiccommons.transmitter.UI;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentHashMap;

public class TransmitterPlayerConnectionKeeper extends TransmitterPlayerDataAcceptor {
    public static final int CONNECT_PORT_PLAYER = 7171;

    volatile WeakReference<UI> uiRef;

    private volatile ConcurrentHashMap<String, TransmitterPlayerWorker> workers =
            new ConcurrentHashMap<>();

    private ServerSocket ss;

    public TransmitterPlayerConnectionKeeper(UI ui) {
        uiRef = new WeakReference(ui);
    }

    public TransmitterPlayerWorker getWorkerByHost(String host) {
        return workers.get(host);
    }

    @Override
    public void writeData(byte[] data) {
        workers.forEachValue(0L, transmitterPlayerWorker -> transmitterPlayerWorker.writeData(data));
    }

    @Override
    public void run() {
        System.out.println("(transmitter player connection keeper) starting");
        try {
            while (true) {
                Thread.sleep(100);
                ss = new ServerSocket(CONNECT_PORT_PLAYER);
                System.out.println("(player) new server socket");
                try {
                    ss.setSoTimeout(Globals.SO_TIMEOUT);
                    Socket s = ss.accept();
                    System.out.println("(player) server socket accept");
                    String host = s.getInetAddress().getHostAddress();
                    TransmitterPlayerWorker worker = new TransmitterPlayerWorker(s);
                    worker.start();
                    System.out.println("(player) worker $host started");
                    workers.put(host, worker);
                    final String msg = "(player) total workers: " + workers.size();
                    System.out.println(msg);
                    worker.onWorkerStopped = () -> {
                        System.out.println("(player) worker $host stopped");
                        workers.remove(host);
                        final String msg2 = "(player) total workers: " + workers.size();
                        System.out.println(msg2);
                        UI ui = uiRef.get();
                        if (ui != null) {
                            ui.updateWifiStatus(workers.size());
                        }
                    };
                } catch (SocketTimeoutException e) {
                    System.out.println("(player) socket timeout");
                } finally {
                    try {
                        ss.close();
                        System.out.println("(player) server socket closed");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    UI ui = uiRef.get();
                    if (ui != null) {
                        ui.updateWifiStatus(workers.size());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("(player) thread interrupted");
        } finally {
            workers.forEachValue(0L, transmitterPlayerWorker -> transmitterPlayerWorker.setFinishWorkerFlag());
            System.out.println("(player) workers interrupted");
            try {
                ss.close();
                System.out.println("(player) server socket closed");
            } catch (IOException | NullPointerException e2) {
                e2.printStackTrace();
            }
            System.out.println("(player) thread finished");
        }
    }
}
