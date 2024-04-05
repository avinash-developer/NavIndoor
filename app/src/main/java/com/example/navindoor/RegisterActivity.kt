package com.example.navindoor

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult
import android.provider.Settings

class RegisterActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        CognitoHelper.initialize(this@RegisterActivity)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)



        if (sharedPreferences.getBoolean("registered", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val signUpButton: Button = findViewById(R.id.signUpButton)
        val usernameEditText: EditText = findViewById(R.id.usernameEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val nameEditText: EditText = findViewById(R.id.nameEditText)
        val enableNearbyPermissionsButton: Button = findViewById(R.id.enableNearbyPermissionsButton)
        val continueWithoutRegisterButton: Button = findViewById(R.id.continueWithoutRegisterButton)
        val turnOnLocationButton: Button = findViewById(R.id.turnOnLocationButton)

        continueWithoutRegisterButton.setOnClickListener {
            startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
            finish()
        }

        enableNearbyPermissionsButton.setOnClickListener {
            // Open the app settings screen
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }

        signUpButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val name = nameEditText.text.toString()
            val attributes = mapOf(
                "email" to username,
                "custom:password" to password,
                "name" to name
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
                        Toast.makeText(applicationContext, "Data not saved", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            })
        }
        turnOnLocationButton.setOnClickListener {
            // Open the location settings screen
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }


    }




}









