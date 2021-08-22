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
            loadEnabledProfiles()
            val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (connManager.activeNetworkInfo?.isAvailable == true) performCheck()
        }
        return START_STICKY
    }

    private fun loadEnabledProfiles() {
        enabledProfiles = profileStore.getProfiles().entries
            .filter { it.value.enabled }.sortedBy { it.key }
            .reversed().map { Pair(it.key, it.value) }
    }

    private fun performCheck() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val teleManager = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val isWifiConnected = wifiManager.connectionInfo.ipAddress > 0
        val wifiName = wifiManager.connectionInfo.ssid.replace("(^\")|(\"$)".toRegex(), "")
        val carrierName = teleManager.simOperatorName

        var currentMatch: Pair<Int, Profile>? = null
        var currentMatchPriority = 0

        for (profile in enabledProfiles) {
            if (!profile.second.enabled || profile.second.priority <= currentMatchPriority) continue

            val matched = if (isWifiConnected) {
                (profile.second.wifiRule == RuleMode.ALL && !profile.second.ssidExclList.contains(wifiName)) ||
                    (profile.second.wifiRule == RuleMode.SOME && profile.second.ssidInclList.contains(wifiName))
            } else {
                ((profile.second.mobileRule == RuleMode.ALL && !profile.second.carrierExclList.contains(carrierName)) ||
                    (profile.second.mobileRule == RuleMode.SOME && profile.second.carrierInclList.contains(carrierName)))
            }

            if (matched) {
                currentMatch = profile
                currentMatchPriority = profile.second.priority
            }
        }

        Log.i(javaClass.name, "Profile check, match name = ${currentMatch?.second?.name} tunnel = ${currentMatch?.second?.tunnelName}")

        setWireguardTunnel(currentMatch?.second?.tunnelName)

        if (currentMatch != null) {
            currentMatch.second.lastConnectionDate = Date().time
            profileStore.storeProfile(this, currentMatch.second, currentMatch.first)
        }
    }

    private fun setWireguardTunnel(tunnelName: String?) {
        Log.i(javaClass.name, "Toggle wireguard tunnel, name: $tunnelName")
        val intent = if (tunnelName != null) {
            val intent = Intent("com.wireguard.android.action.SET_TUNNEL_UP")
            intent.putExtra("tunnel", tunnelName)
        } else {
            Intent("com.wireguard.android.action.SET_TUNNEL_DOWN")
        }
        intent.`package` = "com.wireguard.android"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        sendBroadcast(intent)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(javaClass.name, "Service created")

        profileStore = ProfileStore(this)
        loadEnabledProfiles()

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