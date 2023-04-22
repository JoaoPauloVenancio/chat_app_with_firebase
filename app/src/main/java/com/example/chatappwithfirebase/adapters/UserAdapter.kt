package com.example.chatappwithfirebase.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.chatappwithfirebase.databinding.ItemContainerUserBinding
import com.example.chatappwithfirebase.models.User


class UserAdapter : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    private val userList  = arrayListOf<User>()

    private fun getUserImage(encodedImage: String): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    }

    inner class ViewHolder(private val binding: ItemContainerUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.textName.text = user.name
            binding.textEmail.text = user.email
            binding.imageProfile.load(user.image?.let { getUserImage(it) }) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemContainerUserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(userList[position])
    }

    override fun getItemCount() = userList.size

    fun updateList(newList: List<User>) {
        userList.clear()
        userList.addAll(newList)
        notifyDataSetChanged()
    }
}
