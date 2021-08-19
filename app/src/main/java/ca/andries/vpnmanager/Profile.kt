package ca.andries.vpnmanager

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class Profile(
    val name: String,
    val tunnelName: String,
    val enableForWifi: Boolean,
    val enableForMobile: Boolean,
    val disableForWifi: Boolean,
    val disableForMobile: Boolean,
    val enableSsidList: List<String>,
    val enableCarrierList: List<String>,
    val disableSsidList: List<String>,
    val disableCarrierList: List<String>,

    var enabled: Boolean = true,
    var lastConnectionDate: Long? = null
)