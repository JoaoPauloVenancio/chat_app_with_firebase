package com.example.chatappwithfirebase.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.chatappwithfirebase.databinding.ActivitySignInBinding
import com.example.chatappwithfirebase.utilities.*
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)
        if (preferenceManager.getBoolean(KEY_IS_SIGNED_IN)) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        setListeners()
    }

    private fun setListeners() {
        binding.textCreateNewAccount.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.buttonSignIn.setOnClickListener {
            if (isValidSignInDetails()) {
                signIn()
            }
        }
    }

    private fun signIn() {
        loading(true)
        val database = FirebaseFirestore.getInstance()
        database.collection(KEY_COLLECTIONS_USERS)
            .whereEqualTo(KEY_EMAIL, binding.inputEmail.text.toString())
            .whereEqualTo(KEY_PASSWORD, binding.inputPassword.text.toString())
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful && it.result != null && it.result.documents.size > 0) {
                    val documentSnapshot = it.result.documents[0]
                    preferenceManager.putBoolean(KEY_IS_SIGNED_IN, true)
                    preferenceManager.putString(KEY_USER_ID, documentSnapshot.id)
                    documentSnapshot.getString(KEY_NAME)
                        ?.let { it1 -> preferenceManager.putString(KEY_NAME, it1) }
                    documentSnapshot.getString(KEY_IMAGE)
                        ?.let { it1 -> preferenceManager.putString(KEY_IMAGE, it1) }
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                } else {
                    loading(false)
                    showToast("Unable to sign in")
                }
            }
    }

    private fun loading(isLoading: Boolean) {
        binding.progressBarSignIn.isVisible = isLoading
    }


    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidSignInDetails(): Boolean {
        return if (binding.inputEmail.text.toString().trim().isEmpty()) {
            showToast("Enter email")
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.text.toString()).matches()) {
            showToast("Enter valid email")
            false
        } else if (binding.inputPassword.text.toString().trim().isEmpty()) {
            showToast("Enter password")
            false
        } else {
            true
        }
    }
}