package jatx.musicreceiver.android;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import jatx.musicreceiver.commons.AutoConnectThread;
import jatx.musicreceiver.commons.ReceiverController;
import jatx.musicreceiver.commons.ReceiverPlayer;
import jatx.musicreceiver.interfaces.ServiceController;
import jatx.musicreceiver.interfaces.UIController;

public class MusicReceiverService extends Service implements ServiceController {
    public static final String STOP_SERVIVE = "jatx.musicreceiver.android.stopService";
    private static final String LOG_TAG_SERVICE = "receiverMainService";

    private boolean isRunning;
    private String host;

    private volatile ReceiverPlayer rp;
    private volatile ReceiverController rc;
    private AutoConnectThread act;

    public MusicReceiverService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        host = intent.getStringExtra("host");

        BroadcastReceiver stopSelfReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopSelf();
            }
        };
        IntentFilter stopSelfFilter = new IntentFilter(STOP_SERVIVE);
        registerReceiver(stopSelfReceiver, stopSelfFilter);

        BroadcastReceiver serviceStartJobReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startJob();
            }
        };
        IntentFilter serviceStartJobFilter = new IntentFilter(ServiceController.START_JOB);
        registerReceiver(serviceStartJobReceiver, serviceStartJobFilter);

        BroadcastReceiver serviceStopJobReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopJob();
            }
        };
        IntentFilter serviceStopJobFilter = new IntentFilter(ServiceController.STOP_JOB);
        registerReceiver(serviceStopJobReceiver, serviceStopJobFilter);

        act = new AutoConnectThread(this);
        act.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (act!=null) {
            act.interrupt();
        }
        stopJob();
        super.onDestroy();
    }

    @Override
    public void startJob() {
        if (isRunning) return;
        isRunning = true;
        Log.i(LOG_TAG_SERVICE, "start job");
        rp = new ReceiverPlayer(host, this, new AndroidSoundOut());
        rc = new ReceiverController(host, this);
        rp.start();
        rc.start();
        Intent intent = new Intent(UIController.START_JOB);
        sendBroadcast(intent);
    }

    @Override
    public void stopJob() {
        if (!isRunning) return;
        isRunning = false;
        rp.setFinishFlag();
        rc.setFinishFlag();
        Intent intent = new Intent(UIController.STOP_JOB);
        sendBroadcast(intent);
    }

    @Override
    public void play() {
        rp.play();
    }

    @Override
    public void pause() {
        rp.pause();
    }

    @Override
    public void setVolume(int vol) {
        rp.setVolume(vol);
    }
}
