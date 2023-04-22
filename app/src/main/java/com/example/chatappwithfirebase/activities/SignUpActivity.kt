package com.example.chatappwithfirebase.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import coil.load
import coil.transform.CircleCropTransformation
import com.example.chatappwithfirebase.databinding.ActivitySignUpBinding
import com.example.chatappwithfirebase.utilities.*
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private var encodedImage: String = EMPTY_STRING
    private lateinit var preferenceManager: PreferenceManager
    private val pickImage: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            if (result.data != null) {
                val imageUri = result.data?.data
                try {
                    val inputStream = imageUri?.let { contentResolver.openInputStream(it) }
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding.image.load(bitmap) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                    }
                    binding.txtImage.isVisible = false
                    encodedImage = encodedImage(bitmap);
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)
        setListeners()
    }

    private fun setListeners() {
        binding.textSignIn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.buttonSignUp.setOnClickListener {
            if (isValidSignUpDetails()) {
                signUp()
            }
        }

        binding.image.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pickImage.launch(intent)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun signUp() {
        loading(true)
        val database = FirebaseFirestore.getInstance()
        val user = HashMap<String, Any>()
        user[KEY_NAME] = binding.inputName.text.toString()
        user[KEY_EMAIL] = binding.txtEmail.text.toString()
        user[KEY_PASSWORD] = binding.txtPassword.text.toString()
        user[KEY_IMAGE] = encodedImage

        database.collection(KEY_COLLECTIONS_USERS)
            .add(user)
            .addOnSuccessListener {
                loading(false)
                preferenceManager.putBoolean(KEY_IS_SIGNED_IN, true)
                preferenceManager.putString(KEY_USER_ID, it.id)
                preferenceManager.putString(KEY_NAME, binding.inputName.text.toString())
                preferenceManager.putString(KEY_IMAGE, encodedImage)
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            .addOnFailureListener {
                loading(false)
                showToast(it.message.toString())
            }
    }

    private fun encodedImage(bitmap: Bitmap): String {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun isValidSignUpDetails(): Boolean {
        if (encodedImage.isEmpty()) {
            showToast("Select profile image")
            return false
        } else if (binding.inputName.text.toString().trim().isEmpty()) {
            showToast("Enter name")
            return false
        } else if (binding.txtEmail.text.toString().trim().isEmpty()) {
            showToast("Enter email")
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.txtEmail.text.toString()).matches()) {
            showToast("Enter valid email")
            return false
        } else if (binding.txtPassword.text.toString().trim().isEmpty()) {
            showToast("Enter password")
            return false
        } else if (binding.txtConfirmPassword.text.toString().trim().isEmpty()) {
            showToast("Confirm your password")
            return false
        } else if (binding.txtPassword.text.toString() != binding.txtConfirmPassword.text.toString()
        ) {
            showToast("Password and 'confirm Password' must be same")
            return false
        } else {
            return true
        }
    }

    private fun loading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
    }
}