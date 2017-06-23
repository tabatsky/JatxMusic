package jatx.musictransmitter.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;

/**
 * Created by jatx on 21.06.17.
 */

public class MusicTransmitterNotification /*extends Notification*/ {
    public static final String CLICK_PLAY = "jatx.musictransmitter.android.clickPlay";
    public static final String CLICK_PAUSE = "jatx.musictransmitter.android.clickPause";
    public static final String CLICK_REV = "jatx.musictransmitter.android.clickRev";
    public static final String CLICK_FWD = "jatx.musictransmitter.android.clickFwd";

    public static void showNotification(Context context, String artist, String title, boolean isPlaying) {
        if (Build.VERSION.SDK_INT < 16) return;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(context);

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);

        contentView.setTextViewText(R.id.text_title, title);
        contentView.setTextViewText(R.id.text_artist, artist);
        contentView.setViewVisibility(R.id.pause, isPlaying?View.VISIBLE:View.GONE);
        contentView.setViewVisibility(R.id.play, isPlaying?View.GONE:View.VISIBLE);

        Intent playIntent = new Intent(CLICK_PLAY);
        PendingIntent pPlayIntent = PendingIntent.getBroadcast(context, 0, playIntent, 0);
        contentView.setOnClickPendingIntent(R.id.play, pPlayIntent);

        Intent pauseIntent = new Intent(CLICK_PAUSE);
        PendingIntent pPauseIntent = PendingIntent.getBroadcast(context, 0, pauseIntent, 0);
        contentView.setOnClickPendingIntent(R.id.pause, pPauseIntent);

        Intent revIntent = new Intent(CLICK_REV);
        PendingIntent pRevIntent = PendingIntent.getBroadcast(context, 0, revIntent, 0);
        contentView.setOnClickPendingIntent(R.id.rev, pRevIntent);

        Intent fwdIntent = new Intent(CLICK_FWD);
        PendingIntent pFwdIntent = PendingIntent.getBroadcast(context, 0, fwdIntent, 0);
        contentView.setOnClickPendingIntent(R.id.fwd, pFwdIntent);

        Intent mainActivityIntent = new Intent(context, MusicTransmitterActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, mainActivityIntent, 0);

        builder
                .setTicker("JatxMusicTransmitter")
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.icon_transmitter_96)
                //.setLargeIcon(
                 //       BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_transmitter_96))
                .setContentIntent(contentIntent)
                .setOngoing(true);

        Notification notification = builder.build();
        notification.bigContentView = contentView;
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                notification.priority = Notification.PRIORITY_MAX;

        notificationManager.notify(1, notification);
    }
}
