package ca.andries.vpnmanager

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class Profile(
    val name: String,
    val tunnelName: String,
    val enableForWifi: Boolean,
    val enableForMobile: Boolean,
    val ssidInclList: List<String>,
    val ssidExclList: List<String>,
    val carrierInclList: List<String>,
    val carrierExclList: List<String>,

    var enabled: Boolean = true,
    var lastConnectionDate: Long? = null
)