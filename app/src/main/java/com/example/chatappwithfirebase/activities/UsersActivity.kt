package com.example.chatappwithfirebase.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.chatappwithfirebase.adapters.UserAdapter
import com.example.chatappwithfirebase.databinding.ActivityUsersBinding
import com.example.chatappwithfirebase.models.User
import com.example.chatappwithfirebase.utilities.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot

class UsersActivity : AppCompatActivity(), UserAdapter.UserListener {

    private lateinit var binding: ActivityUsersBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)
        getUsers()
        setListeners()
    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun showErrorMessage() {
        binding.textErrorMessage.text = String.format("%s", "No user available")
        binding.textErrorMessage.isVisible = true
    }

    private fun getUsers() {
        loading(true)
        val database = FirebaseFirestore.getInstance()
        database.collection(KEY_COLLECTIONS_USERS)
            .get()
            .addOnCompleteListener {
                loading(false)
                val currentUserId = preferenceManager.getString(KEY_USER_ID)
                if (it.isSuccessful && it.result != null) {
                    val users = arrayListOf<User>()
                    for (queryDocumentSnapShot: QueryDocumentSnapshot in it.result) {
                        if (currentUserId.equals(queryDocumentSnapShot.id)) {
                            continue
                        }
                        val user = User(
                            name = queryDocumentSnapShot.getString(KEY_NAME),
                            email = queryDocumentSnapShot.getString(KEY_EMAIL),
                            image = queryDocumentSnapShot.getString(KEY_IMAGE),
                            token = queryDocumentSnapShot.getString(KEY_FCM_TOKEN),
                            id = queryDocumentSnapShot.id
                        )
                        users.add(user)
                    }
                    if (users.size > 0) {
                        val adapter = UserAdapter(this)
                        adapter.updateList(users)
                        binding.userRecyclerView.adapter = adapter
                        binding.userRecyclerView.isVisible = true
                    } else {
                        showErrorMessage()
                    }
                } else {
                    showErrorMessage()
                }
            }
    }

    private fun loading(isLoading: Boolean) {
        binding.progressBarUsers.isVisible = isLoading
    }

    override fun onUserClicked(user: User) {
        val intent = Intent(applicationContext, ChatActivity::class.java)
        intent.putExtra(KEY_USER, user)
        startActivity(intent)
        finish()
    }
}