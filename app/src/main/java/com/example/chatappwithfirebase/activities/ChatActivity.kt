package com.example.chatappwithfirebase.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.core.view.isVisible
import com.example.chatappwithfirebase.adapters.ChatAdapter
import com.example.chatappwithfirebase.databinding.ActivityChatBinding
import com.example.chatappwithfirebase.models.ChatMessage
import com.example.chatappwithfirebase.models.User
import com.example.chatappwithfirebase.network.ApiClient
import com.example.chatappwithfirebase.network.ApiService
import com.example.chatappwithfirebase.utilities.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var receiverUser: User
    private var chatMessages = arrayListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var database: FirebaseFirestore
    private var conversionId: String? = null
    private var isReceiverAvailable = false

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
        if (conversionId != null) {
            updateConversion(binding.inputMessage.text.toString())
        } else {
            val conversion = HashMap<String, Any>()
            conversion[KEY_SENDER_ID] = preferenceManager.getString(KEY_USER_ID)!!
            conversion[KEY_SENDER_NAME] = preferenceManager.getString(KEY_NAME)!!
            conversion[KEY_SENDER_IMAGE] = preferenceManager.getString(KEY_IMAGE)!!
            conversion[KEY_RECEIVED_ID] = receiverUser.id!!
            conversion[KEY_RECEIVER_NAME] = receiverUser.name!!
            conversion[KEY_RECEIVER_IMAGE] = receiverUser.image!!
            conversion[KEY_LAST_MESSAGE] = binding.inputMessage.text.toString()
            conversion[KEY_TIMESTAMP] = Date()
            addConversion(conversion)
        }
        if (!isReceiverAvailable) {
            try {
                val tokens = JSONArray()
                tokens.put(receiverUser.token)

                val data = JSONObject()
                data.put(KEY_USER_ID, preferenceManager.getString(KEY_USER_ID))
                data.put(KEY_NAME, preferenceManager.getString(KEY_NAME))
                data.put(KEY_FCM_TOKEN, preferenceManager.getString(KEY_FCM_TOKEN))
                data.put(KEY_MESSAGE, binding.inputMessage.text.toString())

                val body = JSONObject()
                body.put(REMOTE_MSG_DATA, data)
                body.put(REMOTE_MSG_REGISTRATION_IDS, tokens)

                sendNotification(body.toString())
            } catch (e: Exception) {
                showToast(e.message.toString())
            }
        }
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

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun sendNotification(messageBody: String) {
        ApiClient.getClient()?.create(ApiService::class.java)?.sendMessage(
            getRemoteHeaders(),
            messageBody
        )?.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    try {
                        if (response.body() != null) {
                            val responseJson = JSONObject(response.body())
                            val results = responseJson.getJSONArray("results")
                            if (responseJson.getInt("failure") == 1) {
                                val error = results.get(0) as JSONObject
                                showToast(error.getString("error"))
                                return
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    showToast("Notification send successfully")
                } else {
                    showToast("Error ${response.code()}")
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                showToast(t.message.toString())
            }
        })
    }

    private fun listenAvailabilityOfReceiver() {
        database.collection(KEY_COLLECTIONS_USERS).document(receiverUser.id!!)
            .addSnapshotListener(this@ChatActivity) { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (value != null) {
                    if (value.getLong(KEY_AVAILABILITY) != null) {
                        val availability = Objects.requireNonNull(
                            value.getLong(KEY_AVAILABILITY)
                        )?.toInt()
                        isReceiverAvailable = availability == 1
                    }
                    receiverUser.token = value.getString(KEY_FCM_TOKEN)
                    if (receiverUser.image == null) {
                        receiverUser.image = value.getString(KEY_IMAGE)
                        chatAdapter.setReceiverImage(getBitmapFromEncodedString(receiverUser.image!!))
                        chatAdapter.notifyItemRangeInserted(0, chatMessages.size)
                    }
                }
                binding.textAvailability.isVisible = isReceiverAvailable
            }
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
            chatMessages.sortWith { obj1, obj2 -> obj1.dateObject?.compareTo(obj2.dateObject) ?: 0 }
            if (count == 0) {
                chatAdapter.notifyDataSetChanged()
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size, chatMessages.size)
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)
            }
            binding.chatRecyclerView.isVisible = true
        }
        binding.progressBarChat.isVisible = false
        if (conversionId == null) {
            checkForConversion()
        }
    }

    private fun updateConversion(message: String) {
        val documentReference =
            database.collection(KEY_COLLECTION_CONVERSATION).document(conversionId!!)
        documentReference.update(
            KEY_LAST_MESSAGE,
            message,
            KEY_TIMESTAMP,
            Date()
        )
    }

    private fun addConversion(conversion: HashMap<String, Any>) {
        database.collection(KEY_COLLECTION_CONVERSATION)
            .add(conversion)
            .addOnSuccessListener {
                conversionId = it.id
            }
    }

    private fun checkForConversion() {
        if (chatMessages.size != 0) {
            checkForConversionRemotely(
                preferenceManager.getString(KEY_USER_ID)!!,
                receiverUser.id!!
            )
            checkForConversionRemotely(
                receiverUser.id!!,
                preferenceManager.getString(KEY_USER_ID)!!
            )
        }
    }

    private fun checkForConversionRemotely(senderId: String, receiverId: String) {
        database.collection(KEY_COLLECTION_CONVERSATION)
            .whereEqualTo(KEY_SENDER_ID, senderId)
            .whereEqualTo(KEY_RECEIVED_ID, receiverId)
            .get()
            .addOnCompleteListener(conversionOnCompleteListener)
    }

    private val conversionOnCompleteListener = OnCompleteListener<QuerySnapshot> { task ->
        if (task.isSuccessful && task.result != null && task.result.documents.size > 0) {
            val documentShot = task.result.documents[0]
            conversionId = documentShot.id
        }
    }

    override fun onResume() {
        super.onResume()
        listenAvailabilityOfReceiver()
    }

}