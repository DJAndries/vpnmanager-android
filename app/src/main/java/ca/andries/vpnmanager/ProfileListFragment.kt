package ca.andries.vpnmanager

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import ca.andries.vpnmanager.databinding.FragmentProfileListBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A fragment representing a list of Items.
 */
class ProfileListFragment : Fragment() {

    private val model: ProfileViewModel by activityViewModels()

    val profileConfigLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            model.saveProfile(Json.decodeFromString(result.data?.getStringExtra(
                ProfileConfigActivity.PROFILE_KEY
            )!!), result.data?.getIntExtra(ProfileConfigActivity.PROFILE_ID_KEY, 0))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentProfileListBinding.inflate(layoutInflater, container, false)

        val profiles = model.getProfiles().value ?: listOf()
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = MyProfileRecyclerViewAdapter(profiles) { type, item ->
            when (type) {
                MyProfileRecyclerViewAdapter.EventType.EDIT -> editProfile(item.first, item.second)
                MyProfileRecyclerViewAdapter.EventType.TOGGLE -> toggleProfile(item.first)
                MyProfileRecyclerViewAdapter.EventType.DELETE -> deleteProfile(item.first)
            }
        }
        binding.emptyView.visibility = if (profiles.isEmpty()) View.VISIBLE else View.GONE
        binding.list.visibility = if (profiles.isEmpty()) View.INVISIBLE else View.VISIBLE
        model.getProfiles().observe(viewLifecycleOwner, Observer<List<Pair<Int, Profile>>> { list ->
            (binding.list.adapter as MyProfileRecyclerViewAdapter).setValues(list)
            binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.list.visibility = if (list.isEmpty()) View.INVISIBLE else View.VISIBLE
        })

        return binding.root
    }

    private fun editProfile(id: Int, profile: Profile) {
        val intent = Intent(context, ProfileConfigActivity::class.java)
        intent.putExtra(ProfileConfigActivity.PROFILE_KEY, Json.encodeToString(profile))
        intent.putExtra(ProfileConfigActivity.PROFILE_ID_KEY, id)
        profileConfigLauncher.launch(intent)
    }

    private fun toggleProfile(id: Int) {
        model.toggleProfile(id)
    }

    private fun deleteProfile(id: Int) {
        val builder = activity?.let {
            AlertDialog.Builder(it)
        }
        builder?.setTitle(R.string.delete)
            ?.setMessage(R.string.delete_confirm)
            ?.setPositiveButton(R.string.confirm) { _, _ ->
                model.deleteProfile(id)
            }
            ?.setNegativeButton(R.string.cancel) { _, _ -> }
            ?.show()
    }
}