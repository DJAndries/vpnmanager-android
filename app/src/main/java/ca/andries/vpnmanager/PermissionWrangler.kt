package ca.andries.vpnmanager

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class PermissionWrangler(private val finishCallback: () -> Unit) {
    private val settingsToRequest = listOf(
        SettingRequest(Manifest.permission.ACCESS_FINE_LOCATION, true, 27, R.string.fine_location_perm_prompt),
        SettingRequest(Manifest.permission.ACCESS_BACKGROUND_LOCATION, true, 29, R.string.bg_location_perm_prompt, true),
        SettingRequest("com.wireguard.android.permission.CONTROL_TUNNELS", true, 1, null),
        SettingRequest(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, false, 1, R.string.battery_optimization_prompt, true)
    )
    private var settingRequestIndex = 0

    fun startPermissionCheck(ctx: Context, requestLauncher: ActivityResultLauncher<String>) {
        val prefs = ctx.getSharedPreferences(ctx.getString(R.string.main_pref_key), MODE_PRIVATE)
        if (prefs.getBoolean(ctx.getString(R.string.perm_check_key), false)) {
            finishCallback()
            return
        }
        val editor = prefs.edit()
        editor.putBoolean(ctx.getString(R.string.perm_check_key), true)
        editor.commit()
        requestNextPermission(ctx, requestLauncher, false)
    }

    fun requestNextPermission(ctx: Context, requestLauncher: ActivityResultLauncher<String>, lastPermGranted: Boolean) {
        val info = settingsToRequest.elementAtOrNull(settingRequestIndex++)
        if (info == null) {
            finishCallback()
            return
        }
        if (Build.VERSION.SDK_INT >= info.minVersion && (!info.promptIfLastPermGranted || lastPermGranted)) {
            if (info.isPermission) {
                // check and request permission
                if (ContextCompat.checkSelfPermission(ctx, info.action) != PackageManager.PERMISSION_GRANTED) {
                    if (info.educationStringRes != null) {
                        AlertDialog.Builder(ctx)
                            .setTitle(R.string.permission_request)
                            .setMessage(info.educationStringRes)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                requestLauncher.launch(info.action)
                            }.show()
                    } else {
                        requestLauncher.launch(info.action)
                    }
                    return
                }
            } else {
                // start action
                if (info.educationStringRes != null) {
                    AlertDialog.Builder(ctx)
                        .setTitle(R.string.action_request)
                        .setMessage(info.educationStringRes)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            ctx.startActivity(Intent(info.action))
                        }.show()
                } else {
                    ctx.startActivity(Intent(info.action))
                }
            }
        }
        requestNextPermission(ctx, requestLauncher, true)
    }

    private class SettingRequest(
        val action: String,
        val isPermission: Boolean,
        val minVersion: Int,
        val educationStringRes: Int?,
        val promptIfLastPermGranted: Boolean = false
    )
}