package ca.andries.vpnmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.*
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log

class NetworkChangeService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(javaClass.name, "Service created")
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel("test", "test123", NotificationManager.IMPORTANCE_DEFAULT)
            notifManager.createNotificationChannel(chan)
            startForeground(1,
                Notification.Builder(this, chan.id).setContentText("Running...")
                    .setSmallIcon(R.drawable.ic_launcher_foreground).build()
            )
        }
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val teleManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        manager.registerNetworkCallback(NetworkRequest.Builder().build(), object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                val wifiNet = wifiManager.connectionInfo.ssid
                val wifiNetBssid = wifiManager.connectionInfo.networkId
                Log.d(javaClass.name, "Avail! wifi: $wifiNet $wifiNetBssid")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val wifiNet = wifiManager.connectionInfo.ssid
                val teleNet = teleManager.simOperatorName
                Log.d(javaClass.name, "Avail2! wifi: $wifiNet $teleNet")
            }
        })
    }
}