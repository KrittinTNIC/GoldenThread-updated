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
import com.google.firebase.auth.OAuthProvider
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

        firebaseAuth = FirebaseAuth.getInstance()
        userDatabaseHelper = UserDatabaseHelper(this)
        preferencesManager = PreferencesManager(this)

        // If already logged in, skip login
        if (preferencesManager.getLoggedInUserEmail().isNotEmpty()) {
            redirectUser()
            return
        }

        setupGoogleSignIn()
        setupEmailLogin()
        setupGitHubSignIn()

        binding.tvSignUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    // ---------------- Google Sign-In ----------------
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

    // ---------------- GitHub Sign-In ----------------
    private fun setupGitHubSignIn() {
        binding.cardGitHub.setOnClickListener {
            val provider = OAuthProvider.newBuilder("github.com")
            provider.addCustomParameter("allow_signup", "false") // optional

            val pendingResultTask = firebaseAuth.pendingAuthResult
            if (pendingResultTask != null) {
                pendingResultTask
                    .addOnSuccessListener { authResult ->
                        handleFirebaseUser(authResult.user?.email, authResult.user?.displayName, authResult.user?.photoUrl?.toString())
                    }
                    .addOnFailureListener { e ->
                        Log.e("GitHubLogin", "Pending GitHub login failed", e)
                        Toast.makeText(this, "GitHub sign-in failed.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                firebaseAuth
                    .startActivityForSignInWithProvider(this, provider.build())
                    .addOnSuccessListener { authResult ->
                        handleFirebaseUser(authResult.user?.email, authResult.user?.displayName, authResult.user?.photoUrl?.toString())
                    }
                    .addOnFailureListener { e ->
                        Log.e("GitHubLogin", "GitHub login failed", e)
                        Toast.makeText(this, "GitHub sign-in failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // ---------------- Facebook Sign-In ----------------
    private fun setupFacebookSignIn() {
        binding.cardFacebook.setOnClickListener {
            val provider = OAuthProvider.newBuilder("facebook.com")
            provider.addCustomParameter("display", "popup") // optional

            // Handle any pending sign-in result first
            val pendingResultTask = firebaseAuth.pendingAuthResult
            if (pendingResultTask != null) {
                pendingResultTask
                    .addOnSuccessListener { authResult ->
                        handleFirebaseUser(
                            authResult.user?.email,
                            authResult.user?.displayName,
                            authResult.user?.photoUrl?.toString()
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e("FacebookLogin", "Pending Facebook login failed", e)
                        Toast.makeText(this, "Facebook sign-in failed.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                firebaseAuth
                    .startActivityForSignInWithProvider(this, provider.build())
                    .addOnSuccessListener { authResult ->
                        handleFirebaseUser(
                            authResult.user?.email,
                            authResult.user?.displayName,
                            authResult.user?.photoUrl?.toString()
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e("FacebookLogin", "Facebook login failed", e)
                        Toast.makeText(this, "Facebook sign-in failed.", Toast.LENGTH_SHORT).show()
                    }
            }

            // Generate hash key safely
            try {
                val info = packageManager.getPackageInfo(
                    packageName,
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signatures = info.signingInfo?.apkContentsSigners
                if (signatures != null) {
                    for (signature in signatures) {
                        val md = java.security.MessageDigest.getInstance("SHA")
                        md.update(signature.toByteArray())
                        val hashKey = android.util.Base64.encodeToString(md.digest(), android.util.Base64.DEFAULT)
                        Log.d("FacebookHashKey", hashKey)
                    }
                } else {
                    Log.e("FacebookHashKey", "No signatures found")
                }
            } catch (e: Exception) {
                Log.e("FacebookHashKey", "Error generating hash key", e)
            }
        }
    }

    // ---------------- X (Twitter) Sign-In ----------------
    private fun setupXSignIn() {
        binding.cardX.setOnClickListener {
            val provider = OAuthProvider.newBuilder("twitter.com")
            provider.addCustomParameter("force_login", "true") // optional

            val pendingResultTask = firebaseAuth.pendingAuthResult
            if (pendingResultTask != null) {
                pendingResultTask
                    .addOnSuccessListener { authResult ->
                        handleFirebaseUser(authResult.user?.email, authResult.user?.displayName, authResult.user?.photoUrl?.toString())
                    }
                    .addOnFailureListener { e ->
                        Log.e("XLogin", "Pending X login failed", e)
                        Toast.makeText(this, "X sign-in failed.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                firebaseAuth
                    .startActivityForSignInWithProvider(this, provider.build())
                    .addOnSuccessListener { authResult ->
                        handleFirebaseUser(authResult.user?.email, authResult.user?.displayName, authResult.user?.photoUrl?.toString())
                    }
                    .addOnFailureListener { e ->
                        Log.e("XLogin", "X login failed", e)
                        Toast.makeText(this, "X sign-in failed.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // ---------------- Email Sign-In ----------------
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

    // ---------------- Google Activity Result ----------------
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

    // ---------------- Firebase Auth with Google ----------------
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    handleFirebaseUser(user?.email, user?.displayName, user?.photoUrl?.toString())
                } else {
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // ---------------- Handle Firebase User (Google/GitHub) ----------------
    private fun handleFirebaseUser(email: String?, displayName: String?, photoUrl: String?) {
        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "Could not retrieve email", Toast.LENGTH_SHORT).show()
            return
        }

        val nameParts = displayName?.split(" ") ?: listOf("User")
        val firstName = nameParts.firstOrNull() ?: "User"
        val lastName = nameParts.drop(1).joinToString(" ")

        // Save user to local DB if not exists
        if (!userDatabaseHelper.isEmailTaken(email)) {
            userDatabaseHelper.addUser(firstName, lastName, email, "firebase_oauth")
            userDatabaseHelper.updateProfileImage(email, photoUrl ?: "")
        }

        // Sync to Supabase
        lifecycleScope.launch {
            if (!supabaseManager.isUserInCloud(email)) {
                supabaseManager.saveUserToCloud(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    profileImage = photoUrl ?: ""
                )
            }
        }

        preferencesManager.saveLoggedInUserEmail(email)
        preferencesManager.saveProfilePictureUri(photoUrl ?: "")
        Toast.makeText(this, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
        redirectUser()
    }

    // ---------------- Email Login Logic ----------------
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

    // ---------------- Redirect Logic ----------------
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
