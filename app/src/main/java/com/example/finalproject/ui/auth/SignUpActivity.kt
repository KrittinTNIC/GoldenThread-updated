package com.example.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.finalproject.databinding.ActivitySignupBinding
import com.example.finalproject.util.PreferencesManager
import com.example.finalproject.util.UserDatabaseHelper
import com.example.finalproject.util.SupabaseUserManager
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var dbHelper: UserDatabaseHelper
    private lateinit var supabaseManager: SupabaseUserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = UserDatabaseHelper(this)
        preferencesManager = PreferencesManager(this)
        supabaseManager = SupabaseUserManager() // 👈 Cloud sync helper

        // --- Verify database creation
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='users'",
                null
            )
            val exists = cursor.count > 0
            Log.d("SignUpActivity", "Users table exists: $exists")
            cursor.close()
            db.close()
        } catch (e: Exception) {
            Log.e("SignUpActivity", "DB Error", e)
            Toast.makeText(this, "Database error: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        // --- Pick image
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                preferencesManager.saveProfilePictureUri(uri.toString())
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(binding.ivProfilePicture)
            }
        }

        binding.btnSelectPhoto.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        // --- Sign up button
        binding.btnContinueSignUp.setOnClickListener {
            val first = binding.etFirstName.text.toString().trim()
            val last = binding.etLastName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            if (first.isEmpty() || last.isEmpty() || email.isEmpty() ||
                pass.isEmpty() || confirmPass.isEmpty()
            ) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    if (dbHelper.isEmailTaken(email)) {
                        Toast.makeText(
                            this@SignUpActivity,
                            "Email already registered",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    // --- Add to local SQLite
                    val success = dbHelper.addUser(first, last, email, pass)
                    if (success) {
                        val user = dbHelper.getUserByEmail(email)
                        user?.let {
                            // --- Also sync to Supabase
                            supabaseManager.saveUserToCloud(it)
                        }

                        Toast.makeText(
                            this@SignUpActivity,
                            "Account created!",
                            Toast.LENGTH_SHORT
                        ).show()

                        preferencesManager.saveLoggedInUserEmail(email)

                        val intent = Intent(this@SignUpActivity, PreferencesActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@SignUpActivity,
                            "Sign up failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("SignUpActivity", "Signup error", e)
                    Toast.makeText(
                        this@SignUpActivity,
                        "Sign up failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // --- Go to login
        binding.tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
