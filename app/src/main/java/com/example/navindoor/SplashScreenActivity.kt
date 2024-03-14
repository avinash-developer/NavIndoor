package com.example.navindoor

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        // Check if the user is registered
        if (isRegistered()) {
            // User is already registered, navigate to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // User is not registered, navigate to RegisterActivity
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        finish()
    }

    private fun isRegistered(): Boolean {
        // Retrieve the registration status from shared preferences
        return sharedPreferences.getBoolean("isRegistered", false)
    }
}
