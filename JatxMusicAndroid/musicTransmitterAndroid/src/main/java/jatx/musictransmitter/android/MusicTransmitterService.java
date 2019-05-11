package jatx.musictransmitter.android;

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
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jatx.musictransmitter.commons.Globals;
import jatx.musictransmitter.commons.JLayerMp3Decoder;
import jatx.musictransmitter.commons.Mp3Decoder;
import jatx.musictransmitter.commons.TimeUpdater;
import jatx.musictransmitter.commons.TransmitterController;
import jatx.musictransmitter.commons.TransmitterPlayer;
import jatx.musictransmitter.interfaces.ServiceInterface;
import jatx.musictransmitter.interfaces.UIController;

public class MusicTransmitterService extends Service implements UIController {
    public static final String STOP_SERVIVE = "jatx.musictransmitter.android.stopService";
    public static final String STATUS_REQUEST = "jatx.musictransmitter.android.statusRequest";

    private static final String LOG_TAG_SERVICE = "transmitterMainService";

    public static boolean isInstanceRunning = false;

    private TransmitterController tc;
    private TransmitterPlayer tp;
    private TimeUpdater tu;

    private volatile WifiManager mWifiManager;
    private volatile WifiManager.WifiLock mLock;

    private volatile float currentMs;
    private volatile float trackLengthMs;
    private volatile boolean status;
    private volatile int position;

    public MusicTransmitterService() {
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
        actIntent.setClass(this, MusicTransmitterActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, actIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final String channelId;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel("music transmitter service", "Music transmitter service");
        } else {
            channelId = "";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setContentTitle("JatxMusicReceiver");
        builder.setContentText("Foreground service is running");
        builder.setContentIntent(pendingIntent);

        final Notification notification = builder.build();
        startForeground(2315, notification);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mLock = mWifiManager.createWifiLock("music-transmitter-wifi-lock");
        mLock.setReferenceCounted(false);
        mLock.acquire();

        prepareAndStart(intent);
        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy() {
        try {
            mLock.release();

            tu.interrupt();
            tp.interrupt();
            tc.setFinishFlag();
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

    private void prepareAndStart(Intent intent) {
        BroadcastReceiver statusRequestReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setPosition(position);
                setWifiStatus(status);
                setCurrentTime(currentMs, trackLengthMs);
            }
        };
        registerReceiver(statusRequestReceiver, new IntentFilter(STATUS_REQUEST));

        BroadcastReceiver stopSelfReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopSelf();
            }
        };
        IntentFilter stopSelfFilter = new IntentFilter(STOP_SERVIVE);
        registerReceiver(stopSelfReceiver, stopSelfFilter);

        BroadcastReceiver tpSetPositionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int position = intent.getIntExtra("position", 0);
                tp.setPosition(position);
            }
        };
        IntentFilter tpSetPositionFilter = new IntentFilter(ServiceInterface.TP_SET_POSITION);
        registerReceiver(tpSetPositionReceiver, tpSetPositionFilter);

        BroadcastReceiver tpPlayReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tp.play();
            }
        };
        IntentFilter tpPlayFilter = new IntentFilter(ServiceInterface.TP_PLAY);
        registerReceiver(tpPlayReceiver, tpPlayFilter);

        BroadcastReceiver tpPauseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tp.pause();
            }
        };
        IntentFilter tpPauseFilter = new IntentFilter(ServiceInterface.TC_PAUSE);
        registerReceiver(tpPauseReceiver, tpPauseFilter);

        BroadcastReceiver tpSeekReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                double progress = intent.getDoubleExtra("progress", 0.0);
                tp.seek(progress);
            }
        };
        IntentFilter tpSeekFilter = new IntentFilter(ServiceInterface.TP_SEEK);
        registerReceiver(tpSeekReceiver, tpSeekFilter);

        BroadcastReceiver tpSetFileListReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String[] filePathArray = intent.getStringArrayExtra("filePathArray");
                List<File> fileList = new ArrayList<File>();
                for (String path: filePathArray) {
                    fileList.add(new File(path));
                }
                tp.setFileList(fileList);
            }
        };
        IntentFilter tpSetFileListFilter = new IntentFilter(ServiceInterface.TP_SET_FILE_LIST);
        registerReceiver(tpSetFileListReceiver, tpSetFileListFilter);

        BroadcastReceiver tcPlayReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tc.play();
            }
        };
        IntentFilter tcPlayFilter = new IntentFilter(ServiceInterface.TC_PLAY);
        registerReceiver(tcPlayReceiver, tcPlayFilter);

        BroadcastReceiver tcPauseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tc.pause();
            }
        };
        IntentFilter tcPauseFilter = new IntentFilter(ServiceInterface.TC_PAUSE);
        registerReceiver(tcPauseReceiver, tcPauseFilter);

        BroadcastReceiver tcSetVolumeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int volume = intent.getIntExtra("volume", 0);
                tc.setVolume(volume);
            }
        };
        IntentFilter tcSetVolumeFilter = new IntentFilter(ServiceInterface.TC_SET_VOLUME);
        registerReceiver(tcSetVolumeReceiver, tcSetVolumeFilter);

        Mp3Decoder decoder = new JLayerMp3Decoder();

        String[] filePathArray = intent.getStringArrayExtra("filePathArray");
        List<File> fileList = new ArrayList<File>();
        for (String path: filePathArray) {
            fileList.add(new File(path));
        }

        tu = new TimeUpdater(this, decoder);
        tc = new TransmitterController(this);
        tp = new TransmitterPlayer(fileList, this, decoder);
        tc.setTransmitterPlayer(tp);
        tp.setTransmitterController(tc);

        tu.start();
        tp.start();
        tc.start();

        //mVolLabel.setText(Globals.volume.toString()+"%");
        tc.setVolume(Globals.volume);

        //refreshList();
    }

    @Override
    public void setWifiStatus(boolean status) {
        this.status = status;

        Log.e(LOG_TAG_SERVICE, "setting wifi status: " + status);
        Intent intent = new Intent(UIController.SET_WIFI_STATUS);
        intent.putExtra("status", status);
        sendBroadcast(intent);
    }

    @Override
    public void setPosition(int position) {
        this.position = position;

        Intent intent = new Intent(UIController.SET_POSITION);
        intent.putExtra("position", position);
        sendBroadcast(intent);
    }

    @Override
    public void setCurrentTime(float currentMs, float trackLengthMs) {
        this.currentMs = currentMs;
        this.trackLengthMs = trackLengthMs;

        Intent intent = new Intent(UIController.SET_CURRENT_TIME);
        intent.putExtra("currentMs", currentMs);
        intent.putExtra("trackLengthMs", trackLengthMs);
        sendBroadcast(intent);
    }

    @Override
    public void forcePause() {
        Intent intent = new Intent(UIController.SET_WIFI_STATUS);
        sendBroadcast(intent);
    }

    @Override
    public void errorMsg(final String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void nextTrack() {
        Intent intent = new Intent(UIController.NEXT_TRACK);
        sendBroadcast(intent);
    }
}
