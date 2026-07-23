package com.smartlibrary.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.materialswitch.MaterialSwitch
import com.smartlibrary.R
import com.smartlibrary.network.ImageUrls
import com.smartlibrary.network.dto.UserResponse
import com.smartlibrary.ui.UiHelpers

/**
 * Users & Roles row. The switch reflects ADMIN membership; toggling calls [onToggle] with
 * the desired new role ("ADMIN" or "USER"). The current admin's own row is not toggleable.
 */
class UserAdapter(
    private var items: List<UserResponse>,
    private val currentUserId: Long,
    private val onToggle: (UserResponse, String) -> Unit,
) : RecyclerView.Adapter<UserAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
        val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvEmail: TextView = view.findViewById(R.id.tvEmail)
        val tvRole: TextView = view.findViewById(R.id.tvRole)
        val switchAdmin: MaterialSwitch = view.findViewById(R.id.switchAdmin)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = items[position]
        if (!u.imageUrl.isNullOrBlank()) {
            holder.tvAvatar.visibility = View.GONE
            holder.ivAvatar.visibility = View.VISIBLE
            holder.ivAvatar.load(ImageUrls.resolve(u.imageUrl)) { transformations(CircleCropTransformation()) }
        } else {
            holder.ivAvatar.visibility = View.GONE
            holder.tvAvatar.visibility = View.VISIBLE
            holder.tvAvatar.text = initials(u.fullName)
        }
        holder.tvName.text = u.fullName
        holder.tvEmail.text = u.email
        UiHelpers.bindRoleBadge(holder.tvRole, u.role)

        val isAdmin = u.role.equals("ADMIN", true)
        // Set state without firing the listener.
        holder.switchAdmin.setOnCheckedChangeListener(null)
        holder.switchAdmin.isChecked = isAdmin
        holder.switchAdmin.isEnabled = u.id != currentUserId
        holder.switchAdmin.setOnCheckedChangeListener { _, checked ->
            onToggle(u, if (checked) "ADMIN" else "USER")
        }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<UserResponse>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun initials(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts.first().first()}${parts.last().first()}".uppercase()
        }
    }
}
