package ca.andries.vpnmanager

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class PermissionWrangler(private val finishCallback: () -> Unit) {
    private val permissionsToRequest = listOf(
        PermissionInfo(Manifest.permission.ACCESS_FINE_LOCATION, 27, R.string.fine_location_perm_prompt),
        PermissionInfo(Manifest.permission.ACCESS_BACKGROUND_LOCATION, 29, R.string.bg_location_perm_prompt, true),
        PermissionInfo("com.wireguard.android.permission.CONTROL_TUNNELS", 1, null)
    )
    private var permissionRequestIndex = 0

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
        val info = permissionsToRequest.elementAtOrNull(permissionRequestIndex++)
        if (info == null) {
            finishCallback()
            return
        }
        if (Build.VERSION.SDK_INT >= info.minVersion && (!info.promptIfLastPermGranted || lastPermGranted)) {
            if (ContextCompat.checkSelfPermission(ctx, info.permission) != PackageManager.PERMISSION_GRANTED) {
                if (info.educationStringRes != null) {
                    AlertDialog.Builder(ctx)
                        .setTitle(R.string.permission_request)
                        .setMessage(info.educationStringRes)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            requestLauncher.launch(info.permission)
                        }.show()
                } else {
                    requestLauncher.launch(info.permission)
                }
                return
            }
        }
        requestNextPermission(ctx, requestLauncher, true)
    }

    private class PermissionInfo(
        val permission: String,
        val minVersion: Int,
        val educationStringRes: Int?,
        val promptIfLastPermGranted: Boolean = false
    )
}