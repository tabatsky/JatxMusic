package jatx.musictransmitter.android;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

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
    private static final String LOG_TAG_SERVICE = "transmitterMainService";

    private TransmitterController tc;
    private TransmitterPlayer tp;
    private TimeUpdater tu;

    public MusicTransmitterService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        prepareAndStart(intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        tu.interrupt();
        tp.interrupt();
        tc.setFinishFlag();
        super.onDestroy();
    }

    private void prepareAndStart(Intent intent) {
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
        Log.e(LOG_TAG_SERVICE, "setting wifi status: " + status);
        Intent intent = new Intent(UIController.SET_WIFI_STATUS);
        intent.putExtra("status", status);
        sendBroadcast(intent);
    }

    @Override
    public void setPosition(int position) {
        Intent intent = new Intent(UIController.SET_POSITION);
        intent.putExtra("position", position);
        sendBroadcast(intent);
    }

    @Override
    public void setCurrentTime(float currentMs, float trackLengthMs) {
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
}
