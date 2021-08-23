package ca.andries.vpnmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MainBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.getBooleanExtra(TOGGLE_FLAG, false) == true) {
            MainService.toggleByNotification(context!!)
        } else {
            MainService.startService(context!!)
        }
    }

    companion object {
        val TOGGLE_FLAG = "toggle_flag"
    }
}