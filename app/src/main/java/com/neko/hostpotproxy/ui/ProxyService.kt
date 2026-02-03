package com.neko.hostpotproxy.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.neko.hostpotproxy.core.ProxyServer
import io.nekohasekai.sagernet.R 

class ProxyService : Service() {

    companion object {
        const val NOTIF_ID = 101
        const val CHANNEL_ID = "proxy_service_channel"
    }

    private val binder = object : IProxyControl.Stub() {
        override fun getPort(): Int = ProxyServer.getInstance().getPort()
        override fun isRunning(): Boolean = ProxyServer.getInstance().isRunning
        override fun start(): Boolean {
            val started = ProxyServer.getInstance().start()
            if (started) updateNotification(true)
            return started
        }
        override fun stop(): Boolean {
            val stopped = ProxyServer.getInstance().stop()
            stopForeground(true)
            stopSelf()
            return stopped
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification(ProxyServer.getInstance().isRunning))
        return START_STICKY
    }

    override fun onDestroy() {
        ProxyServer.getInstance().stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isRunning: Boolean): Notification {
        val notificationIntent = Intent(this, ProxySettings::class.java)
        
        val flags = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags)
        val statusText = if (isRunning) 
            getString(R.string.status_running_port, ProxyServer.getInstance().getPort()) 
        else 
            getString(R.string.service_active)

        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)

        return builder
            .setContentTitle(getString(R.string.hotspot_proxy_title))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_baseline_vpn_key_24)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(isRunning: Boolean) {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIF_ID, buildNotification(isRunning))
    }
}
