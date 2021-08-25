package ca.andries.vpnmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

object TunnelManager {

    fun toggleTunnel(context: Context, profiles: Map<Int, Profile>, profileToActivate: Map.Entry<Int, Profile>?) {
        val tunnelsShutdown = shutdownTunnels(context, profiles, profileToActivate?.key)
        if (tunnelsShutdown) Thread.sleep(1100)
        if (profileToActivate != null) {
            setTunnel(context, profileToActivate.value, true)
        }
    }

    fun shutdownTunnels(context: Context, profiles: Map<Int, Profile>, profileExceptionId: Int?): Boolean {
        var sentDown = false

        var sentIcsOpenVpn = false
        var sentConnectOpenVpn = false
        for (profile in profiles.entries) {
            if (profileExceptionId == profile.key) continue
            when (profile.value.provider) {
                Provider.OPENVPN_CONNECT_PROFILE, Provider.OPENVPN_CONNECT_AS -> {
                    if (sentConnectOpenVpn) continue
                    sentConnectOpenVpn = true
                }
                Provider.ICS_OPENVPN -> {
                    if (sentIcsOpenVpn) continue
                    sentIcsOpenVpn = true
                }
            }
            setTunnel(context, profile.value, false)
            sentDown = true
        }
        return sentDown
    }

    private fun setTunnel(context: Context, profile: Profile, isUp: Boolean) {
        when (profile.provider) {
            Provider.WIREGUARD -> setWireguardTunnel(context, profile, isUp)
            Provider.OPENVPN_CONNECT_PROFILE -> setOVPNTunnel(context, false, if (isUp) profile else null)
            Provider.OPENVPN_CONNECT_AS -> setOVPNTunnel(context, true, if (isUp) profile else null)
            Provider.ICS_OPENVPN -> setICSOVPNTunnel(context, if (isUp) profile else null)
        }
    }

    private fun setWireguardTunnel(context: Context, profile: Profile, isUp: Boolean) {
        Log.i(javaClass.name, "Toggle wireguard tunnel, name: ${profile.name} tunnel: ${profile?.tunnelName} desired status: $isUp")

        val intent = Intent("com.wireguard.android.action.SET_TUNNEL_DOWN")
        intent.putExtra("tunnel", profile.tunnelName)
        intent.`package` = "com.wireguard.android"
        context.sendBroadcast(intent)
    }

    private fun setOVPNTunnel(context: Context, isAccessServer: Boolean, profile: Profile?) {
        Log.i(javaClass.name, "Toggle OVPN Connect tunnel, name: ${profile?.name} tunnel: ${profile?.tunnelName}")

        val intent = Intent("net.openvpn.openvpn.CONNECT")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setClassName("net.openvpn.openvpn", "net.openvpn.unified.MainActivity")
        if (profile != null) {
            val profileType = if (isAccessServer) "AS" else "PC"
            intent.putExtra("net.openvpn.openvpn.AUTOSTART_PROFILE_NAME", "$profileType ${profile.tunnelName}")
            intent.putExtra("net.openvpn.openvpn.AUTOCONNECT", true)
            intent.putExtra("net.openvpn.openvpn.APP_SECTION", profileType)
        } else {
            intent.putExtra("net.openvpn.openvpn.STOP", true)
        }
        context.startActivity(intent)
    }

    private fun setICSOVPNTunnel(context: Context, profile: Profile?) {
        Log.i(javaClass.name, "Toggle ICS-OVPN tunnel, name: ${profile?.name} tunnel: ${profile?.tunnelName}")

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (profile != null) {
            intent.setClassName("de.blinkt.openvpn", "de.blinkt.openvpn.LaunchVPN")
            val bundle = Bundle()
            bundle.putString("de.blinkt.openvpn.shortcutProfileName", profile.tunnelName)
            intent.putExtras(bundle)
        } else {
            intent.setClassName("de.blinkt.openvpn", "de.blinkt.openvpn.api.DisconnectVPN")
        }
        context.startActivity(intent)
    }
}