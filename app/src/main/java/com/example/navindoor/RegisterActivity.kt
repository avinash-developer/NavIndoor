package com.example.navindoor

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult

class RegisterActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        CognitoHelper.initialize(this@RegisterActivity)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        if (sharedPreferences.getBoolean("registered", false)) {
            // User is already registered, start MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val signUpButton: Button = findViewById(R.id.signUpButton)
        val usernameEditText: EditText = findViewById(R.id.usernameEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)

        signUpButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val attributes = mapOf(
                "email" to username,
                "custom:password" to password
            )

            CognitoHelper.register(username, password, attributes, object : SignUpHandler {
                override fun onSuccess(user: CognitoUser?, signUpResult: SignUpResult?) {
                    sharedPreferences.edit().putBoolean("registered", true).apply()

                    runOnUiThread {
                        Toast.makeText(applicationContext, "Data saved", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                        finish()
                    }
                }

                override fun onFailure(exception: Exception?) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Data not saved", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
}
