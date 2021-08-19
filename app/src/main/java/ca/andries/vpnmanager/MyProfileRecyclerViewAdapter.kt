package ca.andries.vpnmanager

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView

import ca.andries.vpnmanager.databinding.FragmentProfileItemBinding
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class MyProfileRecyclerViewAdapter(
    private var values: List<Pair<Int, Profile>>,
    private val listener: (EventType, Pair<Int, Profile>) -> Unit
) : RecyclerView.Adapter<MyProfileRecyclerViewAdapter.ViewHolder>() {

    val dateFormat: DateFormat = SimpleDateFormat.getDateTimeInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            FragmentProfileItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        val ctx = holder.itemView.context
        val lastConnVal = if (item.second.lastConnectionDate != null) {
            dateFormat.format(Date(item.second.lastConnectionDate!!))
        } else {
            ctx.getString(R.string.never_connected)
        }
        holder.lastTime.text = ctx.getString(R.string.last_connection_time, lastConnVal)
        holder.name.text = item.second.name
        holder.enabledLabel.text = ctx.getString(if (item.second.enabled) R.string.enabled else R.string.disabled)
        holder.provider.text = ctx.getString(R.string.wireguard_with_tunnel, item.second.tunnelName)
        holder.toggleBtn.text = ctx.getString(if (item.second.enabled) R.string.disable else R.string.enable)
        holder.coverImg.alpha = if (item.second.enabled) 1f else .5f
        holder.toggleBtn.setOnClickListener {
            listener(EventType.TOGGLE, item)
        }
        holder.editBtn.setOnClickListener {
            listener(EventType.EDIT, item)
        }
        holder.deleteBtn.setOnClickListener {
            listener(EventType.DELETE, item)
        }
    }

    fun setValues(values: List<Pair<Int, Profile>>) {
        this.values = values
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentProfileItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val lastTime: TextView = binding.lastTime
        val name: TextView = binding.name
        val enabledLabel: TextView = binding.enabled
        val provider: TextView = binding.provider
        val toggleBtn: Button = binding.toggleBtn
        val editBtn: Button = binding.editBtn
        val deleteBtn: ImageButton = binding.deleteBtn
        val coverImg: ImageView = binding.coverImg

        override fun toString(): String {
            return super.toString() + " '" + name.text + "'"
        }
    }

    enum class EventType {
        EDIT,
        DELETE,
        TOGGLE
    }

}