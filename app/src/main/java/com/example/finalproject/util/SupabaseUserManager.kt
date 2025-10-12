package com.example.finalproject.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class SupabaseUserManager {
    companion object {
        private const val SUPABASE_URL = "https://wdcpvzdcukjukmmudwme.supabase.co"
        private const val SUPABASE_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndkY3B2emRjdWtqdWttbXVkd21lIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjAyNTk5NjgsImV4cCI6MjA3NTgzNTk2OH0.Ns64mh-dsLP9UWf50J0kZNbRLMAW9-Ltposw1UNIzWc"
        private const val TABLE_NAME = "users"
    }

    /**
     * Save user to Supabase Cloud
     */
    suspend fun saveUserToCloud(user: UserDatabaseHelper.User): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$SUPABASE_URL/rest/v1/$TABLE_NAME")
                val connection = url.openConnection() as HttpsURLConnection

                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Prefer", "return=minimal")
                    doOutput = true
                }

                val jsonBody = """
                    {
                        "first_name": "${user.firstName}",
                        "last_name": "${user.lastName}",
                        "email": "${user.email}",
                        "profile_image": "${user.profileImageUri ?: ""}"
                    }
                """.trimIndent()

                connection.outputStream.use { output ->
                    output.write(jsonBody.toByteArray())
                }

                val responseCode = connection.responseCode
                connection.disconnect()

                responseCode in 200..299
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Check if user exists in Supabase
     */
    suspend fun isUserInCloud(email: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$SUPABASE_URL/rest/v1/$TABLE_NAME?email=eq.$email&select=email")
                val connection = url.openConnection() as HttpsURLConnection

                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                }

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                responseCode == 200 && response.isNotEmpty() && response != "[]"
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Load user from Supabase by email
     */
    suspend fun loadUserFromCloud(email: String): UserDatabaseHelper.User? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$SUPABASE_URL/rest/v1/$TABLE_NAME?email=eq.$email")
                val connection = url.openConnection() as HttpsURLConnection

                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()

                    if (response.isNotEmpty() && response != "[]") {
                        val userData = response.removeSurrounding("[", "]")
                        if (userData.contains("\"first_name\"")) {
                            val firstName = extractJsonField(userData, "first_name")
                            val lastName = extractJsonField(userData, "last_name")
                            val userEmail = extractJsonField(userData, "email")
                            val profileImage = extractJsonField(userData, "profile_image")

                            return@withContext UserDatabaseHelper.User(
                                firstName = firstName,
                                lastName = lastName,
                                email = userEmail,
                                profileImageUri = profileImage
                            )
                        }
                    }
                }
                connection.disconnect()
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun extractJsonField(json: String, field: String): String {
        val pattern = "\"$field\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1) ?: ""
    }
}
