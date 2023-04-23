package com.example.chatappwithfirebase.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.chatappwithfirebase.adapters.ChatAdapter
import com.example.chatappwithfirebase.databinding.ActivityChatBinding
import com.example.chatappwithfirebase.models.ChatMessage
import com.example.chatappwithfirebase.models.User
import com.example.chatappwithfirebase.utilities.*
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var receiverUser: User
    private var chatMessages = arrayListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var database: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        loadReceiverDetails()
        init()
        listenMessages()
    }

    private fun sendMessage() {
        val message = HashMap<String, Any>()
        preferenceManager.getString(KEY_USER_ID)?.let { message.put(KEY_SENDER_ID, it) }
        receiverUser.id?.let { message.put(KEY_RECEIVED_ID, it) }
        message[KEY_MESSAGE] = binding.inputMessage.text.toString()
        message[KEY_TIMESTAMP] = Date()
        database.collection(KEY_COLLECTION_CHAT)
            .add(message)
        binding.inputMessage.text = null
    }

    private fun init() {
        preferenceManager = PreferenceManager(applicationContext)
        chatAdapter = ChatAdapter(
            chatMessages = chatMessages,
            receiverProfileImage = getBitmapFromEncodedString(receiverUser.image!!),
            senderId = preferenceManager.getString(KEY_USER_ID)!!
        )
        binding.chatRecyclerView.adapter = chatAdapter
        database = FirebaseFirestore.getInstance()
    }

    private fun getBitmapFromEncodedString(encodedImage: String): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun loadReceiverDetails() {
        receiverUser = intent.getSerializableExtra(KEY_USER) as User
        binding.textName.text = receiverUser.name
    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.layoutSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun getReadableDateTime(date: Date): String {
        return SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date)
    }

    private fun listenMessages() {
        database.collection(KEY_COLLECTION_CHAT)
            .whereEqualTo(KEY_SENDER_ID, preferenceManager.getString(KEY_USER_ID))
            .whereEqualTo(KEY_RECEIVED_ID, receiverUser.id)
            .addSnapshotListener(eventListener)
        database.collection(KEY_COLLECTION_CHAT)
            .whereEqualTo(KEY_SENDER_ID, receiverUser.id)
            .whereEqualTo(KEY_RECEIVED_ID, preferenceManager.getString(KEY_USER_ID))
            .addSnapshotListener(eventListener)
    }


    private val eventListener = EventListener<QuerySnapshot> { value, error ->
        if (error != null) {
            return@EventListener
        }
        if (value != null) {
            val count = chatMessages.size
            for (documentChange: DocumentChange in value.documentChanges) {
                if (documentChange.type == DocumentChange.Type.ADDED) {
                    val chatMessage = ChatMessage(
                        senderId = documentChange.document.getString(KEY_SENDER_ID)!!,
                        receiverId = documentChange.document.getString(KEY_RECEIVED_ID)!!,
                        message = documentChange.document.getString(KEY_MESSAGE)!!,
                        dateTime = getReadableDateTime(documentChange.document.getDate(KEY_TIMESTAMP)!!),
                        dateObject = documentChange.document.getDate(KEY_TIMESTAMP)!!
                    )
                    chatMessages.add(chatMessage)
                }
            }
            chatMessages.sortWith { obj1, obj2 -> obj1.dateObject.compareTo(obj2.dateObject) }
            if (count == 0) {
                chatAdapter.notifyDataSetChanged()
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size, chatMessages.size)
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)
            }
            binding.chatRecyclerView.isVisible = true
        }
        binding.progressBarChat.isVisible = false
    }


}