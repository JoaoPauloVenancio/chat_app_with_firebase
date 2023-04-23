package com.example.chatappwithfirebase.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import coil.load
import coil.transform.CircleCropTransformation
import com.example.chatappwithfirebase.adapters.RecentConversationsAdapter
import com.example.chatappwithfirebase.databinding.ActivityMainBinding
import com.example.chatappwithfirebase.models.ChatMessage
import com.example.chatappwithfirebase.models.User
import com.example.chatappwithfirebase.utilities.*
import com.google.firebase.firestore.*
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Collections

class MainActivity : BaseActivity(), RecentConversationsAdapter.RecentConversationListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var conversations: ArrayList<ChatMessage>
    private lateinit var conversationAdapter : RecentConversationsAdapter
    private lateinit var database: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)
        init()
        loadUserDetails()
        getToken()
        setListeners()
        listenConversations()
    }

    private fun init() {
        conversations = arrayListOf()
        conversationAdapter = RecentConversationsAdapter(conversations, this)
        binding.conversationsRecyclerView.adapter = conversationAdapter
        database = FirebaseFirestore.getInstance()
    }

    private fun setListeners() {
        binding.imageSignOut.setOnClickListener {
            signOut()
        }
        binding.fabNewChat.setOnClickListener {
            val intent = Intent(applicationContext, UsersActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserDetails() {
        binding.textName.text = preferenceManager.getString(KEY_NAME)
        val bytes = Base64.decode(preferenceManager.getString(KEY_IMAGE), Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.imageProfile.load(bitmap) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun listenConversations() {
        database.collection(KEY_COLLECTION_CONVERSATION)
            .whereEqualTo(KEY_SENDER_ID, preferenceManager.getString(KEY_USER_ID))
            .addSnapshotListener(eventListener)
        database.collection(KEY_COLLECTION_CONVERSATION)
            .whereEqualTo(KEY_RECEIVED_ID, preferenceManager.getString(KEY_USER_ID))
            .addSnapshotListener(eventListener)
    }

    private val eventListener = EventListener<QuerySnapshot> { value, error ->
        if (error != null) {
            return@EventListener
        }
        if (value != null) {
            for (documentChange: DocumentChange in value.documentChanges) {
                if (documentChange.type == DocumentChange.Type.ADDED) {
                    val senderId = documentChange.document.getString(KEY_SENDER_ID)
                    val receiverId = documentChange.document.getString(KEY_RECEIVED_ID)
                    val chatMessage = ChatMessage(
                        senderId = senderId,
                        receiverId = receiverId
                    )
                    if (preferenceManager.getString(KEY_USER_ID).equals(senderId)) {
                        chatMessage.conversionImage =
                            documentChange.document.getString(KEY_RECEIVER_IMAGE)
                        chatMessage.conversionName =
                            documentChange.document.getString(KEY_RECEIVER_NAME)
                        chatMessage.conversionId =
                            documentChange.document.getString(KEY_RECEIVED_ID)
                    } else {
                        chatMessage.conversionImage =
                            documentChange.document.getString(KEY_SENDER_IMAGE)
                        chatMessage.conversionName =
                            documentChange.document.getString(KEY_SENDER_NAME)
                        chatMessage.conversionId = documentChange.document.getString(KEY_SENDER_ID)
                    }
                    chatMessage.message = documentChange.document.getString(KEY_LAST_MESSAGE)
                    chatMessage.dateObject = documentChange.document.getDate(KEY_TIMESTAMP)
                    conversations.add(chatMessage)
                } else if (documentChange.type == DocumentChange.Type.MODIFIED) {
                    for (i in 0 until conversations.size) {
                        val senderId = documentChange.document.getString(KEY_SENDER_ID)
                        val receiverId = documentChange.document.getString(KEY_RECEIVED_ID)
                        if (conversations[i].senderId.equals(senderId) && conversations[i].receiverId.equals(receiverId)) {
                            conversations[i].message = documentChange.document.getString(KEY_LAST_MESSAGE)
                            conversations[i].dateObject = documentChange.document.getDate(KEY_TIMESTAMP)
                            break
                        }
                    }
                }
            }
            conversations.sortWith { obj1, obj2 -> obj2.dateObject?.compareTo(obj1.dateObject) ?: 0 } // se algo der errado pode ser por essa funcao
            conversationAdapter.notifyDataSetChanged()
            binding.conversationsRecyclerView.smoothScrollToPosition(0)
            binding.conversationsRecyclerView.isVisible = true
            binding.progressBarConversation.isVisible = false
        }
    }

    private fun updateToken(token: String) {
        val database = FirebaseFirestore.getInstance()
        val documentReference = preferenceManager.getString(KEY_USER_ID)?.let {
            database.collection(KEY_COLLECTIONS_USERS).document(
                it
            )
        }
        documentReference?.update(KEY_FCM_TOKEN, token)
            ?.addOnFailureListener {
                showToast("Unable to update token")
            }
    }

    private fun getToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener(this::updateToken)
    }

    private fun signOut() {
        showToast("Signing out...")
        val database = FirebaseFirestore.getInstance()
        val documentReference = preferenceManager.getString(KEY_USER_ID)?.let {
            database.collection(KEY_COLLECTIONS_USERS).document(
                it
            )
        }
        val updates = HashMap<String, Any>()
        updates[KEY_FCM_TOKEN] = FieldValue.delete()
        documentReference?.update(updates)
            ?.addOnSuccessListener {
                preferenceManager.clear()
                val intent = Intent(applicationContext, SignInActivity::class.java)
                startActivity(intent)
                finish()
            }
            ?.addOnFailureListener {
                showToast("Unable to sign out")
            }

    }

    override fun onRecentConversionClicked(user: User) {
        val intent = Intent(applicationContext, ChatActivity::class.java)
        intent.putExtra(KEY_USER, user)
        startActivity(intent)
    }
}