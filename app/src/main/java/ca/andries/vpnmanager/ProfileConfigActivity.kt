package ca.andries.vpnmanager

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
            listOf(getString(R.string.wireguard))
        ));
        binding.vpnProvInput.setText(getString(R.string.wireguard))

    }

    private fun initEnableDisableInputs(existingProfile: Profile?) {
        val wifiEnabled = existingProfile?.enableForWifi ?: false
        val mobileEnabled = existingProfile?.enableForMobile ?: false
        binding.ssidExclListLayout.visibility = if (wifiEnabled) View.VISIBLE else View.GONE
        binding.ssidInclListLayout.visibility = if (wifiEnabled) View.VISIBLE else View.GONE
        binding.carrierExclListLayout.visibility = if (mobileEnabled) View.VISIBLE else View.GONE
        binding.carrierInclListLayout.visibility = if (mobileEnabled) View.VISIBLE else View.GONE

        binding.ssidExclListInput.setText(existingProfile?.ssidExclList?.joinToString("\n") ?: "")
        binding.ssidInclListInput.setText(existingProfile?.ssidInclList?.joinToString("\n") ?: "")
        binding.carrierExclListInput.setText(existingProfile?.carrierExclList?.joinToString("\n") ?: "")
        binding.carrierInclListInput.setText(existingProfile?.carrierInclList?.joinToString("\n") ?: "")

        binding.enableWifiConditionToggle.isChecked = existingProfile?.enableForWifi ?: false
        binding.enableWifiConditionToggle.setOnCheckedChangeListener { _, isChecked ->
            binding.ssidExclListLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.ssidInclListLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        binding.enableMobileConditionToggle.isChecked = existingProfile?.enableForMobile ?: false
        binding.enableMobileConditionToggle.setOnCheckedChangeListener { _, isChecked ->
            binding.carrierExclListLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.carrierInclListLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun validateInputs(): Boolean {
        val textInputs = listOf(
            Pair(binding.profileInputLayout, binding.profileNameInput),
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
        val profile = Profile(
            binding.profileNameInput.text.toString(),
            binding.tunnelInput.text.toString(),
            binding.enableWifiConditionToggle.isChecked,
            binding.enableMobileConditionToggle.isChecked,
            binding.ssidInclListInput.text?.split("\n")?.map { v -> v.trim() }
                ?.filter { v -> v.isNotEmpty() } ?: listOf(),
            binding.ssidExclListInput.text?.split("\n")?.map { v -> v.trim() }
                ?.filter { v -> v.isNotEmpty() } ?: listOf(),
            binding.carrierInclListInput.text?.split("\n")?.map { v -> v.trim() }
                ?.filter { v -> v.isNotEmpty() } ?: listOf(),
            binding.carrierExclListInput.text?.split("\n")?.map { v -> v.trim() }
                ?.filter { v -> v.isNotEmpty() } ?: listOf()
        )

        val id = if (intent.hasExtra(PROFILE_ID_KEY)) intent.getIntExtra(PROFILE_ID_KEY, 0) else null

        val intent = Intent()
        intent.putExtra(PROFILE_KEY, Json.encodeToString(profile))
        if (id != null) intent.putExtra(PROFILE_ID_KEY, id)
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        val PROFILE_KEY = "profile"
        val PROFILE_ID_KEY = "pid"
    }
}