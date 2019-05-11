package jatx.musicreceiver.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import jatx.musicreceiver.commons.AutoConnectThread;
import jatx.musicreceiver.commons.ReceiverController;
import jatx.musicreceiver.commons.ReceiverPlayer;
import jatx.musicreceiver.interfaces.ServiceController;
import jatx.musicreceiver.interfaces.UIController;

public class MusicReceiverService extends Service implements ServiceController {
    public static final String STOP_SERVIVE = "jatx.musicreceiver.android.stopService";
    public static final String STATUS_REQUEST = "jatx.musicreceiver.android.statusRequest";
    public static final String STATUS_RESPONSE = "jatx.musicreceiver.android.statusResponse";

    private static final String LOG_TAG_SERVICE = "receiverMainService";

    public static boolean isInstanceRunning = false;

    private boolean isRunning;
    private String host;

    private volatile ReceiverPlayer rp;
    private volatile ReceiverController rc;
    private AutoConnectThread act;

    private volatile WifiManager mWifiManager;
    private volatile WifiManager.WifiLock mLock;

    public MusicReceiverService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isInstanceRunning) {
            stopSelf();
            return START_STICKY_COMPATIBILITY;
        }

        isInstanceRunning = true;

        final Intent actIntent = new Intent();
        actIntent.setClass(this, MusicReceiverActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, actIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final String channelId;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel("music receiver service", "Music receiver service");
        } else {
            channelId = "";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setContentTitle("JatxMusicReceiver");
        builder.setContentText("Foreground service is running");
        builder.setContentIntent(pendingIntent);
        final Notification notification = builder.build();
        startForeground(1523, notification);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mLock = mWifiManager.createWifiLock("music-receiver-wifi-lock");
        mLock.setReferenceCounted(false);
        mLock.acquire();

        host = intent.getStringExtra("host");

        BroadcastReceiver stopSelfReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopSelf();
            }
        };
        registerReceiver(stopSelfReceiver, new IntentFilter(STOP_SERVIVE));

        BroadcastReceiver serviceStartJobReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startJob();
            }
        };
        registerReceiver(serviceStartJobReceiver, new IntentFilter(ServiceController.START_JOB));

        BroadcastReceiver serviceStopJobReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopJob();
            }
        };
        registerReceiver(serviceStopJobReceiver, new IntentFilter(ServiceController.STOP_JOB));

        BroadcastReceiver statusRequestReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent in = new Intent(STATUS_RESPONSE);
                in.putExtra("isRunning", isRunning);
                in.putExtra("host", host);
                sendBroadcast(in);
            }
        };
        registerReceiver(statusRequestReceiver, new IntentFilter(STATUS_REQUEST));

        act = new AutoConnectThread(this);
        act.start();

        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy() {
        try {
            mLock.release();

            act.interrupt();

            stopJob();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        stopForeground(true);

        isInstanceRunning = false;

        super.onDestroy();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName) {
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
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
        try {
            throw new RuntimeException();
        } catch (Throwable e) {
            e.printStackTrace();
        }
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
