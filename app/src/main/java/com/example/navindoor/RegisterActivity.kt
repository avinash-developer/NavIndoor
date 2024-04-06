package com.example.navindoor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Check if it's the first time opening the register activity
        if (!isPermissionsGranted()) {
            // Show the permissions dialog
            showPermissionsDialog()
        }

        val signUpButton: Button = findViewById(R.id.signUpButton)
        val usernameEditText: EditText = findViewById(R.id.usernameEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val nameEditText: EditText = findViewById(R.id.nameEditText)
     //   val enableNearbyPermissionsButton: Button = findViewById(R.id.enableNearbyPermissionsButton)
        val continueWithoutRegisterButton: Button = findViewById(R.id.continueWithoutRegisterButton)
    //    val turnOnLocationButton: Button = findViewById(R.id.turnOnLocationButton)

        continueWithoutRegisterButton.setOnClickListener {
            if (isPermissionsGranted()) {
                navigateToMainActivity()
            } else {
                // Show permissions dialog if permissions are not granted
                showPermissionsDialog()
            }
        }

//        enableNearbyPermissionsButton.setOnClickListener {
//            // Open the app settings screen
//            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//            val uri = Uri.fromParts("package", packageName, null)
//            intent.data = uri
//            startActivity(intent)
//        }

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
                        if (isPermissionsGranted()) {
                            navigateToMainActivity()
                        } else {
                            // Show permissions dialog if permissions are not granted
                            showPermissionsDialog()
                        }
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
//        turnOnLocationButton.setOnClickListener {
//            // Open the location settings screen
//            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//            startActivity(intent)
//        }


    }
    private fun showPermissionsDialog() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Permissions Required")
            .setMessage("To use this app, you need to enable nearby permissions and turn on location.")
            .setPositiveButton("Enable Nearby Permissions") { dialog, _ ->
                openAppSettings()
                dialog.dismiss()
            }
            .setNegativeButton("Turn On Location") { dialog, _ ->
                openLocationSettings()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun isNearbyPermissionsGranted(): Boolean {
        val bluetoothPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED

        val bluetoothAdminPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_ADMIN
        ) == PackageManager.PERMISSION_GRANTED

        val fineLocationPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return bluetoothPermission && bluetoothAdminPermission && fineLocationPermission && coarseLocationPermission
    }


    private fun isPermissionsGranted(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        val nearbyPermissionsGranted = isNearbyPermissionsGranted()

        return locationEnabled && nearbyPermissionsGranted

    }
    private fun navigateToMainActivity() {
        startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
        finish()
    }





}









