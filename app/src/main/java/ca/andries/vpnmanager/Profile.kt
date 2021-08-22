package ca.andries.vpnmanager

import kotlinx.serialization.Serializable
import kotlin.collections.LinkedHashSet

@Serializable
class Profile(
    val name: String,
    val tunnelName: String,
    val wifiRule: RuleMode,
    val mobileRule: RuleMode,
    val priority: Int,
    val ssidInclList: LinkedHashSet<String>,
    val ssidExclList: LinkedHashSet<String>,
    val carrierInclList: LinkedHashSet<String>,
    val carrierExclList: LinkedHashSet<String>,

    var enabled: Boolean = true,
    var lastConnectionDate: Long? = null
)