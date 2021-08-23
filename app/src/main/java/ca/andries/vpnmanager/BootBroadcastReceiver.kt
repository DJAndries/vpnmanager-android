package ca.andries.vpnmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val intent = Intent(context, MainService::class.java)
        MainService.startService(context!!)
    }
}