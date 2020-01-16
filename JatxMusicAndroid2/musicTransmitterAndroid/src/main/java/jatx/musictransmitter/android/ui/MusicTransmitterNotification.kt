package jatx.musictransmitter.android.ui

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import jatx.musictransmitter.android.R

const val CLICK_PLAY = "jatx.musictransmitter.android.CLICK_PLAY"
const val CLICK_PAUSE = "jatx.musictransmitter.android.CLICK_PAUSE"
const val CLICK_REV = "jatx.musictransmitter.android.CLICK_REV"
const val CLICK_FWD = "jatx.musictransmitter.android.CLICK_FWD"

object MusicTransmitterNotification {
    fun showNotification(context: Context, artist: String, title: String, isPlaying: Boolean) {
        if (Build.VERSION.SDK_INT < 16) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = Notification.Builder(context)
        val contentView = RemoteViews(context.packageName, R.layout.notification)

        contentView.setTextViewText(R.id.text_title, title)
        contentView.setTextViewText(R.id.text_artist, artist)

        contentView.setViewVisibility(R.id.pause, if (isPlaying) View.VISIBLE else View.GONE)
        contentView.setViewVisibility(R.id.play, if (isPlaying) View.GONE else View.VISIBLE)

        val playIntent = Intent(CLICK_PLAY)
        val pPlayIntent = PendingIntent.getBroadcast(context, 0, playIntent, 0)
        contentView.setOnClickPendingIntent(R.id.play, pPlayIntent)

        val pauseIntent = Intent(CLICK_PAUSE)
        val pPauseIntent = PendingIntent.getBroadcast(context, 0, pauseIntent, 0)
        contentView.setOnClickPendingIntent(R.id.pause, pPauseIntent)

        val revIntent = Intent(CLICK_REV)
        val pRevIntent = PendingIntent.getBroadcast(context, 0, revIntent, 0)
        contentView.setOnClickPendingIntent(R.id.rev, pRevIntent)

        val fwdIntent = Intent(CLICK_FWD)
        val pFwdIntent = PendingIntent.getBroadcast(context, 0, fwdIntent, 0)
        contentView.setOnClickPendingIntent(R.id.fwd, pFwdIntent)

        val mainActivityIntent = Intent(context, MusicTransmitterActivity::class.java)
        val contentIntent =
            PendingIntent.getActivity(context, 0, mainActivityIntent, 0)

        builder
            .setTicker("JatxMusicTransmitter")
            .setWhen(System.currentTimeMillis())
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(contentIntent)
            .setOngoing(true)

        val notification = builder.build()
        notification.bigContentView = contentView
        notification.priority = Notification.PRIORITY_MAX

        notificationManager.notify(1, notification)
    }

    fun hideNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
    }
}
