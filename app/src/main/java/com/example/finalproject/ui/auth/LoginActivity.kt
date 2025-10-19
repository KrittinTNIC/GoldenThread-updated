package com.example.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finalproject.MainActivity
import com.example.finalproject.R
import com.example.finalproject.databinding.ActivityLoginBinding
import com.example.finalproject.util.PreferencesManager
import com.example.finalproject.util.SupabaseManager
import com.example.finalproject.util.UserDatabaseHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var userDatabaseHelper: UserDatabaseHelper
    private lateinit var firebaseAuth: FirebaseAuth

    private val supabaseManager = SupabaseManager()
    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize helpers
        userDatabaseHelper = UserDatabaseHelper(this)
        preferencesManager = PreferencesManager(this)
        firebaseAuth = FirebaseAuth.getInstance()

        // Check if already logged in
        if (preferencesManager.getLoggedInUserEmail().isNotEmpty()) {
            redirectUser()
            return
        }

        // Setup login methods
        setupGoogleSignIn()
        setupEmailLogin()

        binding.cardGitHub.setOnClickListener {
            Toast.makeText(this, "GitHub login coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.tvSignUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.cardGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    private fun setupEmailLogin() {
        binding.btnContinueLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            handleLogin(email, password)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                when (e.statusCode) {
                    12501 -> Toast.makeText(this, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
                    else -> {
                        Toast.makeText(this, "Google sign-in failed (${e.statusCode})", Toast.LENGTH_SHORT).show()
                        Log.e("LoginActivity", "Google sign-in failed", e)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Unexpected error during sign-in", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "Unexpected error", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    user?.let {
                        val email = it.email ?: ""
                        val displayName = it.displayName ?: "User"
                        val photoUrl = it.photoUrl?.toString() ?: ""

                        if (email.isEmpty()) {
                            Toast.makeText(this, "Could not get email from Google", Toast.LENGTH_SHORT).show()
                            return@addOnCompleteListener
                        }

                        // Save to local DB if new
                        if (!userDatabaseHelper.isEmailTaken(email)) {
                            val firstName = displayName.split(" ").firstOrNull() ?: "Google"
                            val lastName = displayName.split(" ").lastOrNull() ?: "User"
                            userDatabaseHelper.addUser(firstName, lastName, email, "firebase_google")
                            userDatabaseHelper.updateProfileImage(email, photoUrl)
                        }

                        // Save preferences
                        preferencesManager.saveLoggedInUserEmail(email)
                        preferencesManager.saveProfilePictureUri(photoUrl)

                        Toast.makeText(this, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                        redirectUser()
                    }
                } else {
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleLogin(email: String, password: String) {
        lifecycleScope.launch {
            try {
                if (userDatabaseHelper.checkUser(email, password)) {
                    val user = userDatabaseHelper.getUserByEmail(email)
                    user?.let {
                        if (!supabaseManager.isUserInCloud(email)) {
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
                    val cloudUser = supabaseManager.loadUserFromCloud(email)
                    if (cloudUser != null) {
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

    private fun redirectUser() {
        val intent = if (preferencesManager.hasCompletedPreferences()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, PreferencesActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
