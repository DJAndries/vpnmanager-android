package ca.andries.vpnmanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.*

class ProfileViewModel(app: Application): AndroidViewModel(app) {
    private val store: ProfileStore = ProfileStore(app.applicationContext)
    private val profiles: MutableLiveData<List<Pair<Int, Profile>>> = MutableLiveData<List<Pair<Int, Profile>>>()

    init {
        createList()
    }

    fun load() {
        store.load(getApplication<Application>().applicationContext)
        createList()
    }

    fun saveProfile(profile: Profile, existingId: Int?) {
        store.storeProfile(getApplication<Application>().applicationContext, profile, existingId)
        createList()
    }

    fun deleteProfile(id: Int) {
        store.deleteProfile(getApplication<Application>().applicationContext, id)
        createList()
    }

    fun toggleProfile(id: Int) {
        store.toggleProfile(getApplication<Application>().applicationContext, id)
        createList()
    }

    private fun createList() {
        profiles.value = store
            .getProfiles()
            .entries
            .map { v -> Pair(v.key, v.value) }
            .sortedBy { it.first }
            .sortedBy { it.second.enabled }
            .reversed()
    }

    fun getProfiles(): LiveData<List<Pair<Int, Profile>>> {
        return profiles
    }
}