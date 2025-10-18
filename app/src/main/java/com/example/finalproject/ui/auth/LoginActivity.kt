package com.example.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finalproject.MainActivity
import com.example.finalproject.databinding.ActivityLoginBinding
import com.example.finalproject.util.PreferencesManager
import com.example.finalproject.util.SupabaseManager
import com.example.finalproject.util.UserDatabaseHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var userDatabaseHelper: UserDatabaseHelper

    private val supabaseManager = SupabaseManager()

    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userDatabaseHelper = UserDatabaseHelper(this)
        preferencesManager = PreferencesManager(this)

        // Check if user is already logged in
        if (preferencesManager.getLoggedInUserEmail().isNotEmpty()) {
            redirectUser()
            return
        }

        // --- Google Sign-In setup ---
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Google Sign-In setup failed", e)
        }

        // --- Email/password login ---
        binding.btnContinueLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                if (userDatabaseHelper.checkUser(email, pass)) {
                    preferencesManager.saveLoggedInUserEmail(email)
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    redirectUser()
                } else {
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Login error", e)
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardGoogle.setOnClickListener {
            try {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            } catch (e: Exception) {
                Toast.makeText(this, "Google Sign-In not available in development", Toast.LENGTH_LONG).show()
                Log.d("LoginActivity", "Google Sign-In disabled: ${e.message}")
            }
        }

        binding.cardGitHub.setOnClickListener {
            Toast.makeText(this, "GitHub login coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.tvSignUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun redirectUser() {
        val intent = if (preferencesManager.hasCompletedPreferences()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, PreferencesActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)!!
                handleGoogleSignIn(account)
            } catch (e: ApiException) {
                when (e.statusCode) {
                    10 -> {
                        // This is the development configuration error - show friendly message
                        Toast.makeText(this,
                            "Please use email login for now.",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d("LoginActivity", "Unexpected error")
                    }
                    12501 -> {
                        // User cancelled
                        Toast.makeText(this, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(this, "Sign-in failed (Error ${e.statusCode})", Toast.LENGTH_SHORT).show()
                        Log.e("LoginActivity", "Google sign-in failed: ${e.statusCode}")
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Unexpected error during sign-in", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "Unexpected error", e)
            }
        }
    }

    private fun handleLogin(email: String, password: String) {
        lifecycleScope.launch {
            try {
                // First check local SQLite
                if (userDatabaseHelper.checkUser(email, password)) {
                    // User exists locally - also ensure they're in Supabase
                    val user = userDatabaseHelper.getUserByEmail(email)
                    user?.let {
                        if (!supabaseManager.isUserInCloud(email)) {
                            // Backup to Supabase if not exists
                            supabaseManager.saveUserToCloud(
                                firstName = it.firstName,
                                lastName = it.lastName,
                                email = it.email,
                                profileImage = it.profileImageUri ?: ""
                            )
                        }
                    }

                    preferencesManager.saveLoggedInUserEmail(email)
                    Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                    redirectUser()
                } else {
                    // Check if user exists in Supabase (app was deleted)
                    val cloudUser = supabaseManager.loadUserFromCloud(email)
                    if (cloudUser != null) {
                        // User reinstalled app - restore to local SQLite
                        userDatabaseHelper.addUser(
                            cloudUser.firstName,
                            cloudUser.lastName,
                            cloudUser.email,
                            password
                        )

                        preferencesManager.saveLoggedInUserEmail(email)
                        Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                        redirectUser()
                    } else {
                        Toast.makeText(this@LoginActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Login error", e)
                Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleSignIn(account: GoogleSignInAccount) {
        val email = account.email ?: ""
        val displayName = account.displayName ?: "User"

        if (email.isEmpty()) {
            Toast.makeText(this, "Could not get email from Google", Toast.LENGTH_SHORT).show()
            return
        }

        preferencesManager.saveLoggedInUserEmail(email)

        // Save profile picture if available
        account.photoUrl?.toString()?.let { photoUrl ->
            preferencesManager.saveProfilePictureUri(photoUrl)
        }

        // Create user in database if they don't exist
        if (!userDatabaseHelper.isEmailTaken(email)) {
            val firstName = displayName.split(" ").firstOrNull() ?: "Google"
            val lastName = displayName.split(" ").lastOrNull() ?: "User"

            userDatabaseHelper.addUser(firstName, lastName, email, "google_sign_in")

            account.photoUrl?.toString()?.let { photoUrl ->
                userDatabaseHelper.updateProfileImage(email, photoUrl)
            }
        }

        Toast.makeText(this, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
        redirectUser()
    }
}