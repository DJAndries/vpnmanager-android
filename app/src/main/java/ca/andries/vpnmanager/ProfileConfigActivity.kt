package ca.andries.vpnmanager

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import ca.andries.vpnmanager.databinding.ActivityProfileConfigBinding
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProfileConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileConfigBinding
    private lateinit var providerMap: LinkedHashMap<Provider, String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val existingProfile: Profile? = if (intent.hasExtra(PROFILE_KEY)) {
            Json.decodeFromString(intent.getStringExtra(PROFILE_KEY)!!)
        } else {
            null
        }

        providerMap = linkedMapOf(
            Pair(Provider.WIREGUARD, getString(R.string.wireguard)),
            Pair(Provider.ICS_OPENVPN, getString(R.string.openvpn_ics)),
            Pair(Provider.OPENVPN_CONNECT_PROFILE, getString(R.string.openvpn_connect_profile)),
            Pair(Provider.OPENVPN_CONNECT_AS, getString(R.string.openvpn_connect_as))
        )

        initPrimaryInputs(existingProfile)
        initEnableDisableInputs(existingProfile)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_profile_config, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                if (validateInputs()) submitInputs()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun initPrimaryInputs(existingProfile: Profile?) {
        binding.profileNameInput.setText(existingProfile?.name ?: "")
        binding.tunnelInput.setText(existingProfile?.tunnelName ?: "")
        binding.vpnProvInput.setAdapter(ArrayAdapter(
            applicationContext,
            R.layout.list_provider_item,
            providerMap.values.toList()
        ))
        binding.vpnProvInput.setText(providerMap[existingProfile?.provider] ?: "", false)
    }

    private fun initEnableDisableInputs(existingProfile: Profile?) {
        binding.ssidExclListLayout.visibility = if (existingProfile?.wifiRule == RuleMode.ALL) View.VISIBLE else View.GONE
        binding.ssidInclListLayout.visibility = if (existingProfile?.wifiRule == RuleMode.SOME) View.VISIBLE else View.GONE

        when (existingProfile?.wifiRule) {
            RuleMode.ALL -> binding.wifiRadioAll.isChecked = true
            RuleMode.SOME -> binding.wifiRadioSome.isChecked = true
            RuleMode.NONE -> binding.wifiRadioNone.isChecked = true
        }

        binding.carrierExclListLayout.visibility = if (existingProfile?.mobileRule == RuleMode.ALL) View.VISIBLE else View.GONE
        binding.carrierInclListLayout.visibility = if (existingProfile?.mobileRule == RuleMode.SOME) View.VISIBLE else View.GONE
        when (existingProfile?.mobileRule) {
            RuleMode.ALL -> binding.mobileRadioAll.isChecked = true
            RuleMode.SOME -> binding.mobileRadioSome.isChecked = true
            RuleMode.NONE -> binding.mobileRadioNone.isChecked = true
        }

        binding.ssidExclListInput.setText(existingProfile?.ssidExclList?.joinToString("\n") ?: "")
        binding.ssidInclListInput.setText(existingProfile?.ssidInclList?.joinToString("\n") ?: "")
        binding.carrierExclListInput.setText(existingProfile?.carrierExclList?.joinToString("\n") ?: "")
        binding.carrierInclListInput.setText(existingProfile?.carrierInclList?.joinToString("\n") ?: "")

        binding.wifiRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.ssidExclListLayout.visibility = if (checkedId == R.id.wifiRadioAll) View.VISIBLE else View.GONE
            binding.ssidInclListLayout.visibility = if (checkedId == R.id.wifiRadioSome) View.VISIBLE else View.GONE
        }
        binding.mobileRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.carrierExclListLayout.visibility = if (checkedId == R.id.mobileRadioAll) View.VISIBLE else View.GONE
            binding.carrierInclListLayout.visibility = if (checkedId == R.id.mobileRadioSome) View.VISIBLE else View.GONE
        }
    }

    private fun validateInputs(): Boolean {
        val textInputs = listOf(
            Pair(binding.profileInputLayout, binding.profileNameInput),
            Pair(binding.vpnProvInputLayout, binding.vpnProvInput),
            Pair(binding.tunnelInputLayout, binding.tunnelInput)
        )
        for (textInput in textInputs) {
            textInput.first.error = null
            if (textInput.second.text?.isEmpty() == true) {
                textInput.first.error = getString(R.string.required_value)
                return false
            }
        }
        return true
    }

    private fun submitInputs() {

        val wifiRule = when (binding.wifiRadioGroup.checkedRadioButtonId) {
            R.id.wifiRadioAll -> RuleMode.ALL
            R.id.wifiRadioSome -> RuleMode.SOME
            else -> RuleMode.NONE
        }

        val mobileRule = when (binding.mobileRadioGroup.checkedRadioButtonId) {
            R.id.mobileRadioAll -> RuleMode.ALL
            R.id.mobileRadioSome -> RuleMode.SOME
            else -> RuleMode.NONE
        }

        val profile = Profile(
            binding.profileNameInput.text.toString(),
            providerMap.entries.find { it.value == binding.vpnProvInput.text.toString() }?.key!!,
            binding.tunnelInput.text.toString(),
            wifiRule,
            mobileRule,
            binding.matchingPriority.value.toInt(),
            LinkedHashSet(binding.ssidInclListInput.text?.split("\n")?.map { v -> v.trim() }
                ?.filter { v -> v.isNotEmpty() }),
            LinkedHashSet(binding.ssidExclListInput.text?.split("\n")?.map { v -> v.trim() }
                ?.filter { v -> v.isNotEmpty() }),
            LinkedHashSet(binding.carrierInclListInput.text?.split("\n")?.map { v -> v.trim() }
                ?.filter { v -> v.isNotEmpty() }),
            LinkedHashSet(binding.carrierExclListInput.text?.split("\n")?.map { v -> v.trim() }
                ?.filter { v -> v.isNotEmpty() })
        )

        val id = if (intent.hasExtra(PROFILE_ID_KEY)) intent.getIntExtra(PROFILE_ID_KEY, 0) else null

        val encodedProfile = Json.encodeToString(profile)
        Log.i(javaClass.name, "Saving profile: $encodedProfile")
        val intent = Intent()
        intent.putExtra(PROFILE_KEY, encodedProfile)
        if (id != null) intent.putExtra(PROFILE_ID_KEY, id)
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        const val PROFILE_KEY = "profile"
        const val PROFILE_ID_KEY = "pid"
    }
}