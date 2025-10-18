package com.example.finalproject.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri

class UserDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "UserAccounts.db", null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1

        const val TABLE_USERS = "users"
        const val COLUMN_ID = "id"
        const val COLUMN_FIRST_NAME = "first_name"
        const val COLUMN_LAST_NAME = "last_name"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_PROFILE_IMAGE = "profile_image"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FIRST_NAME TEXT,
                $COLUMN_LAST_NAME TEXT,
                $COLUMN_EMAIL TEXT UNIQUE,
                $COLUMN_PASSWORD TEXT,
                $COLUMN_PROFILE_IMAGE TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // Inserts a new user
    fun addUser(firstName: String, lastName: String, email: String, password: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FIRST_NAME, firstName)
            put(COLUMN_LAST_NAME, lastName)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_PROFILE_IMAGE, "")
        }
        val result = db.insert(TABLE_USERS, null, values)
        db.close()
        return result != -1L
    }

    // Check login credentials
    fun checkUser(email: String, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            "$COLUMN_EMAIL=? AND $COLUMN_PASSWORD=?",
            arrayOf(email, password),
            null, null, null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        db.close()
        return exists
    }

    // Checks if email already exists
    fun isEmailTaken(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            "$COLUMN_EMAIL=?",
            arrayOf(email),
            null, null, null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        db.close()
        return exists
    }

    // Retrieves user data + pfp
    fun getUserByEmail(email: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_FIRST_NAME, COLUMN_LAST_NAME, COLUMN_EMAIL, COLUMN_PROFILE_IMAGE),
            "$COLUMN_EMAIL = ?",
            arrayOf(email),
            null, null, null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                User(
                    firstName = it.getString(0),
                    lastName = it.getString(1),
                    email = it.getString(2),
                    profileImageUri = it.getString(3)
                )
            } else null
        }
    }

    // Updates Name
    fun updateUser(email: String, firstName: String, lastName: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FIRST_NAME, firstName)
            put(COLUMN_LAST_NAME, lastName)
        }
        val rowsAffected = db.update(
            TABLE_USERS,
            values,
            "$COLUMN_EMAIL = ?",
            arrayOf(email)
        )
        db.close()
        return rowsAffected > 0
    }

    // Updates pfp
    fun updateProfileImage(email: String, imageUri: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PROFILE_IMAGE, imageUri)
        }
        val rowsAffected = db.update(
            TABLE_USERS,
            values,
            "$COLUMN_EMAIL = ?",
            arrayOf(email)
        )
        db.close()
        return rowsAffected > 0
    }

    // Updates password
    fun updatePassword(email: String, newPassword: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PASSWORD, newPassword)
        }
        val rowsAffected = db.update(
            TABLE_USERS,
            values,
            "$COLUMN_EMAIL = ?",
            arrayOf(email)
        )
        db.close()
        return rowsAffected > 0
    }

    data class User(
        val firstName: String,
        val lastName: String,
        val email: String,
        val profileImageUri: String?
    )
}