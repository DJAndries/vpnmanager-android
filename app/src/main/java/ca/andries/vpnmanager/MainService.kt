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

        setWireguardTunnel(currentMatch?.key)

        if (currentMatch != null) {
            currentMatch.value.lastConnectionDate = Date().time
            profileStore.storeProfile(this, currentMatch.value, currentMatch.key)
        }
    }

    private fun setWireguardTunnel(profileId: Int?) {
        val profiles = profileStore.getProfiles()
        Log.i(javaClass.name, "Toggle wireguard tunnel, name: $profileId tunnel: ${profiles[profileId]?.tunnelName}")

        for (profileEntry in profileStore.getProfiles().entries) {
            val intent = Intent(
                if (profileId == profileEntry.key) {
                    "com.wireguard.android.action.SET_TUNNEL_UP"
                } else {
                    "com.wireguard.android.action.SET_TUNNEL_DOWN"
                }
            )
            intent.putExtra("tunnel", profileEntry.value.tunnelName)
            intent.`package` = "com.wireguard.android"
            sendBroadcast(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(javaClass.name, "Service created")

        profileStore = ProfileStore(this)

        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                SERV_STATUS_NOTIF_CHANNEL_ID,
                getString(R.string.serv_status_notif_channel_desc),
                NotificationManager.IMPORTANCE_NONE
            )
            notifManager.createNotificationChannel(chan)
            startForeground(1,
                Notification.Builder(this, chan.id)
                    .setContentText(getString(R.string.serv_status_notif_text))
                    .setSmallIcon(R.drawable.ic_shield)
                    .setStyle(Notification.BigTextStyle().bigText(getString(R.string.serv_status_notif_text)))
                    .build()
            )
        }

        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val teleManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        connManager.registerNetworkCallback(NetworkRequest.Builder().build(), object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                val wifiNet = wifiManager.connectionInfo.ssid
                val operatorName = teleManager.simOperatorName
                Log.i(javaClass.name, "Network available! wifi: $wifiNet SIM operator: $operatorName")
                performCheck()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.i(javaClass.name, "Network lost!")
                setWireguardTunnel(null)
            }
        })
    }

    companion object {
        val RELOAD_FLAG = "reload_flag"
        private val SERV_STATUS_NOTIF_CHANNEL_ID = "serv_status"

        fun reloadFromActivity(context: Context) {
            val intent = Intent(context, MainService::class.java)
            intent.putExtra(RELOAD_FLAG, true)
            context.startService(intent)
        }

        fun startService(context: Context) {
            val intent = Intent(context, MainService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}