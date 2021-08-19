package ca.andries.vpnmanager

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class ProfileStore(context: Context) {
    private var profiles: MutableMap<Int, Profile> = mutableMapOf()
    var nextPid: Int = 1
        private set

    init {
        load(context)
    }

    fun load(context: Context) {
        val profilePrefs = context.getSharedPreferences(context.getString(R.string.main_pref_key), Context.MODE_PRIVATE)
        val serializedProfiles = profilePrefs.getString(context.getString(R.string.profiles_pref_key), "{}")
        profiles = Json.decodeFromString(serializedProfiles!!)
        nextPid = profilePrefs.getInt(context.getString(R.string.profiles_next_id_key), 1)
    }

    private fun save(context: Context) {
        val profilePrefs = context.getSharedPreferences(context.getString(R.string.main_pref_key), Context.MODE_PRIVATE)
        val editor = profilePrefs.edit()
        editor.putString(context.getString(R.string.profiles_pref_key), Json.encodeToString(profiles))
        editor.putInt(context.getString(R.string.profiles_next_id_key), nextPid)
        editor.commit()
    }

    fun storeProfile(context: Context, profile: Profile, existingId: Int?) {
        val id = existingId ?: nextPid++
        val existingProfile = profiles[existingId]
        profile.lastConnectionDate = existingProfile?.lastConnectionDate
        profile.enabled = existingProfile?.enabled ?: true
        profiles[id] = profile
        save(context)
    }

    fun updateConnectionDate(context: Context, id: Int) {
        profiles[id]?.lastConnectionDate = Date().time
        save(context)
    }

    fun toggleProfile(context: Context, id: Int) {
        profiles[id]?.enabled = !(profiles[id]?.enabled ?: false)
        save(context)
    }

    fun deleteProfile(context: Context, id: Int) {
        profiles.remove(id)
        save(context)
    }

    fun getProfiles(): Map<Int, Profile> {
        return profiles
    }
}