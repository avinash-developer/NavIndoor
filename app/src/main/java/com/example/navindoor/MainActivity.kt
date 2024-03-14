package com.example.navindoor

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var mapImageView: ImageView
    private lateinit var markerImageView: ImageView
    private lateinit var muteButton: ImageButton


    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private var minScale = 1f
    private var maxScale = 5f
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_LOCATION_PERMISSION = 2
    private val REQUEST_BLUETOOTH_SCAN_PERMISSION = 3
    private val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    private val BLUETOOTH_SCAN_PERMISSION = Manifest.permission.BLUETOOTH_SCAN

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var textToSpeech: TextToSpeech
    private var isMuted = false

    data class LocationRange(
        val lonRange: ClosedFloatingPointRange<Double>,
        val latRange: ClosedFloatingPointRange<Double>,
        val name: String
    )

    private fun mapCoordinatesToName(longitude: Double, latitude: Double): String {
        // Define ranges for longitude and latitude values along with their associated names
        val ranges = listOf(
            LocationRange(1.920..2.250, 2.130..2.300, "Beacon-1 at Block 36"),
            LocationRange(2.200..2.400, 2.150..2.250, "Beacon 2 at Block 37"),
            LocationRange(2.400..2.650, 2.200..2.300, "Beacon 3 at Block 38")
            // Add more ranges as needed
        )

        // Check if the given coordinates lie within any of the defined ranges
        for (range in ranges) {
            if (longitude in range.lonRange && latitude in range.latRange) {
                return range.name
            }
        }

        // Default to a generic name if the coordinates do not lie within any defined range
        return "Unknown Location"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        markerImageView = findViewById(R.id.markerImageView)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            checkLocationAndBluetoothPermissions()
        }
        muteButton = findViewById(R.id.muteButton)

        muteButton.setOnClickListener {
            if (isMuted) {
                // Unmute
                textToSpeech.speak("Unmuted", TextToSpeech.QUEUE_FLUSH, null, null)
                muteButton.setImageResource(R.drawable.volume)
                isMuted = false
            } else {
                // Mute
                textToSpeech.speak("Muted", TextToSpeech.QUEUE_FLUSH, null, null)
                muteButton.setImageResource(R.drawable.volume_unmute)
                isMuted = true
            }
        }


        mapImageView = findViewById(R.id.mapImageView)
        // marker1ImageView = findViewById(R.id.marker1ImageView)
        // marker2ImageView = findViewById(R.id.marker2ImageView)

        // Initialize ScaleGestureDetector
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

        // Initialize GestureDetector
        gestureDetector = GestureDetector(this, GestureListener())

        // Enable ImageView to receive touch events
        mapImageView.setOnTouchListener { _, event ->
            scaleGestureDetector!!.onTouchEvent(event)
            gestureDetector!!.onTouchEvent(event)

            if (event.action == MotionEvent.ACTION_DOWN) {
                // Get the pixel coordinates of the touch event
                val x = event.x.toDouble()
                val y = event.y.toDouble()

                val pixelmessage = "Longitude: $x\nLatitude: $y"
                Toast.makeText(this@MainActivity, pixelmessage, Toast.LENGTH_SHORT).show()

                // Convert pixel coordinates to real-world coordinates
                val coordinates = MapCoordinateConverter().pixelToCoordinate(PixelPoint(x, y))

                // Map coordinates to names
                val name = mapCoordinatesToName(coordinates[0], coordinates[1])

                // Display the name
                Toast.makeText(this@MainActivity, name, Toast.LENGTH_SHORT).show()
                val longitudeFormatted = String.format("%.3f", coordinates[0])
                val latitudeFormatted = String.format("%.3f", coordinates[1])
                val message =
                    "Longitude: $longitudeFormatted\nLatitude: $latitudeFormatted\nLocation: $name"
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()



                return@setOnTouchListener true
            }

            return@setOnTouchListener true
        }
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set language for TTS engine
                textToSpeech.language = Locale.US
            }
        }
    }


    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newScale = mapImageView.scaleX * scaleFactor

            // Restrict scaling within min and max scales
            if (newScale >= minScale && newScale <= maxScale) {
                mapImageView.scaleX = newScale
                mapImageView.scaleY = newScale
            }

            return true
        }
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            mapImageView.translationX -= distanceX
            mapImageView.translationY -= distanceY
            return true
        }
    }


    private fun checkLocationAndBluetoothPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_LOCATION_PERMISSION// Change to REQUEST_BLUETOOTH_SCAN_PERMISSION for Bluetooth scan permission

            )

        } else {
            startScan()
        }
    }


    private fun startScan() {
        if (ActivityCompat.checkSelfPermission(
                this,
                BLUETOOTH_SCAN_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
    }

    private fun stopScan() {
        if (ActivityCompat.checkSelfPermission(
                this,
                BLUETOOTH_SCAN_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            if (device.name?.equals("ESP32") == true) {
                connectToDevice(device)


            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {


                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {


                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(ESP32_1_SERVICE_UUID)
            var characteristic = service?.getCharacteristic(ESP32_1_CHARACTERISTIC_UUID)

            if (characteristic == null) {
                // Check for second ESP32 device
                val secondService = gatt.getService(ESP32_2_SERVICE_UUID)
                characteristic = secondService?.getCharacteristic(ESP32_2_CHARACTERISTIC_UUID)
            }

            if (characteristic == null) {
                // Check for third ESP32 device
                val thirdService = gatt.getService(ESP32_3_SERVICE_UUID)
                characteristic = thirdService?.getCharacteristic(ESP32_3_CHARACTERISTIC_UUID)
            }

            if (characteristic != null) {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Handle permission request
                    return
                }
                gatt.readCharacteristic(characteristic)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val data = characteristic.value // Read data from characteristic
            val dataString = data?.toString(Charsets.UTF_8) // Convert data to string
            runOnUiThread {
                Toast.makeText(applicationContext, "Received data: $dataString", Toast.LENGTH_SHORT)
                    .show()
                if (dataString == "Block_36") {
                    val coordinates = MapCoordinateConverter().pixelToCoordinate(
                        PixelPoint(
                            431.0,
                            1026.0
                        )
                    ) // Replace with the actual pixel coordinates

                    // Place the marker based on the converted pixel coordinates
                    setMarkerToCoordinate(
                        coordinates[0],
                        coordinates[1],
                        mapImageView.scaleX
                    ) // Pass the current scale factor
                    textToSpeech.speak(
                        "You are near Block 36 and Beacon-1.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                }
                else if (dataString == "Block_37") {
                    val coordinates = MapCoordinateConverter().pixelToCoordinate(
                        PixelPoint(
                            484.0,
                            1034.0
                        )
                    ) // Replace with the actual pixel coordinates

                    // Place the marker based on the converted pixel coordinates
                    setMarkerToCoordinate(
                        coordinates[0],
                        coordinates[1],
                        mapImageView.scaleX
                    ) // Pass the current scale factor
                    textToSpeech.speak(
                        "You are near Block 37 and Beacon-2.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                }
                else if (dataString=="Block_38"){
                    val coordinates = MapCoordinateConverter().pixelToCoordinate(
                        PixelPoint(
                            541.0,
                            1072.0
                        )
                    ) // Replace with the actual pixel coordinates

                    // Place the marker based on the converted pixel coordinates
                    setMarkerToCoordinate(
                        coordinates[0],
                        coordinates[1],
                        mapImageView.scaleX
                    ) // Pass the current scale factor
                    textToSpeech.speak(
                        "You are near Block 38 and Beacon-3.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                }



            }


            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            gatt.disconnect()
            gatt.close()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    startScan()
                } else {
                    Toast.makeText(
                        this,
                        "Location permission required for Bluetooth scanning",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }

            REQUEST_BLUETOOTH_SCAN_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    startScan()
                } else {
                    Toast.makeText(
                        this,
                        "Bluetooth scan permission required for Bluetooth scanning",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopScan()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    companion object {
        private val ESP32_1_SERVICE_UUID =
            UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        private val ESP32_1_CHARACTERISTIC_UUID =
            UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

        private val ESP32_2_SERVICE_UUID =
            UUID.fromString("932520fb-68f3-4d89-ba7b-f8d3c6b22772")
        private val ESP32_2_CHARACTERISTIC_UUID =
            UUID.fromString("3693d5c0-f076-4d82-ac97-19f6139e64ce")

        private val ESP32_3_SERVICE_UUID =
            UUID.fromString("0f22311f-2a20-4fe5-b0f2-beb6b8ccab49")
        private val ESP32_3_CHARACTERISTIC_UUID =
            UUID.fromString("4ab015d4-bf71-4aac-9d93-23e2b151c746")
    }

    private fun setMarkerToCoordinate(x: Double, y: Double, scale: Float) {
        // Define the dimensions of the map image view
        val MAP_IMAGE_WIDTH = 2179
        val MAP_IMAGE_HEIGHT = 4669

        // Define the real-world coordinates corresponding to the top-left and bottom-right corners of the map image
        val REAL_WORLD_LONGITUDE_A = 0.0
        val REAL_WORLD_LATITUDE_A = 0.0
        val REAL_WORLD_LONGITUDE_B = 10.0
        val REAL_WORLD_LATITUDE_B = 10.0

        // Calculate marker position on the map image view
        val markerX =
            ((x - REAL_WORLD_LONGITUDE_A) / (REAL_WORLD_LONGITUDE_B - REAL_WORLD_LONGITUDE_A)) * MAP_IMAGE_WIDTH
        val markerY =
            ((y - REAL_WORLD_LATITUDE_A) / (REAL_WORLD_LATITUDE_B - REAL_WORLD_LATITUDE_A)) * MAP_IMAGE_HEIGHT

        // Adjust marker position based on the zoom level (scale factor)
        val adjustedMarkerX = markerX * scale
        val adjustedMarkerY = markerY * scale

        // Calculate the translation of the map image view
        val translationX = mapImageView.translationX
        val translationY = mapImageView.translationY

        // Calculate the adjusted marker position considering translation
        val adjustedX = adjustedMarkerX + translationX
        val adjustedY = adjustedMarkerY + translationY

        // Set layout parameters for marker image view
        val layoutParams = markerImageView.layoutParams as RelativeLayout.LayoutParams
        layoutParams.leftMargin = (adjustedX.toInt() - markerImageView.width / 2).coerceAtLeast(0)
        layoutParams.topMargin = (adjustedY.toInt() - markerImageView.height / 2).coerceAtLeast(0)
        markerImageView.layoutParams = layoutParams

        // Make the marker image view visible
        markerImageView.visibility = View.VISIBLE
    }


}
