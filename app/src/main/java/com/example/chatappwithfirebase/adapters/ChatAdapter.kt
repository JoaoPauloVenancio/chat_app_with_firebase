package com.example.chatappwithfirebase.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.chatappwithfirebase.databinding.ItemContainerReceivedMessageBinding
import com.example.chatappwithfirebase.databinding.ItemContainerSentMessageBinding
import com.example.chatappwithfirebase.models.ChatMessage

const val VIEW_TYPE_SENT = 1
const val VIEW_TYPE_RECEIVED = 2

class ChatAdapter(
    var receiverProfileImage: Bitmap,
    var chatMessages: List<ChatMessage>,
    var senderId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class SentMessageViewHolder(private val binding: ItemContainerSentMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatMessage) {
            binding.textMessage.text = chat.message
            binding.textDateTime.text = chat.dateTime
        }
    }

    inner class ReceivedMessageViewHolder(private val binding: ItemContainerReceivedMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatMessage, receiverProfileImage: Bitmap) {
            binding.textMessage.text = chat.message
            binding.textDateTime.text = chat.dateTime
            binding.imageProfile.load(receiverProfileImage) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_SENT) {
            return SentMessageViewHolder(
                ItemContainerSentMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        } else {
            return ReceivedMessageViewHolder(
                ItemContainerReceivedMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            (holder as SentMessageViewHolder).bind(chatMessages[position])
        } else {
            (holder as ReceivedMessageViewHolder).bind(chatMessages[position], receiverProfileImage)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatMessages[position].senderId == senderId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }
}