package com.example.chatappwithfirebase.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import coil.transform.CircleCropTransformation
import com.example.chatappwithfirebase.databinding.ActivityMainBinding
import com.example.chatappwithfirebase.utilities.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)
        loadUserDetails()
        getToken()
        setListeners()
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
//        binding.imageProfile.setImageBitmap(bitmap)
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
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
}