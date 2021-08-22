package ca.andries.vpnmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.*
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import java.util.*

class MainService : Service() {
    private lateinit var profileStore: ProfileStore
    private lateinit var enabledProfiles: List<Pair<Int, Profile>>

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra(RELOAD_FLAG, false) == true) {
            Log.i(javaClass.name, "Reload intent received")
            profileStore.load(this)
            val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (connManager.activeNetworkInfo?.isAvailable == true) performCheck()
        }
        return START_STICKY
    }

    private fun performCheck() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val teleManager = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val isWifiConnected = wifiManager.connectionInfo.ipAddress > 0
        val wifiName = wifiManager.connectionInfo.ssid.replace("(^\")|(\"$)".toRegex(), "")
        val carrierName = teleManager.simOperatorName

        var currentMatch: Map.Entry<Int, Profile>? = null
        var currentMatchPriority = 0

        for (profileEntry in profileStore.getProfiles().entries) {
            val profile = profileEntry.value
            if (!profile.enabled || profile.priority <= currentMatchPriority) continue

            val matched = if (isWifiConnected) {
                (profile.wifiRule == RuleMode.ALL && !profile.ssidExclList.contains(wifiName)) ||
                    (profile.wifiRule == RuleMode.SOME && profile.ssidInclList.contains(wifiName))
            } else {
                ((profile.mobileRule == RuleMode.ALL && !profile.carrierExclList.contains(carrierName)) ||
                    (profile.mobileRule == RuleMode.SOME && profile.carrierInclList.contains(carrierName)))
            }

            if (matched) {
                currentMatch = profileEntry
                currentMatchPriority = profile.priority
            }
        }

        Log.i(javaClass.name, "Profile check, match name = ${currentMatch?.value?.name} tunnel = ${currentMatch?.value?.tunnelName}")

        setWireguardTunnel(currentMatch?.value?.tunnelName)

        if (currentMatch != null) {
            currentMatch.value.lastConnectionDate = Date().time
            profileStore.storeProfile(this, currentMatch.value, currentMatch.key)
        }
    }

    private fun setWireguardTunnel(tunnelName: String?) {
        Log.i(javaClass.name, "Toggle wireguard tunnel, name: $tunnelName")
        val intents = if (tunnelName != null) {
            val intent = Intent("com.wireguard.android.action.SET_TUNNEL_UP")
            intent.putExtra("tunnel", tunnelName)
            listOf(intent)
        } else {
            profileStore.getProfiles().values.map {
                val intent = Intent("com.wireguard.android.action.SET_TUNNEL_DOWN")
                intent.putExtra("tunnel", it.tunnelName)
            }
        }

        for (intent in intents) {
            intent.`package` = "com.wireguard.android"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_RECEIVER_FOREGROUND
            sendBroadcast(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(javaClass.name, "Service created")

        profileStore = ProfileStore(this)

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
                val wifiNetBssid = wifiManager.connectionInfo.ipAddress
                Log.d(javaClass.name, "Avail! wifi: $wifiNet $wifiNetBssid")
                performCheck()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                setWireguardTunnel(null)
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

    companion object {
        val RELOAD_FLAG = "reload_flag"

        fun reloadFromActivity(context: Context) {
            val intent = Intent(context, MainService::class.java)
            intent.putExtra(RELOAD_FLAG, true)
            context.startService(intent)
        }
    }
}