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





        sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)


        if (isRegistered()) {

            startActivity(Intent(this, MainActivity::class.java))
        } else {

            startActivity(Intent(this, RegisterActivity::class.java))
        }
        finish()
    }

    private fun isRegistered(): Boolean {

        return sharedPreferences.getBoolean("isRegistered", false)
    }
}
