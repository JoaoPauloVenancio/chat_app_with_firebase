package com.example.chatappwithfirebase.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatappwithfirebase.databinding.ItemContainerRecentConversionBinding
import com.example.chatappwithfirebase.models.ChatMessage
import com.example.chatappwithfirebase.models.User

class RecentConversationsAdapter(
    private val messageList: ArrayList<ChatMessage>,
    private val listener: RecentConversationListener
) : RecyclerView.Adapter<RecentConversationsAdapter.ViewHolder>() {

    private val userList: ArrayList<ChatMessage> = messageList

    interface RecentConversationListener {
        fun onRecentConversionClicked(user: User)
    }

    private fun getConversionImage(encodedImage: String): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    inner class ViewHolder(private val binding: ItemContainerRecentConversionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chatMessage: ChatMessage) {
            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage!!))
            binding.textName.text = chatMessage.conversionName
            binding.textRecentMessage.text = chatMessage.message
            binding.root.setOnClickListener {
                val user = User(
                    id = chatMessage.conversionId,
                    name = chatMessage.conversionName,
                    image = chatMessage.conversionImage
                )
                listener.onRecentConversionClicked(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemContainerRecentConversionBinding.inflate(
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


}